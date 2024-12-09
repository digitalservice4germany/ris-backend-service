package de.bund.digitalservice.ris.caselaw.domain.docx;

public enum DocxMetadataProperty {
  FILE_NUMBER("Aktenzeichen"),
  DECISION_DATE("Entscheidungsdatum"),
  COURT_TYPE("Gerichtstyp"),
  COURT_LOCATION("Gerichtsort"),
  // <Gerichtstyp> <Gerichtsort>
  COURT("Gericht"),
  APPRAISAL_BODY("Spruchkoerper"),
  // Juris-Abkürzung aus Wertetabelle
  DOCUMENT_TYPE("Dokumenttyp"),
  ECLI("ECLI"),
  PROCEDURE("Vorgang"),
  LEGAL_EFFECT("Rechtskraft");

  private final String key;

  private DocxMetadataProperty(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public static DocxMetadataProperty fromKey(String key) {
    for (DocxMetadataProperty field : values()) {
      if (field.getKey().equals(key)) {
        return field;
      }
    }
    return null;
  }
}
