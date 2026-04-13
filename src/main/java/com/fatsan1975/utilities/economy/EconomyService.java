package com.fatsan1975.utilities.economy;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.economy.model.Account;
import com.fatsan1975.utilities.economy.model.TopEntry;
import com.fatsan1975.utilities.economy.provider.LegacyVaultBridge;
import com.fatsan1975.utilities.economy.storage.EconomyStorage;
import com.fatsan1975.utilities.economy.storage.SqliteEconomyStorage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Ekonomi cephesi. Üç modda çalışır:
 *
 * <ul>
 *   <li><b>PROVIDER</b>: Plugin kendi SQLite ekonomisini çalıştırır ve Vault/VaultUnlocked’a provider
 *       olarak register eder.</li>
 *   <li><b>CONSUMER</b>: Başka bir Vault provider’ını tüketir (örneğin CMI, EssentialsX).</li>
 *   <li><b>AUTO</b> (varsayılan): Açılışta harici provider yoksa PROVIDER, varsa CONSUMER.</li>
 * </ul>
 */
public final class EconomyService {
  public enum Mode { AUTO, PROVIDER, CONSUMER }

  private final JavaPlugin plugin;
  private final PluginConfiguration configuration;

  private Mode effectiveMode = Mode.AUTO;
  private FatsanEconomy ownEconomy;
  private EconomyStorage ownStorage;
  private Economy externalVault;

  public EconomyService(JavaPlugin plugin, PluginConfiguration configuration) {
    this.plugin = plugin;
    this.configuration = configuration;
  }

  public boolean setup() {
    Mode configured = parseMode(configuration.economy().getString("mode", "auto"));
    boolean externalExists = findExternalVault() != null;

    switch (configured) {
      case CONSUMER -> {
        if (!externalExists) {
          plugin.getLogger().warning("economy.mode=consumer ama Vault provider bulunamadı. Geçici olarak hazır değil.");
          this.effectiveMode = Mode.CONSUMER;
          return false;
        }
        this.externalVault = findExternalVault();
        this.effectiveMode = Mode.CONSUMER;
        plugin.getLogger().info("Ekonomi CONSUMER modunda: " + externalVault.getName());
        return true;
      }
      case PROVIDER -> {
        initOwnEconomy();
        registerAsProvider();
        this.effectiveMode = Mode.PROVIDER;
        return true;
      }
      case AUTO -> {
        if (externalExists) {
          this.externalVault = findExternalVault();
          this.effectiveMode = Mode.CONSUMER;
          plugin.getLogger().info("Ekonomi AUTO → CONSUMER: mevcut provider bulundu (" + externalVault.getName() + ")");
          return true;
        }
        initOwnEconomy();
        registerAsProvider();
        this.effectiveMode = Mode.PROVIDER;
        plugin.getLogger().info("Ekonomi AUTO → PROVIDER: kendi ekonomimiz register edildi.");
        return true;
      }
    }
    return false;
  }

  private Mode parseMode(String raw) {
    if (raw == null) return Mode.AUTO;
    return switch (raw.toLowerCase(Locale.ROOT)) {
      case "provider" -> Mode.PROVIDER;
      case "consumer" -> Mode.CONSUMER;
      default -> Mode.AUTO;
    };
  }

  private Economy findExternalVault() {
    try {
      RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
      if (registration == null) return null;
      // Kendimizinkini tüketmeyelim
      Economy provider = registration.getProvider();
      if (provider instanceof LegacyVaultBridge) return null;
      return provider;
    } catch (NoClassDefFoundError | Exception exception) {
      return null;
    }
  }

  private void initOwnEconomy() {
    Path dbFile = plugin.getDataFolder().toPath().resolve("data").resolve("economy.sqlite");
    SqliteEconomyStorage storage = new SqliteEconomyStorage(dbFile, plugin.getLogger());
    try {
      storage.initialize();
    } catch (Exception exception) {
      plugin.getLogger().log(Level.SEVERE, "Ekonomi storage başlatılamadı", exception);
      throw new IllegalStateException("Economy storage init failed", exception);
    }
    this.ownStorage = storage;

    BigDecimal starting = BigDecimal.valueOf(configuration.economy().getDouble("starting-balance", 100.0D));
    String symbol = configuration.economy().getString("currency.symbol", "$");
    String nameSingular = configuration.economy().getString("currency.name-singular", "credit");
    String namePlural = configuration.economy().getString("currency.name-plural", "credits");
    int digits = configuration.economy().getInt("currency.fractional-digits", 2);

    this.ownEconomy = new FatsanEconomy(storage, starting, symbol, nameSingular, namePlural, digits);
  }

  private void registerAsProvider() {
    try {
      LegacyVaultBridge bridge = new LegacyVaultBridge(ownEconomy, "FatsanEconomy");
      plugin.getServer().getServicesManager().register(Economy.class, bridge, plugin, ServicePriority.High);
    } catch (NoClassDefFoundError error) {
      plugin.getLogger().warning("Vault API sınıfları yok, provider olarak register edilemedi. Sadece yerel ekonomi aktif.");
    }
  }

  public void shutdown() {
    if (ownStorage != null) {
      ownStorage.shutdown();
    }
  }

  public boolean isReady() {
    if (effectiveMode == Mode.PROVIDER) return ownEconomy != null;
    return externalVault != null;
  }

  public boolean trySetupIfNeeded() {
    if (isReady()) return true;
    return setup();
  }

  public Mode mode() {
    return effectiveMode;
  }

  public FatsanEconomy own() {
    return ownEconomy;
  }

  // ---- Unified API ----

  public BigDecimal balance(OfflinePlayer player) {
    if (effectiveMode == Mode.PROVIDER) {
      return ownEconomy.balance(player.getUniqueId());
    }
    return BigDecimal.valueOf(externalVault.getBalance(player));
  }

  public boolean has(OfflinePlayer player, BigDecimal amount) {
    if (effectiveMode == Mode.PROVIDER) {
      return ownEconomy.has(player.getUniqueId(), amount);
    }
    return externalVault.has(player, amount.doubleValue());
  }

  public Optional<Account> findByName(String name) {
    if (effectiveMode == Mode.PROVIDER) {
      return ownEconomy.findByName(name);
    }
    return Optional.empty();
  }

  public Optional<UUID> resolveUuid(String name) {
    // Önce online oyuncu
    org.bukkit.entity.Player online = plugin.getServer().getPlayerExact(name);
    if (online != null) return Optional.of(online.getUniqueId());
    // DB (provider mode)
    if (effectiveMode == Mode.PROVIDER) {
      Optional<Account> acc = ownEconomy.findByName(name);
      if (acc.isPresent()) return acc.map(Account::uuid);
    }
    // Offline player cache (sadece daha önce sunucuda giriş yapmışsa)
    OfflinePlayer cached = plugin.getServer().getOfflinePlayerIfCached(name);
    if (cached != null) return Optional.of(cached.getUniqueId());
    return Optional.empty();
  }

  /**
   * Transfer işlemi. Provider modunda atomik; consumer modunda en iyi çabayla withdraw+deposit.
   */
  public TransferOutcome pay(OfflinePlayer from, OfflinePlayer to, BigDecimal amount) {
    if (amount.signum() <= 0) {
      return new TransferOutcome(false, "invalid-amount");
    }
    if (effectiveMode == Mode.PROVIDER) {
      ownEconomy.ensureAccount(from);
      ownEconomy.ensureAccount(to);
      EconomyStorage.TransferResult result = ownEconomy.transfer(from.getUniqueId(), to.getUniqueId(), amount);
      return new TransferOutcome(result.success(),
        result.success() ? null : result.code().name().toLowerCase(Locale.ROOT));
    }
    // Consumer mode (external)
    double value = amount.doubleValue();
    if (!externalVault.has(from, value)) {
      return new TransferOutcome(false, "insufficient-funds");
    }
    var w = externalVault.withdrawPlayer(from, value);
    if (!w.transactionSuccess()) {
      return new TransferOutcome(false, w.errorMessage);
    }
    var d = externalVault.depositPlayer(to, value);
    if (!d.transactionSuccess()) {
      externalVault.depositPlayer(from, value); // rollback best-effort
      return new TransferOutcome(false, d.errorMessage);
    }
    return new TransferOutcome(true, null);
  }

  /**
   * Tek yönlü para düşme. Home gibi ücretli işlemlerde kullanılır.
   * Yetersiz bakiye ya da servis hazır değilse {@code false} döner.
   */
  public boolean withdraw(OfflinePlayer player, BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) {
      return true;
    }
    if (effectiveMode == Mode.PROVIDER) {
      if (ownEconomy == null) return false;
      ownEconomy.ensureAccount(player);
      if (!ownEconomy.has(player.getUniqueId(), amount)) return false;
      ownEconomy.withdraw(player.getUniqueId(), amount);
      return true;
    }
    if (externalVault == null) return false;
    double value = amount.doubleValue();
    if (!externalVault.has(player, value)) return false;
    return externalVault.withdrawPlayer(player, value).transactionSuccess();
  }

  public String format(BigDecimal amount) {
    if (effectiveMode == Mode.PROVIDER) {
      return ownEconomy.format(amount);
    }
    return externalVault.format(amount.doubleValue());
  }

  public List<TopEntry> top(int limit, int offset) {
    if (effectiveMode == Mode.PROVIDER) {
      return ownEconomy.top(limit, offset);
    }
    // Consumer modunda top query maliyetli — boş döndürüp cache service’e bırakıyoruz.
    return List.of();
  }

  public boolean tryReservePayLimit(UUID player, BigDecimal amount, BigDecimal dailyCap) {
    if (effectiveMode == Mode.PROVIDER) {
      return ownEconomy.tryReservePayLimit(player, amount, dailyCap);
    }
    // Consumer modunda pay limit plugin içinde tutulur (DB yoksa RAM).
    return true;
  }

  public record TransferOutcome(boolean success, String errorCode) {}
}
