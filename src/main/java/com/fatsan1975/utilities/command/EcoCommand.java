package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.economy.model.Account;
import com.fatsan1975.utilities.logging.AuditLogger;
import com.fatsan1975.utilities.util.CommandGate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** /eco give|take|set|reset <oyuncu> [miktar] — sadece PROVIDER modunda tam işlevsel. */
public final class EcoCommand implements CommandExecutor, TabCompleter {
  private final EconomyService economyService;
  private final PluginConfiguration configuration;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;
  private final AuditLogger auditLogger;

  public EcoCommand(EconomyService economyService, PluginConfiguration configuration,
                    ModuleManager modules, RateLimitService rateLimit, AuditLogger auditLogger) {
    this.economyService = economyService;
    this.configuration = configuration;
    this.modules = modules;
    this.rateLimit = rateLimit;
    this.auditLogger = auditLogger;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)
      || !CommandGate.checkModule(sender, configuration, modules, ModuleManager.Module.ECONOMY)) {
      return true;
    }
    if (sender instanceof Player player
      && !CommandGate.checkRateLimit(player, configuration, rateLimit, "eco", "rate-limit.commands.eco")) {
      return true;
    }
    if (!economyService.trySetupIfNeeded()) {
      sender.sendMessage(configuration.locale().message("economy.not-ready", sender));
      return true;
    }
    if (economyService.mode() != EconomyService.Mode.PROVIDER) {
      sender.sendMessage(configuration.locale().message("economy.not-ready", sender));
      return true;
    }

    if (args.length < 2) {
      sender.sendMessage(configuration.locale().message("general.invalid-usage", sender)
        .replace("{usage}", "/eco <give|take|set|reset> <player> [amount]"));
      return true;
    }

    String action = args[0].toLowerCase();
    Optional<UUID> uuid = economyService.resolveUuid(args[1]);
    if (uuid.isEmpty()) {
      sender.sendMessage(configuration.locale().message("general.player-not-found", sender));
      return true;
    }
    String name = Bukkit.getOfflinePlayer(uuid.get()).getName();
    if (name == null) name = args[1];
    economyService.own().ensureAccount(uuid.get(), name);

    switch (action) {
      case "give" -> {
        BigDecimal amount = parseAmount(sender, args);
        if (amount == null) return true;
        Account acc = economyService.own().deposit(uuid.get(), amount);
        sender.sendMessage(configuration.locale().message("economy.admin-give", sender)
          .replace("{player}", name)
          .replace("{amount}", economyService.format(amount))
          .replace("{balance}", economyService.format(acc.balance())));
        auditLogger.log("ECO_GIVE", sender.getName() + " -> " + name + " amount=" + amount);
      }
      case "take" -> {
        BigDecimal amount = parseAmount(sender, args);
        if (amount == null) return true;
        Account acc = economyService.own().withdraw(uuid.get(), amount);
        sender.sendMessage(configuration.locale().message("economy.admin-take", sender)
          .replace("{player}", name)
          .replace("{amount}", economyService.format(amount))
          .replace("{balance}", economyService.format(acc.balance())));
        auditLogger.log("ECO_TAKE", sender.getName() + " -> " + name + " amount=" + amount);
      }
      case "set" -> {
        BigDecimal amount = parseAmount(sender, args);
        if (amount == null) return true;
        Account acc = economyService.own().set(uuid.get(), amount);
        sender.sendMessage(configuration.locale().message("economy.admin-set", sender)
          .replace("{player}", name)
          .replace("{amount}", economyService.format(acc.balance())));
        auditLogger.log("ECO_SET", sender.getName() + " -> " + name + " amount=" + amount);
      }
      case "reset" -> {
        Account acc = economyService.own().reset(uuid.get());
        sender.sendMessage(configuration.locale().message("economy.admin-reset", sender)
          .replace("{player}", name));
        auditLogger.log("ECO_RESET", sender.getName() + " -> " + name + " balance=" + acc.balance());
      }
      default -> sender.sendMessage(configuration.locale().message("general.invalid-usage", sender)
        .replace("{usage}", "/eco <give|take|set|reset> <player> [amount]"));
    }
    return true;
  }

  private BigDecimal parseAmount(CommandSender sender, String[] args) {
    if (args.length < 3) {
      sender.sendMessage(configuration.locale().message("economy.invalid-amount", sender));
      return null;
    }
    try {
      BigDecimal amount = new BigDecimal(args[2]);
      if (amount.signum() < 0) {
        sender.sendMessage(configuration.locale().message("economy.invalid-amount", sender));
        return null;
      }
      return amount;
    } catch (NumberFormatException exception) {
      sender.sendMessage(configuration.locale().message("economy.invalid-amount", sender));
      return null;
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
      return java.util.stream.Stream.of("give", "take", "set", "reset")
          .filter(s -> s.startsWith(prefix))
          .collect(java.util.stream.Collectors.toList());
    }
    if (args.length == 2) {
      String prefix = args[1].toLowerCase(java.util.Locale.ROOT);
      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(prefix))
          .collect(java.util.stream.Collectors.toList());
    }
    return List.of();
  }
}
