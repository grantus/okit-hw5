package ru.hse.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hse.Account;
import ru.hse.IAccountDataSource;
import ru.hse.IAuthorizationSource;
import ru.hse.OperationException;
import ru.hse.OperationResponse;

/** Client facade for account registration, authorization, and balance operations. */
public class Client {
  private static final Logger log = LoggerFactory.getLogger(Client.class);

  private final AccountManager accountManager;

  /**
   * Creates a client using explicit authorization and data sources.
   *
   * @param authSource authorization source
   * @param dataSource account data source
   * @throws OperationException if manager initialization fails
   */
  public Client(IAuthorizationSource authSource, IAccountDataSource dataSource)
      throws OperationException {
    accountManager = new AccountManager(authSource, dataSource);
  }

  /**
   * Creates a client using server URL.
   *
   * @param url server base URL
   * @throws OperationException if manager initialization fails
   */
  public Client(String url) throws OperationException {
    ApiClient baseApiClient = new ApiClient(url);
    accountManager = new AccountManager(baseApiClient, baseApiClient);
  }

  /**
   * Returns account manager instance.
   *
   * @return account manager
   */
  public final AccountManager getAccountManager() {
    return accountManager;
  }

  /**
   * Registers a new account.
   *
   * @param login user login
   * @param password user password
   * @return created account
   * @throws OperationException if registration fails
   */
  public Account register(String login, String password) throws OperationException {
    int size = accountManager.getExceptions().size();
    Account account = accountManager.register(login, password);

    if (account == null) {
      OperationException[] exceptions =
          accountManager.getExceptions().toArray(new OperationException[0]);
      for (int i = size; i < exceptions.length; i++) {
        OperationException operationException = exceptions[i];
        switch (operationException.response.code()) {
          case OperationResponse.NULL_ARGUMENT:
          case OperationResponse.ALREADY_INITIATED:
          case OperationResponse.UNDEFINED_ERROR:
          case OperationResponse.CONNECTION_ERROR:
            throw operationException;
          default:
            log.error("e: ", operationException);
        }
      }
    }

    return account;
  }

  /**
   * Logs in an existing account.
   *
   * @param login user login
   * @param password user password
   * @return logged in account
   * @throws OperationException if login fails
   */
  public Account login(String login, String password) throws OperationException {
    int size = accountManager.getExceptions().size();
    Account account = accountManager.login(login, password);

    if (account == null) {
      OperationException[] exceptions =
          accountManager.getExceptions().toArray(new OperationException[0]);
      for (int i = size; i < exceptions.length; i++) {
        OperationException operationException = exceptions[i];
        switch (operationException.response.code()) {
          case OperationResponse.NULL_ARGUMENT:
          case OperationResponse.UNDEFINED_ERROR:
          case OperationResponse.CONNECTION_ERROR:
          case OperationResponse.ALREADY_LOGGED:
          case OperationResponse.NO_USER_INCORRECT_PASSWORD:
            throw operationException;
          default:
            log.error("e: ", operationException);
        }
      }
    }

    return account;
  }

  /**
   * Logs out the specified account.
   *
   * @param account account to log out
   * @return {@code true} if logout succeeded, otherwise {@code false}
   * @throws OperationException if logout fails
   */
  public boolean logout(Account account) throws OperationException {
    int size = accountManager.getExceptions().size();

    if (!accountManager.logout(account)) {
      OperationException[] exceptions =
          accountManager.getExceptions().toArray(new OperationException[0]);
      for (int i = size; i < exceptions.length; i++) {
        OperationException operationException = exceptions[i];
        switch (operationException.response.code()) {
          case OperationResponse.UNDEFINED_ERROR:
          case OperationResponse.NULL_ARGUMENT:
          case OperationResponse.NOT_LOGGED:
          case OperationResponse.INCORRECT_SESSION:
          case OperationResponse.CONNECTION_ERROR:
            throw operationException;
          default:
            log.error("e: ", operationException);
        }
      }
      return false;
    }

    return true;
  }

  /**
   * Returns current account balance.
   *
   * @param account account to inspect
   * @return current balance
   * @throws OperationException if operation fails
   */
  public static double getBalance(Account account) throws OperationException {
    OperationResponse response = account.getBalance();
    if (response.code() == OperationResponse.SUCCEED) {
      return (Double) response.body();
    }
    if (response.code() == OperationResponse.INCORRECT_RESPONSE) {
      System.err.println(response);
    } else {
      throw new OperationException(response);
    }
    return Double.NaN;
  }

  /**
   * Withdraws money from the account.
   *
   * @param account account to update
   * @param amount amount to withdraw
   * @return resulting balance
   * @throws OperationException if operation fails
   */
  public static double withdraw(Account account, double amount) throws OperationException {
    OperationResponse response = account.withdraw(amount);
    if (response.code() == OperationResponse.SUCCEED) {
      return (Double) response.body();
    }
    if (response.code() == OperationResponse.INCORRECT_RESPONSE) {
      System.err.println(response);
    } else {
      throw new OperationException(response);
    }
    return Double.NaN;
  }

  /**
   * Deposits money to the account.
   *
   * @param account account to update
   * @param amount amount to deposit
   * @return resulting balance
   * @throws OperationException if operation fails
   */
  public static double deposit(Account account, double amount) throws OperationException {
    OperationResponse response = account.deposit(amount);
    if (response.code() == OperationResponse.SUCCEED) {
      return (Double) response.body();
    }
    if (response.code() == OperationResponse.INCORRECT_RESPONSE) {
      System.err.println(response);
    } else {
      throw new OperationException(response);
    }
    return Double.NaN;
  }
}
