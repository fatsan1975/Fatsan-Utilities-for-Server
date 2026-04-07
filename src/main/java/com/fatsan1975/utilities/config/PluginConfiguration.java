package com.fatsan1975.utilities.config;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginConfiguration {
  private final JavaPlugin plugin;

  private FileConfiguration main;
  private FileConfiguration economy;
  private FileConfiguration cooldowns;
  private FileConfiguration teleport;
  private FileConfiguration messages;

  public PluginConfiguration(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void loadAll() {
    plugin.saveDefaultConfig();
    this.main = plugin.getConfig();
    this.economy = load("economy.yml");
    this.cooldowns = load("cooldowns.yml");
    this.teleport = load("teleport.yml");
    this.messages = load("messages_tr.yml");
  }

  public void reloadAll() {
    plugin.reloadConfig();
    loadAll();
  }

  public void saveMain() {
    try {
      main.save(new File(plugin.getDataFolder(), "config.yml"));
    } catch (IOException exception) {
      plugin.getLogger().log(Level.WARNING, "config.yml kaydedilemedi", exception);
    }
  }

  private FileConfiguration load(String fileName) {
    File file = new File(plugin.getDataFolder(), fileName);
    if (!file.exists()) {
      plugin.saveResource(fileName, false);
    }
    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
    try {
      config.save(file);
    } catch (IOException exception) {
      plugin.getLogger().warning(fileName + " dosyası kaydedilemedi: " + exception.getMessage());
    }
    return config;
  }

  public FileConfiguration main() {
    return main;
  }

  public FileConfiguration economy() {
    return economy;
  }

  public FileConfiguration cooldowns() {
    return cooldowns;
  }

  public FileConfiguration teleport() {
    return teleport;
  }

  public FileConfiguration messages() {
    return messages;
  }

  public String message(String path) {
    String raw = messages.getString(path, "&cMesaj bulunamadı: " + path);
    return raw.replace('&', '§');
  }
}
