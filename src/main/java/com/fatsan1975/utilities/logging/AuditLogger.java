package com.fatsan1975.utilities.logging;

import com.fatsan1975.utilities.core.scheduler.FoliaScheduler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Asenkron, kuyruk tabanlı audit logger.
 *
 * <p>{@code logs/audit.log} dosyasına yazar, ana thread’i bloklamaz, Folia-safe. Her girdi
 * {@code [ISO-TIME] [ACTION] details} formatındadır.
 */
public final class AuditLogger {
  private static final DateTimeFormatter FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final JavaPlugin plugin;
  private final Path logFile;
  private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(10_000);
  private final AtomicBoolean running = new AtomicBoolean(true);

  public AuditLogger(JavaPlugin plugin) {
    this.plugin = plugin;
    Path dir = plugin.getDataFolder().toPath().resolve("logs");
    try {
      Files.createDirectories(dir);
    } catch (IOException exception) {
      plugin.getLogger().log(Level.WARNING, "Audit log klasörü oluşturulamadı", exception);
    }
    this.logFile = dir.resolve("audit.log");
    startWorker();
  }

  public void log(String action, String details) {
    if (!running.get()) {
      return;
    }
    String line = "[" + LocalDateTime.now().format(FORMAT) + "] [" + action + "] " + details;
    plugin.getLogger().info("[AUDIT] [" + action + "] " + details);
    queue.offer(line);
  }

  public void shutdown() {
    running.set(false);
    flushQueue();
  }

  private void startWorker() {
    FoliaScheduler.runAsyncRepeating(plugin, this::flushQueue, 1000L, 1000L);
  }

  private void flushQueue() {
    if (queue.isEmpty()) {
      return;
    }
    StringBuilder batch = new StringBuilder();
    String line;
    while ((line = queue.poll()) != null) {
      batch.append(line).append(System.lineSeparator());
    }
    try {
      Files.writeString(logFile, batch.toString(), StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException exception) {
      plugin.getLogger().log(Level.WARNING, "Audit log yazılamadı", exception);
    }
  }
}
