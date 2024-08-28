package de.bund.digitalservice.ris.caselaw.domain.docx;

public class RunTextElement extends TextElement implements RunElement {
  private String text;

  public RunTextElement() {}

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  @Override
  public String toHtmlString() {
    StringBuilder sb = new StringBuilder();

    boolean hasStyle = hasStyle();
    if (hasStyle) {
      sb.append("<span");
      sb.append(getStyleString());
      sb.append(">");
    }

    sb.append(text);

    if (hasStyle) {
      sb.append("</span>");
    }

    return sb.toString();
  }

  @Override
  public String toString() {
    return text;
  }
}
