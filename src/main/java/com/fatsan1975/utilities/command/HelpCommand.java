package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.util.CommandGate;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HelpCommand implements CommandExecutor {
  private final PluginConfiguration configuration;
  private final RateLimitService rateLimit;

  public HelpCommand(PluginConfiguration configuration, RateLimitService rateLimit) {
    this.configuration = configuration;
    this.rateLimit = rateLimit;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)) {
      return true;
    }
    if (sender instanceof Player player && !CommandGate.checkRateLimit(player, configuration, rateLimit, "fuhelp", "rate-limit.commands.fuhelp")) {
      return true;
    }

    List<String> lines = new ArrayList<>();
    lines.add("§6§lFatsanUtilities Yardım");
    add(lines, sender, "fatsanutilities.balance", "/balance [oyuncu] - Bakiye görüntüle");
    add(lines, sender, "fatsanutilities.balancetop", "/balancetop [sayfa] - Zenginlik sıralaması");
    add(lines, sender, "fatsanutilities.pay", "/pay <oyuncu> <miktar> - Para gönder");
    add(lines, sender, "fatsanutilities.tpa", "/tpa <oyuncu> - TPA isteği");
    add(lines, sender, "fatsanutilities.rtp", "/rtp [dünya] - Rastgele güvenli ışınlan");
    add(lines, sender, "fatsanutilities.spawn", "/spawn - Spawn'a dön");
    add(lines, sender, "fatsanutilities.itemchat", "/itemchat - Eşya göster");
    add(lines, sender, "fatsanutilities.invchat", "/invchat - Envanter özeti göster");
    add(lines, sender, "fatsanutilities.admin.invsee", "/invsee <oyuncu> [ender] - Envanter izle");
    add(lines, sender, "fatsanutilities.admin.debug", "/fudebug - Debug bilgileri");
    add(lines, sender, "fatsanutilities.admin.module", "/fumodule <modül> <on|off|status> - Modül yönet");
    add(lines, sender, "fatsanutilities.admin.reload", "/futilitiesreload - Plugin yenile");

    lines.forEach(sender::sendMessage);
    return true;
  }

  private void add(List<String> lines, CommandSender sender, String permission, String line) {
    if (sender.hasPermission(permission)) {
      lines.add("§e" + line);
    }
  }
}
