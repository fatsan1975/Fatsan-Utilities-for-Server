package com.fatsan1975.utilities.economy.storage;

import com.fatsan1975.utilities.economy.model.Account;
import com.fatsan1975.utilities.economy.model.TopEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Ekonomi kalıcılığı için storage cephesi. İmplementasyonlar thread-safe OLMALI. */
public interface EconomyStorage {
  void initialize() throws Exception;

  void shutdown();

  Optional<Account> findByUuid(UUID uuid);

  Optional<Account> findByName(String name);

  Account createIfMissing(UUID uuid, String name, BigDecimal startingBalance);

  /**
   * Atomik transfer: {@code from} hesabından {@code amount} kadar düşür, {@code to} hesabına ekle.
   *
   * @return {@link TransferResult}
   */
  TransferResult transfer(UUID from, UUID to, BigDecimal amount);

  /** {@code delta} pozitifse ekler, negatifse çıkarır. İşlem atomiktir. */
  Account adjust(UUID uuid, BigDecimal delta);

  /** Bakiyeyi doğrudan belirli bir değere set eder. */
  Account set(UUID uuid, BigDecimal newBalance);

  List<TopEntry> top(int limit, int offset);

  long accountCount();

  /** Günlük ödeme limitini takip eder. {@code true} → limit aşılmaz; {@code false} → reddedilmeli. */
  boolean tryReservePayLimit(UUID player, LocalDate day, BigDecimal amount, BigDecimal dailyCap);

  void recordTransaction(UUID from, UUID to, BigDecimal amount, String type, String meta);

  enum ResultCode { SUCCESS, ACCOUNT_NOT_FOUND, INSUFFICIENT_FUNDS, INVALID_AMOUNT, STORAGE_ERROR }

  record TransferResult(ResultCode code, BigDecimal fromBalance, BigDecimal toBalance, String message) {
    public boolean success() {
      return code == ResultCode.SUCCESS;
    }

    public static TransferResult ok(BigDecimal from, BigDecimal to) {
      return new TransferResult(ResultCode.SUCCESS, from, to, "OK");
    }

    public static TransferResult error(ResultCode code, String message) {
      return new TransferResult(code, BigDecimal.ZERO, BigDecimal.ZERO, message);
    }
  }
}
