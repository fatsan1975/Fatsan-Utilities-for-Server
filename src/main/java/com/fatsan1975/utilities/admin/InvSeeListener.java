package com.fatsan1975.utilities.admin;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class InvSeeListener implements Listener {
  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (isInvSeeView(event.getView().getTitle())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (isInvSeeView(event.getView().getTitle())) {
      event.setCancelled(true);
    }
  }

  private boolean isInvSeeView(String title) {
    String stripped = ChatColor.stripColor(title);
    return stripped != null && (stripped.startsWith("InvSee »") || stripped.startsWith("Ender »"));
  }
}
