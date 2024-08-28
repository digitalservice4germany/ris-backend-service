package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

public class DocumentConverterException extends RuntimeException {
  public DocumentConverterException(String message, Exception exception) {
    super(message, exception);
  }

  public DocumentConverterException(String message) {
    super(message);
  }
}
