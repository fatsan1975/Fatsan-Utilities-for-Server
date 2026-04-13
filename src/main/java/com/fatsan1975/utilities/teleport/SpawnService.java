package com.fatsan1975.utilities.teleport;

import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Yapılandırılabilir spawn lokasyonunu {@code spawn.yml} içinde tutar.
 * Dünya bağımsız çalışır — {@code /setspawn} hangi dünyada çalıştırılırsa spawn orası olur.
 */
public final class SpawnService {
  private final JavaPlugin plugin;
  private final File file;
  private FileConfiguration data;

  public SpawnService(JavaPlugin plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "spawn.yml");
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
        plugin.getLogger().warning("spawn.yml oluşturulamadı: " + exception.getMessage());
      }
    }
    this.data = YamlConfiguration.loadConfiguration(file);
  }

  public boolean hasSpawn() {
    return data.isConfigurationSection("spawn") && data.getString("spawn.world") != null;
  }

  public Location getSpawn() {
    if (!hasSpawn()) return null;
    String worldName = data.getString("spawn.world");
    World world = worldName == null ? null : Bukkit.getWorld(worldName);
    if (world == null) return null;
    return new Location(
      world,
      data.getDouble("spawn.x"),
      data.getDouble("spawn.y"),
      data.getDouble("spawn.z"),
      (float) data.getDouble("spawn.yaw"),
      (float) data.getDouble("spawn.pitch"));
  }

  public void setSpawn(Location location) {
    data.set("spawn.world", location.getWorld().getName());
    data.set("spawn.x", location.getX());
    data.set("spawn.y", location.getY());
    data.set("spawn.z", location.getZ());
    data.set("spawn.yaw", location.getYaw());
    data.set("spawn.pitch", location.getPitch());
    save();
  }

  private void save() {
    try {
      data.save(file);
    } catch (IOException exception) {
      plugin.getLogger().warning("spawn.yml kaydedilemedi: " + exception.getMessage());
    }
  }
}
