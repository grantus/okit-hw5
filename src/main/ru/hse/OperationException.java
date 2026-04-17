package ru.hse;

public class OperationException extends Exception {
    public final OperationResponse response;

    public OperationException(OperationResponse resp) {
        response = resp;
    }

    public String toResultString() {
        return response.toResultString();
    }

    @Override
    public String toString() {
        return response.toString();
    }
}
