package com.fatsan1975.utilities.economy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumer modunda (harici Vault provider) kullanılan in-memory pay limit fallback’u.
 * Provider modunda kalıcı limit {@link com.fatsan1975.utilities.economy.storage.EconomyStorage}
 * üstünden yönetilir.
 */
public final class PayLimitService {
  private final Map<UUID, DailyCounter> dailyCounters = new ConcurrentHashMap<>();

  public boolean canSend(UUID player, BigDecimal amount, BigDecimal dailyLimit) {
    if (dailyLimit == null || dailyLimit.signum() <= 0) {
      return true;
    }
    DailyCounter counter = dailyCounters.computeIfAbsent(player, ignored -> new DailyCounter(LocalDate.now(), BigDecimal.ZERO));
    synchronized (counter) {
      if (!counter.day.equals(LocalDate.now())) {
        counter.day = LocalDate.now();
        counter.sent = BigDecimal.ZERO;
      }
      return counter.sent.add(amount).compareTo(dailyLimit) <= 0;
    }
  }

  public void add(UUID player, BigDecimal amount) {
    DailyCounter counter = dailyCounters.computeIfAbsent(player, ignored -> new DailyCounter(LocalDate.now(), BigDecimal.ZERO));
    synchronized (counter) {
      if (!counter.day.equals(LocalDate.now())) {
        counter.day = LocalDate.now();
        counter.sent = BigDecimal.ZERO;
      }
      counter.sent = counter.sent.add(amount);
    }
  }

  // Legacy double API — testler için
  public boolean canSend(UUID player, double amount, double dailyLimit) {
    return canSend(player, BigDecimal.valueOf(amount), BigDecimal.valueOf(dailyLimit));
  }

  public void add(UUID player, double amount) {
    add(player, BigDecimal.valueOf(amount));
  }

  private static final class DailyCounter {
    private LocalDate day;
    private BigDecimal sent;

    private DailyCounter(LocalDate day, BigDecimal sent) {
      this.day = day;
      this.sent = sent;
    }
  }
}
