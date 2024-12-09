package de.bund.digitalservice.ris.caselaw.domain.exception;

import java.util.UUID;

public class DocumentationUnitNotExistsException extends Exception {

  private static final String DEFAULT_MESSAGE = "Documentation unit does not exist";

  public DocumentationUnitNotExistsException() {
    super(DEFAULT_MESSAGE);
  }

  public DocumentationUnitNotExistsException(String documentNumber) {
    super(DEFAULT_MESSAGE + (documentNumber == null ? "" : ": " + documentNumber));
  }

  public DocumentationUnitNotExistsException(UUID documentUUID) {
    super(DEFAULT_MESSAGE + (documentUUID == null ? "" : ": " + documentUUID));
  }
}
