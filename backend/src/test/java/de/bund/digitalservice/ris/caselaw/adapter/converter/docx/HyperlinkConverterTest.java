package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import javax.xml.namespace.QName;
import org.docx4j.wml.P;
import org.docx4j.wml.P.Hyperlink;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;
import org.junit.jupiter.api.Test;

class HyperlinkConverterTest {
  @Test
  void testConvert_PWithHyperlink_shouldGenerateRunElement() {
    P p = new P();
    Hyperlink hyperlink = new Hyperlink();
    R r = new R();
    Text text = new Text();
    text.setValue("hyperlink text");
    JAXBElement<Text> textJAXBElement = new JAXBElement<>(new QName("text"), Text.class, text);
    r.getContent().add(textJAXBElement);
    hyperlink.getContent().add(r);
    JAXBElement<Hyperlink> hyperlinkJAXBElement =
        new JAXBElement<>(new QName("hyperlink"), Hyperlink.class, hyperlink);
    p.getContent().add(hyperlinkJAXBElement);
    DocxConverter converter = mock(DocxConverter.class);

    ParagraphElement paragraphElement = ParagraphConverter.convert(p, converter, new ArrayList<>());

    assertThat(paragraphElement.getRunElements())
        .extracting("text")
        .containsExactly("hyperlink text");
  }
}
