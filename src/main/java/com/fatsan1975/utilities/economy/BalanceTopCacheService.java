package com.fatsan1975.utilities.economy;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.economy.model.TopEntry;
import java.time.Instant;
import java.util.List;

/** TopEntry cache — provider modunda SQL query tabanlı, düşük maliyetli. */
public final class BalanceTopCacheService {
  private final EconomyService economyService;
  private final PluginConfiguration configuration;

  private volatile List<TopEntry> cache = List.of();
  private volatile long expiresAt = 0L;

  public BalanceTopCacheService(EconomyService economyService, PluginConfiguration configuration) {
    this.economyService = economyService;
    this.configuration = configuration;
  }

  public synchronized List<TopEntry> getTop(int limit) {
    long now = Instant.now().toEpochMilli();
    if (now >= expiresAt || cache.size() < Math.min(limit, 10)) {
      refresh(limit);
    }
    return cache.subList(0, Math.min(limit, cache.size()));
  }

  public synchronized void refresh(int limit) {
    this.cache = economyService.top(limit, 0);
    long ttl = configuration.economy().getLong("balancetop.cache-ttl-millis", 30000L);
    this.expiresAt = Instant.now().toEpochMilli() + Math.max(1000L, ttl);
  }
}
