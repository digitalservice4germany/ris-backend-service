package de.bund.digitalservice.ris.caselaw.domain.docx;

public record UnhandledElement(String parentElement, String element, UnhandledElementType type) {

  @Override
  public String toString() {
    String typeString;
    switch (type) {
      case JAXB -> typeString = "J";
      case OBJECT -> typeString = "O";
      default -> typeString = "U";
    }
    return element + " (" + parentElement + ", " + typeString + ")";
  }
}
