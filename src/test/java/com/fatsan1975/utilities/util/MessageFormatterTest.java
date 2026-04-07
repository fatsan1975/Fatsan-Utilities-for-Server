package com.fatsan1975.utilities.util;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MessageFormatterTest {
  @Test
  void replacesPlaceholders() {
    String out = MessageFormatter.format("Merhaba {player}, bakiye {amount}", Map.of("player", "Fatsan", "amount", "100"));
    Assertions.assertEquals("Merhaba Fatsan, bakiye 100", out);
  }
}
