package ru.hse.client;

import okhttp3.*;
import ru.hse.IAccountDataSource;
import ru.hse.IAuthorizationSource;
import ru.hse.OperationResponse;

import java.io.IOException;
import java.net.ConnectException;

public class ApiClient implements IAccountDataSource, IAuthorizationSource {
    private final String connectionURI;
    private final OkHttpClient client;

    public ApiClient(String url) {
        connectionURI = url;
        client = new OkHttpClient();
    }

    @Override
    public OperationResponse withdraw(String login, long session, double balance) {
        String jsonBody =
                "{\"login\":\""
                        + login
                        + "\",\"session\":\""
                        + session
                        + "\",\"amount\":\""
                        + balance
                        + "\"}";
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request =
                new Request.Builder()
                        .url(connectionURI + "/account/withdraw")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();
        try {
            try (Response response = client.newCall(request).execute()) {
                String result = response.body().string();
                return OperationResponse.fromString(result);
            }
        } catch (ConnectException ce) {
            return new OperationResponse(OperationResponse.CONNECTION_ERROR, ce.getMessage());
        } catch (IOException e) {
            return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
        }
    }

    @Override
    public OperationResponse deposit(String login, long session, double balance) {
        String jsonBody =
                "{\"login\":\""
                        + login
                        + "\",\"session\":\""
                        + session
                        + "\",\"amount\":\""
                        + balance
                        + "\"}";
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request =
                new Request.Builder()
                        .url(connectionURI + "/account/deposit")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();
        try {
            try (Response response = client.newCall(request).execute()) {
                String result = response.body().string();
                return OperationResponse.fromString(result);
            }
        } catch (ConnectException ce) {
            return new OperationResponse(OperationResponse.CONNECTION_ERROR, ce.getMessage());
        } catch (IOException e) {
            return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
        }
    }

    @Override
    public OperationResponse getBalance(String login, long session) {
        String jsonBody = "{\"login\":\"" + login + "\",\"session\":\"" + session + "\"}";
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request =
                new Request.Builder()
                        .url(connectionURI + "/account/balance")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();
        try {
            try (Response response = client.newCall(request).execute()) {
                String result = response.body().string();
                return OperationResponse.fromString(result);
            }
        } catch (ConnectException ce) {
            return new OperationResponse(OperationResponse.CONNECTION_ERROR, ce.getMessage());
        } catch (IOException e) {
            return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
        }
    }

    @Override
    public OperationResponse register(String login, String password) {
        String jsonBody = "{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request =
                new Request.Builder()
                        .url(connectionURI + "/register")
                        .method("post", body)
                        .addHeader("Content-Type", "application/json")
                        .build();
        try {
            try (Response response = client.newCall(request).execute()) {
                String result = response.body().string();
                return OperationResponse.fromString(result);
            }
        } catch (ConnectException ce) {
            return new OperationResponse(OperationResponse.CONNECTION_ERROR, ce.getMessage());
        } catch (IOException e) {
            return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
        }
    }

    @Override
    public OperationResponse login(String login, String password) {
        String jsonBody = "{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request =
                new Request.Builder()
                        .url(connectionURI + "/login")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();
        try {
            try (Response response = client.newCall(request).execute()) {
                String result = response.body().string();
                return OperationResponse.fromString(result);
            }
        } catch (ConnectException ce) {
            return new OperationResponse(OperationResponse.CONNECTION_ERROR, ce.getMessage());
        } catch (IOException e) {
            return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
        }
    }

    @Override
    public OperationResponse logout(String login, Long activeSession) {
        String jsonBody = "{\"login\":\"" + login + "\",\"session\":\"" + activeSession + "\"}";
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request =
                new Request.Builder()
                        .url(connectionURI + "/account/logout")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();
        try {
            try (Response response = client.newCall(request).execute()) {
                String result = response.body().string();
                return OperationResponse.fromString(result);
            }
        } catch (ConnectException ce) {
            return new OperationResponse(OperationResponse.CONNECTION_ERROR, ce.getMessage());
        } catch (IOException e) {
            return new OperationResponse(OperationResponse.UNDEFINED_ERROR, e.getMessage());
        }
    }
}
