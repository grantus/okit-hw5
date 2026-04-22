package ru.hse.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hse.Account;
import ru.hse.IAccountDataSource;
import ru.hse.IAuthorizationSource;
import ru.hse.OperationException;
import ru.hse.OperationResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AccountServer {
    private static final Logger log = LoggerFactory.getLogger(AccountServer.class);

    private final Lock accountsLock = new ReentrantLock();
    private final Map<String, Account> activeAccounts = new HashMap<>();
    private final Map<Long, Account> activeSessions = new HashMap<>();
    private final IAccountDataSource dataSource;
    private final IAuthorizationSource authSource;

    public AccountServer(IAuthorizationSource ast, IAccountDataSource dst) {
        this.authSource = ast;
        this.dataSource = dst;
        Runtime.getRuntime().addShutdownHook(new Thread(this::logoutAllSafely));
    }

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
            if (response.code() != OperationResponse.SUCCEED) {
                throw new OperationException(response);
            }

            Account account = new Account(login);
            account.initDataStorage(dataSource);
            account.setActiveSession(parseSessionId(response.body()));

            activeSessions.put(account.getActiveSession(), account);
            activeAccounts.put(login, account);
            return account;
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
        try {
            Account activeAccount = activeAccounts.get(login);
            if (activeAccount != null) {
                throw new OperationException(
                    new OperationResponse(
                        OperationResponse.ALREADY_LOGGED, activeAccount.getActiveSession()));
            }

            OperationResponse response = authSource.login(login, password);
            if (response.code() != OperationResponse.SUCCEED) {
                throw new OperationException(response);
            }

            Account account = new Account(login);
            account.initDataStorage(dataSource);
            account.setActiveSession(parseSessionId(response.body()));

            activeSessions.put(account.getActiveSession(), account);
            activeAccounts.put(login, account);
            return account;
        } finally {
            accountsLock.unlock();
        }
    }

    public Account testSession(String login, Long session) {
        if (login == null || session == null) {
            return null;
        }

        accountsLock.lock();
        try {
            Account account = activeSessions.get(session);
            if (account != null && Objects.equals(account.getLogin(), login)) {
                return account;
            }
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
            Account storedAccount = activeAccounts.get(account.getLogin());
            if (storedAccount == null || storedAccount.getActiveSession() == Account.NO_SESSION) {
                throw new OperationException(
                    new OperationResponse(OperationResponse.NO_USER_INCORRECT_PASSWORD, null));
            }

            OperationResponse response =
                authSource.logout(storedAccount.getLogin(), storedAccount.getActiveSession());
            if (response.code() != OperationResponse.SUCCEED) {
                throw new OperationException(response);
            }

            Long sessionId = storedAccount.getActiveSession();
            activeSessions.remove(sessionId);
            activeAccounts.remove(storedAccount.getLogin());
            storedAccount.invalidateSession();
            account.invalidateSession();
        } finally {
            accountsLock.unlock();
        }
    }

    public int getActiveAccountCount() {
        accountsLock.lock();
        try {
            return activeAccounts.size();
        } finally {
            accountsLock.unlock();
        }
    }

    private void logoutAllSafely() {
        ArrayList<Account> accountsToLogout;
        accountsLock.lock();
        try {
            accountsToLogout = new ArrayList<>(activeAccounts.values());
        } finally {
            accountsLock.unlock();
        }

        for (Account account : accountsToLogout) {
            try {
                logout(account);
            } catch (OperationException e) {
                log.error("Logout failed during shutdown", e);
            }
        }
    }

    private long parseSessionId(Object body) throws OperationException {
        if (body instanceof Long value) {
            return value;
        }
        if (body instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new OperationException(
                    new OperationResponse(OperationResponse.INCORRECT_RESPONSE, body));
            }
        }
        throw new OperationException(
            new OperationResponse(OperationResponse.INCORRECT_RESPONSE, body));
    }
}
