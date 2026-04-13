package com.fatsan1975.utilities.economy;

import com.fatsan1975.utilities.economy.model.Account;
import com.fatsan1975.utilities.economy.model.TopEntry;
import com.fatsan1975.utilities.economy.storage.EconomyStorage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.OfflinePlayer;

/**
 * Plugin’in kendi ekonomi çekirdeği. {@link EconomyStorage} üstünde BigDecimal temelli API sağlar.
 * Vault/VaultUnlocked bridge’leri bu sınıfı çağırır.
 */
public final class FatsanEconomy {
  private final EconomyStorage storage;
  private final BigDecimal startingBalance;
  private final String currencySymbol;
  private final String currencyNameSingular;
  private final String currencyNamePlural;
  private final int fractionalDigits;
  private final DecimalFormat decimalFormat;

  public FatsanEconomy(EconomyStorage storage, BigDecimal startingBalance,
                       String currencySymbol, String currencyNameSingular, String currencyNamePlural,
                       int fractionalDigits) {
    this.storage = storage;
    this.startingBalance = startingBalance;
    this.currencySymbol = currencySymbol;
    this.currencyNameSingular = currencyNameSingular;
    this.currencyNamePlural = currencyNamePlural;
    this.fractionalDigits = fractionalDigits;
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    symbols.setGroupingSeparator(',');
    symbols.setDecimalSeparator('.');
    StringBuilder pattern = new StringBuilder("#,##0");
    if (fractionalDigits > 0) {
      pattern.append('.');
      for (int i = 0; i < fractionalDigits; i++) pattern.append('0');
    }
    this.decimalFormat = new DecimalFormat(pattern.toString(), symbols);
  }

  public BigDecimal startingBalance() {
    return startingBalance;
  }

  public String currencySymbol() {
    return currencySymbol;
  }

  public String currencyNameSingular() {
    return currencyNameSingular;
  }

  public String currencyNamePlural() {
    return currencyNamePlural;
  }

  public int fractionalDigits() {
    return fractionalDigits;
  }

  public Account ensureAccount(UUID uuid, String name) {
    return storage.createIfMissing(uuid, name, startingBalance);
  }

  public Account ensureAccount(OfflinePlayer player) {
    String name = player.getName();
    if (name == null) name = player.getUniqueId().toString();
    return ensureAccount(player.getUniqueId(), name);
  }

  public Optional<Account> findByUuid(UUID uuid) {
    return storage.findByUuid(uuid);
  }

  public Optional<Account> findByName(String name) {
    return storage.findByName(name);
  }

  public BigDecimal balance(UUID uuid) {
    return storage.findByUuid(uuid).map(Account::balance).orElse(BigDecimal.ZERO);
  }

  public boolean has(UUID uuid, BigDecimal amount) {
    return balance(uuid).compareTo(amount) >= 0;
  }

  public EconomyStorage.TransferResult transfer(UUID from, UUID to, BigDecimal amount) {
    EconomyStorage.TransferResult result = storage.transfer(from, to, amount);
    if (result.success()) {
      storage.recordTransaction(from, to, amount, "TRANSFER", null);
    }
    return result;
  }

  public Account deposit(UUID uuid, BigDecimal amount) {
    Account result = storage.adjust(uuid, amount.abs());
    if (result != null) {
      storage.recordTransaction(null, uuid, amount.abs(), "DEPOSIT", null);
    }
    return result;
  }

  public Account withdraw(UUID uuid, BigDecimal amount) {
    Account result = storage.adjust(uuid, amount.abs().negate());
    if (result != null) {
      storage.recordTransaction(uuid, null, amount.abs(), "WITHDRAW", null);
    }
    return result;
  }

  public Account set(UUID uuid, BigDecimal newBalance) {
    Account result = storage.set(uuid, newBalance);
    if (result != null) {
      storage.recordTransaction(null, uuid, newBalance, "SET", null);
    }
    return result;
  }

  public Account reset(UUID uuid) {
    return set(uuid, startingBalance);
  }

  public List<TopEntry> top(int limit, int offset) {
    return storage.top(limit, offset);
  }

  public boolean tryReservePayLimit(UUID player, BigDecimal amount, BigDecimal dailyCap) {
    return storage.tryReservePayLimit(player, LocalDate.now(), amount, dailyCap);
  }

  public String format(BigDecimal amount) {
    BigDecimal normalized = amount.setScale(fractionalDigits, RoundingMode.HALF_UP);
    return decimalFormat.format(normalized) + " " + currencySymbol;
  }

  public EconomyStorage storage() {
    return storage;
  }
}
