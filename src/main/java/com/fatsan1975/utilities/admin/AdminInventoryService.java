package com.fatsan1975.utilities.admin;

import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AdminInventoryService {
  public Inventory createInventoryView(Player target) {
    Inventory inventory = Bukkit.createInventory(null, 54,
      ChatColor.DARK_RED + "InvSee » " + target.getName());

    ItemStack[] contents = target.getInventory().getStorageContents();
    for (int i = 0; i < Math.min(contents.length, 36); i++) {
      if (contents[i] != null) {
        inventory.setItem(i, contents[i].clone());
      }
    }

    inventory.setItem(45, named(target.getInventory().getHelmet(), Material.BARRIER, "Kask"));
    inventory.setItem(46, named(target.getInventory().getChestplate(), Material.BARRIER, "Göğüslük"));
    inventory.setItem(47, named(target.getInventory().getLeggings(), Material.BARRIER, "Pantolon"));
    inventory.setItem(48, named(target.getInventory().getBoots(), Material.BARRIER, "Bot"));
    inventory.setItem(50, named(target.getInventory().getItemInOffHand(), Material.BARRIER, "2. El"));

    inventory.setItem(53, info(Material.ENDER_CHEST, "Enderchest için: /invsee " + target.getName() + " ender"));
    return inventory;
  }

  public Inventory createEnderView(Player target) {
    Inventory inventory = Bukkit.createInventory(null, 27,
      ChatColor.DARK_PURPLE + "Ender » " + target.getName());
    ItemStack[] contents = target.getEnderChest().getContents();
    for (int i = 0; i < contents.length; i++) {
      if (contents[i] != null) {
        inventory.setItem(i, contents[i].clone());
      }
    }
    return inventory;
  }

  private ItemStack named(ItemStack source, Material fallback, String name) {
    ItemStack item = (source == null || source.getType() == Material.AIR)
      ? info(fallback, name + " (boş)") : source.clone();

    ItemMeta meta = item.getItemMeta();
    if (meta != null && !meta.hasDisplayName()) {
      meta.setDisplayName(ChatColor.YELLOW + name + ChatColor.GRAY + " - " + pretty(item.getType()));
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack info(Material material, String text) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(ChatColor.AQUA + text);
      item.setItemMeta(meta);
    }
    return item;
  }

  private String pretty(Material material) {
    return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
  }
}
