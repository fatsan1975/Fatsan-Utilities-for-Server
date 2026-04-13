package com.fatsan1975.utilities.core;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe rate limit servis. */
public final class RateLimitService {
  private final Map<String, Map<UUID, Long>> actionTimes = new ConcurrentHashMap<>();

  public long remainingMillis(String key, UUID player) {
    Long next = actionTimes.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>()).get(player);
    if (next == null) {
      return 0L;
    }
    return Math.max(0L, next - Instant.now().toEpochMilli());
  }

  public void mark(String key, UUID player, long intervalMillis) {
    actionTimes.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>())
      .put(player, Instant.now().toEpochMilli() + Math.max(0L, intervalMillis));
  }

  public void clear(UUID player) {
    actionTimes.values().forEach(map -> map.remove(player));
  }
}
