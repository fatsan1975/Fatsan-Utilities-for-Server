package com.fatsan1975.utilities.economy;

import com.fatsan1975.utilities.config.PluginConfiguration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.OfflinePlayer;

public final class BalanceTopCacheService {
  private final EconomyService economyService;
  private final PluginConfiguration configuration;

  private List<OfflinePlayer> cache = List.of();
  private long expiresAt = 0L;

  public BalanceTopCacheService(EconomyService economyService, PluginConfiguration configuration) {
    this.economyService = economyService;
    this.configuration = configuration;
  }

  public List<OfflinePlayer> getTop(int limit) {
    long now = Instant.now().toEpochMilli();
    if (now >= expiresAt || cache.isEmpty()) {
      refresh(limit);
    }
    int size = Math.min(limit, cache.size());
    return new ArrayList<>(cache.subList(0, size));
  }

  public void refresh(int limit) {
    this.cache = economyService.topBalances(limit);
    long ttl = configuration.economy().getLong("balancetop.cache-ttl-millis", 30000L);
    this.expiresAt = Instant.now().toEpochMilli() + Math.max(1000L, ttl);
  }
}
