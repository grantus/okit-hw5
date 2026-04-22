package ru.hse.server;

public interface IAccountChangeVerifier {
    int approveChange(String user, double changeDelta);
}
