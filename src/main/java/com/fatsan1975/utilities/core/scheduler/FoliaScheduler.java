package com.fatsan1975.utilities.core.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Folia/Paper uyumlu scheduler cephe sınıfı.
 *
 * <p>Folia region-thread modelinde {@link org.bukkit.scheduler.BukkitScheduler} bazı metodları
 * {@code UnsupportedOperationException} fırlatır. Bu sınıf çalışma zamanında Folia’nın olup
 * olmadığını tespit edip doğru scheduler’ı seçer.
 */
public final class FoliaScheduler {
  private static final boolean FOLIA;

  static {
    boolean folia;
    try {
      Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
      folia = true;
    } catch (ClassNotFoundException exception) {
      folia = false;
    }
    FOLIA = folia;
  }

  private FoliaScheduler() {}

  public static boolean isFolia() {
    return FOLIA;
  }

  /** Ana/global region üzerinde çalıştırır. */
  public static void runGlobal(Plugin plugin, Runnable task) {
    if (FOLIA) {
      Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    } else {
      if (Bukkit.isPrimaryThread()) {
        task.run();
      } else {
        Bukkit.getScheduler().runTask(plugin, task);
      }
    }
  }

  public static void runDelayedGlobal(Plugin plugin, Runnable task, long delayTicks) {
    if (delayTicks <= 0L) {
      runGlobal(plugin, task);
      return;
    }
    if (FOLIA) {
      Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
    } else {
      Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
  }

  public static void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
    if (FOLIA) {
      entity.getScheduler().run(plugin, t -> task.run(), null);
    } else {
      if (Bukkit.isPrimaryThread()) {
        task.run();
      } else {
        Bukkit.getScheduler().runTask(plugin, task);
      }
    }
  }

  public static void runAtEntityDelayed(Plugin plugin, Entity entity, Runnable task, long delayTicks) {
    if (delayTicks <= 0L) {
      runAtEntity(plugin, entity, task);
      return;
    }
    if (FOLIA) {
      entity.getScheduler().runDelayed(plugin, t -> task.run(), null, delayTicks);
    } else {
      Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
  }

  public static void runAtLocation(Plugin plugin, Location location, Runnable task) {
    if (FOLIA) {
      Bukkit.getRegionScheduler().execute(plugin, location, task);
    } else {
      if (Bukkit.isPrimaryThread()) {
        task.run();
      } else {
        Bukkit.getScheduler().runTask(plugin, task);
      }
    }
  }

  public static void runAsync(Plugin plugin, Runnable task) {
    if (FOLIA) {
      Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
    } else {
      Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
  }

  public static void runAsyncDelayed(Plugin plugin, Runnable task, long delayMillis) {
    if (FOLIA) {
      Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.run(), Math.max(1L, delayMillis), java.util.concurrent.TimeUnit.MILLISECONDS);
    } else {
      Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, Math.max(1L, delayMillis / 50L));
    }
  }

  public static void runAsyncRepeating(Plugin plugin, Runnable task, long initialDelayMillis, long periodMillis) {
    if (FOLIA) {
      Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(),
        Math.max(1L, initialDelayMillis), Math.max(1L, periodMillis),
        java.util.concurrent.TimeUnit.MILLISECONDS);
    } else {
      Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task,
        Math.max(1L, initialDelayMillis / 50L), Math.max(1L, periodMillis / 50L));
    }
  }
}
