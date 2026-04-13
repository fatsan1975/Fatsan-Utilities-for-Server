package com.fatsan1975.utilities.util;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe cooldown servis. Folia paralel region thread’leri için {@link ConcurrentHashMap} kullanılır. */
public final class CooldownService {
  private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

  public long remainingMillis(String key, UUID uuid) {
    Long expireAt = cooldowns.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>()).get(uuid);
    if (expireAt == null) {
      return 0L;
    }
    long now = Instant.now().toEpochMilli();
    return Math.max(0L, expireAt - now);
  }

  public boolean hasCooldown(String key, UUID uuid) {
    return remainingMillis(key, uuid) > 0;
  }

  public void set(String key, UUID uuid, long millis) {
    long expireAt = Instant.now().toEpochMilli() + Math.max(0L, millis);
    cooldowns.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>()).put(uuid, expireAt);
  }

  public void clear(UUID uuid) {
    cooldowns.values().forEach(map -> map.remove(uuid));
  }
}
