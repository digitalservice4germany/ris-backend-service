package de.bund.digitalservice.ris.caselaw.domain.docx;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public abstract class TextElement extends StyledElement implements DocumentationUnitDocx {
  private static final DecimalFormat DECIMAL_FORMATTER;

  static {
    DECIMAL_FORMATTER = new DecimalFormat();
    DecimalFormatSymbols formatSymbol = DecimalFormatSymbols.getInstance();
    formatSymbol.setDecimalSeparator('.');
    DECIMAL_FORMATTER.setDecimalFormatSymbols(formatSymbol);
    DECIMAL_FORMATTER.setMaximumFractionDigits(2);
  }

  public void setBold(boolean bold) {
    if (bold) addStyle("font-weight", "bold");
  }

  public void setItalic(boolean italic) {
    if (italic) addStyle("font-style", "italic");
  }

  public void setStrike(boolean strike) {
    if (strike) addStyle("text-decoration", "line-through");
  }

  public void setSize(Integer size) {
    addStyle("font-size", DECIMAL_FORMATTER.format(size / 2.0f) + "pt");
  }

  public void setUnderline(String underline) {
    if (underline.equals("single")) addStyle("text-decoration", "underline");
  }

  public void setVertAlign(VerticalAlign vertAlign) {
    if (vertAlign == null) {
      return;
    }

    if (vertAlign == VerticalAlign.SUBSCRIPT) {
      addStyle("vertical-align", "sub");
    } else if (vertAlign == VerticalAlign.SUPERSCRIPT) {
      addStyle("vertical-align", "super");
    }
  }

  public void setColor(String color) {
    if (color == null) {
      return;
    }

    addStyle("color", "#" + color.toLowerCase());
  }
}
