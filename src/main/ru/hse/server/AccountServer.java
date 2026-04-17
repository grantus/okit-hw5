package ru.hse.server;

import ru.hse.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AccountServer {
    private final Lock accountsLock = new ReentrantLock();
    private final HashMap<String, Account> activeAccounts = new HashMap<>();
    private final IAccountDataSource dataSource;
    private final IAuthorizationSource authSource;

    public AccountServer(IAuthorizationSource ast, IAccountDataSource dst) {
        this.authSource = ast;
        this.dataSource = dst;
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        accountsLock.lock();
                                        try {
                                            for (Account a : activeAccounts.values()) {
                                                try {
                                                    logout(a);
                                                } catch (OperationException e) {
                                                    System.err.println(e.toString());
                                                }
                                            }
                                        } finally {
                                            accountsLock.unlock();
                                        }
                                    }
                                }));
    }

    private final Map<Long, Account> activeSessions = new HashMap<>();
    private final Map<Long, Boolean> sessionEnabled = new HashMap<>();

    public Account register(String login, String password) throws OperationException {
        if (login == null || password == null) {
            throw new OperationException(OperationResponse.NULL_ARGUMENT_EXCEPTION);
        }
        accountsLock.lock();
        try {
            Account activeAccount = activeAccounts.get(login);
            if (activeAccount != null) {
                throw new OperationException(OperationResponse.ALREADY_INITIATED_RESPONSE);
            }
            OperationResponse response = authSource.register(login, password);
            if (response.code == OperationResponse.SUCCEED) {
                Account a = new Account(login);
                a.initDataStorage(dataSource);
                a.activeSession = Long.parseLong((String) response.body);
                activeSessions.put(a.activeSession, a);
                sessionEnabled.put(a.activeSession, true);
                activeAccounts.put(login, a);
                return a;
            } else throw new OperationException(response);
        } finally {
            accountsLock.unlock();
        }
    }

    public Account login(String login, String password) throws OperationException {
        if (login == null || password == null) {
            throw new OperationException(
                    new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null));
        }
        accountsLock.lock();
        Account activeAccount = activeAccounts.get(login);
        if (activeAccount != null) {
            throw new OperationException(
                    new OperationResponse(OperationResponse.ALREADY_LOGGED, activeAccount.activeSession));
        }
        try {
            OperationResponse storedResponse = authSource.login(login, password);
            if (storedResponse.code == OperationResponse.SUCCEED) {
                Account a = new Account(login);
                a.initDataStorage(dataSource);
                a.activeSession = Long.parseLong((String) storedResponse.body);
                activeSessions.put(a.activeSession, a);
                sessionEnabled.put(a.activeSession, true);
                activeAccounts.put(login, a);
                return a;
            } else throw new OperationException(storedResponse);
        } finally {
            accountsLock.unlock();
        }
    }

    public Account testSession(String login, Long session) {
        accountsLock.lock();
        Account activeAccount = activeAccounts.get(login);
        try {
            if (activeAccount != null && Objects.equals(activeAccount.activeSession, session))
                return activeAccount;
            return null;
        } finally {
            accountsLock.unlock();
        }
    }

    public void logout(Account account) throws OperationException {
        if (account == null || account.getLogin() == null) {
            throw new OperationException(
                    new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null));
        }
        accountsLock.lock();
        try {

            Account b = activeAccounts.get(account.getLogin());
            if (b == null) {
                throw new OperationException(
                        new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null));
            }
            OperationResponse storedResponse = authSource.logout(b.getLogin(), b.activeSession);
            if (storedResponse.code == OperationResponse.SUCCEED) {
                sessionEnabled.put(account.activeSession, false);
                activeAccounts.remove(account.getLogin());
            } else throw new OperationException(storedResponse);
        } finally {
            accountsLock.unlock();
        }
    }
}
