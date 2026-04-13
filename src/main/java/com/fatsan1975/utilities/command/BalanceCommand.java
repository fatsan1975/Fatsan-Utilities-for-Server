package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.util.CommandGate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class BalanceCommand implements CommandExecutor, TabCompleter {
  private final EconomyService economyService;
  private final PluginConfiguration configuration;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;

  public BalanceCommand(EconomyService economyService, PluginConfiguration configuration, ModuleManager modules, RateLimitService rateLimit) {
    this.economyService = economyService;
    this.configuration = configuration;
    this.modules = modules;
    this.rateLimit = rateLimit;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)
      || !CommandGate.checkModule(sender, configuration, modules, ModuleManager.Module.ECONOMY)) {
      return true;
    }

    if (sender instanceof Player player && !CommandGate.checkRateLimit(player, configuration, rateLimit, "balance", "rate-limit.commands.balance")) {
      return true;
    }
    if (!economyService.trySetupIfNeeded()) {
      sender.sendMessage(configuration.locale().message("economy.not-ready", sender));
      return true;
    }

    if (args.length == 0) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(configuration.locale().message("general.player-only", sender));
        return true;
      }
      if (economyService.mode() == EconomyService.Mode.PROVIDER) {
        economyService.own().ensureAccount(player);
      }
      BigDecimal bal = economyService.balance(player);
      sender.sendMessage(configuration.locale().message("economy.balance-self", sender)
        .replace("{amount}", economyService.format(bal)));
      return true;
    }

    Optional<UUID> resolved = economyService.resolveUuid(args[0]);
    if (resolved.isEmpty()) {
      sender.sendMessage(configuration.locale().message("general.player-not-found", sender));
      return true;
    }
    OfflinePlayer target = Bukkit.getOfflinePlayer(resolved.get());
    BigDecimal bal = economyService.balance(target);
    sender.sendMessage(configuration.locale().message("economy.balance-other", sender)
      .replace("{player}", target.getName() == null ? args[0] : target.getName())
      .replace("{amount}", economyService.format(bal)));
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(prefix))
          .collect(Collectors.toList());
    }
    return List.of();
  }
}
