package ru.hse.server;

public interface IAccountChangeVerifier {
    public int approveChange(String user, double changeDelta);
}
