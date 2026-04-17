package ru.hse;

public interface IAccountDataSource {
    public OperationResponse withdraw(String login, long session, double balance);

    public OperationResponse deposit(String login, long session, double balance);

    public OperationResponse getBalance(String login, long session);
}
