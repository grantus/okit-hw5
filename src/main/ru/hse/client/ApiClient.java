package ru.hse.client;

import java.io.IOException;
import java.net.ConnectException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import ru.hse.IAccountDataSource;
import ru.hse.IAuthorizationSource;
import ru.hse.OperationResponse;

/** HTTP client for server authorization and account operations. */
public class ApiClient implements IAccountDataSource, IAuthorizationSource {
  private static final String APPLICATION_JSON = "application/json";
  private static final String CONTENT_TYPE = "Content-Type";
  private final String connectionUri;
  private final OkHttpClient client;

  /**
   * Creates API client bound to the provided server URL.
   *
   * @param url server base URL
   */
  public ApiClient(String url) {
    connectionUri = url;
    client = new OkHttpClient();
  }

  private String authJson(String login, String password) {
    return "{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}";
  }

  private String sessionJson(String login, long session) {
    return "{\"login\":\"" + login + "\",\"session\":\"" + session + "\"}";
  }

  private String amountJson(String login, long session, double amount) {
    return "{\"login\":\""
        + login
        + "\",\"session\":\""
        + session
        + "\",\"amount\":\""
        + amount
        + "\"}";
  }

  /**
   * Sends POST request to the server and parses operation response.
   *
   * @param path endpoint path
   * @param jsonBody request body in JSON form
   * @return parsed server response
   */
  private OperationResponse post(String path, String jsonBody) {
    RequestBody body = RequestBody.create(jsonBody, MediaType.parse(APPLICATION_JSON));

    Request request =
        new Request.Builder()
            .url(connectionUri + path)
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
