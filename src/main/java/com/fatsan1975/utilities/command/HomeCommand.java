package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.core.scheduler.FoliaScheduler;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.teleport.HomeService;
import com.fatsan1975.utilities.util.CommandCost;
import com.fatsan1975.utilities.util.CommandGate;
import com.fatsan1975.utilities.util.CooldownService;
import com.fatsan1975.utilities.util.PermissionAccess;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HomeCommand implements CommandExecutor, TabCompleter {
  public enum Mode { HOME, SETHOME, DELHOME, HOMES }

  private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9_\\-]{1,24}");

  private final JavaPlugin plugin;
  private final PluginConfiguration configuration;
  private final HomeService homeService;
  private final CooldownService cooldownService;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;
  private final EconomyService economy;
  private final Mode mode;

  public HomeCommand(
      JavaPlugin plugin,
      PluginConfiguration configuration,
      HomeService homeService,
      CooldownService cooldownService,
      ModuleManager modules,
      RateLimitService rateLimit,
      EconomyService economy,
      Mode mode) {
    this.plugin = plugin;
    this.configuration = configuration;
    this.homeService = homeService;
    this.cooldownService = cooldownService;
    this.modules = modules;
    this.rateLimit = rateLimit;
    this.economy = economy;
    this.mode = mode;
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
    String rateKey = rateKey();
    if (!CommandGate.checkRateLimit(player, configuration, rateLimit, rateKey, "rate-limit.commands." + rateKey)) {
      return true;
    }

    switch (mode) {
      case HOMES -> handleList(player);
      case SETHOME -> handleSet(player, args);
      case DELHOME -> handleDelete(player, args);
      case HOME -> handleTeleport(player, args);
    }
    return true;
  }

  private String rateKey() {
    return switch (mode) {
      case HOME -> "home";
      case SETHOME -> "sethome";
      case DELHOME -> "delhome";
      case HOMES -> "homes";
    };
  }

  private void handleList(Player player) {
    Set<String> list = homeService.list(player.getUniqueId());
    if (list.isEmpty()) {
      player.sendMessage(configuration.locale().message("teleport.homes-empty", player));
      return;
    }
    player.sendMessage(configuration.locale().message("teleport.homes-list", player)
      .replace("{homes}", String.join(", ", list))
      .replace("{count}", String.valueOf(list.size()))
      .replace("{max}", String.valueOf(maxHomes(player))));
  }

  private void handleSet(Player player, String[] args) {
    String name = (args.length > 0 ? args[0] : "home").toLowerCase(Locale.ROOT);
    if (!VALID_NAME.matcher(name).matches()) {
      player.sendMessage(configuration.locale().message("teleport.home-name-invalid", player));
      return;
    }

    long cdRemaining = cooldownService.remainingMillis("sethome", player.getUniqueId());
    if (cdRemaining > 0) {
      player.sendMessage(configuration.locale().message("general.cooldown", player)
        .replace("{seconds}", String.valueOf(Math.ceil(cdRemaining / 1000.0))));
      return;
    }

    boolean replacing = homeService.exists(player.getUniqueId(), name);
    if (!replacing) {
      int max = maxHomes(player);
      int current = homeService.count(player.getUniqueId());
      if (current >= max) {
        player.sendMessage(configuration.locale().message("teleport.home-limit-reached", player)
          .replace("{max}", String.valueOf(max)));
        return;
      }
    }

    if (!CommandCost.charge(player, configuration, economy, "sethome")) {
      return;
    }

    homeService.set(player.getUniqueId(), name, player.getLocation());
    long cd = configuration.teleport().getLong("home.sethome-cooldown-millis", 0L);
    if (cd > 0) {
      cooldownService.set("sethome", player.getUniqueId(), cd);
    }
    player.sendMessage(configuration.locale().message("teleport.sethome-success", player)
      .replace("{name}", name));
  }

  private void handleDelete(Player player, String[] args) {
    String name = (args.length > 0 ? args[0] : "home").toLowerCase(Locale.ROOT);
    if (!homeService.delete(player.getUniqueId(), name)) {
      player.sendMessage(configuration.locale().message("teleport.home-not-found", player)
        .replace("{name}", name));
      return;
    }
    player.sendMessage(configuration.locale().message("teleport.delhome-success", player)
      .replace("{name}", name));
  }

  private void handleTeleport(Player player, String[] args) {
    String name = (args.length > 0 ? args[0] : "home").toLowerCase(Locale.ROOT);
    long remaining = cooldownService.remainingMillis("home", player.getUniqueId());
    if (remaining > 0) {
      player.sendMessage(configuration.locale().message("general.cooldown", player)
        .replace("{seconds}", String.valueOf(Math.ceil(remaining / 1000.0))));
      return;
    }

    Location target = homeService.get(player.getUniqueId(), name);
    if (target == null) {
      player.sendMessage(configuration.locale().message("teleport.home-not-found", player)
        .replace("{name}", name));
      return;
    }

    if (!CommandCost.charge(player, configuration, economy, "home")) {
      return;
    }

    long delayTicks = configuration.teleport().getLong("home.delay-ticks", 0L);
    boolean cancelOnMove = configuration.teleport().getBoolean("home.cancel-on-move", true);
    long cdMillis = configuration.teleport().getLong(
      "home.teleport-cooldown-millis",
      configuration.cooldowns().getLong("commands.home", 5000L));

    String successMessage = configuration.locale().message("teleport.home-success", player)
      .replace("{name}", name);
    String cancelMessage = configuration.locale().message("teleport.teleport-cancelled-move", player);

    if (delayTicks <= 0) {
      player.teleportAsync(target);
      player.sendMessage(successMessage);
      cooldownService.set("home", player.getUniqueId(), cdMillis);
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
      cooldownService.set("home", player.getUniqueId(), cdMillis);
    }, delayTicks);
  }

  private boolean moved(Location first, Location second) {
    return first.getBlockX() != second.getBlockX()
      || first.getBlockY() != second.getBlockY()
      || first.getBlockZ() != second.getBlockZ();
  }

  private int maxHomes(Player player) {
    if (PermissionAccess.has(player, "fatsanutilities.homes.unlimited")) {
      return Integer.MAX_VALUE;
    }
    int max = configuration.teleport().getInt("home.default-max", 3);
    for (int i = max + 1; i <= 128; i++) {
      if (PermissionAccess.has(player, "fatsanutilities.homes.max." + i)) {
        max = i;
      }
    }
    return max;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (!(sender instanceof Player player)) {
      return List.of();
    }
    if ((mode == Mode.HOME || mode == Mode.DELHOME) && args.length == 1) {
      String prefix = args[0].toLowerCase(Locale.ROOT);
      return homeService.list(player.getUniqueId()).stream()
        .filter(home -> home.startsWith(prefix))
        .collect(Collectors.toList());
    }
    return List.of();
  }
}
