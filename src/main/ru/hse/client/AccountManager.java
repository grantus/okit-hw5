package ru.hse.client;

import ru.hse.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class AccountManager {
    private IAuthorizationSource serverAuthData;
    private IAccountDataSource serverAccountsData;
    private final ConcurrentLinkedDeque<OperationException> exceptionsList =
            new ConcurrentLinkedDeque<>();
    private final Lock accountsLock = new ReentrantLock();
    private final Lock exceptionsLock = new ReentrantLock();
    private final HashMap<String, Account> activeAccounts = new HashMap<>();

    public AccountManager(IAuthorizationSource serv, IAccountDataSource serverAccountsData)
            throws OperationException {
        init(serv, serverAccountsData);
    }

    public static String getEncodedPassword(String password) {
        return password;
    }

    private void init(IAuthorizationSource authSource, IAccountDataSource dataSource)
            throws OperationException {
        if (this.serverAuthData != null || this.serverAccountsData != null)
            throw new OperationException(OperationResponse.ALREADY_INITIATED_RESPONSE);
        if (authSource == null || dataSource == null)
            throw new OperationException(OperationResponse.NULL_ARGUMENT_EXCEPTION);
        this.serverAuthData = authSource;
        this.serverAccountsData = dataSource;
    }

    public Account register(String login, String password) {
        if (login == null || password == null) {
            registerException(new OperationException(OperationResponse.NULL_ARGUMENT_EXCEPTION));
            return null;
        }
        accountsLock.lock();
        try {
            Account activeAccount = activeAccounts.get(login);
            if (activeAccount != null) {
                registerException(new OperationException(OperationResponse.ALREADY_INITIATED_RESPONSE));
                return null;
            }
            String hashed = getEncodedPassword(password);
            OperationResponse response = callRegister(login, hashed);
            if (response.code() == OperationResponse.SUCCEED) {
                Account a = (Account) response.body();
                activeAccounts.put(login, a);
                return a;
            } else {
                switch (response.code()) {
                    case OperationResponse.CONNECTION_ERROR:
                    case OperationResponse.UNDEFINED_ERROR:
                    case OperationResponse.INCORRECT_RESPONSE:
                    case OperationResponse.ALREADY_LOGGED:
                        registerException(new OperationException(response));
                        break;
                    default:
                        registerException(
                                new OperationException(
                                        new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response)));
                }
                return null;
            }
        } finally {
            accountsLock.unlock();
        }
    }

    public Account login(String login, String password) {
        if (login == null || password == null) {
            registerException(new OperationException(OperationResponse.NULL_ARGUMENT_EXCEPTION));
            return null;
        }
        accountsLock.lock();
        try {
            Account activeAccount = activeAccounts.get(login);
            if (activeAccount != null) {
                registerException(
                        new OperationException(
                                new OperationResponse(
                                        OperationResponse.ALREADY_LOGGED, activeAccount.getActiveSession())));
                return null;
            }
            String hashed = getEncodedPassword(password);
            OperationResponse response = callLogin(login, hashed);
            if (response.code() == OperationResponse.SUCCEED) {
                Account a = (Account) response.body();
                activeAccounts.put(login, a);
                return a;
            } else {
                switch (response.code()) {
                    case OperationResponse.CONNECTION_ERROR:
                    case OperationResponse.UNDEFINED_ERROR:
                    case OperationResponse.NO_USER_INCORRECT_PASSWORD:
                    case OperationResponse.INCORRECT_RESPONSE:
                    case OperationResponse.ALREADY_LOGGED:
                        registerException(new OperationException(response));
                        break;
                    default:
                        registerException(
                                new OperationException(
                                        new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response)));
                }
                return null;
            }
        } finally {
            accountsLock.unlock();
        }
    }

    private OperationResponse callRegister(String login, String password) {
        OperationResponse response = serverAuthData.register(login, password);
        if (response.code() == OperationResponse.SUCCEED) {
            Object answer = response.body();
            if (answer instanceof Long id) {
                try {
                  Account a = new Account(login);
                    a.setActiveSession(id);
                    a.initDataStorage(serverAccountsData);
                    return new OperationResponse(OperationResponse.SUCCEED, a);
                } catch (NumberFormatException nfe) {
                    return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, nfe.getMessage());
                }
            }
        } else {
            switch (response.code()) {
                case OperationResponse.CONNECTION_ERROR:
                case OperationResponse.UNDEFINED_ERROR:
                case OperationResponse.ALREADY_INITIATED:
                    return response;
            }
        }
        return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
    }

    private OperationResponse callLogin(String login, String password) {
        OperationResponse response = serverAuthData.login(login, password);
        switch (response.code()) {
            case OperationResponse.SUCCEED: {
                Object answer = response.body();
                if (answer instanceof Long) {
                    Account a = new Account(login);
                    a.setActiveSession((Long) answer);
                    a.initDataStorage(serverAccountsData);
                    return new OperationResponse(OperationResponse.SUCCEED, a);
                }
            }
            break;
            case OperationResponse.CONNECTION_ERROR:
            case OperationResponse.UNDEFINED_ERROR:
            case OperationResponse.NO_USER_INCORRECT_PASSWORD:
            case OperationResponse.ALREADY_LOGGED:
                return response;
        }
        return new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
    }

    private OperationResponse callLogout(Account a) {
        if (a.getActiveSession() == Account.NO_SESSION) return OperationResponse.NOT_LOGGED_RESPONSE;
        OperationResponse response = serverAuthData.logout(a.getLogin(), a.getActiveSession());
      return switch (response.code()) {
        case OperationResponse.SUCCEED, OperationResponse.CONNECTION_ERROR, OperationResponse.UNDEFINED_ERROR,
             OperationResponse.NOT_LOGGED, OperationResponse.INCORRECT_SESSION -> response;
        default -> new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response);
      };
    }

    public boolean logout(Account account) {
        if (account == null || account.getLogin() == null) {
            registerException(new OperationException(OperationResponse.NULL_ARGUMENT_EXCEPTION));
            return false;
        }
        Account b = activeAccounts.get(account.getLogin());
        if (b == null) {
            registerException(new OperationException(OperationResponse.NOT_LOGGED_RESPONSE));
            return false;
        }
        OperationResponse response = callLogout(b);
        if (response.code() == OperationResponse.SUCCEED) {
            activeAccounts.remove(account.getLogin());
            return true;
        } else {
            switch (response.code()) {
                case OperationResponse.CONNECTION_ERROR:
                case OperationResponse.UNDEFINED_ERROR:
                case OperationResponse.NOT_LOGGED:
                case OperationResponse.INCORRECT_SESSION:
                case OperationResponse.INCORRECT_RESPONSE:
                    registerException(new OperationException(response));
                    break;
                default:
                    registerException(
                            new OperationException(
                                    new OperationResponse(OperationResponse.INCORRECT_RESPONSE, response)));
            }
        }
        return false;
    }

    private void registerException(OperationException exception) {
        exceptionsLock.lock();
        try {
            exceptionsList.add(exception);
        } finally {
            exceptionsLock.unlock();
        }
    }

    public Collection<OperationException> getExceptions() {
        exceptionsLock.lock();
        try {
            return Collections.unmodifiableCollection(exceptionsList);
        } finally {
            exceptionsLock.unlock();
        }
    }
}
