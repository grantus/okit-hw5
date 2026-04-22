package ru.hse;

import java.io.Serial;

public class OperationException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

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
