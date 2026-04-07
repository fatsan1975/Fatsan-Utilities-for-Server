package com.fatsan1975.utilities.teleport;

import com.fatsan1975.utilities.config.PluginConfiguration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TeleportService {
  public enum RtpFailReason {
    NO_SAFE_LOCATION,
    OUTSIDE_WORLD_BORDER,
    INVALID_TARGET_WORLD
  }

  public record RtpResult(boolean success, Location location, RtpFailReason reason) {}

  private final JavaPlugin plugin;
  private final PluginConfiguration config;

  public TeleportService(JavaPlugin plugin, PluginConfiguration config) {
    this.plugin = plugin;
    this.config = config;
  }

  public void teleportSpawn(Player player) {
    player.teleportAsync(player.getWorld().getSpawnLocation());
  }

  public RtpResult findRandomSafeLocation(World world, RtpOptions options) {
    if (world == null) {
      return new RtpResult(false, null, RtpFailReason.INVALID_TARGET_WORLD);
    }

    boolean sawBorderReject = false;

    for (int attempt = 0; attempt < options.maxAttempts(); attempt++) {
      int x = ThreadLocalRandom.current().nextInt(options.minCoordinate(), options.maxCoordinate() + 1);
      int z = ThreadLocalRandom.current().nextInt(options.minCoordinate(), options.maxCoordinate() + 1);

      Block highest = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
      int y = highest.getY() + 1;
      if (y >= world.getMaxHeight() || y <= world.getMinHeight()) {
        continue;
      }

      Location candidate = new Location(world, x + 0.5, y, z + 0.5);
      if (!world.getWorldBorder().isInside(candidate)) {
        sawBorderReject = true;
        continue;
      }
      if (!isSafe(candidate, options)) {
        continue;
      }

      return new RtpResult(true, candidate, null);
    }

    RtpFailReason reason = sawBorderReject ? RtpFailReason.OUTSIDE_WORLD_BORDER : RtpFailReason.NO_SAFE_LOCATION;
    plugin.getLogger().warning("RTP lokasyonu bulunamadı. world=" + world.getName() + " reason=" + reason);
    return new RtpResult(false, null, reason);
  }

  private boolean isSafe(Location location, RtpOptions options) {
    Block feet = location.getBlock();
    Block head = location.clone().add(0, 1, 0).getBlock();
    Block ground = location.clone().subtract(0, 1, 0).getBlock();

    if (feet.isLiquid() || head.isLiquid() || feet.getType().isSolid() || head.getType().isSolid()) {
      return false;
    }

    Material groundType = ground.getType();
    if (groundType == Material.AIR || options.deniedGroundMaterials().contains(groundType)) {
      return false;
    }

    Biome biome = location.getWorld().getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    return !options.deniedBiomes().contains(biome);
  }

  public RtpOptions optionsForWorld(World world) {
    ConfigurationSection rtp = config.teleport().getConfigurationSection("rtp");
    if (rtp == null) {
      return new RtpOptions(true, -1500, 1500, 30, Set.of(), Set.of(Material.WATER, Material.LAVA));
    }

    int defaultMin = rtp.getInt("default.min-coordinate", -1500);
    int defaultMax = rtp.getInt("default.max-coordinate", 1500);
    int defaultAttempts = rtp.getInt("default.max-attempts", 30);
    Set<Biome> defaultDeniedBiomes = parseBiomes(rtp.getStringList("default.denied-biomes"));
    Set<Material> defaultDeniedGround = parseMaterials(rtp.getStringList("default.denied-ground-materials"));

    ConfigurationSection worlds = rtp.getConfigurationSection("worlds");
    if (worlds == null) {
      return new RtpOptions(true, defaultMin, defaultMax, defaultAttempts, defaultDeniedBiomes, defaultDeniedGround);
    }

    ConfigurationSection current = worlds.getConfigurationSection(world.getName());
    if (current == null) {
      return new RtpOptions(true, defaultMin, defaultMax, defaultAttempts, defaultDeniedBiomes, defaultDeniedGround);
    }

    if (!current.getBoolean("enabled", true)) {
      return new RtpOptions(false, defaultMin, defaultMax, defaultAttempts, defaultDeniedBiomes, defaultDeniedGround);
    }

    int min = current.getInt("min-coordinate", defaultMin);
    int max = current.getInt("max-coordinate", defaultMax);
    int attempts = current.getInt("max-attempts", defaultAttempts);

    Set<Biome> deniedBiomes = defaultDeniedBiomes;
    if (current.isList("denied-biomes")) {
      deniedBiomes = parseBiomes(current.getStringList("denied-biomes"));
    }

    Set<Material> deniedGround = defaultDeniedGround;
    if (current.isList("denied-ground-materials")) {
      deniedGround = parseMaterials(current.getStringList("denied-ground-materials"));
    }

    return new RtpOptions(true, min, max, attempts, deniedBiomes, deniedGround);
  }

  private Set<Biome> parseBiomes(List<String> entries) {
    Set<Biome> biomes = new HashSet<>();
    for (String entry : entries) {
      String key = entry.toLowerCase(Locale.ROOT);
      Biome biome = Registry.BIOME.get(NamespacedKey.minecraft(key));
      if (biome == null) {
        plugin.getLogger().warning("Geçersiz biome (rtp): " + entry);
        continue;
      }
      biomes.add(biome);
    }
    return biomes;
  }

  private Set<Material> parseMaterials(List<String> entries) {
    Set<Material> materials = new HashSet<>();
    for (String entry : entries) {
      Material material = Material.matchMaterial(entry);
      if (material == null) {
        plugin.getLogger().warning("Geçersiz materyal (rtp): " + entry);
        continue;
      }
      materials.add(material);
    }
    return materials;
  }

  public record RtpOptions(
    boolean enabled,
    int minCoordinate,
    int maxCoordinate,
    int maxAttempts,
    Set<Biome> deniedBiomes,
    Set<Material> deniedGroundMaterials
  ) {}
}
