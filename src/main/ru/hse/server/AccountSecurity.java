package ru.hse.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ru.hse.OperationResponse;

/** Tracks suspicious account activity and clears related state on logout. */
public class AccountSecurity implements IAccountChangeVerifier, IAccountAuthListener {
  private static final double SUSPECT_THRESHOLD = 100.0d;

  private final AccountServer accountServer;
  private final Map<String, Double> degreeOfSuspect = new ConcurrentHashMap<>();

  /**
   * Creates security helper bound to account server state.
   *
   * @param auth account server instance
   */
  public AccountSecurity(AccountServer auth) {
    this.accountServer = auth;
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
    if (accountServer.getActiveAccountCount() == 0) {
      degreeOfSuspect.clear();
      return;
    }
    degreeOfSuspect.remove(login);
  }
}
