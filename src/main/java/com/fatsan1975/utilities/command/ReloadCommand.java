package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.PluginConfigValidator;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.logging.AuditLogger;
import com.fatsan1975.utilities.util.CommandGate;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReloadCommand implements CommandExecutor {
  private final PluginConfiguration configuration;
  private final PluginConfigValidator validator;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;
  private final AuditLogger auditLogger;

  public ReloadCommand(PluginConfiguration configuration, PluginConfigValidator validator,
                       ModuleManager modules, RateLimitService rateLimit, AuditLogger auditLogger) {
    this.configuration = configuration;
    this.validator = validator;
    this.modules = modules;
    this.rateLimit = rateLimit;
    this.auditLogger = auditLogger;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)
      || !CommandGate.checkModule(sender, configuration, modules, ModuleManager.Module.ADMIN)) {
      return true;
    }

    if (sender instanceof Player player
      && !CommandGate.checkRateLimit(player, configuration, rateLimit, "futilitiesreload", "rate-limit.commands.futilitiesreload")) {
      return true;
    }

    configuration.reloadAll();
    validator.validateAndLog();
    auditLogger.log("RELOAD", sender.getName() + " reloaded plugin configuration");
    sender.sendMessage(configuration.message("general.reload-success"));
    return true;
  }
}
