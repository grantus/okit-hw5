package ru.hse.server;

import io.javalin.Javalin;
import io.javalin.http.Context;
import ru.hse.Account;
import ru.hse.OperationException;
import ru.hse.OperationResponse;

import java.util.Collection;
import java.util.LinkedList;

public class ApiServer {
  private static final String BAD_AUTH_MESSAGE = "Username и password обязательны";
  private static final String BAD_SESSION_MESSAGE =
      "Произошла ошибка при разлогировании. Возможно номер сессии указан некорреткно "
          + "или аккаунт/сессия не существуют";

  private final AccountServer server;
  private final Javalin app;
  private final LinkedList<IAccountAuthListener> listenerList = new LinkedList<>();

  public ApiServer(AccountServer server, int port) {
    this.server = server;
    this.app = Javalin.create(config -> {}).start(port);
    configureRoutes();

    System.out.println("Server is running on http://localhost:" + port);
  }

  public synchronized boolean addAuthListener(IAccountAuthListener list) {
    return listenerList.add(list);
  }

  public synchronized boolean removeAuthListener(IAccountAuthListener list) {
    return listenerList.remove(list);
  }

  public synchronized Collection<IAccountAuthListener> getAuthListeners() {
    return new LinkedList<>(listenerList);
  }

  public void stop() {
    app.stop();
  }

  private void configureRoutes() {
    app.get(
        "/",
        ctx ->
            ctx.result(
                "Server is online. Possible routes are: /register, /login, "
                    + "/account/logout, /account/withdraw, /account/deposit, "
                    + "/account/balance"));

    app.post("/register", this::handleRegister);
    app.post("/login", this::handleLogin);
    app.post("/account/logout", this::handleLogout);
    app.post("/account/withdraw", this::handleWithdraw);
    app.post("/account/deposit", this::handleDeposit);
    app.post("/account/balance", this::handleBalance);
  }

  private void handleRegister(Context ctx) {
    AuthRequest req = ctx.bodyAsClass(AuthRequest.class);
    if (isValidAuthRequest(req)) {
      ctx.result(new OperationResponse(OperationResponse.NOT_LOGGED, BAD_AUTH_MESSAGE).toResultString());
      return;
    }

    try {
      Account account = server.register(req.login, req.password);
      ctx.result(new OperationResponse(OperationResponse.SUCCEED, account.getActiveSession()).toResultString());
    } catch (OperationException e) {
      ctx.result(e.toResultString());
    }
  }

  private void handleLogin(Context ctx) {
    AuthRequest req = ctx.bodyAsClass(AuthRequest.class);
    if (isValidAuthRequest(req)) {
      ctx.result(new OperationResponse(OperationResponse.NOT_LOGGED, BAD_AUTH_MESSAGE).toResultString());
      return;
    }

    try {
      Account account = server.login(req.login, req.password);
      for (IAccountAuthListener listener : getAuthListeners()) {
        listener.accountLogin(req.login);
      }
      ctx.result(new OperationResponse(OperationResponse.SUCCEED, account.getActiveSession()).toResultString());
    } catch (OperationException e) {
      ctx.result(e.toResultString());
    }
  }

  private void handleLogout(Context ctx) {
    LoggedRequest req = ctx.bodyAsClass(LoggedRequest.class);
    try {
      Account acc = server.testSession(req.login, req.session);
      if (acc == null) {
        ctx.result(new OperationResponse(OperationResponse.NOT_LOGGED, BAD_SESSION_MESSAGE).toResultString());
        return;
      }

      server.logout(acc);
      for (IAccountAuthListener listener : getAuthListeners()) {
        listener.accountLogout(req.login);
      }

      if (ctx.req().getSession(false) != null) {
        ctx.req().getSession().invalidate();
      }

      ctx.result(Integer.toString(OperationResponse.SUCCEED));
    } catch (OperationException e) {
      ctx.result(e.toResultString());
    }
  }

  private void handleWithdraw(Context ctx) {
    LoggedRequestDouble req = ctx.bodyAsClass(LoggedRequestDouble.class);
    Account acc = server.testSession(req.login, req.session);
    if (acc == null) {
      ctx.result(new OperationResponse(OperationResponse.NOT_LOGGED, BAD_SESSION_MESSAGE).toResultString());
      return;
    }

    OperationResponse res = acc.withdraw(req.amount);
    ctx.result(res.toResultString());
  }

  private void handleDeposit(Context ctx) {
    LoggedRequestDouble req = ctx.bodyAsClass(LoggedRequestDouble.class);
    Account acc = server.testSession(req.login, req.session);
    if (acc == null) {
      ctx.result(new OperationResponse(OperationResponse.NOT_LOGGED, BAD_SESSION_MESSAGE).toResultString());
      return;
    }

    OperationResponse res = acc.deposit(req.amount);
    ctx.result(res.toResultString());
  }

  private void handleBalance(Context ctx) {
    LoggedRequest req = ctx.bodyAsClass(LoggedRequest.class);
    Account acc = server.testSession(req.login, req.session);
    if (acc == null) {
      ctx.result(new OperationResponse(OperationResponse.NOT_LOGGED, BAD_SESSION_MESSAGE).toResultString());
      return;
    }

    OperationResponse res = acc.getBalance();
    ctx.result(res.toResultString());
  }

  private boolean isValidAuthRequest(AuthRequest req) {
    return req != null
        && req.login != null
        && req.password != null
        && !req.login.isBlank()
        && !req.password.isBlank();
  }

  private static class AuthRequest {
    public String login;
    public String password;
  }

  private static class LoggedRequest {
    public String login;
    public long session;
  }

  private static class LoggedRequestDouble {
    public String login;
    public double amount;
    public long session;
  }
}
