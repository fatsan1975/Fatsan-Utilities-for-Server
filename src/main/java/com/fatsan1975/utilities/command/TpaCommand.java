package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.core.scheduler.FoliaScheduler;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.util.CommandCost;
import com.fatsan1975.utilities.util.CommandGate;
import com.fatsan1975.utilities.util.CooldownService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TpaCommand implements CommandExecutor, TabCompleter {
  private final JavaPlugin plugin;
  private final PluginConfiguration configuration;
  private final CooldownService cooldownService;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;
  private final EconomyService economyService;

  private final Map<UUID, TpaRequest> requests = new ConcurrentHashMap<>();

  public TpaCommand(
      JavaPlugin plugin,
      PluginConfiguration configuration,
      CooldownService cooldownService,
      ModuleManager modules,
      RateLimitService rateLimit,
      EconomyService economyService) {
    this.plugin = plugin;
    this.configuration = configuration;
    this.cooldownService = cooldownService;
    this.modules = modules;
    this.rateLimit = rateLimit;
    this.economyService = economyService;
  }

  public void clear(UUID uuid) {
    requests.remove(uuid);
    requests.values().removeIf(request -> request.sender.equals(uuid));
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
      sender.sendMessage(configuration.locale().message("general.invalid-usage", sender)
        .replace("{usage}", "/tpa <player>"));
      return true;
    }

    long remaining = cooldownService.remainingMillis("tpa", sender.getUniqueId());
    if (remaining > 0) {
      sender.sendMessage(configuration.locale().message("general.cooldown", sender)
        .replace("{seconds}", String.valueOf(Math.ceil(remaining / 1000.0))));
      return true;
    }

    Player target = Bukkit.getPlayerExact(args[0]);
    if (target == null) {
      sender.sendMessage(configuration.locale().message("general.player-not-found", sender));
      return true;
    }
    if (target.getUniqueId().equals(sender.getUniqueId())) {
      sender.sendMessage(configuration.locale().message("teleport.tpa-self", sender));
      return true;
    }

    boolean overwrite = configuration.teleport().getBoolean("tpa.overwrite-request", true);
    if (!overwrite && requests.containsKey(target.getUniqueId())) {
      sender.sendMessage(configuration.locale().message("teleport.tpa-request-exists", sender));
      return true;
    }

    long expireMillis = configuration.teleport().getLong("tpa.request-expire-millis", 30000L);
    requests.put(target.getUniqueId(), new TpaRequest(sender.getUniqueId(), Instant.now().toEpochMilli() + expireMillis));

    sender.sendMessage(configuration.locale().message("teleport.tpa-request-sent", sender)
      .replace("{player}", target.getName()));
    target.sendMessage(configuration.locale().message("teleport.tpa-request-received", target)
      .replace("{player}", sender.getName()));

    cooldownService.set("tpa", sender.getUniqueId(), configuration.cooldowns().getLong("commands.tpa", 3000L));
    return true;
  }

  private boolean handleAccept(Player target) {
    TpaRequest request = requests.remove(target.getUniqueId());
    if (request == null || request.expiresAt < Instant.now().toEpochMilli()) {
      target.sendMessage(configuration.locale().message("teleport.tpa-request-none", target));
      return true;
    }

    Player sender = plugin.getServer().getPlayer(request.sender);
    if (sender == null) {
      target.sendMessage(configuration.locale().message("general.player-not-found", target));
      return true;
    }

    if (!CommandCost.charge(sender, configuration, economyService, "tpa")) {
      target.sendMessage(configuration.locale().message("teleport.tpa-request-cancelled", target)
        .replace("{player}", sender.getName()));
      return true;
    }

    long delayTicks = configuration.teleport().getLong("tpa.teleport-delay-ticks", 0L);
    boolean cancelOnMove = configuration.teleport().getBoolean("tpa.cancel-on-move", true);

    if (delayTicks <= 0) {
      teleport(sender, target.getLocation());
      sender.sendMessage(configuration.locale().message("teleport.tpa-accepted", sender)
        .replace("{player}", target.getName()));
      target.sendMessage(configuration.locale().message("teleport.tpa-you-accepted", target)
        .replace("{player}", sender.getName()));
      return true;
    }

    Location start = sender.getLocation().clone();
    sender.sendMessage(configuration.locale().message("teleport.teleport-delay", sender)
      .replace("{seconds}", String.valueOf(delayTicks / 20.0)));
    target.sendMessage(configuration.locale().message("teleport.tpa-you-accepted", target)
      .replace("{player}", sender.getName()));

    FoliaScheduler.runAtEntityDelayed(plugin, sender, () -> {
      if (!sender.isOnline()) {
        return;
      }
      if (cancelOnMove && moved(start, sender.getLocation())) {
        sender.sendMessage(configuration.locale().message("teleport.teleport-cancelled-move", sender));
        return;
      }
      teleport(sender, target.getLocation());
      sender.sendMessage(configuration.locale().message("teleport.tpa-accepted", sender)
        .replace("{player}", target.getName()));
    }, delayTicks);
    return true;
  }

  private void teleport(Player player, Location destination) {
    player.teleportAsync(destination);
  }

  private boolean moved(Location first, Location second) {
    return first.getBlockX() != second.getBlockX()
      || first.getBlockY() != second.getBlockY()
      || first.getBlockZ() != second.getBlockZ();
  }

  private boolean handleDeny(Player target) {
    TpaRequest request = requests.remove(target.getUniqueId());
    if (request == null || request.expiresAt < Instant.now().toEpochMilli()) {
      target.sendMessage(configuration.locale().message("teleport.tpa-request-none", target));
      return true;
    }

    Player sender = plugin.getServer().getPlayer(request.sender);
    if (sender != null) {
      sender.sendMessage(configuration.locale().message("teleport.tpa-denied", sender)
        .replace("{player}", target.getName()));
    }
    target.sendMessage(configuration.locale().message("teleport.tpa-you-denied", target));
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (command.getName().equalsIgnoreCase("tpa") && args.length == 1) {
      String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
      return Bukkit.getOnlinePlayers().stream()
        .map(Player::getName)
        .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(prefix))
        .collect(Collectors.toList());
    }
    return List.of();
  }

  private record TpaRequest(UUID sender, long expiresAt) {}
}
