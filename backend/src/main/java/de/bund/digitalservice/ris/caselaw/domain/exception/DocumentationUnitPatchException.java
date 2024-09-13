package de.bund.digitalservice.ris.caselaw.domain.exception;

/**
 * Exception for all exceptions which are thrown by the handling of {@link
 * com.gravity9.jsonpatch.JsonPatch}s
 */
public class DocumentationUnitPatchException extends RuntimeException {

  public DocumentationUnitPatchException(String message, Exception cause) {
    super(message + ": " + cause.getMessage(), cause);
  }
}
