package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.util.CommandGate;
import com.fatsan1975.utilities.util.PermissionAccess;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HelpCommand implements CommandExecutor {
  private final PluginConfiguration configuration;
  private final RateLimitService rateLimit;

  public HelpCommand(PluginConfiguration configuration, RateLimitService rateLimit) {
    this.configuration = configuration;
    this.rateLimit = rateLimit;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)) {
      return true;
    }
    if (sender instanceof Player player
      && !CommandGate.checkRateLimit(player, configuration, rateLimit, "fuhelp", "rate-limit.commands.fuhelp")) {
      return true;
    }

    List<String> lines = new ArrayList<>();
    lines.add(configuration.locale().message("help.header", sender));
    add(lines, sender, "fatsanutilities.balance", "help.line-balance");
    add(lines, sender, "fatsanutilities.balancetop", "help.line-balancetop");
    add(lines, sender, "fatsanutilities.pay", "help.line-pay");
    add(lines, sender, "fatsanutilities.tpa", "help.line-tpa");
    add(lines, sender, "fatsanutilities.rtp", "help.line-rtp");
    add(lines, sender, "fatsanutilities.spawn", "help.line-spawn");
    add(lines, sender, "fatsanutilities.itemchat", "help.line-itemchat");
    add(lines, sender, "fatsanutilities.invchat", "help.line-invchat");
    add(lines, sender, "fatsanutilities.admin.invsee", "help.line-invsee");
    add(lines, sender, "fatsanutilities.admin.debug", "help.line-debug");
    add(lines, sender, "fatsanutilities.admin.module", "help.line-module");
    add(lines, sender, "fatsanutilities.admin.reload", "help.line-reload");
    add(lines, sender, "fatsanutilities.admin.eco", "help.line-eco");

    lines.forEach(sender::sendMessage);
    return true;
  }

  private void add(List<String> lines, CommandSender sender, String permission, String messageKey) {
    if (PermissionAccess.has(sender, permission)) {
      lines.add("§e" + configuration.locale().message(messageKey, sender));
    }
  }
}
