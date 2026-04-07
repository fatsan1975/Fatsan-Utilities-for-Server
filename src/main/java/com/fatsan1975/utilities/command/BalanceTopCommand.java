package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.economy.BalanceTopCacheService;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.util.CommandGate;
import java.util.List;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BalanceTopCommand implements CommandExecutor {
  private final EconomyService economyService;
  private final BalanceTopCacheService topCache;
  private final PluginConfiguration configuration;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;

  public BalanceTopCommand(EconomyService economyService, BalanceTopCacheService topCache,
                           PluginConfiguration configuration, ModuleManager modules, RateLimitService rateLimit) {
    this.economyService = economyService;
    this.topCache = topCache;
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
    if (sender instanceof Player player && !CommandGate.checkRateLimit(player, configuration, rateLimit, "balancetop", "rate-limit.commands.balancetop")) {
      return true;
    }
    if (!economyService.trySetupIfNeeded()) {
      sender.sendMessage(configuration.message("economy.not-ready"));
      return true;
    }

    int page = 1;
    if (args.length > 0) {
      try {
        page = Math.max(1, Integer.parseInt(args[0]));
      } catch (NumberFormatException ignored) {
        page = 1;
      }
    }

    int pageSize = Math.max(1, configuration.economy().getInt("balancetop.page-size", 10));
    int cacheLimit = Math.max(pageSize * page, configuration.economy().getInt("balancetop.max-entries", 50));
    List<OfflinePlayer> top = topCache.getTop(cacheLimit);

    int start = (page - 1) * pageSize;
    if (start >= top.size()) {
      sender.sendMessage(configuration.message("economy.balancetop-empty-page"));
      return true;
    }

    int end = Math.min(start + pageSize, top.size());
    sender.sendMessage(configuration.message("economy.balancetop-header").replace("{page}", String.valueOf(page)));

    for (int i = start; i < end; i++) {
      OfflinePlayer player = top.get(i);
      String line = configuration.message("economy.balancetop-line")
        .replace("{rank}", String.valueOf(i + 1))
        .replace("{player}", player.getName() == null ? "Bilinmiyor" : player.getName())
        .replace("{amount}", economyService.format(economyService.balance(player)));
      sender.sendMessage(line);
    }
    return true;
  }
}
