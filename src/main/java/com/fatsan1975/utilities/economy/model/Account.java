package com.fatsan1975.utilities.economy.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Tek bir ekonomi hesabı kaydı. */
public record Account(UUID uuid, String name, BigDecimal balance, long updatedAt) {
  public Account withBalance(BigDecimal newBalance) {
    return new Account(uuid, name, newBalance, System.currentTimeMillis());
  }
}
