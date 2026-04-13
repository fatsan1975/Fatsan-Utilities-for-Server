package com.fatsan1975.utilities.admin;

import com.fatsan1975.utilities.config.PluginConfiguration;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AdminInventoryService {
  private final PluginConfiguration configuration;

  public AdminInventoryService(PluginConfiguration configuration) {
    this.configuration = configuration;
  }

  public Inventory createInventoryView(Player viewer, Player target) {
    String title = configuration.locale().message("inventory.invsee-title", viewer)
      .replace("{player}", target.getName());
    Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.DARK_RED + stripColor(title));

    ItemStack[] contents = target.getInventory().getStorageContents();
    for (int i = 0; i < Math.min(contents.length, 36); i++) {
      if (contents[i] != null) {
        inventory.setItem(i, contents[i].clone());
      }
    }

    inventory.setItem(45, named(viewer, target.getInventory().getHelmet(), Material.BARRIER, "inventory.slot-helmet"));
    inventory.setItem(46, named(viewer, target.getInventory().getChestplate(), Material.BARRIER, "inventory.slot-chestplate"));
    inventory.setItem(47, named(viewer, target.getInventory().getLeggings(), Material.BARRIER, "inventory.slot-leggings"));
    inventory.setItem(48, named(viewer, target.getInventory().getBoots(), Material.BARRIER, "inventory.slot-boots"));
    inventory.setItem(50, named(viewer, target.getInventory().getItemInOffHand(), Material.BARRIER, "inventory.slot-offhand"));

    String hint = configuration.locale().message("inventory.ender-hint", viewer).replace("{player}", target.getName());
    inventory.setItem(53, info(Material.ENDER_CHEST, hint));
    return inventory;
  }

  public Inventory createEnderView(Player viewer, Player target) {
    String title = configuration.locale().message("inventory.ender-title", viewer)
      .replace("{player}", target.getName());
    Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_PURPLE + stripColor(title));
    ItemStack[] contents = target.getEnderChest().getContents();
    for (int i = 0; i < contents.length; i++) {
      if (contents[i] != null) {
        inventory.setItem(i, contents[i].clone());
      }
    }
    return inventory;
  }

  private ItemStack named(Player viewer, ItemStack source, Material fallback, String nameKey) {
    String baseName = configuration.locale().message(nameKey, viewer);
    String emptySuffix = " (" + configuration.locale().message("inventory.slot-empty", viewer) + ")";
    ItemStack item = (source == null || source.getType() == Material.AIR)
      ? info(fallback, baseName + emptySuffix) : source.clone();

    ItemMeta meta = item.getItemMeta();
    if (meta != null && !meta.hasDisplayName()) {
      meta.setDisplayName(ChatColor.YELLOW + baseName + ChatColor.GRAY + " - " + pretty(item.getType()));
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack info(Material material, String text) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(ChatColor.AQUA + stripColor(text));
      item.setItemMeta(meta);
    }
    return item;
  }

  private String pretty(Material material) {
    return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
  }

  private String stripColor(String text) {
    String stripped = ChatColor.stripColor(text);
    return stripped == null ? text : stripped;
  }
}
