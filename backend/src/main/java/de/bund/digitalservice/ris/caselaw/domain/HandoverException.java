package de.bund.digitalservice.ris.caselaw.domain;

/** Exception thrown when a documentation unit handover fails. */
public class HandoverException extends RuntimeException {
  public HandoverException(String message) {
    super(message);
  }

  public HandoverException(String message, Throwable cause) {
    super(message, cause);
  }
}
