package com.fatsan1975.utilities.economy.model;

import java.math.BigDecimal;
import java.util.UUID;

public record TopEntry(UUID uuid, String name, BigDecimal balance) {}
