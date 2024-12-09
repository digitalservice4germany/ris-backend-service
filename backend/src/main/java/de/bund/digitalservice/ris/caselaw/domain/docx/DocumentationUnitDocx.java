package de.bund.digitalservice.ris.caselaw.domain.docx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public interface DocumentationUnitDocx {
  String toHtmlString();
}
