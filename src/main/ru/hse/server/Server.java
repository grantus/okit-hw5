package ru.hse.server;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    ServerStorage storage = null;
    ServerLogicProxy storageProxy = null;
    AccountServer server = null;
    ApiServer apiServer = null;
    AccountSecurity accountSecurity = null;

    public void start(int port) {
        storage = new ServerStorage("accounts");
        storageProxy = new ServerLogicProxy(storage);
        server = new AccountServer(storage, storageProxy);
        // security server, tends to be active only when accounts are logged
        accountSecurity = new AccountSecurity(server, storage);
        apiServer = new ApiServer(server, port);

        storage.setChangeVerifier(accountSecurity);
        apiServer.addAuthListener(accountSecurity);
    }

    public void stop() {
        apiServer.stop();
    }

    public static void main(String[] args) {
        Server s = new Server();
        int port = 7000;
        if (args.length>0)
            try{
                port = Integer.parseInt(args[0]);
            }catch(NumberFormatException ignore){}
        s.start(port);

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter stop to stop a server: ");
        while (!scanner.nextLine().trim().equals("stop")) {
            System.out.print("Wrong command, enter stop to stop a server:");
        }
        s.stop();
    }

    public void waitStarted() {
        while (!apiServer.isStarted) {
            synchronized (apiServer) {
                try {
                    apiServer.wait(500);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
