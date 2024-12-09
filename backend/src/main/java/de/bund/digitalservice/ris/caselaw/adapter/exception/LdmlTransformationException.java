package de.bund.digitalservice.ris.caselaw.adapter.exception;

public class LdmlTransformationException extends RuntimeException {
  public LdmlTransformationException(String message, Exception ex) {
    super(message, ex);
  }
}
