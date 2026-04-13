package com.fatsan1975.utilities.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin açılışında harici Vault provider gecikmeli register olursa devreye girer.
 * PROVIDER modunda bu listener no-op’tur çünkü provider biziz.
 */
public final class EconomyHookListener implements Listener {
  private final JavaPlugin plugin;
  private final EconomyService economyService;

  public EconomyHookListener(JavaPlugin plugin, EconomyService economyService) {
    this.plugin = plugin;
    this.economyService = economyService;
  }

  @EventHandler
  public void onServiceRegister(ServiceRegisterEvent event) {
    if (economyService.mode() == EconomyService.Mode.PROVIDER) {
      return;
    }
    try {
      if (!Economy.class.isAssignableFrom(event.getProvider().getService())) {
        return;
      }
      if (economyService.trySetupIfNeeded()) {
        plugin.getLogger().info("Vault ekonomi servisine bağlanıldı: " + event.getProvider().getPlugin().getName());
      }
    } catch (NoClassDefFoundError ignored) {
      // Vault yoksa
    }
  }
}
