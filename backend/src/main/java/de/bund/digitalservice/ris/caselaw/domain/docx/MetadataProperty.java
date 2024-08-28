package de.bund.digitalservice.ris.caselaw.domain.docx;

import java.util.UUID;

/** Property element of the docx file */
public class MetadataProperty implements DocumentationUnitDocx, HasElementId {

  private DocxMetadataProperty key;
  private String value;

  public MetadataProperty() {}

  public MetadataProperty(DocxMetadataProperty key, String value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public String toHtmlString() {
    return null;
  }

  @Override
  public String toHtmlString(UUID elementId) {
    return null;
  }

  public DocxMetadataProperty getKey() {
    return key;
  }

  public void setKey(DocxMetadataProperty key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
