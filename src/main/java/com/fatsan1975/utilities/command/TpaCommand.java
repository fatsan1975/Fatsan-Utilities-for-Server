package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.util.CommandGate;
import com.fatsan1975.utilities.util.CooldownService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TpaCommand implements CommandExecutor {
  private final JavaPlugin plugin;
  private final PluginConfiguration configuration;
  private final CooldownService cooldownService;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;

  private final Map<UUID, TpaRequest> requests = new HashMap<>();

  public TpaCommand(JavaPlugin plugin, PluginConfiguration configuration, CooldownService cooldownService,
                    ModuleManager modules, RateLimitService rateLimit) {
    this.plugin = plugin;
    this.configuration = configuration;
    this.cooldownService = cooldownService;
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

    String cmd = command.getName().toLowerCase();
    if (!CommandGate.checkRateLimit(player, configuration, rateLimit, cmd, "rate-limit.commands." + cmd)) {
      return true;
    }

    return switch (cmd) {
      case "tpa" -> handleRequest(player, args);
      case "tpaccept" -> handleAccept(player);
      case "tpdeny" -> handleDeny(player);
      default -> false;
    };
  }

  private boolean handleRequest(Player sender, String[] args) {
    if (args.length < 1) {
      sender.sendMessage(configuration.message("general.invalid-usage").replace("{usage}", "/tpa <oyuncu>"));
      return true;
    }

    long remaining = cooldownService.remainingMillis("tpa", sender.getUniqueId());
    if (remaining > 0) {
      sender.sendMessage(configuration.message("general.cooldown").replace("{seconds}", String.valueOf(Math.ceil(remaining / 1000.0))));
      return true;
    }

    Player target = Bukkit.getPlayerExact(args[0]);
    if (target == null || target.getUniqueId().equals(sender.getUniqueId())) {
      sender.sendMessage(configuration.message("general.player-not-found"));
      return true;
    }

    boolean overwrite = configuration.teleport().getBoolean("tpa.overwrite-request", true);
    if (!overwrite && requests.containsKey(target.getUniqueId())) {
      sender.sendMessage(configuration.message("teleport.tpa-request-exists"));
      return true;
    }

    long expireMillis = configuration.teleport().getLong("tpa.request-expire-millis", 30000L);
    requests.put(target.getUniqueId(), new TpaRequest(sender.getUniqueId(), Instant.now().toEpochMilli() + expireMillis));

    sender.sendMessage(configuration.message("teleport.tpa-request-sent").replace("{player}", target.getName()));
    target.sendMessage(configuration.message("teleport.tpa-request-received").replace("{player}", sender.getName()));

    cooldownService.set("tpa", sender.getUniqueId(), configuration.cooldowns().getLong("commands.tpa", 3000L));
    return true;
  }

  private boolean handleAccept(Player target) {
    TpaRequest request = requests.remove(target.getUniqueId());
    if (request == null || request.expiresAt < Instant.now().toEpochMilli()) {
      target.sendMessage(configuration.message("teleport.tpa-request-none"));
      return true;
    }

    Player sender = plugin.getServer().getPlayer(request.sender);
    if (sender == null) {
      target.sendMessage(configuration.message("general.player-not-found"));
      return true;
    }

    long delayTicks = configuration.teleport().getLong("tpa.teleport-delay-ticks", 0L);
    boolean cancelOnMove = configuration.teleport().getBoolean("tpa.cancel-on-move", true);

    if (delayTicks <= 0) {
      sender.teleportAsync(target.getLocation());
      sender.sendMessage(configuration.message("teleport.tpa-accepted").replace("{player}", target.getName()));
      target.sendMessage(configuration.message("teleport.tpa-you-accepted").replace("{player}", sender.getName()));
      return true;
    }

    Location start = sender.getLocation().clone();
    sender.sendMessage(configuration.message("teleport.teleport-delay").replace("{seconds}", String.valueOf(delayTicks / 20.0)));
    target.sendMessage(configuration.message("teleport.tpa-you-accepted").replace("{player}", sender.getName()));

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (!sender.isOnline()) {
        return;
      }
      if (cancelOnMove && moved(start, sender.getLocation())) {
        sender.sendMessage(configuration.message("teleport.teleport-cancelled-move"));
        return;
      }
      sender.teleportAsync(target.getLocation());
      sender.sendMessage(configuration.message("teleport.tpa-accepted").replace("{player}", target.getName()));
    }, delayTicks);
    return true;
  }

  private boolean moved(Location first, Location second) {
    return first.getBlockX() != second.getBlockX()
      || first.getBlockY() != second.getBlockY()
      || first.getBlockZ() != second.getBlockZ();
  }

  private boolean handleDeny(Player target) {
    TpaRequest request = requests.remove(target.getUniqueId());
    if (request == null || request.expiresAt < Instant.now().toEpochMilli()) {
      target.sendMessage(configuration.message("teleport.tpa-request-none"));
      return true;
    }

    Player sender = plugin.getServer().getPlayer(request.sender);
    if (sender != null) {
      sender.sendMessage(configuration.message("teleport.tpa-denied").replace("{player}", target.getName()));
    }
    target.sendMessage(configuration.message("teleport.tpa-you-denied"));
    return true;
  }

  private record TpaRequest(UUID sender, long expiresAt) {}
}
