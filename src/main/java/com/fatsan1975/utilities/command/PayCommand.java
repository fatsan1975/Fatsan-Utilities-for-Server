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
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PayCommand implements CommandExecutor {
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
      sender.sendMessage(configuration.message("general.player-only"));
      return true;
    }
    if (!CommandGate.checkRateLimit(player, configuration, rateLimit, "pay", "rate-limit.commands.pay")) {
      return true;
    }
    if (args.length < 2) {
      sender.sendMessage(configuration.message("general.invalid-usage").replace("{usage}", "/pay <oyuncu> <miktar>"));
      return true;
    }

    long cooldownMillis = configuration.cooldowns().getLong("commands.pay", 3000L);
    long remaining = cooldownService.remainingMillis("pay", player.getUniqueId());
    if (remaining > 0) {
      sender.sendMessage(configuration.message("general.cooldown").replace("{seconds}", String.valueOf(Math.ceil(remaining / 1000.0))));
      return true;
    }

    OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
    if (target == null || target.getName() == null || target.getUniqueId().equals(player.getUniqueId())) {
      sender.sendMessage(configuration.message("general.player-not-found"));
      return true;
    }

    double amount;
    try {
      amount = Double.parseDouble(args[1]);
    } catch (NumberFormatException exception) {
      sender.sendMessage(configuration.message("economy.invalid-amount"));
      return true;
    }

    double minAmount = configuration.economy().getDouble("pay.min-amount", 1.0D);
    double maxAmount = configuration.economy().getDouble("pay.max-amount", 1_000_000D);
    if (amount <= 0D || amount < minAmount || amount > maxAmount) {
      sender.sendMessage(configuration.message("economy.invalid-amount"));
      return true;
    }

    double dailyLimit = configuration.economy().getDouble("pay.daily-limit", 5_000_000D);
    if (!payLimitService.canSend(player.getUniqueId(), amount, dailyLimit)) {
      sender.sendMessage(configuration.message("economy.daily-limit-exceeded"));
      return true;
    }

    if (!economyService.has(player, amount)) {
      sender.sendMessage(configuration.message("economy.not-enough-money"));
      return true;
    }

    EconomyResponse result = economyService.pay(player, target, amount);
    if (!result.transactionSuccess()) {
      sender.sendMessage(configuration.message("economy.transfer-failed"));
      return true;
    }

    payLimitService.add(player.getUniqueId(), amount);
    plugin.getServer().getPluginManager().callEvent(new EconomyTransferEvent(player, target, amount));
    auditLogger.log("PAY", player.getName() + " -> " + target.getName() + " amount=" + amount);

    cooldownService.set("pay", player.getUniqueId(), cooldownMillis);
    player.sendMessage(configuration.message("economy.pay-sent")
      .replace("{player}", target.getName())
      .replace("{amount}", economyService.format(amount)));

    if (target.isOnline() && target.getPlayer() != null) {
      target.getPlayer().sendMessage(configuration.message("economy.pay-received")
        .replace("{player}", player.getName())
        .replace("{amount}", economyService.format(amount)));
    }
    return true;
  }
}
