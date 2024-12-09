package de.bund.digitalservice.ris.caselaw.domain.exception;

import java.io.IOException;

public class DocumentNumberRecyclingException extends IOException {
  public DocumentNumberRecyclingException(String message) {
    super(message);
  }
}
