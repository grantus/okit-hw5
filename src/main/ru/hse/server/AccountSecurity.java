package ru.hse.server;

import ru.hse.OperationResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AccountSecurity implements IAccountChangeVerifier, IAccountAuthListener {
    private static final double SUSPECT_THRESHOLD = 100.0d;

    private final AccountServer aserver;
    private final Map<String, Double> degreeOfSuspect = new ConcurrentHashMap<>();

    public AccountSecurity(AccountServer auth) {
        this.aserver = auth;
    }

    @Override
    public int approveChange(String user, double change) {
        if (testOperationIsSuspect(user) && change < 0) {
            return OperationResponse.UNDEFINED_ERROR;
        }
        upSuspectLevel(user, change);
        return OperationResponse.SUCCEED;
    }

    private void upSuspectLevel(String user, double change) {
        degreeOfSuspect.merge(user, Math.abs(change), Double::sum);
    }

    private boolean testOperationIsSuspect(String user) {
        degreeOfSuspect.putIfAbsent(user, 0d);
        return degreeOfSuspect.get(user) > SUSPECT_THRESHOLD;
    }

    @Override
    public void accountLogin(String login) {
        degreeOfSuspect.putIfAbsent(login, 0d);
    }

    @Override
    public void accountLogout(String login) {
        if (aserver.getActiveAccountCount() == 0) {
            degreeOfSuspect.clear();
            return;
        }
        degreeOfSuspect.remove(login);
    }
}
