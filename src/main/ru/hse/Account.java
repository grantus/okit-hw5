package ru.hse;

import java.util.Random;

public class Account {
    private IAccountDataSource storage;
    protected final String login;
    private String[] privateKey = new String[256];
    public Long activeSession = null;

    public final String getLogin() {
        return login;
    }

    public final Long getActiveSession() {
        return activeSession;
    }

    public Account(String login) {
        this.login = login;
        Random r = new Random(login.hashCode());
        for(int i = 0;i<privateKey.length;i++)
            privateKey[i] = Long.toString(r.nextLong())+Double.toString(r.nextDouble());
    }

    public OperationResponse withdraw(double amount) {
        if (storage == null) return OperationResponse.CONNECTION_ERROR_RESPONSE;
        if (activeSession == null) return OperationResponse.NOT_LOGGED_RESPONSE;
        OperationResponse response = storage.withdraw(login, activeSession, amount);
        switch (response.code) {
            case OperationResponse.CONNECTION_ERROR:
                return OperationResponse.CONNECTION_ERROR_RESPONSE;
            case OperationResponse.INCORRECT_SESSION:
                return OperationResponse.INCORRECT_SESSION_RESPONSE;
            case OperationResponse.NOT_LOGGED:
                return OperationResponse.NOT_LOGGED_RESPONSE;
            case OperationResponse.NO_MONEY:
                Object r = response.body;
                if (r != null && r instanceof Double)
                    return new OperationResponse(OperationResponse.NO_MONEY, r);
                break;
            case OperationResponse.UNDEFINED_ERROR:
                return response;
            case OperationResponse.SUCCEED:
                r = response.body;
                if (r != null && r instanceof Double)
                    return new OperationResponse(OperationResponse.SUCCEED, r);
                break;
        }
        return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
    }

    public OperationResponse deposit(double amount) {
        if (storage == null) return OperationResponse.CONNECTION_ERROR_RESPONSE;
        if (activeSession == null) return OperationResponse.NOT_LOGGED_RESPONSE;
        OperationResponse response = storage.deposit(login, activeSession, amount);
        switch (response.code) {
            case OperationResponse.CONNECTION_ERROR:
                return OperationResponse.CONNECTION_ERROR_RESPONSE;
            case OperationResponse.NOT_LOGGED:
                return OperationResponse.NOT_LOGGED_RESPONSE;
            case OperationResponse.UNDEFINED_ERROR:
                return response;
            case OperationResponse.SUCCEED:
                Object r = response.body;
                if (r != null && r instanceof Double)
                    return new OperationResponse(OperationResponse.SUCCEED, r);
                break;
        }
        return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
    }

    public OperationResponse getBalance() {
        if (storage == null) return OperationResponse.CONNECTION_ERROR_RESPONSE;
        if (activeSession == null) return OperationResponse.NOT_LOGGED_RESPONSE;
        OperationResponse response = storage.getBalance(login, activeSession);
        switch (response.code) {
            case OperationResponse.CONNECTION_ERROR:
                return OperationResponse.CONNECTION_ERROR_RESPONSE;
            case OperationResponse.NOT_LOGGED:
                return OperationResponse.NOT_LOGGED_RESPONSE;
            case OperationResponse.INCORRECT_SESSION:
                return OperationResponse.INCORRECT_SESSION_RESPONSE;
            case OperationResponse.UNDEFINED_ERROR:
                return response;
            case OperationResponse.SUCCEED:
                Object r = response.body;
                if (r != null && r instanceof Double)
                    return new OperationResponse(OperationResponse.SUCCEED, r);
        }
        return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
    }

    public void initDataStorage(IAccountDataSource serverAccountsData) {
        this.storage = serverAccountsData;
    }
}
