package ru.hse;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.*;
import java.util.Base64;

public class OperationResponse {
    public static final int SUCCEED = 0,
            ALREADY_LOGGED = 1,
            NOT_LOGGED = 2,
            NO_USER_INCORRECT_PASSWORD = 3,
            INCORRECT_RESPONSE = 4,
            UNDEFINED_ERROR = 5,
            INCORRECT_SESSION = 6,
            NO_MONEY = 7,
            ENCODING_ERROR = 8,
            ALREADY_INITIATED = 9,
            NULL_ARGUMENT = 10,
            CONNECTION_ERROR = 11;
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
    public static final OperationResponse SUCCEED_RESPONSE = new OperationResponse(SUCCEED, null);
    public static final OperationResponse NO_MONEY_RESPONSE = new OperationResponse(NO_MONEY, null);
    public static final OperationResponse ENCODING_ERROR_RESPONSE =
            new OperationResponse(ENCODING_ERROR, null);
    public static final OperationResponse ALREADY_INITIATED_RESPONSE =
            new OperationResponse(ALREADY_INITIATED, null);
    public static final OperationResponse NULL_ARGUMENT_EXCEPTION =
            new OperationResponse(NULL_ARGUMENT, null);
    public static final OperationResponse CONNECTION_ERROR_RESPONSE =
            new OperationResponse(CONNECTION_ERROR, null);
    public final int code;
    public final Object body;

    public OperationResponse(int code, Object obj) {
        this.code = code;
        this.body = obj;
    }

    public String toResultString() {
        StringBuilder sb = new StringBuilder(Integer.toString(code));
        if (body != null) {
            if (body instanceof OperationResponse) {
                sb.append("|3|");
                sb.append(((OperationResponse) body).toResultString());
            } else if (body instanceof String) {
                sb.append("|1|");
                sb.append(body);
            } else {
                sb.append("|2|");
                try {
                    sb.append(responceToString((Serializable) body));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return sb.toString();
    }

    public static OperationResponse fromString(String str) {
        int ind = str.indexOf("|");
        try {
            if (ind > -1) {
                int code = Integer.parseInt(str.substring(0, ind));
                str = str.substring(ind + 1);
                ind = str.indexOf("|");
                int mode = Integer.parseInt(str.substring(0, ind));
                if (mode == 1) return new OperationResponse(code, str.substring(ind + 1));
                else if (mode == 2) {
                    return new OperationResponse(code, responceFromString(str.substring(ind + 1)));
                } else if (mode == 3) {
                    return new OperationResponse(code, fromString(str.substring(ind + 1)));
                }
            }
            int code = Integer.parseInt(str);
            if (code >= 0) return new OperationResponse(code, null);
        } catch (NumberFormatException | IOException one) {
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return new OperationResponse(UNDEFINED_ERROR, str);
    }

    public static String codeToErrorMessage(int code) {
        switch (code) {
            case OperationResponse.SUCCEED:
                return "SUCCEED";
            case OperationResponse.ALREADY_LOGGED:
                return "ALREADY LOGGED OR REGISTERED";
            case OperationResponse.NOT_LOGGED:
                return "NOT LOGGED";
            case OperationResponse.NO_USER_INCORRECT_PASSWORD:
                return "INCORRECT PASSWORD OR NO SUCH USER";
            case OperationResponse.INCORRECT_RESPONSE:
                return "INCORRECT RESPONCE";
            case OperationResponse.UNDEFINED_ERROR:
                return "UNDEFINED ERROR";
            case OperationResponse.INCORRECT_SESSION:
                return "INCORRECT SESSION NUMBER";
            case OperationResponse.NO_MONEY:
                return "NOT ENOUGH MONEYS ON BALANCE";
            case OperationResponse.ENCODING_ERROR:
                return "ENCODING CANNOT BE MADE";
            case OperationResponse.ALREADY_INITIATED:
                return "ACCOUNT WAS ALREADY INITIATED";
            case OperationResponse.NULL_ARGUMENT:
                return "Null argument is prohibited";
            case OperationResponse.CONNECTION_ERROR:
                return "No connection to server";
        }
        return "CODE_" + Integer.toString(code);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(codeToErrorMessage(code));
        if (body != null) {
            sb.append("[");
            sb.append(body.toString());
            sb.append("]");
        }
        return sb.toString();
    }

    public static String responceToString(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /**
     * Deserializes a Base64 encoded string back to an object.
     */
    @SuppressFBWarnings("SECOBDES")
    public static Object responceFromString(String s) throws IOException, ClassNotFoundException {
        byte[] data = Base64.getDecoder().decode(s);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o = ois.readObject();
        ois.close();
        return o;
    }
}
