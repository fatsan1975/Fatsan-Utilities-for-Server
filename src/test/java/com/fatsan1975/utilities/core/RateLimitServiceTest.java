package com.fatsan1975.utilities.core;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {
  @Test
  void appliesAndExpiresRateLimit() throws InterruptedException {
    RateLimitService service = new RateLimitService();
    UUID user = UUID.randomUUID();

    service.mark("pay", user, 50);
    Assertions.assertTrue(service.remainingMillis("pay", user) > 0);

    Thread.sleep(60);
    Assertions.assertEquals(0, service.remainingMillis("pay", user));
  }
}
