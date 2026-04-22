package ru.hse.server;

/** Verifies whether an account balance change is allowed. */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface IAccountChangeVerifier {
  /**
   * Approves or rejects account balance change.
   *
   * @param user account login
   * @param changeDelta requested balance change
   * @return operation response code
   */
  int approveChange(String user, double changeDelta);
}
