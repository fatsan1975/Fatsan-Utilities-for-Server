package com.fatsan1975.utilities.command;

import com.fatsan1975.utilities.config.PluginConfiguration;
import com.fatsan1975.utilities.core.ModuleManager;
import com.fatsan1975.utilities.core.RateLimitService;
import com.fatsan1975.utilities.util.CommandGate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ChatShowcaseCommand implements CommandExecutor {
  public enum Mode { ITEM, INVENTORY }

  private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

  private final PluginConfiguration configuration;
  private final Mode mode;
  private final ModuleManager modules;
  private final RateLimitService rateLimit;

  public ChatShowcaseCommand(PluginConfiguration configuration, Mode mode, ModuleManager modules, RateLimitService rateLimit) {
    this.configuration = configuration;
    this.mode = mode;
    this.modules = modules;
    this.rateLimit = rateLimit;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!CommandGate.checkPermission(sender, command, configuration)
      || !CommandGate.checkModule(sender, configuration, modules, ModuleManager.Module.SOCIAL)) {
      return true;
    }
    if (!(sender instanceof Player player)) {
      sender.sendMessage(configuration.locale().message("general.player-only", sender));
      return true;
    }
    String key = command.getName().toLowerCase();
    if (!CommandGate.checkRateLimit(player, configuration, rateLimit, key, "rate-limit.commands." + key)) {
      return true;
    }

    if (mode == Mode.ITEM) {
      showItem(player);
      return true;
    }
    showInventory(player);
    return true;
  }

  private void showItem(Player player) {
    ItemStack item = player.getInventory().getItemInMainHand();
    if (item.getType() == Material.AIR) {
      player.sendMessage(configuration.locale().message("chat.item-empty", player));
      return;
    }
    String prefix = configuration.locale().message("chat.item-prefix", player).replace("{player}", player.getName()) + " ";
    Component message = LEGACY.deserialize(prefix)
      .append(item.displayName())
      .hoverEvent(item.asHoverEvent());
    Bukkit.getServer().broadcast(message);
  }

  private void showInventory(Player player) {
    StringBuilder details = new StringBuilder();
    for (ItemStack content : player.getInventory().getContents()) {
      if (content == null || content.getType() == Material.AIR) {
        continue;
      }
      details.append(content.getAmount()).append("x ").append(content.getType()).append("\n");
    }
    String prefix = configuration.locale().message("chat.inventory-prefix", player).replace("{player}", player.getName());
    Component message = LEGACY.deserialize(prefix)
      .hoverEvent(HoverEvent.showText(Component.text(details.isEmpty() ? "-" : details.toString())));
    Bukkit.getServer().broadcast(message);
  }
}
