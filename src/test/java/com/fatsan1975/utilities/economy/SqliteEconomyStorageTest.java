package com.fatsan1975.utilities.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fatsan1975.utilities.economy.model.Account;
import com.fatsan1975.utilities.economy.model.TopEntry;
import com.fatsan1975.utilities.economy.storage.EconomyStorage;
import com.fatsan1975.utilities.economy.storage.SqliteEconomyStorage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteEconomyStorageTest {
  private SqliteEconomyStorage storage;

  @BeforeEach
  void setUp(@TempDir Path tempDir) throws Exception {
    storage = new SqliteEconomyStorage(tempDir.resolve("economy.sqlite"), Logger.getLogger("test"));
    storage.initialize();
  }

  @AfterEach
  void tearDown() {
    storage.shutdown();
  }

  @Test
  void createAndFindAccount() {
    UUID id = UUID.randomUUID();
    Account created = storage.createIfMissing(id, "Alice", BigDecimal.valueOf(100));
    assertEquals(new BigDecimal("100.00"), created.balance());
    assertTrue(storage.findByUuid(id).isPresent());
    assertTrue(storage.findByName("alice").isPresent());
  }

  @Test
  void atomicTransfer() {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    storage.createIfMissing(a, "A", new BigDecimal("100"));
    storage.createIfMissing(b, "B", new BigDecimal("0"));

    EconomyStorage.TransferResult ok = storage.transfer(a, b, new BigDecimal("30"));
    assertTrue(ok.success());
    assertEquals(new BigDecimal("70.00"), storage.findByUuid(a).get().balance());
    assertEquals(new BigDecimal("30.00"), storage.findByUuid(b).get().balance());

    EconomyStorage.TransferResult fail = storage.transfer(a, b, new BigDecimal("999"));
    assertFalse(fail.success());
    assertEquals(EconomyStorage.ResultCode.INSUFFICIENT_FUNDS, fail.code());
  }

  @Test
  void concurrentTransfersConserveTotal() throws Exception {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    storage.createIfMissing(a, "A", new BigDecimal("1000"));
    storage.createIfMissing(b, "B", new BigDecimal("1000"));

    int threads = 8;
    int perThread = 50;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    for (int i = 0; i < threads; i++) {
      final boolean fwd = (i % 2 == 0);
      pool.submit(() -> {
        try {
          start.await();
          for (int j = 0; j < perThread; j++) {
            if (fwd) storage.transfer(a, b, BigDecimal.ONE);
            else storage.transfer(b, a, BigDecimal.ONE);
          }
        } catch (InterruptedException ignored) {}
      });
    }
    start.countDown();
    pool.shutdown();
    assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));

    BigDecimal total = storage.findByUuid(a).get().balance().add(storage.findByUuid(b).get().balance());
    assertEquals(new BigDecimal("2000.00"), total);
  }

  @Test
  void topOrdersByBalanceDesc() {
    storage.createIfMissing(UUID.randomUUID(), "A", new BigDecimal("50"));
    storage.createIfMissing(UUID.randomUUID(), "B", new BigDecimal("200"));
    storage.createIfMissing(UUID.randomUUID(), "C", new BigDecimal("100"));
    List<TopEntry> top = storage.top(10, 0);
    assertEquals("B", top.get(0).name());
    assertEquals("C", top.get(1).name());
    assertEquals("A", top.get(2).name());
  }

  @Test
  void payLimitTracksDaily() {
    UUID p = UUID.randomUUID();
    LocalDate today = LocalDate.now();
    BigDecimal cap = new BigDecimal("100");
    assertTrue(storage.tryReservePayLimit(p, today, new BigDecimal("60"), cap));
    assertTrue(storage.tryReservePayLimit(p, today, new BigDecimal("40"), cap));
    assertFalse(storage.tryReservePayLimit(p, today, new BigDecimal("1"), cap));
  }
}
