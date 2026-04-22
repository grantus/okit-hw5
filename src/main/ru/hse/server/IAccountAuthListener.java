package ru.hse.server;

/** Listener for account authorization events. */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface IAccountAuthListener {
  /**
   * Handles account login event.
   *
   * @param login account login
   */
  void accountLogin(String login);

  /**
   * Handles account logout event.
   *
   * @param login account login
   */
  void accountLogout(String login);
}
