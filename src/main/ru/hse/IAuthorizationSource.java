package ru.hse;

public interface IAuthorizationSource {
    OperationResponse register(String login, String password);

    OperationResponse login(String login, String password);

    OperationResponse logout(String login, Long activeSession);
}
