package com.fatsan1975.utilities.permission;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class FoliaPermsAdapter {
  private static volatile Object cachedPermissionProvider;
  private static volatile boolean lookedUp = false;

  private FoliaPermsAdapter() {}

  public static boolean has(OfflinePlayer player, String permission) {
    if (permission == null || permission.isBlank()) {
      return true;
    }

    Player online = player.getPlayer();
    String worldName = online != null ? online.getWorld().getName() : null;
    Boolean providerResult = queryProvider(player, worldName, permission);
    if (providerResult != null) {
      return providerResult || (online != null && online.hasPermission(permission));
    }
    return online != null && online.hasPermission(permission);
  }

  public static boolean has(UUID uuid, String permission) {
    Player online = Bukkit.getPlayer(uuid);
    if (online != null) {
      return has(online, permission);
    }
    return has(Bukkit.getOfflinePlayer(uuid), permission);
  }

  private static Boolean queryProvider(OfflinePlayer player, String worldName, String permission) {
    Object provider = resolveProvider();
    if (provider == null) {
      return null;
    }

    try {
      var method = provider.getClass().getMethod("playerHas", String.class, OfflinePlayer.class, String.class);
      Object result = method.invoke(provider, worldName, player, permission);
      return result instanceof Boolean b ? b : null;
    } catch (NoSuchMethodException ignored) {
      if (player.getName() == null || player.getName().isBlank()) {
        return null;
      }
      try {
        var method = provider.getClass().getMethod("playerHas", String.class, String.class, String.class);
        Object result = method.invoke(provider, worldName, player.getName(), permission);
        return result instanceof Boolean b ? b : null;
      } catch (Throwable exception) {
        return null;
      }
    } catch (Throwable exception) {
      return null;
    }
  }

  private static Object resolveProvider() {
    if (lookedUp) {
      return cachedPermissionProvider;
    }
    synchronized (FoliaPermsAdapter.class) {
      if (lookedUp) {
        return cachedPermissionProvider;
      }
      try {
        Class<?> permissionClass = Class.forName("net.milkbowl.vault.permission.Permission");
        var registration = Bukkit.getServer().getServicesManager().getRegistration(
          (Class<Object>) (Class<?>) permissionClass);
        if (registration != null) {
          cachedPermissionProvider = registration.getProvider();
        }
      } catch (Throwable ignored) {
        cachedPermissionProvider = null;
      }
      lookedUp = true;
      return cachedPermissionProvider;
    }
  }

  public static void invalidate() {
    synchronized (FoliaPermsAdapter.class) {
      lookedUp = false;
      cachedPermissionProvider = null;
    }
  }

  public static String providerName() {
    Object provider = resolveProvider();
    if (provider == null) {
      return "vanilla";
    }
    try {
      var nameMethod = provider.getClass().getMethod("getName");
      Object name = nameMethod.invoke(provider);
      return name == null ? provider.getClass().getSimpleName() : name.toString();
    } catch (Throwable exception) {
      return provider.getClass().getSimpleName();
    }
  }

  public static boolean isPluginLoaded(String name) {
    Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
    return plugin != null && plugin.isEnabled();
  }
}
