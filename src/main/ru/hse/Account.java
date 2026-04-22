package ru.hse;

/** Represents client account state and delegates balance operations to data source. */
public class Account {
  public static final long NO_SESSION = -1L;

  private IAccountDataSource storage;
  protected final String login;
  private long activeSession = NO_SESSION;

  /**
   * Creates account object for the specified login.
   *
   * @param login account login
   */
  public Account(String login) {
    this.login = login;
  }

  /**
   * Returns account login.
   *
   * @return account login
   */
  public final String getLogin() {
    return login;
  }

  /**
   * Returns current active session identifier.
   *
   * @return active session id
   */
  public final long getActiveSession() {
    return activeSession;
  }

  /**
   * Sets active session identifier.
   *
   * @param session session id
   */
  public void setActiveSession(long session) {
    this.activeSession = session;
  }

  /**
   * Invalidates active session.
   */
  public void invalidateSession() {
    this.activeSession = NO_SESSION;
  }

  /**
   * Withdraws amount from account balance.
   *
   * @param amount amount to withdraw
   * @return operation response
   */
  public OperationResponse withdraw(double amount) {
    if (storage == null) {
      return OperationResponse.CONNECTION_ERROR_RESPONSE;
    }
    if (activeSession == NO_SESSION) {
      return OperationResponse.NOT_LOGGED_RESPONSE;
    }

    OperationResponse response = storage.withdraw(login, activeSession, amount);
    Object body = response.body();

    switch (response.code()) {
      case OperationResponse.CONNECTION_ERROR:
        return OperationResponse.CONNECTION_ERROR_RESPONSE;
      case OperationResponse.INCORRECT_SESSION:
        return OperationResponse.INCORRECT_SESSION_RESPONSE;
      case OperationResponse.NOT_LOGGED:
        return OperationResponse.NOT_LOGGED_RESPONSE;
      case OperationResponse.NO_MONEY:
        if (body instanceof Double) {
          return new OperationResponse(OperationResponse.NO_MONEY, body);
        }
        break;
      case OperationResponse.UNDEFINED_ERROR:
        return response;
      case OperationResponse.SUCCEED:
        if (body instanceof Double) {
          return new OperationResponse(OperationResponse.SUCCEED, body);
        }
        break;
      default:
        break;
    }

    return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
  }

  /**
   * Deposits amount to account balance.
   *
   * @param amount amount to deposit
   * @return operation response
   */
  public OperationResponse deposit(double amount) {
    if (storage == null) {
      return OperationResponse.CONNECTION_ERROR_RESPONSE;
    }
    if (activeSession == NO_SESSION) {
      return OperationResponse.NOT_LOGGED_RESPONSE;
    }

    OperationResponse response = storage.deposit(login, activeSession, amount);
    Object body = response.body();

    switch (response.code()) {
      case OperationResponse.CONNECTION_ERROR:
        return OperationResponse.CONNECTION_ERROR_RESPONSE;
      case OperationResponse.NOT_LOGGED:
        return OperationResponse.NOT_LOGGED_RESPONSE;
      case OperationResponse.UNDEFINED_ERROR:
        return response;
      case OperationResponse.SUCCEED:
        if (body instanceof Double) {
          return new OperationResponse(OperationResponse.SUCCEED, body);
        }
        break;
      default:
        break;
    }

    return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
  }

  /**
   * Returns current account balance.
   *
   * @return operation response with balance
   */
  public OperationResponse getBalance() {
    if (storage == null) {
      return OperationResponse.CONNECTION_ERROR_RESPONSE;
    }
    if (activeSession == NO_SESSION) {
      return OperationResponse.NOT_LOGGED_RESPONSE;
    }

    OperationResponse response = storage.getBalance(login, activeSession);
    Object body = response.body();

    switch (response.code()) {
      case OperationResponse.CONNECTION_ERROR:
        return OperationResponse.CONNECTION_ERROR_RESPONSE;
      case OperationResponse.NOT_LOGGED:
        return OperationResponse.NOT_LOGGED_RESPONSE;
      case OperationResponse.INCORRECT_SESSION:
        return OperationResponse.INCORRECT_SESSION_RESPONSE;
      case OperationResponse.UNDEFINED_ERROR:
        return response;
      case OperationResponse.SUCCEED:
        if (body instanceof Double) {
          return new OperationResponse(OperationResponse.SUCCEED, body);
        }
        break;
      default:
        break;
    }

    return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
  }

  /**
   * Initializes backing data source for account operations.
   *
   * @param serverAccountsData account data source
   */
  public void initDataStorage(IAccountDataSource serverAccountsData) {
    this.storage = serverAccountsData;
  }
}
