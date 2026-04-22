package ru.hse.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hse.Account;
import ru.hse.IAccountDataSource;
import ru.hse.IAuthorizationSource;
import ru.hse.OperationException;
import ru.hse.OperationResponse;

/** Manages active accounts and sessions on the server side. */
public class AccountServer {
  private static final Logger log = LoggerFactory.getLogger(AccountServer.class);

  private final Lock accountsLock = new ReentrantLock();
  private final Map<String, Account> activeAccounts = new HashMap<>();
  private final Map<Long, Account> activeSessions = new HashMap<>();
  private final IAccountDataSource dataSource;
  private final IAuthorizationSource authSource;

  /**
   * Creates account server using authorization and account data sources.
   *
   * @param ast authorization source
   * @param dst account data source
   */
  public AccountServer(IAuthorizationSource ast, IAccountDataSource dst) {
    this.authSource = ast;
    this.dataSource = dst;
    Runtime.getRuntime().addShutdownHook(new Thread(this::logoutAllSafely));
  }

  /**
   * Registers a new account and activates its session.
   *
   * @param login user login
   * @param password user password
   * @return created account
   * @throws OperationException if registration fails
   */
  public Account register(String login, String password) throws OperationException {
    if (login == null || password == null) {
      throw new OperationException(OperationResponse.NULL_ARGUMENT_EXCEPTION);
    }

    accountsLock.lock();
    try {
      Account activeAccount = activeAccounts.get(login);
      if (activeAccount != null) {
        throw new OperationException(OperationResponse.ALREADY_INITIATED_RESPONSE);
      }

      OperationResponse response = authSource.register(login, password);
      if (response.code() != OperationResponse.SUCCEED) {
        throw new OperationException(response);
      }

      Account account = new Account(login);
      account.initDataStorage(dataSource);
      account.setActiveSession(parseSessionId(response.body()));

      activeSessions.put(account.getActiveSession(), account);
      activeAccounts.put(login, account);
      return account;
    } finally {
      accountsLock.unlock();
    }
  }

  /**
   * Logs in an existing account and activates its session.
   *
   * @param login user login
   * @param password user password
   * @return logged in account
   * @throws OperationException if login fails
   */
  public Account login(String login, String password) throws OperationException {
    if (login == null || password == null) {
      throw new OperationException(
          new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null));
    }

    accountsLock.lock();
    try {
      Account activeAccount = activeAccounts.get(login);
      if (activeAccount != null) {
        throw new OperationException(
            new OperationResponse(
                OperationResponse.ALREADY_LOGGED, activeAccount.getActiveSession()));
      }

      OperationResponse response = authSource.login(login, password);
      if (response.code() != OperationResponse.SUCCEED) {
        throw new OperationException(response);
      }

      Account account = new Account(login);
      account.initDataStorage(dataSource);
      account.setActiveSession(parseSessionId(response.body()));

      activeSessions.put(account.getActiveSession(), account);
      activeAccounts.put(login, account);
      return account;
    } finally {
      accountsLock.unlock();
    }
  }

  /**
   * Returns active account by login and session.
   *
   * @param login user login
   * @param session session identifier
   * @return matching account or {@code null}
   */
  public Account testSession(String login, Long session) {
    if (login == null || session == null) {
      return null;
    }

    accountsLock.lock();
    try {
      Account account = activeSessions.get(session);
      if (account != null && Objects.equals(account.getLogin(), login)) {
        return account;
      }
      return null;
    } finally {
      accountsLock.unlock();
    }
  }

  /**
   * Logs out the specified account and removes it from active collections.
   *
   * @param account account to log out
   * @throws OperationException if logout fails
   */
  public void logout(Account account) throws OperationException {
    if (account == null || account.getLogin() == null) {
      throw new OperationException(
          new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null));
    }

    accountsLock.lock();
    try {
      Account storedAccount = activeAccounts.get(account.getLogin());
      if (storedAccount == null || storedAccount.getActiveSession() == Account.NO_SESSION) {
        throw new OperationException(
            new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null));
      }

      OperationResponse response =
          authSource.logout(storedAccount.getLogin(), storedAccount.getActiveSession());
      if (response.code() != OperationResponse.SUCCEED) {
        throw new OperationException(response);
      }

      Long sessionId = storedAccount.getActiveSession();
      activeSessions.remove(sessionId);
      activeAccounts.remove(storedAccount.getLogin());
      storedAccount.invalidateSession();
      account.invalidateSession();
    } finally {
      accountsLock.unlock();
    }
  }

  /**
   * Returns number of currently active accounts.
   *
   * @return active account count
   */
  public int getActiveAccountCount() {
    accountsLock.lock();
    try {
      return activeAccounts.size();
    } finally {
      accountsLock.unlock();
    }
  }

  /**
   * Parses session id from response body.
   *
   * @param body response body
   * @return parsed session id
   * @throws OperationException if body cannot be converted to session id
   */
  private long parseSessionId(Object body) throws OperationException {
    if (body instanceof Long value) {
      return value;
    }
    if (body instanceof String value) {
      try {
        return Long.parseLong(value);
      } catch (NumberFormatException e) {
        throw new OperationException(
            new OperationResponse(OperationResponse.INCORRECT_RESPONSE, body));
      }
    }
    throw new OperationException(new OperationResponse(OperationResponse.INCORRECT_RESPONSE, body));
  }

  private void logoutAllSafely() {
    ArrayList<Account> accountsToLogout;
    accountsLock.lock();
    try {
      accountsToLogout = new ArrayList<>(activeAccounts.values());
    } finally {
      accountsLock.unlock();
    }

    for (Account account : accountsToLogout) {
      try {
        logout(account);
      } catch (OperationException e) {
        log.error("Logout failed during shutdown", e);
      }
    }
  }
}
