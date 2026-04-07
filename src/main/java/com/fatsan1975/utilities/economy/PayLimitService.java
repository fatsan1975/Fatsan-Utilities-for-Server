package com.fatsan1975.utilities.economy;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PayLimitService {
  private final Map<UUID, DailyCounter> dailyCounters = new HashMap<>();

  public boolean canSend(UUID player, double amount, double dailyLimit) {
    DailyCounter counter = dailyCounters.computeIfAbsent(player, ignored -> new DailyCounter(LocalDate.now(), 0D));
    if (!counter.day.equals(LocalDate.now())) {
      counter.day = LocalDate.now();
      counter.sent = 0D;
    }
    return counter.sent + amount <= dailyLimit;
  }

  public void add(UUID player, double amount) {
    DailyCounter counter = dailyCounters.computeIfAbsent(player, ignored -> new DailyCounter(LocalDate.now(), 0D));
    if (!counter.day.equals(LocalDate.now())) {
      counter.day = LocalDate.now();
      counter.sent = 0D;
    }
    counter.sent += amount;
  }

  private static final class DailyCounter {
    private LocalDate day;
    private double sent;

    private DailyCounter(LocalDate day, double sent) {
      this.day = day;
      this.sent = sent;
    }
  }
}
