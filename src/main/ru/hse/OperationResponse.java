package ru.hse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Base64;
import org.jetbrains.annotations.NotNull;

/** Represents result of account operation with optional payload. */
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

  /**
   * Converts response to string form suitable for transport.
   *
   * @return serialized response string
   */
  public String toResultString() {
    StringBuilder stringBuilder = new StringBuilder(Integer.toString(code));

    if (body != null) {
      if (body instanceof OperationResponse response) {
        stringBuilder.append("|").append(MODE_RESPONSE).append("|");
        stringBuilder.append(response.toResultString());
      } else if (body instanceof String) {
        stringBuilder.append("|").append(MODE_STRING).append("|");
        stringBuilder.append(body);
      } else if (body instanceof Serializable serializableBody) {
        stringBuilder.append("|").append(MODE_OBJECT).append("|");
        try {
          stringBuilder.append(responseToString(serializableBody));
        } catch (IOException exception) {
          return new OperationResponse(ENCODING_ERROR, exception.getMessage()).toResultString();
        }
      } else {
        return new OperationResponse(
            INCORRECT_RESPONSE,
            "Body is not serializable: " + body.getClass().getName())
            .toResultString();
      }
    }

    return stringBuilder.toString();
  }

  /**
   * Restores response from its string form.
   *
   * @param str serialized response string
   * @return parsed operation response
   */
  public static OperationResponse fromString(String str) {
    int separatorIndex = str.indexOf('|');

    try {
      if (separatorIndex > -1) {
        int responseCode = Integer.parseInt(str.substring(0, separatorIndex));
        str = str.substring(separatorIndex + 1);

        separatorIndex = str.indexOf('|');
        int mode = Integer.parseInt(str.substring(0, separatorIndex));
        String payload = str.substring(separatorIndex + 1);

        if (mode == MODE_STRING) {
          return new OperationResponse(responseCode, payload);
        }
        if (mode == MODE_OBJECT) {
          return new OperationResponse(responseCode, responseFromString(payload));
        }
        if (mode == MODE_RESPONSE) {
          return new OperationResponse(responseCode, fromString(payload));
        }
      }

      int responseCode = Integer.parseInt(str);
      if (responseCode >= 0) {
        return new OperationResponse(responseCode, null);
      }
    } catch (NumberFormatException | IOException | ClassNotFoundException exception) {
      return new OperationResponse(UNDEFINED_ERROR, str);
    }

    return new OperationResponse(UNDEFINED_ERROR, str);
  }

  /**
   * Converts response code to human-readable message.
   *
   * @param code operation code
   * @return human-readable message
   */
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

  /**
   * Returns human-readable representation of response.
   *
   * @return response text
   */
  @NotNull
  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder(codeToErrorMessage(code));
    if (body != null) {
      stringBuilder.append("[");
      stringBuilder.append(body);
      stringBuilder.append("]");
    }
    return stringBuilder.toString();
  }

  /**
   * Serializes object payload to string.
   *
   * @param object serializable object
   * @return serialized object string
   * @throws IOException if serialization fails
   */
  public static String responseToString(Serializable object) throws IOException {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         ObjectOutputStream objectOutputStream =
             new ObjectOutputStream(byteArrayOutputStream)) {
      objectOutputStream.writeObject(object);
      objectOutputStream.flush();
      return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    }
  }

  /**
   * Deserializes object payload from string.
   *
   * @param value serialized object string
   * @return restored object
   * @throws IOException if deserialization fails
   * @throws ClassNotFoundException if class cannot be resolved
   */
  public static Object responseFromString(String value)
      throws IOException, ClassNotFoundException {
    byte[] data = Base64.getDecoder().decode(value);
    try (ObjectInputStream objectInputStream =
             new ObjectInputStream(new ByteArrayInputStream(data))) {
      return objectInputStream.readObject();
    }
  }
}
