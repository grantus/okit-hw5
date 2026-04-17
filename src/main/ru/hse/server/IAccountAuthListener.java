package ru.hse.server;

public interface IAccountAuthListener {
    public void accountLogin(String login);

    public void accountLogout(String login);
}
