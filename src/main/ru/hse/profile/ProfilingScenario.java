package ru.hse.profile;

import ru.hse.Account;
import ru.hse.OperationException;
import ru.hse.client.Client;
import ru.hse.server.Server;

public final class ProfilingScenario {
  private ProfilingScenario() {}

  public static void main(String[] args) throws Exception {
    int port = args.length > 0 ? Integer.parseInt(args[0]) : 7080;
    int users = args.length > 1 ? Integer.parseInt(args[1]) : 2000;
    int loops = args.length > 2 ? Integer.parseInt(args[2]) : 300000;

    Server server = new Server();
    server.start(port);
    server.waitStarted();

    try {
      Client client = new Client("http://localhost:" + port);

      for (int i = 0; i < users; i++) {
        runRegistrationScenario(client, i, i % 10 == 0);
      }

      for (int i = 0; i < loops; i++) {
        runLoginScenario(client, i % users, i % 25 == 0);
      }
    } finally {
      server.stop();
    }
  }

  private static void runRegistrationScenario(Client client, int index, boolean badLoginAttempt)
      throws OperationException {
    String login = "load_user_" + index;
    String password = "pwd_" + index;

    Account account = client.register(login, password);
    try {
      Client.deposit(account, 100);
      safeWithdraw(account, 200);
      safeWithdraw(account, 50);

      if (badLoginAttempt) {
        safeBadLogin(client, login);
      }
    } finally {
      client.logout(account);
    }
  }

  private static void runLoginScenario(Client client, int index, boolean badLoginAttempt)
      throws OperationException {
    String login = "load_user_" + index;
    String password = "pwd_" + index;

    Account account = client.login(login, password);
    try {
      Client.deposit(account, 100);
      safeWithdraw(account, 200);
      safeWithdraw(account, 50);

      if (badLoginAttempt) {
        safeBadLogin(client, login);
      }
    } finally {
      client.logout(account);
    }
  }

  private static void safeWithdraw(Account account, double amount) {
    try {
      Client.withdraw(account, amount);
    } catch (OperationException ignored) {
      // нагрузочный сценарий без assert
    }
  }

  private static void safeBadLogin(Client client, String login) {
    try {
      client.login(login, "wrong_password");
    } catch (OperationException ignored) {
      // важен сам факт неудачной авторизации
    }
  }
}
