package ru.hse;

/** Provides registration, login, and logout operations. */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface IAuthorizationSource {
  /**
   * Registers a new account.
   *
   * @param login account login
   * @param password account password
   * @return operation response
   */
  OperationResponse register(String login, String password);

  /**
   * Logs in an existing account.
   *
   * @param login account login
   * @param password account password
   * @return operation response
   */
  OperationResponse login(String login, String password);

  /**
   * Logs out account session.
   *
   * @param login account login
   * @param activeSession active session identifier
   * @return operation response
   */
  OperationResponse logout(String login, Long activeSession);
}
