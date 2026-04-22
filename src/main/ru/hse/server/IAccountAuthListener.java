package ru.hse.server;

public interface IAccountAuthListener {
    void accountLogin(String login);

    void accountLogout(String login);
}
