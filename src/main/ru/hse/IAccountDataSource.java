package ru.hse;

/** Provides account balance operations for a given login and session. */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface IAccountDataSource {
  /**
   * Withdraws money from account balance.
   *
   * @param login account login
   * @param session session identifier
   * @param balance amount to withdraw
   * @return operation response
   */
  OperationResponse withdraw(String login, long session, double balance);

  /**
   * Deposits money to account balance.
   *
   * @param login account login
   * @param session session identifier
   * @param balance amount to deposit
   * @return operation response
   */
  OperationResponse deposit(String login, long session, double balance);

  /**
   * Returns current account balance.
   *
   * @param login account login
   * @param session session identifier
   * @return operation response with balance
   */
  OperationResponse getBalance(String login, long session);
}
