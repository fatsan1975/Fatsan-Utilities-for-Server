package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.util.CommandGate;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BalanceCommand implements CommandExecutor {
  private final EconomyService economyService;
  private final PluginConfiguration configuration;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;

  public BalanceCommand(EconomyService economyService, PluginConfiguration configuration, ModuleManager modules, RateLimitService rateLimit) {
    this.economyService = economyService;
    this.configuration = configuration;
    this.modules = modules;
    this.rateLimit = rateLimit;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)
      || !CommandGate.checkModule(sender, configuration, modules, ModuleManager.Module.ECONOMY)) {
      return true;
    }

    if (sender instanceof Player player && !CommandGate.checkRateLimit(player, configuration, rateLimit, "balance", "rate-limit.commands.balance")) {
      return true;
    }
    if (!economyService.trySetupIfNeeded()) {
      sender.sendMessage(configuration.message("economy.not-ready"));
      return true;
    }

    if (args.length == 0) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(configuration.message("general.player-only"));
        return true;
      }
      String msg = configuration.message("economy.balance-self")
        .replace("{amount}", economyService.format(economyService.balance(player)));
      sender.sendMessage(msg);
      return true;
    }

    OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
    if (target == null || target.getName() == null) {
      sender.sendMessage(configuration.message("general.player-not-found"));
      return true;
    }

    String msg = configuration.message("economy.balance-other")
      .replace("{player}", target.getName())
      .replace("{amount}", economyService.format(economyService.balance(target)));
    sender.sendMessage(msg);
    return true;
  }
}
