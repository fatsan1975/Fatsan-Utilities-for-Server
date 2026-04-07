package com.fatsan1975.utilities.economy;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PayLimitServiceTest {
  @Test
  void blocksWhenDailyLimitExceeded() {
    PayLimitService service = new PayLimitService();
    UUID user = UUID.randomUUID();

    Assertions.assertTrue(service.canSend(user, 100, 150));
    service.add(user, 100);
    Assertions.assertFalse(service.canSend(user, 60, 150));
    Assertions.assertTrue(service.canSend(user, 50, 150));
  }
}
