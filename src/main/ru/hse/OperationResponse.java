package ru.hse;

import java.io.*;
import java.util.Base64;

public record OperationResponse(int code, Object body) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MODE_STRING = 1;
    private static final int MODE_OBJECT = 2;
    private static final int MODE_RESPONSE = 3;

    public static final int SUCCEED = 0;
    public static final int ALREADY_LOGGED = 1;
    public static final int NOT_LOGGED = 2;
    public static final int NO_USER_INCORRECT_PASSWORD = 3;
    public static final int INCORRECT_RESPONSE = 4;
    public static final int UNDEFINED_ERROR = 5;
    public static final int INCORRECT_SESSION = 6;
    public static final int NO_MONEY = 7;
    public static final int ENCODING_ERROR = 8;
    public static final int ALREADY_INITIATED = 9;
    public static final int NULL_ARGUMENT = 10;
    public static final int CONNECTION_ERROR = 11;

    public static final OperationResponse ACCOUNT_MANAGER_RESPONSE =
        new OperationResponse(ALREADY_LOGGED, null);
    public static final OperationResponse NO_USER_INCORRECT_PASSWORD_RESPONSE =
        new OperationResponse(NO_USER_INCORRECT_PASSWORD, null);
    public static final OperationResponse UNDEFINED_ERROR_RESPONSE =
        new OperationResponse(UNDEFINED_ERROR, null);
    public static final OperationResponse NOT_LOGGED_RESPONSE =
        new OperationResponse(NOT_LOGGED, null);
    public static final OperationResponse INCORRECT_SESSION_RESPONSE =
        new OperationResponse(INCORRECT_SESSION, null);
    public static final OperationResponse SUCCEED_RESPONSE =
        new OperationResponse(SUCCEED, null);
    public static final OperationResponse NO_MONEY_RESPONSE =
        new OperationResponse(NO_MONEY, null);
    public static final OperationResponse ENCODING_ERROR_RESPONSE =
        new OperationResponse(ENCODING_ERROR, null);
    public static final OperationResponse ALREADY_INITIATED_RESPONSE =
        new OperationResponse(ALREADY_INITIATED, null);
    public static final OperationResponse NULL_ARGUMENT_EXCEPTION =
        new OperationResponse(NULL_ARGUMENT, null);
    public static final OperationResponse CONNECTION_ERROR_RESPONSE =
        new OperationResponse(CONNECTION_ERROR, null);

    public String toResultString() {
        StringBuilder sb = new StringBuilder(Integer.toString(code));

        if (body != null) {
            if (body instanceof OperationResponse response) {
                sb.append("|").append(MODE_RESPONSE).append("|");
                sb.append(response.toResultString());
            } else if (body instanceof String) {
                sb.append("|").append(MODE_STRING).append("|");
                sb.append(body);
            } else if (body instanceof Serializable serializableBody) {
                sb.append("|").append(MODE_OBJECT).append("|");
                try {
                    sb.append(responseToString(serializableBody));
                } catch (IOException e) {
                    return new OperationResponse(ENCODING_ERROR, e.getMessage()).toResultString();
                }
            } else {
                return new OperationResponse(
                    INCORRECT_RESPONSE,
                    "Body is not serializable: " + body.getClass().getName())
                    .toResultString();
            }
        }

        return sb.toString();
    }

    public static OperationResponse fromString(String str) {
        int ind = str.indexOf('|');

        try {
            if (ind > -1) {
                int code = Integer.parseInt(str.substring(0, ind));
                str = str.substring(ind + 1);

                ind = str.indexOf('|');
                int mode = Integer.parseInt(str.substring(0, ind));
                String payload = str.substring(ind + 1);

                if (mode == MODE_STRING) {
                    return new OperationResponse(code, payload);
                }
                if (mode == MODE_OBJECT) {
                    return new OperationResponse(code, responseFromString(payload));
                }
                if (mode == MODE_RESPONSE) {
                    return new OperationResponse(code, fromString(payload));
                }
            }

            int code = Integer.parseInt(str);
            if (code >= 0) {
                return new OperationResponse(code, null);
            }
        } catch (NumberFormatException | IOException | ClassNotFoundException e) {
            return new OperationResponse(UNDEFINED_ERROR, str);
        }

        return new OperationResponse(UNDEFINED_ERROR, str);
    }

    public static String codeToErrorMessage(int code) {
      return switch (code) {
        case SUCCEED -> "SUCCEED";
        case ALREADY_LOGGED -> "ALREADY LOGGED OR REGISTERED";
        case NOT_LOGGED -> "NOT LOGGED";
        case NO_USER_INCORRECT_PASSWORD -> "INCORRECT PASSWORD OR NO SUCH USER";
        case INCORRECT_RESPONSE -> "INCORRECT RESPONSE";
        case UNDEFINED_ERROR -> "UNDEFINED ERROR";
        case INCORRECT_SESSION -> "INCORRECT SESSION NUMBER";
        case NO_MONEY -> "NOT ENOUGH MONEY ON BALANCE";
        case ENCODING_ERROR -> "ENCODING CANNOT BE MADE";
        case ALREADY_INITIATED -> "ACCOUNT WAS ALREADY INITIATED";
        case NULL_ARGUMENT -> "Null argument is prohibited";
        case CONNECTION_ERROR -> "No connection to server";
        default -> "CODE_" + code;
      };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(codeToErrorMessage(code));
        if (body != null) {
            sb.append("[");
            sb.append(body);
            sb.append("]");
        }
        return sb.toString();
    }

    public static String responseToString(Serializable object) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    public static Object responseFromString(String value)
        throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(value);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return ois.readObject();
        }
    }
}
