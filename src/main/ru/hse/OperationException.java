package ru.hse;

import java.io.Serial;

/** Exception wrapper for operation responses returned by account logic. */
public class OperationException extends Exception {
  @Serial private static final long serialVersionUID = 1L;

  public final OperationResponse response;

  /**
   * Creates exception from operation response.
   *
   * @param resp operation response
   */
  public OperationException(OperationResponse resp) {
    response = resp;
  }

  /**
   * Returns serialized response string.
   *
   * @return response string
   */
  public String toResultString() {
    return response.toResultString();
  }

  @Override
  public String toString() {
    return response.toString();
  }
}
