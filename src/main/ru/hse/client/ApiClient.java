package ru.hse.client;

import okhttp3.*;
import ru.hse.IAccountDataSource;
import ru.hse.IAuthorizationSource;
import ru.hse.OperationResponse;

import java.io.IOException;
import java.net.ConnectException;

public class ApiClient implements IAccountDataSource, IAuthorizationSource {
    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "Content-Type";
    private final String connectionURI;
    private final OkHttpClient client;

    private String authJson(String login, String password) {
        return "{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}";
    }

    private String sessionJson(String login, long session) {
        return "{\"login\":\"" + login + "\",\"session\":\"" + session + "\"}";
    }

    private String amountJson(String login, long session, double amount) {
        return "{\"login\":\"" + login + "\",\"session\":\"" + session
            + "\",\"amount\":\"" + amount + "\"}";
    }

    public ApiClient(String url) {
        connectionURI = url;
        client = new OkHttpClient();
    }

    private OperationResponse post(String path, String jsonBody) {
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse(APPLICATION_JSON));

        Request request =
            new Request.Builder()
                .url(connectionURI + path)
                .post(body)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String result = response.body().string();
            return OperationResponse.fromString(result);
        } catch (ConnectException ce) {
            return new OperationResponse(OperationResponse.CONNECTION_ERROR, ce.getMessage());
        } catch (IOException e) {
            return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
        }
    }

    @Override
    public OperationResponse withdraw(String login, long session, double balance) {
        return post("/account/withdraw", amountJson(login, session, balance));
    }

    @Override
    public OperationResponse deposit(String login, long session, double balance) {
        return post("/account/deposit", amountJson(login, session, balance));
    }

    @Override
    public OperationResponse getBalance(String login, long session) {
        return post("/account/balance", sessionJson(login, session));
    }

    @Override
    public OperationResponse register(String login, String password) {
        return post("/register", authJson(login, password));
    }

    @Override
    public OperationResponse login(String login, String password) {
        return post("/login", authJson(login, password));
    }

    @Override
    public OperationResponse logout(String login, Long activeSession) {
        return post("/account/logout", sessionJson(login, activeSession));
    }
}
