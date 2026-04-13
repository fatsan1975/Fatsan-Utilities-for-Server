package com.fatsan1975.utilities.config;

import com.fatsan1975.utilities.i18n.LocaleService;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Tüm YAML config dosyalarını ve lokalizasyon servisini yükler.
 *
 * <p>Eskisinden farklı olarak {@code load(..)} artık dosyayı geri yazmaz — böylece kullanıcının
 * yorum satırları ve formatı korunur.
 */
public final class PluginConfiguration {
  private final JavaPlugin plugin;

  private FileConfiguration main;
  private FileConfiguration economy;
  private FileConfiguration cooldowns;
  private FileConfiguration teleport;
  private LocaleService localeService;

  public PluginConfiguration(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void loadAll() {
    plugin.saveDefaultConfig();
    plugin.reloadConfig();
    this.main = plugin.getConfig();
    this.economy = load("economy.yml");
    this.cooldowns = load("cooldowns.yml");
    this.teleport = load("teleport.yml");

    if (this.localeService == null) {
      this.localeService = new LocaleService(plugin, this);
    }
    this.localeService.reload();
  }

  public void reloadAll() {
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
    return YamlConfiguration.loadConfiguration(file);
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

  public LocaleService locale() {
    return localeService;
  }

  /** Sunucu/konsol varsayılan dilinde mesaj döndürür. Legacy alias — yeni kod {@link #locale()} kullanmalı. */
  public String message(String path) {
    return localeService.message(path);
  }
}
