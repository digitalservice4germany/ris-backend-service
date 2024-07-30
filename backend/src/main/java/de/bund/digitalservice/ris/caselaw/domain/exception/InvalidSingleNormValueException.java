package de.bund.digitalservice.ris.caselaw.domain.exception;

public class InvalidSingleNormValueException extends RuntimeException {

  public InvalidSingleNormValueException(String message) {
    super(message);
  }

  public InvalidSingleNormValueException(String message, Throwable cause) {
    super(message, cause);
  }
}
