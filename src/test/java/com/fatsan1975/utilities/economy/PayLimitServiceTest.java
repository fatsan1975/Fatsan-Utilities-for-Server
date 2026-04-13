package com.fatsan1975.utilities.economy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayLimitServiceTest {
  @Test
  void respectsDailyLimit() {
    PayLimitService svc = new PayLimitService();
    UUID p = UUID.randomUUID();
    BigDecimal cap = BigDecimal.valueOf(100);
    assertTrue(svc.canSend(p, BigDecimal.valueOf(40), cap));
    svc.add(p, BigDecimal.valueOf(40));
    assertTrue(svc.canSend(p, BigDecimal.valueOf(60), cap));
    svc.add(p, BigDecimal.valueOf(60));
    assertFalse(svc.canSend(p, BigDecimal.ONE, cap));
  }

  @Test
  void zeroCapDisablesLimit() {
    PayLimitService svc = new PayLimitService();
    UUID p = UUID.randomUUID();
    assertTrue(svc.canSend(p, BigDecimal.valueOf(999999), BigDecimal.ZERO));
  }

  @Test
  void legacyDoubleApi() {
    PayLimitService svc = new PayLimitService();
    UUID p = UUID.randomUUID();
    assertTrue(svc.canSend(p, 50.0, 100.0));
    svc.add(p, 60.0);
    assertFalse(svc.canSend(p, 50.0, 100.0));
  }
}
