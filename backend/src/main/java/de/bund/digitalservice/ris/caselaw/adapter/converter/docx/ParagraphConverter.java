package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import de.bund.digitalservice.ris.caselaw.domain.docx.AnchorImageElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.UnhandledElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.UnhandledElementType;
import jakarta.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.wml.Jc;
import org.docx4j.wml.JcEnumeration;
import org.docx4j.wml.P;
import org.docx4j.wml.P.Hyperlink;
import org.docx4j.wml.PPr;
import org.docx4j.wml.R;
import org.docx4j.wml.Style;

/**
 * Converter to convert docx4j {@link org.docx4j.wml.P} element to internal used {@link
 * ParagraphElement}
 */
@Slf4j
public class ParagraphConverter {

  public static final int HTML_INDENT_SIZE_IN_PX = 40;

  private ParagraphConverter() {}

  /**
   * Convert the {@link org.docx4j.wml.P} to a {@link ParagraphElement}
   *
   * @param paragraph original docx4j {@link P} element
   * @param converter reference to the converter
   * @param unhandledElements
   * @return the internal representation as {@link ParagraphElement}
   * @see DocxConverter
   */
  public static ParagraphElement convert(
      P paragraph, DocxConverter converter, List<UnhandledElement> unhandledElements) {

    if (paragraph == null) {
      return null;
    }

    var paragraphElement = new ParagraphElement();

    PPr paragraphProperties = paragraph.getPPr();
    if (paragraphProperties != null) {
      if (paragraphProperties.getPStyle() != null) {
        paragraphElement.setStyleReference(paragraphProperties.getPStyle().getVal());
      }
      if (paragraphProperties.getInd() != null
          && paragraphProperties.getInd().getLeft() != null
          && paragraphProperties.getNumPr() == null) {
        // Default Tab Size in Docx = 1.27cm = 48px = 720 twips
        int baseIndentTwips = 720;
        int leftIndentInTwips = paragraphProperties.getInd().getLeft().intValue();
        Optional<BigInteger> hangingIndentInTwips =
            Optional.ofNullable(paragraphProperties.getInd().getHanging());
        int actualIndentInTwips =
            leftIndentInTwips - hangingIndentInTwips.orElse(BigInteger.ZERO).intValue();
        double numberOfEstimatedIndentations =
            Math.ceil((double) actualIndentInTwips / baseIndentTwips);
        // We use 40px as the default indentation size
        paragraphElement.addStyle(
            "margin-left", HTML_INDENT_SIZE_IN_PX * numberOfEstimatedIndentations + "px");
      }

      String alignment = getAlignment(paragraphProperties, converter);
      if (alignment != null) {
        paragraphElement.setAlignment(alignment);
      }
    }

    paragraph
        .getContent()
        .forEach(
            content -> {
              if (content instanceof R run) {
                RunElementConverter.convert(run, paragraphElement, converter, unhandledElements);
              } else if (content instanceof JAXBElement<?> jaxbElement) {
                if (jaxbElement.getValue() instanceof Hyperlink hyperlink) {
                  HyperlinkConverter.convert(
                      hyperlink, paragraphElement, converter, unhandledElements);
                } else {
                  unhandledElements.add(
                      new UnhandledElement(
                          "paragraph",
                          jaxbElement.getDeclaredType().toString(),
                          UnhandledElementType.JAXB));
                }
              } else {
                unhandledElements.add(
                    new UnhandledElement(
                        "paragraph", content.getClass().toString(), UnhandledElementType.OBJECT));
              }
            });

    sortParagraphElements(paragraphElement);

    return paragraphElement;
  }

  private static String getAlignment(PPr pPr, DocxConverter converter) {
    if (pPr == null) {
      return null;
    }

    Jc jc = null;

    var pStyle = pPr.getPStyle();
    if (pStyle != null && pStyle.getVal() != null) {
      Style style = converter.getStyles().get(pStyle.getVal());
      if (style != null && style.getPPr() != null) {
        jc = style.getPPr().getJc();
      }
    }

    if (pPr.getJc() != null) {
      jc = pPr.getJc();
    }

    if (jc != null && jc.getVal() != null && jc.getVal() == JcEnumeration.CENTER) {
      return "center";
    }

    if (jc != null && jc.getVal() != null && jc.getVal() == JcEnumeration.RIGHT) {
      return "right";
    }

    if (jc != null && jc.getVal() != null && jc.getVal() == JcEnumeration.LEFT) {
      return "left";
    }

    if (jc != null && jc.getVal() != null && jc.getVal() == JcEnumeration.BOTH) {
      return "justify";
    }

    return null;
  }

  private static void sortParagraphElements(ParagraphElement paragraphElement) {
    List<RunElement> sortedList =
        paragraphElement.getRunElements().stream()
            .sorted(
                (o1, o2) -> {
                  if (o1.getClass().equals(o2.getClass())) {
                    return 0;
                  }

                  if (o1 instanceof AnchorImageElement && !(o2 instanceof AnchorImageElement)) {
                    return -1;
                  }

                  if (!(o1 instanceof AnchorImageElement) && o2 instanceof AnchorImageElement) {
                    return 1;
                  }

                  return 0;
                })
            .toList();

    paragraphElement.setRunElements(sortedList);
  }
}
