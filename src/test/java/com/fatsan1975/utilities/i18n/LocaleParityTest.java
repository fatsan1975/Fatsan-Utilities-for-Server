package com.fatsan1975.utilities.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/** TR ve EN dil dosyalarının aynı anahtar setine sahip olduğunu doğrular. */
class LocaleParityTest {
  @Test
  void trAndEnHaveSameKeys() throws Exception {
    YamlConfiguration tr = load("messages_tr.yml");
    YamlConfiguration en = load("messages_en.yml");

    Set<String> trKeys = new HashSet<>(tr.getKeys(true));
    Set<String> enKeys = new HashSet<>(en.getKeys(true));

    Set<String> missingInEn = new TreeSet<>(trKeys);
    missingInEn.removeAll(enKeys);
    Set<String> missingInTr = new TreeSet<>(enKeys);
    missingInTr.removeAll(trKeys);

    assertTrue(missingInEn.isEmpty(), "EN'de eksik anahtarlar: " + missingInEn);
    assertTrue(missingInTr.isEmpty(), "TR'de eksik anahtarlar: " + missingInTr);
  }

  private YamlConfiguration load(String resource) throws Exception {
    var stream = getClass().getClassLoader().getResourceAsStream(resource);
    if (stream == null) throw new IllegalStateException(resource + " bulunamadı");
    try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      return YamlConfiguration.loadConfiguration(reader);
    }
  }
}
