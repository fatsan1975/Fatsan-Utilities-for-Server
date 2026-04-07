package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.teleport.TeleportService;
import com.fatsan1975.utilities.util.CommandGate;
import com.fatsan1975.utilities.util.CooldownService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpawnCommand implements CommandExecutor, TabCompleter {
  public enum Mode { SPAWN, RTP }

  private final PluginConfiguration configuration;
  private final TeleportService teleportService;
  private final CooldownService cooldownService;
  private final Mode mode;
  private final JavaPlugin plugin;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;

  public SpawnCommand(JavaPlugin plugin, PluginConfiguration configuration, TeleportService teleportService, CooldownService cooldownService, Mode mode,
                      ModuleManager modules, RateLimitService rateLimit) {
    this.plugin = plugin;
    this.configuration = configuration;
    this.teleportService = teleportService;
    this.cooldownService = cooldownService;
    this.mode = mode;
    this.modules = modules;
    this.rateLimit = rateLimit;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)
      || !CommandGate.checkModule(sender, configuration, modules, ModuleManager.Module.TELEPORT)) {
      return true;
    }
    if (!(sender instanceof Player player)) {
      sender.sendMessage(configuration.message("general.player-only"));
      return true;
    }

    String key = mode == Mode.SPAWN ? "spawn" : "rtp";
    if (!CommandGate.checkRateLimit(player, configuration, rateLimit, key, "rate-limit.commands." + key)) {
      return true;
    }
    long remaining = cooldownService.remainingMillis(key, player.getUniqueId());
    if (remaining > 0) {
      sender.sendMessage(configuration.message("general.cooldown").replace("{seconds}", String.valueOf(Math.ceil(remaining / 1000.0))));
      return true;
    }

    if (mode == Mode.SPAWN) {
      long delayTicks = configuration.teleport().getLong("spawn.delay-ticks", 0L);
      boolean cancelOnMove = configuration.teleport().getBoolean("spawn.cancel-on-move", false);
      scheduleTeleport(player, player.getWorld().getSpawnLocation(), delayTicks, cancelOnMove,
        configuration.message("teleport.spawn-success"), configuration.message("teleport.teleport-cancelled-move"));
      cooldownService.set("spawn", player.getUniqueId(), configuration.cooldowns().getLong("commands.spawn", 3000L));
      return true;
    }

    World targetWorld = resolveRtpWorld(player, args);
    if (targetWorld == null) {
      return true;
    }

    String worldPermission = "fatsanutilities.rtp.world." + targetWorld.getName().toLowerCase();
    if (!player.hasPermission(worldPermission)) {
      player.sendMessage(configuration.message("teleport.rtp-no-world-permission").replace("{world}", targetWorld.getName()));
      return true;
    }

    TeleportService.RtpOptions options = teleportService.optionsForWorld(targetWorld);
    if (!options.enabled()) {
      player.sendMessage(configuration.message("teleport.rtp-world-disabled").replace("{world}", targetWorld.getName()));
      return true;
    }

    TeleportService.RtpResult result = teleportService.findRandomSafeLocation(targetWorld, options);
    if (!result.success()) {
      if (result.reason() == TeleportService.RtpFailReason.OUTSIDE_WORLD_BORDER) {
        player.sendMessage(configuration.message("teleport.rtp-fail-border"));
      } else {
        player.sendMessage(configuration.message("teleport.rtp-fail"));
      }
      return true;
    }

    long delayTicks = configuration.teleport().getLong("rtp.delay-ticks", 0L);
    boolean cancelOnMove = configuration.teleport().getBoolean("rtp.cancel-on-move", false);
    scheduleTeleport(player, result.location(), delayTicks, cancelOnMove,
      configuration.message("teleport.rtp-success").replace("{world}", targetWorld.getName()),
      configuration.message("teleport.teleport-cancelled-move"));

    long cooldownMillis = configuration.teleport().getLong("rtp.cooldown-millis",
      configuration.cooldowns().getLong("commands.rtp", 5000L));
    cooldownService.set("rtp", player.getUniqueId(), cooldownMillis);
    return true;
  }

  private void scheduleTeleport(Player player, Location target, long delayTicks, boolean cancelOnMove,
                                String successMessage, String cancelMessage) {
    if (delayTicks <= 0) {
      player.teleportAsync(target);
      player.sendMessage(successMessage);
      return;
    }

    Location start = player.getLocation().clone();
    player.sendMessage(configuration.message("teleport.teleport-delay").replace("{seconds}", String.valueOf(delayTicks / 20.0)));

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!player.isOnline()) {
        return;
      }
      if (cancelOnMove && moved(start, player.getLocation())) {
        player.sendMessage(cancelMessage);
        return;
      }
      player.teleportAsync(target);
      player.sendMessage(successMessage);
    }, delayTicks);
  }

  private boolean moved(Location first, Location second) {
    return first.getBlockX() != second.getBlockX()
      || first.getBlockY() != second.getBlockY()
      || first.getBlockZ() != second.getBlockZ();
  }

  private World resolveRtpWorld(Player player, String[] args) {
    if (args.length > 0) {
      World selected = Bukkit.getWorld(args[0]);
      if (selected == null) {
        player.sendMessage(configuration.message("teleport.rtp-world-not-found"));
        return null;
      }
      return selected;
    }

    String defaultWorldName = configuration.teleport().getString("rtp.default-world", player.getWorld().getName());
    World defaultWorld = Bukkit.getWorld(defaultWorldName);
    if (defaultWorld == null) {
      return player.getWorld();
    }
    return defaultWorld;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (mode == Mode.RTP && args.length == 1) {
      return Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
    }
    return List.of();
  }
}
