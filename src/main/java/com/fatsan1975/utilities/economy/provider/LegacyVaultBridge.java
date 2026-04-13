package com.fatsan1975.utilities.economy.provider;

import com.fatsan1975.utilities.economy.FatsanEconomy;
import com.fatsan1975.utilities.economy.model.Account;
import com.fatsan1975.utilities.economy.storage.EconomyStorage;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

/**
 * Legacy Vault {@link Economy} arayüzü üzerinden {@link FatsanEconomy}’yi sağlayıcı olarak register eder.
 *
 * <p>VaultUnlocked de bu arayüze geriye dönük uyum sağlar; bu bridge her iki durumu da kapsar.
 */
public final class LegacyVaultBridge implements Economy {
  private final FatsanEconomy economy;
  private final String providerName;

  public LegacyVaultBridge(FatsanEconomy economy, String providerName) {
    this.economy = economy;
    this.providerName = providerName;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String getName() {
    return providerName;
  }

  @Override
  public boolean hasBankSupport() {
    return false;
  }

  @Override
  public int fractionalDigits() {
    return economy.fractionalDigits();
  }

  @Override
  public String format(double amount) {
    return economy.format(BigDecimal.valueOf(amount));
  }

  @Override
  public String currencyNamePlural() {
    return economy.currencyNamePlural();
  }

  @Override
  public String currencyNameSingular() {
    return economy.currencyNameSingular();
  }

  @Override
  public boolean hasAccount(String playerName) {
    return economy.findByName(playerName).isPresent();
  }

  @Override
  public boolean hasAccount(OfflinePlayer player) {
    return economy.findByUuid(player.getUniqueId()).isPresent();
  }

  @Override
  public boolean hasAccount(String playerName, String worldName) {
    return hasAccount(playerName);
  }

  @Override
  public boolean hasAccount(OfflinePlayer player, String worldName) {
    return hasAccount(player);
  }

  @Override
  public double getBalance(String playerName) {
    return economy.findByName(playerName).map(a -> a.balance().doubleValue()).orElse(0D);
  }

  @Override
  public double getBalance(OfflinePlayer player) {
    return economy.balance(player.getUniqueId()).doubleValue();
  }

  @Override
  public double getBalance(String playerName, String world) {
    return getBalance(playerName);
  }

  @Override
  public double getBalance(OfflinePlayer player, String world) {
    return getBalance(player);
  }

  @Override
  public boolean has(String playerName, double amount) {
    return economy.findByName(playerName)
      .map(a -> a.balance().compareTo(BigDecimal.valueOf(amount)) >= 0).orElse(false);
  }

  @Override
  public boolean has(OfflinePlayer player, double amount) {
    return economy.has(player.getUniqueId(), BigDecimal.valueOf(amount));
  }

  @Override
  public boolean has(String playerName, String world, double amount) {
    return has(playerName, amount);
  }

  @Override
  public boolean has(OfflinePlayer player, String world, double amount) {
    return has(player, amount);
  }

  @Override
  public EconomyResponse withdrawPlayer(String playerName, double amount) {
    Optional<Account> acc = economy.findByName(playerName);
    if (acc.isEmpty()) return fail("Account not found");
    if (amount < 0) return fail("amount<0");
    Account before = acc.get();
    if (before.balance().compareTo(BigDecimal.valueOf(amount)) < 0) {
      return new EconomyResponse(amount, before.balance().doubleValue(), EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
    }
    Account after = economy.withdraw(before.uuid(), BigDecimal.valueOf(amount));
    return new EconomyResponse(amount, after.balance().doubleValue(), EconomyResponse.ResponseType.SUCCESS, null);
  }

  @Override
  public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
    Account acc = economy.ensureAccount(player);
    if (amount < 0) return fail("amount<0");
    if (acc.balance().compareTo(BigDecimal.valueOf(amount)) < 0) {
      return new EconomyResponse(amount, acc.balance().doubleValue(), EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
    }
    Account after = economy.withdraw(acc.uuid(), BigDecimal.valueOf(amount));
    return new EconomyResponse(amount, after.balance().doubleValue(), EconomyResponse.ResponseType.SUCCESS, null);
  }

  @Override
  public EconomyResponse withdrawPlayer(String playerName, String world, double amount) {
    return withdrawPlayer(playerName, amount);
  }

  @Override
  public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount) {
    return withdrawPlayer(player, amount);
  }

  @Override
  public EconomyResponse depositPlayer(String playerName, double amount) {
    Optional<Account> acc = economy.findByName(playerName);
    if (acc.isEmpty()) return fail("Account not found");
    if (amount < 0) return fail("amount<0");
    Account after = economy.deposit(acc.get().uuid(), BigDecimal.valueOf(amount));
    return new EconomyResponse(amount, after.balance().doubleValue(), EconomyResponse.ResponseType.SUCCESS, null);
  }

  @Override
  public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
    Account acc = economy.ensureAccount(player);
    if (amount < 0) return fail("amount<0");
    Account after = economy.deposit(acc.uuid(), BigDecimal.valueOf(amount));
    return new EconomyResponse(amount, after.balance().doubleValue(), EconomyResponse.ResponseType.SUCCESS, null);
  }

  @Override
  public EconomyResponse depositPlayer(String playerName, String world, double amount) {
    return depositPlayer(playerName, amount);
  }

  @Override
  public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount) {
    return depositPlayer(player, amount);
  }

  @Override
  public boolean createPlayerAccount(String playerName) {
    return economy.findByName(playerName).isPresent();
  }

  @Override
  public boolean createPlayerAccount(OfflinePlayer player) {
    economy.ensureAccount(player);
    return true;
  }

  @Override
  public boolean createPlayerAccount(String playerName, String world) {
    return createPlayerAccount(playerName);
  }

  @Override
  public boolean createPlayerAccount(OfflinePlayer player, String world) {
    return createPlayerAccount(player);
  }

  // ---- Banking not supported ----
  @Override
  public EconomyResponse createBank(String name, String player) { return bankUnsupported(); }
  @Override
  public EconomyResponse createBank(String name, OfflinePlayer player) { return bankUnsupported(); }
  @Override
  public EconomyResponse deleteBank(String name) { return bankUnsupported(); }
  @Override
  public EconomyResponse bankBalance(String name) { return bankUnsupported(); }
  @Override
  public EconomyResponse bankHas(String name, double amount) { return bankUnsupported(); }
  @Override
  public EconomyResponse bankWithdraw(String name, double amount) { return bankUnsupported(); }
  @Override
  public EconomyResponse bankDeposit(String name, double amount) { return bankUnsupported(); }
  @Override
  public EconomyResponse isBankOwner(String name, String playerName) { return bankUnsupported(); }
  @Override
  public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return bankUnsupported(); }
  @Override
  public EconomyResponse isBankMember(String name, String playerName) { return bankUnsupported(); }
  @Override
  public EconomyResponse isBankMember(String name, OfflinePlayer player) { return bankUnsupported(); }
  @Override
  public List<String> getBanks() { return List.of(); }

  private EconomyResponse fail(String msg) {
    return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, msg);
  }

  private EconomyResponse bankUnsupported() {
    return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banking not supported");
  }
}
