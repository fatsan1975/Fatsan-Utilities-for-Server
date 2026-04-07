package com.fatsan1975.utilities.logging;

import org.bukkit.plugin.java.JavaPlugin;

public final class AuditLogger {
  private final JavaPlugin plugin;

  public AuditLogger(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void log(String action, String details) {
    plugin.getLogger().info("[AUDIT] [" + action + "] " + details);
  }
}
