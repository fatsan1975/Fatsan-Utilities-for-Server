package com.fatsan1975.utilities.util;

import com.fatsan1975.utilities.config.PluginConfiguration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public final class CommandPermission {
  private CommandPermission() {}

  public static boolean check(CommandSender sender, Command command, PluginConfiguration configuration) {
    String permission = command.getPermission();
    if (PermissionAccess.has(sender, permission)) {
      return true;
    }
    sender.sendMessage(configuration.message("general.no-permission"));
    return false;
  }
}
