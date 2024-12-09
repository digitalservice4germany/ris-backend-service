package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.UnhandledElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.UnhandledElementType;
import jakarta.xml.bind.JAXBElement;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.wml.P.Hyperlink;
import org.docx4j.wml.R;

/**
 * Extract {@link R} children of the {@link Hyperlink} to {@link RunElement}s
 *
 * @see DocxConverter
 */
@Slf4j
public class HyperlinkConverter {
  private HyperlinkConverter() {}

  /**
   * Convert the R element and all its children elements into {@link RunElement}.
   *
   * @param hyperlink the hyperlink element
   * @param paragraphElement the parent paragraph element
   * @param converter the used docx converter (contains meta information of the docx file)
   * @param unhandledElements
   */
  public static void convert(
      Hyperlink hyperlink,
      ParagraphElement paragraphElement,
      DocxConverter converter,
      List<UnhandledElement> unhandledElements) {
    hyperlink
        .getContent()
        .forEach(
            content -> {
              if (content instanceof R run) {
                RunElementConverter.convert(run, paragraphElement, converter, unhandledElements);
              } else if (content instanceof JAXBElement<?> jaxbElement) {
                unhandledElements.add(
                    new UnhandledElement(
                        "hyperlink",
                        jaxbElement.getDeclaredType().toString(),
                        UnhandledElementType.JAXB));
              } else {
                unhandledElements.add(
                    new UnhandledElement(
                        "hyperlink", content.getClass().toString(), UnhandledElementType.OBJECT));
              }
            });
  }
}
