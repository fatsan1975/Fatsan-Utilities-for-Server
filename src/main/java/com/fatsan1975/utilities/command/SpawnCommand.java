package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.core.scheduler.FoliaScheduler;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.teleport.SpawnService;
import com.fatsan1975.utilities.teleport.TeleportService;
import com.fatsan1975.utilities.util.CommandCost;
import com.fatsan1975.utilities.util.CommandGate;
import com.fatsan1975.utilities.util.CooldownService;
import com.fatsan1975.utilities.util.PermissionAccess;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnCommand implements CommandExecutor, TabCompleter {
  public enum Mode { SPAWN, RTP }

  private final PluginConfiguration configuration;
  private final TeleportService teleportService;
  private final CooldownService cooldownService;
  private final Mode mode;
  private final JavaPlugin plugin;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;
  private final SpawnService spawnService;
  private final EconomyService economyService;

  public SpawnCommand(
      JavaPlugin plugin,
      PluginConfiguration configuration,
      TeleportService teleportService,
      CooldownService cooldownService,
      Mode mode,
      ModuleManager modules,
      RateLimitService rateLimit,
      SpawnService spawnService,
      EconomyService economyService) {
    this.plugin = plugin;
    this.configuration = configuration;
    this.teleportService = teleportService;
    this.cooldownService = cooldownService;
    this.mode = mode;
    this.modules = modules;
    this.rateLimit = rateLimit;
    this.spawnService = spawnService;
    this.economyService = economyService;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)
      || !CommandGate.checkModule(sender, configuration, modules, ModuleManager.Module.TELEPORT)) {
      return true;
    }
    if (!(sender instanceof Player player)) {
      sender.sendMessage(configuration.locale().message("general.player-only", sender));
      return true;
    }

    String key = mode == Mode.SPAWN ? "spawn" : "rtp";
    if (!CommandGate.checkRateLimit(player, configuration, rateLimit, key, "rate-limit.commands." + key)) {
      return true;
    }

    long remaining = cooldownService.remainingMillis(key, player.getUniqueId());
    if (remaining > 0) {
      sender.sendMessage(configuration.locale().message("general.cooldown", sender)
        .replace("{seconds}", String.valueOf(Math.ceil(remaining / 1000.0))));
      return true;
    }

    if (mode == Mode.SPAWN) {
      if (!CommandCost.charge(player, configuration, economyService, "spawn")) {
        return true;
      }

      long delayTicks = configuration.teleport().getLong("spawn.delay-ticks", 0L);
      boolean cancelOnMove = configuration.teleport().getBoolean("spawn.cancel-on-move", false);
      Location spawnTarget = spawnService != null ? spawnService.getSpawn() : null;
      if (spawnTarget == null) {
        spawnTarget = player.getWorld().getSpawnLocation();
      }
      scheduleTeleport(
        player,
        spawnTarget,
        delayTicks,
        cancelOnMove,
        configuration.locale().message("teleport.spawn-success", sender),
        configuration.locale().message("teleport.teleport-cancelled-move", sender));
      cooldownService.set("spawn", player.getUniqueId(), configuration.cooldowns().getLong("commands.spawn", 3000L));
      return true;
    }

    World targetWorld = resolveRtpWorld(player, args);
    if (targetWorld == null) {
      return true;
    }

    String worldPermission = "fatsanutilities.rtp.world." + targetWorld.getName().toLowerCase();
    if (!PermissionAccess.has(player, worldPermission)) {
      player.sendMessage(configuration.locale().message("teleport.rtp-no-world-permission", sender)
        .replace("{world}", targetWorld.getName()));
      return true;
    }

    TeleportService.RtpOptions options = teleportService.optionsForWorld(targetWorld);
    if (!options.enabled()) {
      player.sendMessage(configuration.locale().message("teleport.rtp-world-disabled", sender)
        .replace("{world}", targetWorld.getName()));
      return true;
    }

    Player finalPlayer = player;
    FoliaScheduler.runAsync(plugin, () -> {
      TeleportService.RtpResult result = teleportService.findRandomSafeLocation(targetWorld, options);
      FoliaScheduler.runAtEntity(plugin, finalPlayer, () -> {
        if (!result.success()) {
          if (result.reason() == TeleportService.RtpFailReason.OUTSIDE_WORLD_BORDER) {
            finalPlayer.sendMessage(configuration.locale().message("teleport.rtp-fail-border", finalPlayer));
          } else {
            finalPlayer.sendMessage(configuration.locale().message("teleport.rtp-fail", finalPlayer));
          }
          return;
        }

        if (!CommandCost.charge(finalPlayer, configuration, economyService, "rtp")) {
          return;
        }

        long delayTicks = configuration.teleport().getLong("rtp.delay-ticks", 0L);
        boolean cancelOnMove = configuration.teleport().getBoolean("rtp.cancel-on-move", false);
        scheduleTeleport(
          finalPlayer,
          result.location(),
          delayTicks,
          cancelOnMove,
          configuration.locale().message("teleport.rtp-success", finalPlayer).replace("{world}", targetWorld.getName()),
          configuration.locale().message("teleport.teleport-cancelled-move", finalPlayer));
        long cooldownMillis = configuration.teleport().getLong(
          "rtp.cooldown-millis",
          configuration.cooldowns().getLong("commands.rtp", 5000L));
        cooldownService.set("rtp", finalPlayer.getUniqueId(), cooldownMillis);
      });
    });
    return true;
  }

  private void scheduleTeleport(
      Player player,
      Location target,
      long delayTicks,
      boolean cancelOnMove,
      String successMessage,
      String cancelMessage) {
    if (delayTicks <= 0) {
      player.teleportAsync(target);
      player.sendMessage(successMessage);
      return;
    }

    Location start = player.getLocation().clone();
    player.sendMessage(configuration.locale().message("teleport.teleport-delay", player)
      .replace("{seconds}", String.valueOf(delayTicks / 20.0)));

    FoliaScheduler.runAtEntityDelayed(plugin, player, () -> {
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
        player.sendMessage(configuration.locale().message("teleport.rtp-world-not-found", player));
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
      String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
      return Bukkit.getWorlds().stream()
        .map(World::getName)
        .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(prefix))
        .collect(Collectors.toList());
    }
    return List.of();
  }
}
