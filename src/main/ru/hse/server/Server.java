package ru.hse.server;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class Server {
  private ServerStorage storage;
  private ApiServer apiServer;
  private final CountDownLatch startedLatch = new CountDownLatch(1);

  public void start(int port) {
    storage = new ServerStorage("accounts");
    ServerLogicProxy storageProxy = new ServerLogicProxy(storage);
    AccountServer server = new AccountServer(storage, storageProxy);
    AccountSecurity accountSecurity = new AccountSecurity(server);
    apiServer = new ApiServer(server, port);

    storage.setChangeVerifier(accountSecurity);
    apiServer.addAuthListener(accountSecurity);
    startedLatch.countDown();
  }

  public void stop() {
    if (apiServer != null) {
      apiServer.stop();
    }
    if (storage != null) {
      storage.close();
    }
  }

  public void waitStarted() {
    try {
      startedLatch.await();
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  public static void main(String[] args) {
    Server s = new Server();
    int port;

    if (args.length > 0) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        port = 7000;
      }
    } else {
      port = 7000;
    }

    s.start(port);

    try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
      System.out.print("Enter stop to stop a server: ");
      while (!scanner.nextLine().trim().equals("stop")) {
        System.out.print("Wrong command, enter stop to stop a server:");
      }
    }

    s.stop();
  }
}
