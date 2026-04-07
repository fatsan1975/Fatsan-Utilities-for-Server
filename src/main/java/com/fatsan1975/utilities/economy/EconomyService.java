package com.fatsan1975.utilities.economy;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyService {
  private final JavaPlugin plugin;
  private Economy economy;

  public EconomyService(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public boolean setup() {
    RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
    if (registration == null) {
      return false;
    }
    this.economy = registration.getProvider();
    return this.economy != null;
  }

  public double balance(OfflinePlayer player) {
    return economy.getBalance(player);
  }

  public EconomyResponse pay(OfflinePlayer from, OfflinePlayer to, double amount) {
    EconomyResponse withdraw = economy.withdrawPlayer(from, amount);
    if (!withdraw.transactionSuccess()) {
      return withdraw;
    }

    EconomyResponse deposit = economy.depositPlayer(to, amount);
    if (!deposit.transactionSuccess()) {
      economy.depositPlayer(from, amount);
      return deposit;
    }
    return deposit;
  }

  public String format(double amount) {
    return economy.format(amount);
  }

  public boolean has(OfflinePlayer player, double amount) {
    return economy.has(player, amount);
  }

  public List<OfflinePlayer> topBalances(int limit) {
    return Bukkit.getOfflinePlayers()
      .length == 0 ? List.of() :
      List.of(Bukkit.getOfflinePlayers()).stream()
      .filter(player -> player.getName() != null)
      .sorted(Comparator.comparingDouble(this::balance).reversed())
      .limit(limit)
      .collect(Collectors.toList());
  }
}
