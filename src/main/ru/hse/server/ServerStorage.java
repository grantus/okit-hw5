package ru.hse.server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Properties;
import org.mindrot.jbcrypt.BCrypt;
import ru.hse.IAccountDataSource;
import ru.hse.IAuthorizationSource;
import ru.hse.OperationResponse;

/** Stores accounts and sessions in the database. */
public final class ServerStorage
    implements IAccountDataSource, IAuthorizationSource, AutoCloseable {
  private HikariDataSource source;
  private final String filename;
  private IAccountChangeVerifier verifier;

  /**
   * Creates storage and initializes database structures.
   *
   * @param filename database file name
   */
  public ServerStorage(String filename) {
    this.filename = filename;
    setupDatabase();
    initDatabase();
  }

  /**
   * Sets balance change verifier.
   *
   * @param accountSecurity verifier implementation
   */
  public void setChangeVerifier(IAccountChangeVerifier accountSecurity) {
    this.verifier = accountSecurity;
  }

  @Override
  public OperationResponse withdraw(String login, long session, double balance) {
    if (verifier != null) {
      int ret = verifier.approveChange(login, -balance);
      if (ret != OperationResponse.SUCCEED) {
        if (ret == OperationResponse.NO_MONEY) {
          return new OperationResponse(ret, getBalance(login, session));
        }
        return new OperationResponse(ret, null);
      }
    }

    String sql = "UPDATE accounts SET amount = amount - ? WHERE login = ?";
    try (Connection conn = source.getConnection()) {
      OperationResponse sessionResponse = validateSession(conn, login, session);
      if (sessionResponse.code() != OperationResponse.SUCCEED) {
        return sessionResponse;
      }

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setDouble(1, balance);
        stmt.setString(2, login);
        int rs = stmt.executeUpdate();
        if (rs > 0) {
          return getBalance(login, session);
        }
      }
      return new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null);
    } catch (SQLException e) {
      return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
    }
  }

  @Override
  public OperationResponse deposit(String login, long session, double balance) {
    if (verifier != null) {
      int ret = verifier.approveChange(login, balance);
      if (ret != OperationResponse.SUCCEED) {
        if (ret == OperationResponse.NO_MONEY) {
          return new OperationResponse(ret, getBalance(login, session));
        }
        return new OperationResponse(ret, null);
      }
    }

    String sql = "UPDATE accounts SET amount = amount + ? WHERE login = ?";
    try (Connection conn = source.getConnection()) {
      OperationResponse sessionResponse = validateSession(conn, login, session);
      if (sessionResponse.code() != OperationResponse.SUCCEED) {
        return sessionResponse;
      }

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setDouble(1, balance);
        stmt.setString(2, login);
        int rs = stmt.executeUpdate();
        if (rs > 0) {
          return getBalance(login, session);
        }
      }
      return new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null);
    } catch (SQLException e) {
      return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
    }
  }

  @Override
  public OperationResponse getBalance(String login, long session) {
    String sql = "SELECT amount FROM accounts WHERE login = ?";
    try (Connection conn = source.getConnection()) {
      OperationResponse sessionResponse = validateSession(conn, login, session);
      if (sessionResponse.code() != OperationResponse.SUCCEED) {
        return sessionResponse;
      }

      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, login);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return new OperationResponse(OperationResponse.SUCCEED, rs.getDouble(1));
          }
        }
      }
      return new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null);
    } catch (SQLException e) {
      return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
    }
  }

  @Override
  public OperationResponse register(String login, String password) {
    if (login == null || password == null) {
      return OperationResponse.NULL_ARGUMENT_EXCEPTION;
    }

    String sql = "SELECT login FROM accounts WHERE login = ?";
    try (Connection conn = source.getConnection()) {
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, login);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            return new OperationResponse(OperationResponse.ALREADY_LOGGED, null);
          }
        }
      }

      String insertSql = "INSERT INTO accounts(login, password_hash, amount) VALUES (?, ?, 0)";
      try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
        stmt.setString(1, login);
        stmt.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
        int cnt = stmt.executeUpdate();
        if (cnt > 0) {
          return initSession(conn, login);
        }
        return new OperationResponse(OperationResponse.ALREADY_LOGGED, null);
      }
    } catch (SQLException e) {
      return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
    }
  }

  @Override
  public OperationResponse login(String login, String password) {
    if (login == null || password == null) {
      return OperationResponse.NULL_ARGUMENT_EXCEPTION;
    }

    String sql = "SELECT password_hash FROM accounts WHERE login = ?";
    try (Connection conn = source.getConnection()) {
      try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, login);
        try (ResultSet rs = stmt.executeQuery()) {
          if (rs.next()) {
            String storedHash = rs.getString("password_hash");
            if (matchesPassword(password, storedHash)) {
              return initSession(conn, login);
            }
          }
        }
        return new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null);
      }
    } catch (SQLException e) {
      return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
    }
  }

  @Override
  public OperationResponse logout(String login, Long activeSession) {
    if (login == null || activeSession == null) {
      return OperationResponse.NULL_ARGUMENT_EXCEPTION;
    }

    try (Connection conn = source.getConnection()) {
      OperationResponse sessionResponse = validateSession(conn, login, activeSession);
      if (sessionResponse.code() != OperationResponse.SUCCEED) {
        return sessionResponse;
      }
      return stopSession(conn, activeSession);
    } catch (SQLException e) {
      return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
    }
  }

  @Override
  public void close() {
    if (source != null) {
      source.close();
    }
  }

  private void setupDatabase() {
    String pass = "";
    String username = "SA";

    try (InputStream input =
             Thread.currentThread().getContextClassLoader().getResourceAsStream("db.properties")) {
      Properties prop = new Properties();
      if (input != null) {
        prop.load(input);
        pass = prop.getProperty("db.password", "");
        username = prop.getProperty("db.user", "SA");
      }
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to read db.properties", ex);
    }

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:hsqldb:file:./" + filename + ";shutdown=true");
    config.setUsername(username);
    config.setPassword(pass);
    config.setMaximumPoolSize(10);
    config.setConnectionTimeout(30000);
    config.setLeakDetectionThreshold(10000);
    source = new HikariDataSource(config);
  }

  private void initDatabase() {
    String createSessionsTable =
        """
        CREATE TABLE IF NOT EXISTS sessions (
          id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
          login VARCHAR(50) UNIQUE NOT NULL
        )
        """;

    String createAccountsTable =
        """
        CREATE TABLE IF NOT EXISTS accounts(
          login VARCHAR(50) UNIQUE NOT NULL,
          password_hash VARCHAR(60) NOT NULL,
          amount DOUBLE DEFAULT 0 NOT NULL
        )
        """;

    try (Connection conn = source.getConnection();
         Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(createAccountsTable);
      stmt.executeUpdate(createSessionsTable);
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось инициализировать БД", e);
    }
  }

  private OperationResponse initSession(Connection conn, String login) {
    String sql = "INSERT INTO sessions (login) VALUES (?)";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, login);
      int cnt = stmt.executeUpdate();
      if (cnt == 0) {
        return new OperationResponse(OperationResponse.ALREADY_LOGGED, null);
      }
    } catch (SQLIntegrityConstraintViolationException notFirst) {
      return new OperationResponse(OperationResponse.ALREADY_LOGGED, notFirst.getMessage());
    } catch (SQLException e) {
      return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
    }

    sql = "SELECT id FROM sessions WHERE login = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, login);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return new OperationResponse(OperationResponse.SUCCEED, rs.getLong("id"));
        }
      }
    } catch (SQLException e) {
      return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
    }

    return new OperationResponse(OperationResponse.UNDEFINED_ERROR, null);
  }

  private OperationResponse stopSession(Connection conn, Long sessId) {
    String sql = "DELETE FROM sessions WHERE id = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, sessId);
      int cnt = stmt.executeUpdate();
      if (cnt == 0) {
        return new OperationResponse(OperationResponse.NOT_LOGGED, null);
      }
      return new OperationResponse(OperationResponse.SUCCEED, null);
    } catch (SQLException e) {
      return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
    }
  }

  private OperationResponse validateSession(Connection conn, String login, long session) {
    String sql = "SELECT id FROM sessions WHERE id = ? AND login = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, session);
      stmt.setString(2, login);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return OperationResponse.SUCCEED_RESPONSE;
        }
      }
      return OperationResponse.NOT_LOGGED_RESPONSE;
    } catch (SQLException e) {
      return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
    }
  }

  private boolean matchesPassword(String password, String storedHash) {
    if (storedHash == null) {
      return false;
    }
    if (storedHash.startsWith("$2")) {
      return BCrypt.checkpw(password, storedHash);
    }
    return password.equals(storedHash);
  }
}
