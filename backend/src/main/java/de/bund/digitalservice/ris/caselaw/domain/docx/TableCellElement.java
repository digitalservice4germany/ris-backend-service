package de.bund.digitalservice.ris.caselaw.domain.docx;

import java.util.List;
import java.util.stream.Collectors;

public class TableCellElement extends BlockElement implements DocumentUnitDocx {
  public final List<DocumentUnitDocx> paragraphElements;
  private final Integer usedStyles;
  public Integer columnSpan;

  public TableCellElement(List<DocumentUnitDocx> paragraphElements, Integer usedStyles) {
    this.paragraphElements = paragraphElements;
    this.usedStyles = usedStyles;
    addStyle("min-width", "5px");
    addStyle("padding", "5px");
  }

  public void setColumnSpan(Integer columnSpan) {
    this.columnSpan = columnSpan;
  }

  public Integer getUsedStyles() {
    return usedStyles;
  }

  private String columnSpanToHtmlString() {
    return columnSpan != null ? " colspan=\"" + columnSpan + "\"" : "";
  }

  @Override
  public String toHtmlString() {

    return "<td"
        + columnSpanToHtmlString()
        + super.getStyleString()
        + ">"
        + paragraphElements.stream()
            .map(DocumentUnitDocx::toHtmlString)
            .collect(Collectors.joining())
        + "</td>";
  }
}
