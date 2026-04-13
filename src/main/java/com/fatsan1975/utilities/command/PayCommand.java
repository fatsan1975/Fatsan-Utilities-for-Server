package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.economy.PayLimitService;
import com.fatsan1975.utilities.economy.event.EconomyTransferEvent;
import com.fatsan1975.utilities.logging.AuditLogger;
import com.fatsan1975.utilities.util.CommandGate;
import com.fatsan1975.utilities.util.CooldownService;
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
import org.bukkit.plugin.java.JavaPlugin;

public final class PayCommand implements CommandExecutor, TabCompleter {
  private final JavaPlugin plugin;
  private final EconomyService economyService;
  private final PayLimitService payLimitService;
  private final AuditLogger auditLogger;
  private final PluginConfiguration configuration;
  private final CooldownService cooldownService;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;

  public PayCommand(JavaPlugin plugin, EconomyService economyService, PayLimitService payLimitService, AuditLogger auditLogger,
                    PluginConfiguration configuration, CooldownService cooldownService,
                    ModuleManager modules, RateLimitService rateLimit) {
    this.plugin = plugin;
    this.economyService = economyService;
    this.payLimitService = payLimitService;
    this.auditLogger = auditLogger;
    this.configuration = configuration;
    this.cooldownService = cooldownService;
    this.modules = modules;
    this.rateLimit = rateLimit;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)
      || !CommandGate.checkModule(sender, configuration, modules, ModuleManager.Module.ECONOMY)) {
      return true;
    }
    if (!(sender instanceof Player player)) {
      sender.sendMessage(configuration.locale().message("general.player-only", sender));
      return true;
    }
    if (!CommandGate.checkRateLimit(player, configuration, rateLimit, "pay", "rate-limit.commands.pay")) {
      return true;
    }
    if (!economyService.trySetupIfNeeded()) {
      sender.sendMessage(configuration.locale().message("economy.not-ready", sender));
      return true;
    }
    if (args.length < 2) {
      sender.sendMessage(configuration.locale().message("general.invalid-usage", sender)
        .replace("{usage}", "/pay <player> <amount>"));
      return true;
    }

    long cooldownMillis = configuration.cooldowns().getLong("commands.pay", 3000L);
    long remaining = cooldownService.remainingMillis("pay", player.getUniqueId());
    if (remaining > 0) {
      sender.sendMessage(configuration.locale().message("general.cooldown", sender)
        .replace("{seconds}", String.valueOf(Math.ceil(remaining / 1000.0))));
      return true;
    }

    Optional<UUID> resolvedTarget = economyService.resolveUuid(args[0]);
    if (resolvedTarget.isEmpty()) {
      sender.sendMessage(configuration.locale().message("general.player-not-found", sender));
      return true;
    }
    if (resolvedTarget.get().equals(player.getUniqueId())) {
      sender.sendMessage(configuration.locale().message("economy.pay-self", sender));
      return true;
    }
    OfflinePlayer target = Bukkit.getOfflinePlayer(resolvedTarget.get());

    BigDecimal amount;
    try {
      amount = new BigDecimal(args[1]);
    } catch (NumberFormatException exception) {
      sender.sendMessage(configuration.locale().message("economy.invalid-amount", sender));
      return true;
    }

    BigDecimal minAmount = BigDecimal.valueOf(configuration.economy().getDouble("pay.min-amount", 1.0D));
    BigDecimal maxAmount = BigDecimal.valueOf(configuration.economy().getDouble("pay.max-amount", 1_000_000D));
    if (amount.signum() <= 0 || amount.compareTo(minAmount) < 0 || amount.compareTo(maxAmount) > 0) {
      sender.sendMessage(configuration.locale().message("economy.invalid-amount", sender));
      return true;
    }

    BigDecimal dailyLimit = BigDecimal.valueOf(configuration.economy().getDouble("pay.daily-limit", 5_000_000D));

    // Provider modunda persistent limit, consumer modunda RAM limit
    if (economyService.mode() == EconomyService.Mode.PROVIDER) {
      if (!economyService.tryReservePayLimit(player.getUniqueId(), amount, dailyLimit)) {
        sender.sendMessage(configuration.locale().message("economy.daily-limit-exceeded", sender));
        return true;
      }
    } else {
      if (!payLimitService.canSend(player.getUniqueId(), amount, dailyLimit)) {
        sender.sendMessage(configuration.locale().message("economy.daily-limit-exceeded", sender));
        return true;
      }
    }

    if (!economyService.has(player, amount)) {
      sender.sendMessage(configuration.locale().message("economy.not-enough-money", sender));
      return true;
    }

    EconomyService.TransferOutcome outcome = economyService.pay(player, target, amount);
    if (!outcome.success()) {
      sender.sendMessage(configuration.locale().message("economy.transfer-failed", sender));
      return true;
    }

    if (economyService.mode() != EconomyService.Mode.PROVIDER) {
      payLimitService.add(player.getUniqueId(), amount);
    }

    plugin.getServer().getPluginManager().callEvent(new EconomyTransferEvent(player, target, amount));
    auditLogger.log("PAY", player.getName() + " -> " + (target.getName() == null ? target.getUniqueId() : target.getName()) + " amount=" + amount);

    cooldownService.set("pay", player.getUniqueId(), cooldownMillis);
    player.sendMessage(configuration.locale().message("economy.pay-sent", sender)
      .replace("{player}", target.getName() == null ? args[0] : target.getName())
      .replace("{amount}", economyService.format(amount)));

    if (target.isOnline() && target.getPlayer() != null) {
      Player online = target.getPlayer();
      online.sendMessage(configuration.locale().message("economy.pay-received", online)
        .replace("{player}", player.getName())
        .replace("{amount}", economyService.format(amount)));
    }
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
