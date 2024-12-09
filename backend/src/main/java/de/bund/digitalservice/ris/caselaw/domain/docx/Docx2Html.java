package de.bund.digitalservice.ris.caselaw.domain.docx;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Object with the content of the word file (docx).
 *
 * @param html - generated html of the word document
 * @param ecliList - list of ecli values in the word document (normally only one entry)
 */
public record Docx2Html(
    String html, List<String> ecliList, Map<DocxMetadataProperty, String> properties) {
  public static final Docx2Html EMPTY =
      new Docx2Html(null, Collections.emptyList(), Collections.emptyMap());
}
