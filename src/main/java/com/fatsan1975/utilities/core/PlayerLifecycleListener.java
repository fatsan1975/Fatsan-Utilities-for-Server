package com.fatsan1975.utilities.core;

import com.fatsan1975.utilities.command.TpaCommand;
import com.fatsan1975.utilities.economy.EconomyService;
import com.fatsan1975.utilities.util.CooldownService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Oyuncu join/quit yaşam döngüsü: ensureAccount, cooldown/rate-limit/TPA temizliği. */
public final class PlayerLifecycleListener implements Listener {
  private final EconomyService economyService;
  private final CooldownService cooldownService;
  private final RateLimitService rateLimitService;
  private final TpaCommand tpaCommand;

  public PlayerLifecycleListener(EconomyService economyService, CooldownService cooldownService,
                                 RateLimitService rateLimitService, TpaCommand tpaCommand) {
    this.economyService = economyService;
    this.cooldownService = cooldownService;
    this.rateLimitService = rateLimitService;
    this.tpaCommand = tpaCommand;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (economyService.mode() == EconomyService.Mode.PROVIDER && economyService.own() != null) {
      economyService.own().ensureAccount(event.getPlayer());
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    var uuid = event.getPlayer().getUniqueId();
    cooldownService.clear(uuid);
    rateLimitService.clear(uuid);
    tpaCommand.clear(uuid);
  }
}
