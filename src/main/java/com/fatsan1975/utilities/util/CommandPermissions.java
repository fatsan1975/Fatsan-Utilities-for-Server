package com.fatsan1975.utilities.util;

import java.util.Map;

public final class CommandPermissions {
  private static final Map<String, String> COMMANDS = Map.ofEntries(
    Map.entry("balance", "fatsanutilities.balance"),
    Map.entry("balancetop", "fatsanutilities.balancetop"),
    Map.entry("pay", "fatsanutilities.pay"),
    Map.entry("eco", "fatsanutilities.admin.eco"),
    Map.entry("tpa", "fatsanutilities.tpa"),
    Map.entry("tpaccept", "fatsanutilities.tpa.accept"),
    Map.entry("tpdeny", "fatsanutilities.tpa.deny"),
    Map.entry("rtp", "fatsanutilities.rtp"),
    Map.entry("spawn", "fatsanutilities.spawn"),
    Map.entry("setspawn", "fatsanutilities.admin.setspawn"),
    Map.entry("home", "fatsanutilities.home"),
    Map.entry("sethome", "fatsanutilities.sethome"),
    Map.entry("delhome", "fatsanutilities.delhome"),
    Map.entry("homes", "fatsanutilities.homes"),
    Map.entry("itemchat", "fatsanutilities.itemchat"),
    Map.entry("invchat", "fatsanutilities.invchat"),
    Map.entry("invsee", "fatsanutilities.admin.invsee"),
    Map.entry("fuhelp", "fatsanutilities.help"),
    Map.entry("fudebug", "fatsanutilities.admin.debug"),
    Map.entry("fumodule", "fatsanutilities.admin.module"),
    Map.entry("futilitiesreload", "fatsanutilities.admin.reload")
  );

  private CommandPermissions() {}

  public static String permissionFor(String commandName) {
    return COMMANDS.get(commandName);
  }
}
