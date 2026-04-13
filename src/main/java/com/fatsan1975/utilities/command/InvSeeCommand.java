package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.admin.AdminInventoryService;
import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.logging.AuditLogger;
import com.fatsan1975.utilities.util.CommandGate;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class InvSeeCommand implements CommandExecutor, TabCompleter {
  private final PluginConfiguration configuration;
  private final AdminInventoryService inventoryService;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;
  private final AuditLogger auditLogger;

  public InvSeeCommand(PluginConfiguration configuration, AdminInventoryService inventoryService,
                       ModuleManager modules, RateLimitService rateLimit, AuditLogger auditLogger) {
    this.configuration = configuration;
    this.inventoryService = inventoryService;
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

    if (!(sender instanceof Player viewer)) {
      sender.sendMessage(configuration.locale().message("general.player-only", sender));
      return true;
    }

    if (!CommandGate.checkRateLimit(viewer, configuration, rateLimit, "invsee", "rate-limit.commands.invsee")) {
      return true;
    }

    if (args.length < 1) {
      viewer.sendMessage(configuration.locale().message("general.invalid-usage", viewer)
        .replace("{usage}", "/invsee <player> [ender]"));
      return true;
    }

    Player target = Bukkit.getPlayerExact(args[0]);
    if (target == null) {
      viewer.sendMessage(configuration.locale().message("general.player-not-found", viewer));
      return true;
    }

    if (args.length > 1 && args[1].equalsIgnoreCase("ender")) {
      viewer.openInventory(inventoryService.createEnderView(viewer, target));
      viewer.sendMessage(configuration.locale().message("admin.invsee-open-ender", viewer).replace("{player}", target.getName()));
      auditLogger.log("INVSEE", viewer.getName() + " opened ender of " + target.getName());
      return true;
    }

    viewer.openInventory(inventoryService.createInventoryView(viewer, target));
    viewer.sendMessage(configuration.locale().message("admin.invsee-open", viewer).replace("{player}", target.getName()));
    auditLogger.log("INVSEE", viewer.getName() + " opened inventory of " + target.getName());
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(prefix))
          .collect(Collectors.toList());
    }
    if (args.length == 2) {
      return List.of("ender");
    }
    return List.of();
  }
}
