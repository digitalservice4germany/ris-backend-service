package de.bund.digitalservice.ris.caselaw.domain.docx;

public record NumberingListEntry(
    DocumentationUnitDocx paragraphElement, NumberingListEntryIndex numberingListEntryIndex)
    implements DocumentationUnitDocx {
  @Override
  public String toHtmlString() {
    return paragraphElement.toHtmlString();
  }
}
