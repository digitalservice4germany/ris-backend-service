package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import static org.mockito.Mockito.mock;

import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.docx4j.wml.P;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class FooterConverterTest {

  @Test
  void testConvert() {
    DocxConverter mockConverter = mock(DocxConverter.class);
    P mockP = mock(P.class);

    try (MockedStatic<ParagraphConverter> mockedStatic =
        Mockito.mockStatic(ParagraphConverter.class)) {
      ParagraphElement mockParagraphElement = new ParagraphElement();
      mockedStatic
          .when(() -> ParagraphConverter.convert(mockP, mockConverter, new ArrayList<>()))
          .thenReturn(mockParagraphElement);
      List<Object> content = Arrays.asList(mockP);

      ParagraphElement result = FooterConverter.convert(content, mockConverter, new ArrayList<>());

      Assertions.assertNotNull(result);
      Assertions.assertEquals(mockParagraphElement, result);
    }
  }
}
