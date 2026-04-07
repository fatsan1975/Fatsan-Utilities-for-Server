package com.fatsan1975.utilities.economy.event;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class EconomyTransferEvent extends Event {
  private static final HandlerList HANDLERS = new HandlerList();

  private final OfflinePlayer from;
  private final OfflinePlayer to;
  private final double amount;

  public EconomyTransferEvent(OfflinePlayer from, OfflinePlayer to, double amount) {
    this.from = from;
    this.to = to;
    this.amount = amount;
  }

  public OfflinePlayer from() {
    return from;
  }

  public OfflinePlayer to() {
    return to;
  }

  public double amount() {
    return amount;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
