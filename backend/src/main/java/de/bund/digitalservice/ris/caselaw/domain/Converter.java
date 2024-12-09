package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.docx.UnhandledElement;
import java.util.List;

public interface Converter<T> {
  T convert(Object part, List<UnhandledElement> unhandledElements);
}
