package ru.hse;

public class Account {
    public static final long NO_SESSION = -1L;

    private IAccountDataSource storage;
    protected final String login;
    private long activeSession = NO_SESSION;

    public Account(String login) {
        this.login = login;
    }

    public final String getLogin() {
        return login;
    }

    public final long getActiveSession() {
        return activeSession;
    }

    public void setActiveSession(long session) {
        this.activeSession = session;
    }

    public void invalidateSession() {
        this.activeSession = NO_SESSION;
    }

    public OperationResponse withdraw(double amount) {
        if (storage == null) {
            return OperationResponse.CONNECTION_ERROR_RESPONSE;
        }
        if (activeSession == NO_SESSION) {
            return OperationResponse.NOT_LOGGED_RESPONSE;
        }

        OperationResponse response = storage.withdraw(login, activeSession, amount);
        Object body = response.body();

        switch (response.code()) {
            case OperationResponse.CONNECTION_ERROR:
                return OperationResponse.CONNECTION_ERROR_RESPONSE;
            case OperationResponse.INCORRECT_SESSION:
                return OperationResponse.INCORRECT_SESSION_RESPONSE;
            case OperationResponse.NOT_LOGGED:
                return OperationResponse.NOT_LOGGED_RESPONSE;
            case OperationResponse.NO_MONEY:
                if (body instanceof Double) {
                    return new OperationResponse(OperationResponse.NO_MONEY, body);
                }
                break;
            case OperationResponse.UNDEFINED_ERROR:
                return response;
            case OperationResponse.SUCCEED:
                if (body instanceof Double) {
                    return new OperationResponse(OperationResponse.SUCCEED, body);
                }
                break;
            default:
                break;
        }

        return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
    }

    public OperationResponse deposit(double amount) {
        if (storage == null) {
            return OperationResponse.CONNECTION_ERROR_RESPONSE;
        }
        if (activeSession == NO_SESSION) {
            return OperationResponse.NOT_LOGGED_RESPONSE;
        }

        OperationResponse response = storage.deposit(login, activeSession, amount);
        Object body = response.body();

        switch (response.code()) {
            case OperationResponse.CONNECTION_ERROR:
                return OperationResponse.CONNECTION_ERROR_RESPONSE;
            case OperationResponse.NOT_LOGGED:
                return OperationResponse.NOT_LOGGED_RESPONSE;
            case OperationResponse.UNDEFINED_ERROR:
                return response;
            case OperationResponse.SUCCEED:
                if (body instanceof Double) {
                    return new OperationResponse(OperationResponse.SUCCEED, body);
                }
                break;
            default:
                break;
        }

        return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
    }

    public OperationResponse getBalance() {
        if (storage == null) {
            return OperationResponse.CONNECTION_ERROR_RESPONSE;
        }
        if (activeSession == NO_SESSION) {
            return OperationResponse.NOT_LOGGED_RESPONSE;
        }

        OperationResponse response = storage.getBalance(login, activeSession);
        Object body = response.body();

        switch (response.code()) {
            case OperationResponse.CONNECTION_ERROR:
                return OperationResponse.CONNECTION_ERROR_RESPONSE;
            case OperationResponse.NOT_LOGGED:
                return OperationResponse.NOT_LOGGED_RESPONSE;
            case OperationResponse.INCORRECT_SESSION:
                return OperationResponse.INCORRECT_SESSION_RESPONSE;
            case OperationResponse.UNDEFINED_ERROR:
                return response;
            case OperationResponse.SUCCEED:
                if (body instanceof Double) {
                    return new OperationResponse(OperationResponse.SUCCEED, body);
                }
                break;
            default:
                break;
        }

        return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
    }

    public void initDataStorage(IAccountDataSource serverAccountsData) {
        this.storage = serverAccountsData;
    }
}
