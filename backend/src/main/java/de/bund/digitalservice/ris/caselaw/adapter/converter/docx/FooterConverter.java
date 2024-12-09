package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.UnhandledElement;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.docx4j.wml.P;

/** Converter to convert docx4j footer content elements to internal used {@link ParagraphElement} */
public class FooterConverter {
  private FooterConverter() {}

  /**
   * Convert a list of docx4j content elements to {@link ParagraphElement}
   *
   * @param content list of docx4j objects
   * @param converter converter to convert the objects to domain objects
   * @return a parent paragraph element with the content elements as children
   */
  public static ParagraphElement convert(
      List<Object> content, DocxConverter converter, List<UnhandledElement> unhandledElements) {
    AtomicReference<ParagraphElement> paragraphElement = new AtomicReference<>();

    content.forEach(
        c -> {
          if (c instanceof P p) {
            paragraphElement.set(ParagraphConverter.convert(p, converter, unhandledElements));
          }
        });

    return paragraphElement.get();
  }
}
