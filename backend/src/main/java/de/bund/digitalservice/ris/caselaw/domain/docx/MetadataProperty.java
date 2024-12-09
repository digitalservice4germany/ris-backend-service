package de.bund.digitalservice.ris.caselaw.domain.docx;

/** Property element of the docx file */
public class MetadataProperty implements DocumentationUnitDocx {

  private DocxMetadataProperty key;
  private final String value;

  public MetadataProperty(DocxMetadataProperty key, String value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public String toHtmlString() {
    return null;
  }

  public DocxMetadataProperty getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }
}
