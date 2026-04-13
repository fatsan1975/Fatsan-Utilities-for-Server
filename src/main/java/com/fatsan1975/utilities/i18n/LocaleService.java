package com.fatsan1975.utilities.i18n;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.util.PermissionAccess;
import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Çoklu dil yönetimi. TR varsayılan, fallback zinciri: oyuncu dili → sunucu dili → tr → key.
 *
 * <p>Mesajlar hem legacy {@code &} renk kodu hem de MiniMessage formatını destekler. MiniMessage
 * öncelikle denenir; başarılı olmazsa legacy seri çözümleyici çalışır.
 */
public final class LocaleService {
  private static final String DEFAULT_LANG = "tr";
  private static final String FALLBACK_LANG = "tr";

  private final JavaPlugin plugin;
  private final PluginConfiguration configuration;
  private final Map<String, FileConfiguration> languages = new ConcurrentHashMap<>();
  private final MiniMessage miniMessage = MiniMessage.miniMessage();
  private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

  private String serverLang = DEFAULT_LANG;

  public LocaleService(JavaPlugin plugin, PluginConfiguration configuration) {
    this.plugin = plugin;
    this.configuration = configuration;
  }

  public void reload() {
    languages.clear();
    loadLang("tr");
    loadLang("en");
    String configured = configuration.main().getString("plugin.language", DEFAULT_LANG);
    if (configured == null || configured.isBlank()) {
      configured = DEFAULT_LANG;
    }
    // tr_TR → tr
    this.serverLang = configured.toLowerCase(Locale.ROOT).split("[_-]")[0];
    if (!languages.containsKey(serverLang)) {
      plugin.getLogger().warning("Yapılandırılan dil bulunamadı: " + serverLang + ", 'tr' kullanılıyor.");
      this.serverLang = FALLBACK_LANG;
    }
  }

  private void loadLang(String code) {
    String fileName = "messages_" + code + ".yml";
    File file = new File(plugin.getDataFolder(), fileName);
    if (!file.exists()) {
      try {
        plugin.saveResource(fileName, false);
      } catch (IllegalArgumentException ignored) {
        plugin.getLogger().warning("Dil dosyası bulunamadı: " + fileName);
        return;
      }
    }
    languages.put(code, YamlConfiguration.loadConfiguration(file));
  }

  /** Verilen yol için ham string döndürür (legacy §-prefixed). Placeholder’ları çağıran doldurur. */
  public String message(String path) {
    return message(path, serverLang);
  }

  public String message(String path, CommandSender sender) {
    return message(path, resolveLang(sender));
  }

  public String message(String path, String langCode) {
    String raw = lookup(path, langCode);
    if (raw == null) {
      raw = lookup(path, FALLBACK_LANG);
    }
    if (raw == null) {
      return "§cMissing key: " + path;
    }
    return raw.replace('&', '§');
  }

  /** Component versiyonu — MiniMessage + legacy renk desteğiyle. */
  public Component component(String path, CommandSender sender) {
    String raw = lookup(path, resolveLang(sender));
    if (raw == null) {
      raw = lookup(path, FALLBACK_LANG);
    }
    if (raw == null) {
      return Component.text("Missing key: " + path);
    }
    // Legacy & sembollerini algıla
    if (raw.indexOf('&') >= 0 && !raw.contains("<")) {
      return legacy.deserialize(raw);
    }
    try {
      return miniMessage.deserialize(raw);
    } catch (Exception exception) {
      return legacy.deserialize(raw);
    }
  }

  public String format(String path, CommandSender sender, Map<String, String> placeholders) {
    String msg = message(path, sender);
    for (Map.Entry<String, String> e : placeholders.entrySet()) {
      msg = msg.replace("{" + e.getKey() + "}", e.getValue());
    }
    return msg;
  }

  private String lookup(String path, String lang) {
    FileConfiguration cfg = languages.get(lang);
    if (cfg == null) {
      return null;
    }
    return cfg.getString(path);
  }

  private String resolveLang(CommandSender sender) {
    if (sender instanceof Player player) {
      // Permission tabanlı override: fatsanutilities.lang.en, fatsanutilities.lang.tr
      for (String lang : languages.keySet()) {
        if (PermissionAccess.has(player, "fatsanutilities.lang." + lang)) {
          return lang;
        }
      }
      // Paper client locale
      try {
        java.util.Locale clientLocale = player.locale();
        if (clientLocale != null) {
          String code = clientLocale.getLanguage().toLowerCase(Locale.ROOT);
          if (languages.containsKey(code) && configuration.main().getBoolean("plugin.follow-client-language", false)) {
            return code;
          }
        }
      } catch (Throwable ignored) {
        // Bukkit fallback - ignore
      }
    }
    return serverLang;
  }

  public String serverLang() {
    return serverLang;
  }
}
