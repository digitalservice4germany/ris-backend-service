package de.bund.digitalservice.ris.caselaw.adapter.exception;

public class BucketException extends RuntimeException {
  public BucketException(String message, Exception ex) {
    super(message, ex);
  }
}
