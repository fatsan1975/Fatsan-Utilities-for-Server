package com.fatsan1975.utilities.core;

import com.fatsan1975.utilities.config.PluginConfiguration;
import java.util.Locale;

public final class ModuleManager {
  public enum Module {
    ECONOMY("modules.economy"),
    TELEPORT("modules.teleport"),
    SOCIAL("modules.social"),
    ADMIN("modules.admin");

    private final String path;

    Module(String path) {
      this.path = path;
    }

    public String path() {
      return path;
    }

    public static Module from(String value) {
      return switch (value.toLowerCase(Locale.ROOT)) {
        case "economy" -> ECONOMY;
        case "teleport" -> TELEPORT;
        case "social" -> SOCIAL;
        case "admin" -> ADMIN;
        default -> null;
      };
    }
  }

  private final PluginConfiguration configuration;

  public ModuleManager(PluginConfiguration configuration) {
    this.configuration = configuration;
  }

  public boolean isEnabled(Module module) {
    return configuration.main().getBoolean(module.path(), true);
  }

  public void setEnabled(Module module, boolean enabled) {
    configuration.main().set(module.path(), enabled);
    configuration.saveMain();
  }
}
