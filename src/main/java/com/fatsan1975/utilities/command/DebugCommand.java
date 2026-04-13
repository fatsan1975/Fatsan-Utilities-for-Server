package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.core.scheduler.FoliaScheduler;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.util.CommandGate;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DebugCommand implements CommandExecutor {
  private final PluginConfiguration configuration;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;
  private final EconomyService economyService;

  public DebugCommand(PluginConfiguration configuration, ModuleManager modules, RateLimitService rateLimit, EconomyService economyService) {
    this.configuration = configuration;
    this.modules = modules;
    this.rateLimit = rateLimit;
    this.economyService = economyService;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)) {
      return true;
    }
    if (sender instanceof Player player && !CommandGate.checkRateLimit(player, configuration, rateLimit, "fudebug", "rate-limit.commands.fudebug")) {
      return true;
    }

    sender.sendMessage("§6§lFatsanUtilities Debug");
    sender.sendMessage("§eFolia runtime: §f" + FoliaScheduler.isFolia());
    sender.sendMessage("§eEconomy mode: §f" + economyService.mode());
    sender.sendMessage("§eEconomy ready: §f" + economyService.isReady());
    sender.sendMessage("§eLocale: §f" + configuration.locale().serverLang());
    sender.sendMessage("§eModules: §feconomy=" + modules.isEnabled(ModuleManager.Module.ECONOMY)
      + " teleport=" + modules.isEnabled(ModuleManager.Module.TELEPORT)
      + " social=" + modules.isEnabled(ModuleManager.Module.SOCIAL)
      + " admin=" + modules.isEnabled(ModuleManager.Module.ADMIN));
    sender.sendMessage("§eRTP default world: §f" + configuration.teleport().getString("rtp.default-world", "(none)"));
    sender.sendMessage("§ePay daily limit: §f" + configuration.economy().getDouble("pay.daily-limit", 0));
    return true;
  }
}
