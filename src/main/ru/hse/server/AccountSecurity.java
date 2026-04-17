package ru.hse.server;

import ru.hse.OperationResponse;

import java.util.HashMap;
import java.util.Map;

public class AccountSecurity implements IAccountChangeVerifier, IAccountAuthListener {
    private final AccountServer aserver;
    private final ServerStorage data;
    private Map<String, Double> degreeOfSuspect = new HashMap<>();
    private Map<String, Double> maxChange = new HashMap<>();

    public AccountSecurity(AccountServer auth, ServerStorage data) {
        this.aserver = auth;
        this.data = data;
    }

    @Override
    public int approveChange(String user, double change) {
        if (testOperationIsSuspect(user, change) && change < 0)
            return OperationResponse.UNDEFINED_ERROR;
        upSuspectLevel(user, change);
        return OperationResponse.SUCCEED;
    }

    private void upSuspectLevel(String user, double change) {
    }

    private boolean testOperationIsSuspect(String user, double change) {
        Double dos = degreeOfSuspect.get(user);
        if (dos == null) {
            synchronized (degreeOfSuspect) {
                degreeOfSuspect.put(user, 0d);
            }
        } else {
            if (dos > 100d) return true;
        }
        return false;
    }

    @Override
    public void accountLogin(String login) {
    }

    @Override
    public void accountLogout(String login) {
    }
}
