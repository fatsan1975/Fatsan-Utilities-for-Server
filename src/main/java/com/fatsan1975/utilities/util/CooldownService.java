package com.fatsan1975.utilities.util;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownService {
  private final Map<String, Map<UUID, Long>> cooldowns = new HashMap<>();

  public long remainingMillis(String key, UUID uuid) {
    Long expireAt = cooldowns.computeIfAbsent(key, ignored -> new HashMap<>()).get(uuid);
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
    long expireAt = Instant.now().toEpochMilli() + millis;
    cooldowns.computeIfAbsent(key, ignored -> new HashMap<>()).put(uuid, expireAt);
  }
}
