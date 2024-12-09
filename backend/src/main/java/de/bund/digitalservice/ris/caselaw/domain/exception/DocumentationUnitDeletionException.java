package de.bund.digitalservice.ris.caselaw.domain.exception;

import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationType;
import java.util.Map;

public class DocumentationUnitDeletionException extends RuntimeException {

  public DocumentationUnitDeletionException(String message) {
    super(message);
  }

  public DocumentationUnitDeletionException(
      String message, Map<RelatedDocumentationType, Long> relatedEntities) {
    super(printRelatedEntities(message, relatedEntities));
  }

  private static String printRelatedEntities(
      String message, Map<RelatedDocumentationType, Long> linkCounter) {
    StringBuilder stringBuilder = new StringBuilder(message);
    stringBuilder.append(" (");
    for (var entry : linkCounter.entrySet()) {

      stringBuilder
          .append(entry.getValue())
          .append(": ")
          .append(entry.getKey().getName())
          .append(",");
    }
    stringBuilder.append(")");
    return stringBuilder.toString();
  }
}
