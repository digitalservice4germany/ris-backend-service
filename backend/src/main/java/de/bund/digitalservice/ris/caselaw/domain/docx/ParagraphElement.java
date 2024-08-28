package de.bund.digitalservice.ris.caselaw.domain.docx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParagraphElement extends TextElement implements HasElementId {
  private boolean clearfix;
  private List<RunElement> runElements = new ArrayList<>();

  public void setAlignment(String alignment) {
    addStyle("text-align", alignment);
  }

  public void setClearfix(boolean clearfix) {
    this.clearfix = clearfix;
  }

  public void addRunElement(RunElement element) {
    this.runElements.add(element);
  }

  public List<RunElement> getRunElements() {
    return runElements;
  }

  public void setRunElements(List<RunElement> runElements) {
    this.runElements = runElements;
  }

  @Override
  public String toHtmlString() {
    return toHtmlString(null);
  }

  public String toHtmlString(UUID elementId) {
    StringBuilder sb = new StringBuilder("<p");

    if (elementId != null) {
      sb.append(" element-Id=\"").append(elementId).append("\"");
    }
    if (clearfix) {
      sb.append(" class=\"clearfix\"");
    }
    sb.append(getStyleString());
    sb.append(">");

    for (RunElement element : runElements) {
      sb.append(element.toHtmlString());
    }
    sb.append("</p>");

    return sb.toString();
  }

  public String getText() {
    StringBuilder text = new StringBuilder();

    runElements.stream()
        .filter(RunTextElement.class::isInstance)
        .forEach(runElement -> text.append(((RunTextElement) runElement).getText()));

    return text.toString();
  }
}
