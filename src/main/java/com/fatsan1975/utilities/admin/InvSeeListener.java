package com.fatsan1975.utilities.admin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class InvSeeListener implements Listener {
  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (isInvSeeView(event.getView().title())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (isInvSeeView(event.getView().title())) {
      event.setCancelled(true);
    }
  }

  private boolean isInvSeeView(Component title) {
    String plain = PlainTextComponentSerializer.plainText().serialize(title);
    return plain.contains("InvSee »") || plain.contains("Ender »");
  }
}
