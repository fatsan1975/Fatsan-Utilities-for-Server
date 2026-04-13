package com.fatsan1975.utilities.util;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.economy.EconomyService;
import java.math.BigDecimal;
import org.bukkit.entity.Player;

public final class CommandCost {
  private CommandCost() {}

  public static BigDecimal amount(PluginConfiguration configuration, String key) {
    return BigDecimal.valueOf(configuration.teleport().getDouble("command-costs." + key, 100.0D));
  }

  public static boolean charge(Player player, PluginConfiguration configuration, EconomyService economy, String key) {
    BigDecimal cost = amount(configuration, key);
    if (cost.signum() <= 0) {
      return true;
    }
    if (economy == null || !economy.trySetupIfNeeded() || !economy.isReady()) {
      player.sendMessage(configuration.locale().message("economy.not-ready", player));
      return false;
    }
    if (!economy.withdraw(player, cost)) {
      player.sendMessage(configuration.locale().message("economy.not-enough-money", player));
      return false;
    }
    return true;
  }
}
