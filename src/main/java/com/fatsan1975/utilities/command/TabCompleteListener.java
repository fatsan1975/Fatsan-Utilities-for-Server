package com.fatsan1975.utilities.command;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent.Completion;
import com.fatsan1975.utilities.teleport.HomeService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Folia'da Bukkit TabCompleter Brigadier ile route edilmiyor.
 * AsyncTabCompleteEvent Paper'ın primary async completion hook'u — her zaman çalışır.
 */
public final class TabCompleteListener implements Listener {

  private final HomeService homeService;

  public TabCompleteListener(final HomeService homeService) {
    this.homeService = homeService;
  }

  @EventHandler
  public void onTabComplete(final AsyncTabCompleteEvent event) {
    final String buffer = event.getBuffer();
    if (buffer == null || buffer.isEmpty() || buffer.charAt(0) != '/') return;

    final int spaceIdx = buffer.indexOf(' ');
    if (spaceIdx == -1) return; // sadece komut adı var, arg yok

    final String cmd = buffer.substring(1, spaceIdx).toLowerCase(Locale.ROOT);
    final String argsStr = buffer.substring(spaceIdx + 1);
    final String[] parts = argsStr.split(" ", -1);
    final int argIndex = parts.length; // kaçıncı arg'dayız (1-based)
    final String prefix = parts[parts.length - 1].toLowerCase(Locale.ROOT);

    List<String> suggestions = null;

    switch (cmd) {
      case "balance", "bal", "money" -> {
        if (argIndex == 1) suggestions = playerNames(prefix);
      }
      case "pay" -> {
        if (argIndex == 1) suggestions = playerNames(prefix);
      }
      case "tpa" -> {
        if (argIndex == 1) suggestions = playerNames(prefix);
      }
      case "eco", "economy" -> {
        if (argIndex == 1) suggestions = filter(prefix, "give", "take", "set", "reset");
        else if (argIndex == 2) suggestions = playerNames(prefix);
      }
      case "invsee" -> {
        if (argIndex == 1) suggestions = playerNames(prefix);
        else if (argIndex == 2) suggestions = filter(prefix, "ender");
      }
      case "rtp" -> {
        if (argIndex == 1)
          suggestions = Bukkit.getWorlds().stream()
              .map(World::getName)
              .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
              .collect(Collectors.toList());
      }
      case "home", "delhome" -> {
        if (argIndex == 1 && event.getSender() instanceof Player player)
          suggestions = homeService.list(player.getUniqueId()).stream()
              .filter(h -> h.startsWith(prefix))
              .collect(Collectors.toList());
      }
      case "fumodule" -> {
        if (argIndex == 1) suggestions = filter(prefix, "economy", "teleport", "social", "admin");
        else if (argIndex == 2) suggestions = filter(prefix, "on", "off", "status");
      }
    }

    if (suggestions != null && !suggestions.isEmpty()) {
      event.completions(
          suggestions.stream().map(Completion::completion).collect(Collectors.toList()));
    }
  }

  private List<String> playerNames(final String prefix) {
    return Bukkit.getOnlinePlayers().stream()
        .map(Player::getName)
        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
        .collect(Collectors.toList());
  }

  private List<String> filter(final String prefix, final String... candidates) {
    return Stream.of(candidates)
        .filter(s -> s.startsWith(prefix))
        .collect(Collectors.toList());
  }
}
