package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.FatsanUtilitiesPlugin;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Brigadier düzeyinde komut kaydı — Folia'da tab complete buradan çalışır.
 * LifecycleEvents.COMMANDS plugin constructor'ında kayıt edilmeli (onEnable'dan önce).
 * execute() → plugin.yml executor'ını çağırır (CommandMap bypass olmadan).
 * suggest() → Brigadier native completion (AsyncTabCompleteEvent yerine geçer).
 */
@SuppressWarnings("UnstableApiUsage")
public final class BrigadierCommands {

  private BrigadierCommands() {}

  public static void register(final FatsanUtilitiesPlugin plugin, final Commands reg) {

    // balance / bal / money — arg1: player name
    reg.register("balance", "Bakiye görüntüler", List.of("bal", "money"),
        make(plugin, "balance", (source, args) -> {
          if (args.length == 1) return playerNames(args[0]);
          return List.of();
        }));

    // balancetop — no arg suggestions
    reg.register("balancetop", "Zenginlik sıralaması", List.of("topbalance", "baltop"),
        make(plugin, "balancetop", (source, args) -> List.of()));

    // pay — arg1: player name
    reg.register("pay", "Para gönder", List.of(),
        make(plugin, "pay", (source, args) -> {
          if (args.length == 1) return playerNames(args[0]);
          return List.of();
        }));

    // eco / economy — arg1: subcommand, arg2: player name
    reg.register("eco", "Ekonomi yönetimi", List.of("economy"),
        make(plugin, "eco", (source, args) -> {
          if (args.length == 1) return filter(args[0], "give", "take", "set", "reset");
          if (args.length == 2) return playerNames(args[1]);
          return List.of();
        }));

    // tpa — arg1: player name
    reg.register("tpa", "TPA isteği", List.of(),
        make(plugin, "tpa", (source, args) -> {
          if (args.length == 1) return playerNames(args[0]);
          return List.of();
        }));

    // tpaccept / tpdeny — no args
    reg.register("tpaccept", "TPA kabul", List.of(),
        make(plugin, "tpaccept", (source, args) -> List.of()));
    reg.register("tpdeny", "TPA reddet", List.of(),
        make(plugin, "tpdeny", (source, args) -> List.of()));

    // rtp — arg1: world name
    reg.register("rtp", "Rastgele güvenli ışınlanma", List.of(),
        make(plugin, "rtp", (source, args) -> {
          if (args.length == 1) {
            final String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getWorlds().stream()
                .map(World::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                .collect(Collectors.toList());
          }
          return List.of();
        }));

    // spawn / setspawn — no args
    reg.register("spawn", "Spawn'a dön", List.of(),
        make(plugin, "spawn", (source, args) -> List.of()));
    reg.register("setspawn", "Spawn ayarla", List.of(),
        make(plugin, "setspawn", (source, args) -> List.of()));

    // home — arg1: home name
    reg.register("home", "Eve ışınlan", List.of(),
        make(plugin, "home", (source, args) -> {
          if (args.length == 1 && source.getSender() instanceof final Player player) {
            final String prefix = args[0];
            return plugin.homeService().list(player.getUniqueId()).stream()
                .filter(h -> h.startsWith(prefix))
                .collect(Collectors.toList());
          }
          return List.of();
        }));

    // sethome / homes — no arg suggestions
    reg.register("sethome", "Ev ayarla", List.of(),
        make(plugin, "sethome", (source, args) -> List.of()));
    reg.register("homes", "Evleri listele", List.of(),
        make(plugin, "homes", (source, args) -> List.of()));

    // delhome — arg1: home name
    reg.register("delhome", "Ev sil", List.of(),
        make(plugin, "delhome", (source, args) -> {
          if (args.length == 1 && source.getSender() instanceof final Player player) {
            final String prefix = args[0];
            return plugin.homeService().list(player.getUniqueId()).stream()
                .filter(h -> h.startsWith(prefix))
                .collect(Collectors.toList());
          }
          return List.of();
        }));

    // itemchat / invchat — no args
    reg.register("itemchat", "Eşya göster", List.of("showitem"),
        make(plugin, "itemchat", (source, args) -> List.of()));
    reg.register("invchat", "Envanter göster", List.of("showinv"),
        make(plugin, "invchat", (source, args) -> List.of()));

    // invsee — arg1: player name, arg2: "ender"
    reg.register("invsee", "Envanter görüntüle", List.of(),
        make(plugin, "invsee", (source, args) -> {
          if (args.length == 1) return playerNames(args[0]);
          if (args.length == 2) return filter(args[1], "ender");
          return List.of();
        }));

    // fumodule — arg1: module, arg2: action
    reg.register("fumodule", "Modül yönetimi", List.of(),
        make(plugin, "fumodule", (source, args) -> {
          if (args.length == 1) return filter(args[0], "economy", "teleport", "social", "admin", "chat");
          if (args.length == 2) return filter(args[1], "on", "off", "status");
          return List.of();
        }));

    // fudebug / futilitiesreload / fuhelp — no args
    reg.register("fudebug", "Debug bilgileri", List.of(),
        make(plugin, "fudebug", (source, args) -> List.of()));
    reg.register("futilitiesreload", "Plugin yenile", List.of("fureload"),
        make(plugin, "futilitiesreload", (source, args) -> List.of()));
    reg.register("fuhelp", "Yardım", List.of(),
        make(plugin, "fuhelp", (source, args) -> List.of()));
  }

  // ──────────────────────────────────────────────────────────────────────────

  @FunctionalInterface
  private interface Suggester {
    Collection<String> suggest(CommandSourceStack source, String[] args);
  }

  /**
   * BasicCommand'ı sarmalar.
   * execute → plugin.invokeExecutor() → executor registry üzerinden çalışır
   *           (getCommand()'a bağımlı değil, LifecycleEvents override'ından etkilenmez).
   * suggest → Brigadier suggestion'larını döndürür.
   */
  private static BasicCommand make(
      final FatsanUtilitiesPlugin plugin,
      final String commandName,
      final Suggester suggester) {

    return new BasicCommand() {

      @Override
      public void execute(final CommandSourceStack source, final String[] args) {
        plugin.invokeExecutor(commandName, source.getSender(), args);
      }

      @Override
      public Collection<String> suggest(final CommandSourceStack source, final String[] args) {
        return suggester.suggest(source, args);
      }
    };
  }

  private static List<String> playerNames(final String prefix) {
    final String lower = prefix.toLowerCase(Locale.ROOT);
    return Bukkit.getOnlinePlayers().stream()
        .map(Player::getName)
        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(lower))
        .collect(Collectors.toList());
  }

  private static List<String> filter(final String prefix, final String... candidates) {
    final String lower = prefix.toLowerCase(Locale.ROOT);
    return Stream.of(candidates)
        .filter(s -> s.startsWith(lower))
        .collect(Collectors.toList());
  }
}
