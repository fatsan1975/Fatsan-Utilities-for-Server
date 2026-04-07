package com.fatsan1975.utilities.core;

import com.fatsan1975.utilities.config.PluginConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfigValidator {
  private final JavaPlugin plugin;
  private final PluginConfiguration configuration;

  public PluginConfigValidator(JavaPlugin plugin, PluginConfiguration configuration) {
    this.plugin = plugin;
    this.configuration = configuration;
  }

  public void validateAndLog() {
    validateInt("cooldowns.commands.pay", 0);
    validateInt("cooldowns.commands.tpa", 0);
    validateInt("cooldowns.commands.rtp", 0);
    validateInt("cooldowns.commands.spawn", 0);

    validateInt("teleport.rtp.default.min-coordinate", -30000000);
    validateInt("teleport.rtp.default.max-coordinate", -30000000);
    validateInt("teleport.rtp.default.max-attempts", 1);
  }

  private void validateInt(String path, int min) {
    Integer value = readInt(path);
    if (value == null) {
      plugin.getLogger().warning("Config eksik: " + path);
      return;
    }
    if (value < min) {
      plugin.getLogger().warning("Config hatalı (" + path + "): " + value + " (min " + min + ")");
    }
  }

  private Integer readInt(String path) {
    if (path.startsWith("cooldowns.")) {
      String key = path.substring("cooldowns.".length());
      return configuration.cooldowns().contains(key) ? configuration.cooldowns().getInt(key) : null;
    }
    if (path.startsWith("teleport.")) {
      String key = path.substring("teleport.".length());
      return configuration.teleport().contains(key) ? configuration.teleport().getInt(key) : null;
    }
    return null;
  }
}
