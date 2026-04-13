package com.fatsan1975.utilities.teleport;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Oyuncu home lokasyonlarını {@code homes.yml} içinde saklar.
 * Çok dünyalı sunucularla uyumlu: her home kendi dünyası ile birlikte saklanır.
 */
public final class HomeService {
  private final JavaPlugin plugin;
  private final File file;
  private final Object lock = new Object();
  private volatile FileConfiguration data;

  public HomeService(JavaPlugin plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "homes.yml");
    reload();
  }

  public void reload() {
    if (!plugin.getDataFolder().exists()) {
      plugin.getDataFolder().mkdirs();
    }
    if (!file.exists()) {
      try {
        file.createNewFile();
      } catch (IOException exception) {
        plugin.getLogger().warning("homes.yml oluşturulamadı: " + exception.getMessage());
      }
    }
    this.data = YamlConfiguration.loadConfiguration(file);
  }

  public Set<String> list(UUID uuid) {
    ConfigurationSection section = data.getConfigurationSection("homes." + uuid);
    if (section == null) return Collections.emptySet();
    return new TreeSet<>(section.getKeys(false));
  }

  public int count(UUID uuid) {
    return list(uuid).size();
  }

  public Location get(UUID uuid, String name) {
    String base = path(uuid, name);
    if (!data.isConfigurationSection(base)) return null;
    String worldName = data.getString(base + ".world");
    World world = worldName == null ? null : Bukkit.getWorld(worldName);
    if (world == null) return null;
    return new Location(
      world,
      data.getDouble(base + ".x"),
      data.getDouble(base + ".y"),
      data.getDouble(base + ".z"),
      (float) data.getDouble(base + ".yaw"),
      (float) data.getDouble(base + ".pitch"));
  }

  public void set(UUID uuid, String name, Location location) {
    synchronized (lock) {
      String base = path(uuid, name);
      data.set(base + ".world", location.getWorld().getName());
      data.set(base + ".x", location.getX());
      data.set(base + ".y", location.getY());
      data.set(base + ".z", location.getZ());
      data.set(base + ".yaw", location.getYaw());
      data.set(base + ".pitch", location.getPitch());
      save();
    }
  }

  public boolean delete(UUID uuid, String name) {
    synchronized (lock) {
      String base = path(uuid, name);
      if (!data.isConfigurationSection(base)) return false;
      data.set(base, null);
      save();
      return true;
    }
  }

  public boolean exists(UUID uuid, String name) {
    return data.isConfigurationSection(path(uuid, name));
  }

  private String path(UUID uuid, String name) {
    return "homes." + uuid + "." + name.toLowerCase(Locale.ROOT);
  }

  private void save() {
    try {
      data.save(file);
    } catch (IOException exception) {
      plugin.getLogger().warning("homes.yml kaydedilemedi: " + exception.getMessage());
    }
  }
}
