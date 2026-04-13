package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.logging.AuditLogger;
import com.fatsan1975.utilities.util.CommandGate;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class ModuleCommand implements CommandExecutor, TabCompleter {
  private final PluginConfiguration configuration;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;
  private final AuditLogger auditLogger;

  public ModuleCommand(PluginConfiguration configuration, ModuleManager modules,
                       RateLimitService rateLimit, AuditLogger auditLogger) {
    this.configuration = configuration;
    this.modules = modules;
    this.rateLimit = rateLimit;
    this.auditLogger = auditLogger;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)) {
      return true;
    }
    if (sender instanceof Player player && !CommandGate.checkRateLimit(player, configuration, rateLimit, "fumodule", "rate-limit.commands.fumodule")) {
      return true;
    }

    if (args.length < 2) {
      sender.sendMessage(configuration.locale().message("general.invalid-usage", sender)
        .replace("{usage}", "/fumodule <economy|teleport|social|admin> <on|off|status>"));
      return true;
    }

    ModuleManager.Module module = ModuleManager.Module.from(args[0]);
    if (module == null) {
      sender.sendMessage(configuration.locale().message("admin.module-invalid", sender));
      return true;
    }

    String action = args[1].toLowerCase();
    switch (action) {
      case "status" -> sender.sendMessage(configuration.locale().message("admin.module-status", sender)
        .replace("{module}", args[0])
        .replace("{status}", modules.isEnabled(module) ? "ON" : "OFF"));
      case "on" -> {
        modules.setEnabled(module, true);
        sender.sendMessage(configuration.locale().message("admin.module-updated", sender).replace("{module}", args[0]).replace("{status}", "ON"));
        auditLogger.log("MODULE", sender.getName() + " set " + module.name() + " = ON");
      }
      case "off" -> {
        modules.setEnabled(module, false);
        sender.sendMessage(configuration.locale().message("admin.module-updated", sender).replace("{module}", args[0]).replace("{status}", "OFF"));
        auditLogger.log("MODULE", sender.getName() + " set " + module.name() + " = OFF");
      }
      default -> sender.sendMessage(configuration.locale().message("general.invalid-usage", sender)
        .replace("{usage}", "/fumodule <economy|teleport|social|admin> <on|off|status>"));
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
      return java.util.stream.Stream.of("economy", "teleport", "social", "admin")
          .filter(s -> s.startsWith(prefix))
          .collect(java.util.stream.Collectors.toList());
    }
    if (args.length == 2) {
      String prefix = args[1].toLowerCase(java.util.Locale.ROOT);
      return java.util.stream.Stream.of("on", "off", "status")
          .filter(s -> s.startsWith(prefix))
          .collect(java.util.stream.Collectors.toList());
    }
    return List.of();
  }
}
