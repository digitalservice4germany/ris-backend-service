package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.docx4j.wml.P;

/** Converter to convert docx4j footer content elements to internal used {@link ParagraphElement} */
public class FooterConverter {
  private FooterConverter() {}

  /**
   * Convert a list of docx4j content elements to {@link ParagraphElement}
   *
   * @param content
   * @param converter
   * @return
   */
  public static ParagraphElement convert(List<Object> content, DocxConverter converter) {
    AtomicReference<ParagraphElement> paragraphElement = new AtomicReference<>();

    content.forEach(
        c -> {
          if (c instanceof P p) {
            paragraphElement.set(ParagraphConverter.convert(p, converter));
          }
        });

    return paragraphElement.get();
  }
}
