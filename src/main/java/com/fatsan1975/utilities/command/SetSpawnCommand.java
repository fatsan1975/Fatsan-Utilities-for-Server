package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.teleport.SpawnService;
import com.fatsan1975.utilities.util.CommandGate;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SetSpawnCommand implements CommandExecutor {
  private final PluginConfiguration configuration;
  private final SpawnService spawnService;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;

  public SetSpawnCommand(PluginConfiguration configuration, SpawnService spawnService,
                         ModuleManager modules, RateLimitService rateLimit) {
    this.configuration = configuration;
    this.spawnService = spawnService;
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
      sender.sendMessage(configuration.locale().message("general.player-only", sender));
      return true;
    }
    if (!CommandGate.checkRateLimit(player, configuration, rateLimit, "setspawn", "rate-limit.commands.setspawn")) {
      return true;
    }
    spawnService.setSpawn(player.getLocation());
    player.sendMessage(configuration.locale().message("teleport.setspawn-success", player)
      .replace("{world}", player.getWorld().getName()));
    return true;
  }
}
