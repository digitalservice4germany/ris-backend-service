package de.bund.digitalservice.ris.caselaw.domain.docx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** ECLI element in the footers of the docx file */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ECLIElement extends FooterElement {
  public ECLIElement() {}

  public ECLIElement(ParagraphElement paragraph) {
    super(paragraph);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (obj instanceof ECLIElement other) {
      return getText().equals(other.getText());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return getText().hashCode();
  }
}
