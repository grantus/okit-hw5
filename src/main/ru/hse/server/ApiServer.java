package ru.hse.server;

import io.javalin.Javalin;
import ru.hse.Account;
import ru.hse.OperationException;
import ru.hse.OperationResponse;

import java.util.Collection;
import java.util.LinkedList;

public class ApiServer {
    private final AccountServer server;
    private final Javalin app;
    private final LinkedList<IAccountAuthListener> listenerList =
            new LinkedList<IAccountAuthListener>();

    public boolean isStarted;

    public ApiServer(AccountServer server, int port) {
        this.server = server;
        app = Javalin.create(config -> {
        }).start(port);

        // ----- Публичные эндпоинты -----
        app.get(
                "/",
                ctx ->
                        ctx.result(
                                "Server is online. Possible routes are: /register, /login, /account/logout, /account/withdraw, /account/deposit, /account/balance"));

        // Регистрация нового пользователя
        app.post(
                "/register",
                ctx -> {
                    AuthRequest req = ctx.bodyAsClass(AuthRequest.class);
                    if (req.login == null
                            || req.password == null
                            || req.login.isBlank()
                            || req.password.isBlank()) {
                        ctx.result(
                                new OperationResponse(
                                        OperationResponse.NOT_LOGGED, "Username и password обязательны")
                                        .toResultString());
                        return;
                    }
                    try {
                        Account account = server.register(req.login, req.password);
                        if (account != null)
                            ctx.result(
                                    new OperationResponse(OperationResponse.SUCCEED, account.getActiveSession())
                                            .toResultString());
                        else
                            ctx.result(
                                    new OperationResponse(
                                            OperationResponse.NOT_LOGGED,
                                            "Произошла ошибка при регистрации. Возможно аккаунт уже существует")
                                            .toResultString());
                    } catch (OperationException ofe) {
                        ctx.result(ofe.toResultString());
                    }
                });
        app.post(
                "/login",
                ctx -> {
                    AuthRequest req = ctx.bodyAsClass(AuthRequest.class);
                    if (req.login == null
                            || req.password == null
                            || req.login.isBlank()
                            || req.password.isBlank()) {
                        ctx.result(
                                new OperationResponse(
                                        OperationResponse.NOT_LOGGED, "Username и password обязательны")
                                        .toResultString());
                        return;
                    }
                    try {
                        Account account = server.login(req.login, req.password);
                        for (IAccountAuthListener list : getAuthListeners()) list.accountLogin(req.login);
                        if (account != null) {
                            ctx.result(
                                    new OperationResponse(OperationResponse.SUCCEED, account.getActiveSession())
                                            .toResultString());
                        } else
                            ctx.result(
                                    new OperationResponse(
                                            OperationResponse.NOT_LOGGED,
                                            "Произошла ошибка при регистрации. Возможно аккаунт уже существует")
                                            .toResultString());
                    } catch (OperationException ofe) {
                        ctx.result(ofe.toResultString());
                    }
                });

        // Выход из системы
        app.post(
                "/account/logout",
                ctx -> {
                    LoggedRequest req = ctx.bodyAsClass(LoggedRequest.class);
                    try {
                        Account acc = server.testSession(req.login, req.session);
                        if (acc != null) {
                            server.logout(acc);
                            for (IAccountAuthListener list : getAuthListeners()) list.accountLogout(req.login);
                            ctx.req().getSession().invalidate();
                            ctx.result(Integer.toString(OperationResponse.SUCCEED));
                        } else
                            ctx.result(
                                    new OperationResponse(
                                            OperationResponse.NOT_LOGGED,
                                            "Произошла ошибка при разлогировании. Возможно номер сессии указан некорреткно или аккаунт/сессия не существуют")
                                            .toResultString());
                    } catch (OperationException ofe) {
                        ctx.result(ofe.toResultString());
                    }
                });

        app.post(
                "/account/withdraw",
                ctx -> {
                    LoggedRequestDouble req = ctx.bodyAsClass(LoggedRequestDouble.class);
                    Account acc = server.testSession(req.login, req.session);
                    if (acc != null) {
                        OperationResponse res = acc.withdraw(req.amount);
                        ctx.result(res.toResultString());
                    } else
                        ctx.result(
                                new OperationResponse(
                                        OperationResponse.NOT_LOGGED,
                                        "Произошла ошибка при разлогировании. Возможно номер сессии указан некорреткно или аккаунт/сессия не существуют")
                                        .toResultString());
                });
        app.post(
                "/account/deposit",
                ctx -> {
                    LoggedRequestDouble req = ctx.bodyAsClass(LoggedRequestDouble.class);
                    Account acc = server.testSession(req.login, req.session);
                    if (acc != null) {

                        OperationResponse res = acc.deposit(req.amount);
                        ctx.result(res.toResultString());
                    } else
                        ctx.result(
                                new OperationResponse(
                                        OperationResponse.NOT_LOGGED,
                                        "Произошла ошибка при разлогировании. Возможно номер сессии указан некорреткно или аккаунт/сессия не существуют")
                                        .toResultString());
                });
        app.post(
                "/account/balance",
                ctx -> {
                    LoggedRequestDouble req = ctx.bodyAsClass(LoggedRequestDouble.class);
                    Account acc = server.testSession(req.login, req.session);
                    if (acc != null) {
                        OperationResponse res = acc.getBalance();
                        ctx.result(res.toResultString());
                    } else
                        ctx.result(
                                new OperationResponse(
                                        OperationResponse.NOT_LOGGED,
                                        "Произошла ошибка при разлогировании. Возможно номер сессии указан некорреткно или аккаунт/сессия не существуют")
                                        .toResultString());
                });
        synchronized (this) {
            isStarted = true;
            this.notifyAll();
        }
        System.out.println("Server is running on http://localhost:"+port);
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

    public void stop() {
        app.stop();
    }
}
