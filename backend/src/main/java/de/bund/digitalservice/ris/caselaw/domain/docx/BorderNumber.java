package de.bund.digitalservice.ris.caselaw.domain.docx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BorderNumber implements DocumentationUnitDocx, HasElementId {
  private final StringBuilder number = new StringBuilder();
  private List<DocumentationUnitDocx> children = new ArrayList<>();
  private Integer numId = null;

  public String getNumber() {
    return number.toString();
  }

  public void addNumberText(String text) {
    number.append(text);
  }

  public void setNumberText(String text) {
    number.setLength(0);
    addNumberText(text);
  }

  @Override
  public String toHtmlString() {
    return toHtmlString(null);
  }

  @Override
  public String toHtmlString(UUID elementId) {
    StringBuilder sb = new StringBuilder();

    sb.append("<border-number");
    if (elementId != null) {
      sb.append(" element-id=\"").append(elementId).append("\"");
    }
    sb.append(">");
    sb.append("<number>");
    sb.append(number);
    sb.append("</number>");
    if (!children.isEmpty()) {
      sb.append("<content>");
      for (DocumentationUnitDocx child : children) {
        sb.append(child.toHtmlString());
      }
      sb.append("</content>");
    }
    sb.append("</border-number>");

    return sb.toString();
  }

  public void addChild(DocumentationUnitDocx element) {
    children.add(element);
  }

  public int getChildrenSize() {
    return children.size();
  }

  public List<DocumentationUnitDocx> getChildren() {
    return children;
  }

  public void setNumId(int numId) {
    this.numId = numId;
  }

  public Integer getNumId() {
    return numId;
  }
}
