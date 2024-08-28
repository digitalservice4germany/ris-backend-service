package de.bund.digitalservice.ris.caselaw.domain.docx;

import java.util.UUID;

public interface HasElementId {
  String toHtmlString(UUID elementId);
}
