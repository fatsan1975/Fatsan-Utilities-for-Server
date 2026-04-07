package com.fatsan1975.utilities.util;

import java.util.Map;

public final class MessageFormatter {
  private MessageFormatter() {}

  public static String format(String message, Map<String, String> placeholders) {
    String out = message;
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      out = out.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return out;
  }
}
