package de.bund.digitalservice.ris.caselaw.domain.docx;

import java.util.ArrayList;
import java.util.List;

public class BorderNumber implements DocumentUnitDocx {
  private final StringBuilder number = new StringBuilder();
  private List<DocumentUnitDocx> children = new ArrayList<>();
  private Integer numId = null;

  public String getNumber() {
    return number.toString();
  }

  public void addNumberText(String text) {
    number.append(text);
  }

  @Override
  public String toHtmlString() {
    StringBuilder sb = new StringBuilder();

    sb.append("<border-number>");
    sb.append("<number>");
    sb.append(number);
    sb.append("</number>");
    if (!children.isEmpty()) {
      sb.append("<content>");
      for (DocumentUnitDocx child : children) {
        sb.append(child.toHtmlString());
      }
      sb.append("</content>");
    }
    sb.append("</border-number>");

    return sb.toString();
  }

  public void addChild(DocumentUnitDocx element) {
    children.add(element);
  }

  public int getChildrenSize() {
    return children.size();
  }

  public List<DocumentUnitDocx> getChildren() {
    return children;
  }

  public void setNumId(int numId) {
    this.numId = numId;
  }

  public Integer getNumId() {
    return numId;
  }
}
