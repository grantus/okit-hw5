package ru.hse;

public interface IAccountDataSource {
    OperationResponse withdraw(String login, long session, double balance);

    OperationResponse deposit(String login, long session, double balance);

    OperationResponse getBalance(String login, long session);
}
