package de.bund.digitalservice.ris.caselaw.domain.docx;

import java.util.UUID;

/** Footer element in the docx file */
public class FooterElement extends ParagraphElement implements HasElementId {

  private ParagraphElement paragraph;

  public FooterElement() {}

  public FooterElement(ParagraphElement paragraph) {
    this.paragraph = paragraph;
  }

  public ParagraphElement getParagraph() {
    return paragraph;
  }

  public void setParagraph(ParagraphElement paragraph) {
    this.paragraph = paragraph;
  }

  @Override
  public String toHtmlString() {
    return paragraph.toHtmlString();
  }

  @Override
  public String toHtmlString(UUID elementId) {
    return paragraph.toHtmlString(elementId);
  }

  @Override
  public String getText() {
    return paragraph.getText();
  }
}
