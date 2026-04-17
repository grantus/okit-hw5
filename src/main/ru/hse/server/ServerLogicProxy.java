package ru.hse.server;

import ru.hse.IAccountDataSource;
import ru.hse.OperationResponse;

public class ServerLogicProxy implements IAccountDataSource {
    private final IAccountDataSource dataSource;

    public ServerLogicProxy(ServerStorage source) {
        dataSource = source;
    }

    @Override
    public OperationResponse withdraw(String login, long session, double delta) {
        OperationResponse balanceResponse = dataSource.getBalance(login, session);
        if (balanceResponse.code == OperationResponse.SUCCEED) {
            Double amount = (Double) balanceResponse.body;
            if (delta > amount) return new OperationResponse(OperationResponse.NO_MONEY, amount);
            return dataSource.withdraw(login, session, delta);
        }
        return balanceResponse;
    }

    @Override
    public OperationResponse deposit(String login, long session, double delta) {
        return dataSource.deposit(login, session, delta);
    }

    @Override
    public OperationResponse getBalance(String login, long session) {
        return dataSource.getBalance(login, session);
    }
}
