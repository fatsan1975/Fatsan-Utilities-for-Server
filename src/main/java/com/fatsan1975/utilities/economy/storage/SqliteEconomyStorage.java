package com.fatsan1975.utilities.economy.storage;

import com.fatsan1975.utilities.economy.model.Account;
import com.fatsan1975.utilities.economy.model.TopEntry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basit SQLite tabanlı ekonomi storage.
 *
 * <p>SQLite single-file, kurulum gerektirmez. WAL mode açılır. Tüm parasal değerler
 * {@link BigDecimal} olarak scale=2 tutulur; DB’de {@code TEXT} olarak saklanır (SQLite numeric
 * precision’ı garanti etmez).
 */
public final class SqliteEconomyStorage implements EconomyStorage {
  private static final int SCALE = 2;

  private final Path dbFile;
  private final Logger logger;
  private final ReentrantLock writeLock = new ReentrantLock();
  private Connection connection;

  public SqliteEconomyStorage(Path dbFile, Logger logger) {
    this.dbFile = dbFile;
    this.logger = logger;
  }

  @Override
  public void initialize() throws Exception {
    Files.createDirectories(dbFile.getParent());
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException("SQLite JDBC driver bulunamadı", exception);
    }
    this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("PRAGMA journal_mode=WAL");
      stmt.execute("PRAGMA synchronous=NORMAL");
      stmt.execute("PRAGMA foreign_keys=ON");
      stmt.execute("""
        CREATE TABLE IF NOT EXISTS accounts (
          uuid TEXT PRIMARY KEY,
          name TEXT NOT NULL,
          balance TEXT NOT NULL,
          updated_at INTEGER NOT NULL
        )
        """);
      stmt.execute("CREATE INDEX IF NOT EXISTS idx_accounts_name ON accounts(name COLLATE NOCASE)");
      stmt.execute("CREATE INDEX IF NOT EXISTS idx_accounts_balance ON accounts(CAST(balance AS REAL) DESC)");
      stmt.execute("""
        CREATE TABLE IF NOT EXISTS pay_limits (
          uuid TEXT NOT NULL,
          day TEXT NOT NULL,
          sent TEXT NOT NULL,
          PRIMARY KEY (uuid, day)
        )
        """);
      stmt.execute("""
        CREATE TABLE IF NOT EXISTS transactions (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          from_uuid TEXT,
          to_uuid TEXT,
          amount TEXT NOT NULL,
          type TEXT NOT NULL,
          meta TEXT,
          ts INTEGER NOT NULL
        )
        """);
      stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_from ON transactions(from_uuid, ts DESC)");
      stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_to ON transactions(to_uuid, ts DESC)");
    }
  }

  @Override
  public void shutdown() {
    try {
      if (connection != null && !connection.isClosed()) {
        connection.close();
      }
    } catch (SQLException exception) {
      logger.log(Level.WARNING, "SQLite bağlantısı kapatılırken hata", exception);
    }
  }

  @Override
  public Optional<Account> findByUuid(UUID uuid) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT uuid, name, balance, updated_at FROM accounts WHERE uuid = ?")) {
      ps.setString(1, uuid.toString());
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }
        return Optional.of(mapAccount(rs));
      }
    } catch (SQLException exception) {
      logger.log(Level.SEVERE, "findByUuid hata", exception);
      return Optional.empty();
    }
  }

  @Override
  public Optional<Account> findByName(String name) {
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT uuid, name, balance, updated_at FROM accounts WHERE name = ? COLLATE NOCASE")) {
      ps.setString(1, name);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }
        return Optional.of(mapAccount(rs));
      }
    } catch (SQLException exception) {
      logger.log(Level.SEVERE, "findByName hata", exception);
      return Optional.empty();
    }
  }

  @Override
  public Account createIfMissing(UUID uuid, String name, BigDecimal startingBalance) {
    writeLock.lock();
    try {
      Optional<Account> existing = findByUuid(uuid);
      if (existing.isPresent()) {
        // Name değişmiş olabilir, güncelle.
        if (!existing.get().name().equals(name)) {
          try (PreparedStatement ps = connection.prepareStatement(
              "UPDATE accounts SET name = ?, updated_at = ? WHERE uuid = ?")) {
            ps.setString(1, name);
            ps.setLong(2, System.currentTimeMillis());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
          } catch (SQLException exception) {
            logger.log(Level.WARNING, "name update hata", exception);
          }
          return existing.get();
        }
        return existing.get();
      }
      BigDecimal normalized = normalize(startingBalance);
      try (PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO accounts(uuid, name, balance, updated_at) VALUES(?, ?, ?, ?)")) {
        ps.setString(1, uuid.toString());
        ps.setString(2, name);
        ps.setString(3, normalized.toPlainString());
        ps.setLong(4, System.currentTimeMillis());
        ps.executeUpdate();
      } catch (SQLException exception) {
        logger.log(Level.SEVERE, "createIfMissing hata", exception);
      }
      return new Account(uuid, name, normalized, System.currentTimeMillis());
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public TransferResult transfer(UUID from, UUID to, BigDecimal amount) {
    BigDecimal normalized = normalize(amount);
    if (normalized.signum() <= 0) {
      return TransferResult.error(ResultCode.INVALID_AMOUNT, "amount <= 0");
    }
    if (from.equals(to)) {
      return TransferResult.error(ResultCode.INVALID_AMOUNT, "self transfer");
    }

    writeLock.lock();
    try {
      boolean previousAuto = connection.getAutoCommit();
      connection.setAutoCommit(false);
      try {
        Optional<Account> src = findByUuid(from);
        Optional<Account> dst = findByUuid(to);
        if (src.isEmpty() || dst.isEmpty()) {
          connection.rollback();
          return TransferResult.error(ResultCode.ACCOUNT_NOT_FOUND, "account missing");
        }
        if (src.get().balance().compareTo(normalized) < 0) {
          connection.rollback();
          return TransferResult.error(ResultCode.INSUFFICIENT_FUNDS, "not enough");
        }
        BigDecimal newSrc = normalize(src.get().balance().subtract(normalized));
        BigDecimal newDst = normalize(dst.get().balance().add(normalized));
        updateBalance(from, newSrc);
        updateBalance(to, newDst);
        connection.commit();
        return TransferResult.ok(newSrc, newDst);
      } catch (SQLException exception) {
        connection.rollback();
        logger.log(Level.SEVERE, "transfer hata", exception);
        return TransferResult.error(ResultCode.STORAGE_ERROR, exception.getMessage());
      } finally {
        connection.setAutoCommit(previousAuto);
      }
    } catch (SQLException exception) {
      logger.log(Level.SEVERE, "transfer tx hata", exception);
      return TransferResult.error(ResultCode.STORAGE_ERROR, exception.getMessage());
    } finally {
      writeLock.unlock();
    }
  }

  private void updateBalance(UUID uuid, BigDecimal amount) throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement(
        "UPDATE accounts SET balance = ?, updated_at = ? WHERE uuid = ?")) {
      ps.setString(1, amount.toPlainString());
      ps.setLong(2, System.currentTimeMillis());
      ps.setString(3, uuid.toString());
      ps.executeUpdate();
    }
  }

  @Override
  public Account adjust(UUID uuid, BigDecimal delta) {
    writeLock.lock();
    try {
      Optional<Account> existing = findByUuid(uuid);
      if (existing.isEmpty()) {
        return null;
      }
      BigDecimal newBalance = normalize(existing.get().balance().add(delta));
      if (newBalance.signum() < 0) {
        newBalance = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
      }
      try {
        updateBalance(uuid, newBalance);
      } catch (SQLException exception) {
        logger.log(Level.SEVERE, "adjust hata", exception);
        return existing.get();
      }
      return existing.get().withBalance(newBalance);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Account set(UUID uuid, BigDecimal newBalance) {
    writeLock.lock();
    try {
      Optional<Account> existing = findByUuid(uuid);
      if (existing.isEmpty()) {
        return null;
      }
      BigDecimal normalized = normalize(newBalance);
      try {
        updateBalance(uuid, normalized);
      } catch (SQLException exception) {
        logger.log(Level.SEVERE, "set hata", exception);
        return existing.get();
      }
      return existing.get().withBalance(normalized);
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public List<TopEntry> top(int limit, int offset) {
    List<TopEntry> result = new ArrayList<>();
    try (PreparedStatement ps = connection.prepareStatement(
        "SELECT uuid, name, balance FROM accounts ORDER BY CAST(balance AS REAL) DESC LIMIT ? OFFSET ?")) {
      ps.setInt(1, limit);
      ps.setInt(2, offset);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          result.add(new TopEntry(
            UUID.fromString(rs.getString("uuid")),
            rs.getString("name"),
            new BigDecimal(rs.getString("balance"))));
        }
      }
    } catch (SQLException exception) {
      logger.log(Level.SEVERE, "top hata", exception);
    }
    return result;
  }

  @Override
  public long accountCount() {
    try (Statement stmt = connection.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM accounts")) {
      return rs.next() ? rs.getLong(1) : 0L;
    } catch (SQLException exception) {
      logger.log(Level.WARNING, "accountCount hata", exception);
      return 0L;
    }
  }

  @Override
  public boolean tryReservePayLimit(UUID player, LocalDate day, BigDecimal amount, BigDecimal dailyCap) {
    writeLock.lock();
    try {
      BigDecimal current = BigDecimal.ZERO;
      try (PreparedStatement ps = connection.prepareStatement(
          "SELECT sent FROM pay_limits WHERE uuid = ? AND day = ?")) {
        ps.setString(1, player.toString());
        ps.setString(2, day.toString());
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            current = new BigDecimal(rs.getString("sent"));
          }
        }
      }
      BigDecimal next = normalize(current.add(amount));
      if (dailyCap.signum() > 0 && next.compareTo(dailyCap) > 0) {
        return false;
      }
      try (PreparedStatement ps = connection.prepareStatement(
          "INSERT INTO pay_limits(uuid, day, sent) VALUES(?, ?, ?) "
            + "ON CONFLICT(uuid, day) DO UPDATE SET sent = excluded.sent")) {
        ps.setString(1, player.toString());
        ps.setString(2, day.toString());
        ps.setString(3, next.toPlainString());
        ps.executeUpdate();
      }
      return true;
    } catch (SQLException exception) {
      logger.log(Level.SEVERE, "pay limit hata", exception);
      return false;
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public void recordTransaction(UUID from, UUID to, BigDecimal amount, String type, String meta) {
    try (PreparedStatement ps = connection.prepareStatement(
        "INSERT INTO transactions(from_uuid, to_uuid, amount, type, meta, ts) VALUES(?, ?, ?, ?, ?, ?)")) {
      ps.setString(1, from == null ? null : from.toString());
      ps.setString(2, to == null ? null : to.toString());
      ps.setString(3, normalize(amount).toPlainString());
      ps.setString(4, type);
      ps.setString(5, meta);
      ps.setLong(6, System.currentTimeMillis());
      ps.executeUpdate();
    } catch (SQLException exception) {
      logger.log(Level.WARNING, "tx log hata", exception);
    }
  }

  private Account mapAccount(ResultSet rs) throws SQLException {
    return new Account(
      UUID.fromString(rs.getString("uuid")),
      rs.getString("name"),
      new BigDecimal(rs.getString("balance")),
      rs.getLong("updated_at"));
  }

  private BigDecimal normalize(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }
    return value.setScale(SCALE, RoundingMode.HALF_UP);
  }
}
