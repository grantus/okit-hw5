package ru.hse.client;

import ru.hse.*;
import java.util.*;

public class Client {
    private final AccountManager accountManager;

    public Client(IAuthorizationSource authSource, IAccountDataSource dataSource)
            throws OperationException {
        accountManager = new AccountManager(authSource, dataSource);
    }

    public Client(String url) throws OperationException {
        ApiClient baseApiClient = new ApiClient(url);
        accountManager = new AccountManager(baseApiClient, baseApiClient);
    }

    // for tests only, do not use in production code
    public final AccountManager getAccountManager() {
        return accountManager;
    }

    public Account register(String login, String password) throws OperationException {
        int size = accountManager.getExceptions().size();
        Account a = accountManager.register(login, password);
        if (a == null) {
            OperationException[] exs = accountManager.getExceptions().toArray(new OperationException[0]);
            for (int i = size; i < exs.length; i++) {
                OperationException oe = exs[i];
                switch (oe.response.code) {
                    case OperationResponse.NULL_ARGUMENT:
                    case OperationResponse.ALREADY_INITIATED:
                    case OperationResponse.UNDEFINED_ERROR:
                    case OperationResponse.CONNECTION_ERROR:
                        throw oe;
                    default:
                        System.err.println(oe.toString());
                }
            }
        }
        return a;
    }

    public Account login(String login, String password) throws OperationException {
        int size = accountManager.getExceptions().size();
        Account a = accountManager.login(login, password);
        if (a == null) {
            OperationException[] exs = accountManager.getExceptions().toArray(new OperationException[0]);
            for (int i = size; i < exs.length; i++) {
                OperationException oe = exs[i];
                switch (oe.response.code) {
                    case OperationResponse.NULL_ARGUMENT:
                    case OperationResponse.UNDEFINED_ERROR:
                    case OperationResponse.CONNECTION_ERROR:
                    case OperationResponse.ALREADY_LOGGED:
                    case OperationResponse.NO_USER_INCORRECT_PASSWORD:
                        throw oe;
                    default:
                        System.err.println(oe.toString());
                }
            }
        }
        return a;
    }

    public boolean logout(Account a) throws OperationException {
        int size = accountManager.getExceptions().size();
        if (!accountManager.logout(a)) {
            OperationException[] exs = accountManager.getExceptions().toArray(new OperationException[0]);
            for (int i = size; i < exs.length; i++) {
                OperationException oe = exs[i];
                switch (oe.response.code) {
                    case OperationResponse.UNDEFINED_ERROR:
                    case OperationResponse.NULL_ARGUMENT:
                    case OperationResponse.NOT_LOGGED:
                    case OperationResponse.INCORRECT_SESSION:
                    case OperationResponse.CONNECTION_ERROR:
                        throw oe;
                    default:
                        System.err.println(oe.toString());
                }
            }

            return false;
        }
        return true;
    }

    public static double getBalance(Account a) throws OperationException {
        OperationResponse response = a.getBalance();
        if (response.code == OperationResponse.SUCCEED) return (Double) response.body;
        if (response.code == OperationResponse.INCORRECT_RESPONSE)
            System.err.println(response.toString());
        else throw new OperationException(response);
        return Double.NaN;
    }

    public static double withdraw(Account a, double amound) throws OperationException {
        OperationResponse response = a.withdraw(amound);
        if (response.code == OperationResponse.SUCCEED) return (Double) response.body;
        if (response.code == OperationResponse.INCORRECT_RESPONSE)
            System.err.println(response.toString());
        else throw new OperationException(response);
        return Double.NaN;
    }

    public static double deposit(Account a, double amound) throws OperationException {
        OperationResponse response = a.deposit(amound);
        if (response.code == OperationResponse.SUCCEED) return (Double) response.body;
        if (response.code == OperationResponse.INCORRECT_RESPONSE)
            System.err.println(response.toString());
        else throw new OperationException(response);
        return Double.NaN;
    }
}
