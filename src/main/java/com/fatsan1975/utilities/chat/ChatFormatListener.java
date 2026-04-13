package com.fatsan1975.utilities.chat;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class ChatFormatListener implements Listener {
  private static final LegacyComponentSerializer LEGACY =
      LegacyComponentSerializer.legacySection();

  private final PluginConfiguration configuration;
  private final ModuleManager moduleManager;

  public ChatFormatListener(PluginConfiguration configuration, ModuleManager moduleManager) {
    this.configuration = configuration;
    this.moduleManager = moduleManager;
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void onChat(AsyncChatEvent event) {
    if (!moduleManager.isEnabled(ModuleManager.Module.CHAT)) {
      return;
    }

    Chat chatProvider = resolveVaultChat();
    if (chatProvider == null) {
      return;
    }

    String format = configuration.main().getString("chat.format", "{prefix}{player}{suffix} &8\u00BB &f{message}");

    event.renderer(new PrefixRenderer(chatProvider, format));
  }

  private Chat resolveVaultChat() {
    try {
      RegisteredServiceProvider<Chat> rsp =
          Bukkit.getServicesManager().getRegistration(Chat.class);
      return rsp != null ? rsp.getProvider() : null;
    } catch (Throwable ignored) {
      return null;
    }
  }

  static final class PrefixRenderer implements ChatRenderer {
    private final Chat chatProvider;
    private final String format;

    PrefixRenderer(Chat chatProvider, String format) {
      this.chatProvider = chatProvider;
      this.format = format;
    }

    @Override
    public Component render(Player source, Component sourceDisplayName, Component message, Audience viewer) {
      String prefix = chatProvider.getPlayerPrefix(source);
      String suffix = chatProvider.getPlayerSuffix(source);
      if (prefix == null) prefix = "";
      if (suffix == null) suffix = "";

      String raw = format
          .replace("{prefix}", prefix)
          .replace("{player}", source.getName())
          .replace("{suffix}", suffix);

      // Translate legacy color codes (& and section sign)
      String colored = raw.replace('&', '\u00A7');

      // "{message}" placeholder'ından önceki ve sonraki kısımları ayır
      int msgIdx = colored.indexOf("{message}");
      if (msgIdx < 0) {
        // Format'ta {message} yoksa, sonuna ekle
        return LEGACY.deserialize(colored).append(message);
      }

      String before = colored.substring(0, msgIdx);
      String after = colored.substring(msgIdx + "{message}".length());

      Component beforeComponent = LEGACY.deserialize(before);
      Component afterComponent = after.isEmpty() ? Component.empty() : LEGACY.deserialize(after);

      return beforeComponent.append(message).append(afterComponent);
    }
  }
}
