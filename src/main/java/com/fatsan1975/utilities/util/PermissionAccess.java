package com.fatsan1975.utilities.util;

import com.fatsan1975.utilities.permission.FoliaPermsAdapter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PermissionAccess {
  private PermissionAccess() {}

  public static boolean has(CommandSender sender, String permission) {
    if (permission == null || permission.isBlank()) {
      return true;
    }
    if (sender instanceof Player player) {
      return FoliaPermsAdapter.has(player, permission);
    }
    return sender.hasPermission(permission);
  }
}
