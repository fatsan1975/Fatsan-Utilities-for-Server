package com.fatsan1975.utilities.util;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandGate {
  private CommandGate() {}

  public static boolean checkPermission(CommandSender sender, Command command, PluginConfiguration configuration) {
    return CommandPermission.check(sender, command, configuration);
  }

  public static boolean checkModule(CommandSender sender, PluginConfiguration configuration, ModuleManager modules, ModuleManager.Module module) {
    if (modules.isEnabled(module)) {
      return true;
    }
    sender.sendMessage(configuration.message("general.module-disabled"));
    return false;
  }

  public static boolean checkRateLimit(Player player, PluginConfiguration configuration, RateLimitService rateLimit,
                                       String key, String configPath) {
    long remaining = rateLimit.remainingMillis(key, player.getUniqueId());
    if (remaining > 0) {
      player.sendMessage(configuration.message("general.rate-limit").replace("{seconds}", String.valueOf(Math.ceil(remaining / 1000.0))));
      return false;
    }

    long interval = configuration.main().getLong(configPath, 0L);
    if (interval > 0) {
      rateLimit.mark(key, player.getUniqueId(), interval);
    }
    return true;
  }
}
