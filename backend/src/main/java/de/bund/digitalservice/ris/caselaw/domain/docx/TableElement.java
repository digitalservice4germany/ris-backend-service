package de.bund.digitalservice.ris.caselaw.domain.docx;

import java.util.List;
import java.util.stream.Collectors;

public class TableElement extends BlockElement implements DocumentationUnitDocx {
  public final List<TableRowElement> rows;

  public TableElement(List<TableRowElement> rows) {
    this.rows = rows;
    addStyle("border-collapse", "collapse");
  }

  @Override
  public String toHtmlString() {
    return "<table"
        + super.getStyleString()
        + ">"
        + rows.stream().map(TableRowElement::toHtmlString).collect(Collectors.joining())
        + "</table>";
  }
}
