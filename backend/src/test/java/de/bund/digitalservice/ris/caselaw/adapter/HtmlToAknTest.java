package de.bund.digitalservice.ris.caselaw.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.bund.digitalservice.ris.caselaw.adapter.caselawldml.AknKeyword;
import de.bund.digitalservice.ris.caselaw.adapter.caselawldml.CaseLawLdml;
import de.bund.digitalservice.ris.caselaw.adapter.caselawldml.Classification;
import de.bund.digitalservice.ris.caselaw.adapter.caselawldml.JaxbHtml;
import de.bund.digitalservice.ris.caselaw.adapter.caselawldml.Judgment;
import de.bund.digitalservice.ris.caselaw.adapter.caselawldml.JudgmentBody;
import de.bund.digitalservice.ris.caselaw.adapter.caselawldml.Meta;
import de.bund.digitalservice.ris.caselaw.adapter.exception.LdmlTransformationException;
import jakarta.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.TransformerFactoryImpl;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mapping.MappingException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class HtmlToAknTest {

  private static final String AKN_START =
      """
          <?xml version="1.0" encoding="utf-8"?>
          <akn:akomaNtoso xmlns:akn="http://docs.oasis-open.org/legaldocml/ns/akn/3.0/WD17"
                          xmlns:ris="http://example.com/0.1/"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://docs.oasis-open.org/legaldocml/ns/akn/3.0/WD17 https://docs.oasis-open.org/legaldocml/akn-core/v1.0/csprd02/part2-specs/schemas/akomantoso30.xsd">
             <akn:judgment name="attributsemantik-noch-undefiniert">
          """;
  private static final String AKN_END =
      """
             </akn:judgment>
          </akn:akomaNtoso>
          """;

  private static String getAkn(Judgment judgment) {
    CaseLawLdml caseLawLdml = CaseLawLdml.builder().judgment(judgment).build();
    StringWriter writer = new StringWriter();
    JAXB.marshal(caseLawLdml, writer);

    Templates templates;
    try {
      ClassPathResource xsltResource = new ClassPathResource("caselawhandover/htmlToAknHtml.xslt");
      String fileContent = IOUtils.toString(xsltResource.getInputStream(), StandardCharsets.UTF_8);
      templates =
          new TransformerFactoryImpl()
              .newTemplates(new StreamSource(new StringReader(fileContent)));
    } catch (TransformerConfigurationException | IOException e) {
      throw new LdmlTransformationException("XSLT initialization error.", e);
    }

    return XmlUtilService.xsltTransform(templates, writer.toString());
  }

  @Test
  void borderNumberInIntroTransformationTest() {

    String inputHtml =
        """
                <border-number>
                    <number>1</number>
                    <content>
                      <p>Lorem ipsum</p>
                    </content>
                </border-number>
                """;

    String expectedXml =
        """
        <akn:judgmentBody>
          <akn:motivation>
            <akn:hcontainer name="randnummer">
               <akn:num>1</akn:num>
               <akn:content>
                 <akn:p>Lorem ipsum</akn:p>
               </akn:content>
            </akn:hcontainer>
          </akn:motivation>
        </akn:judgmentBody>
        """;
    String result =
        getAkn(
            Judgment.builder()
                .judgmentBody(
                    JudgmentBody.builder()
                        .motivation(JaxbHtml.build(htmlStringToObjectList(inputHtml)))
                        .build())
                .build());

    assertEquals(
        StringUtils.deleteWhitespace(AKN_START + expectedXml + AKN_END),
        StringUtils.deleteWhitespace(result));
  }

  @Test
  void mixedTextInHeaderTest() {

    String inputHtml = "Hello<p> paragraph</p> world!";

    String expectedXml =
        """
        <akn:header>
          <akn:p alternativeTo="textWrapper">Hello</akn:p>
          <akn:p> paragraph</akn:p>
          <akn:p alternativeTo="textWrapper"> world!</akn:p>
        </akn:header>
        """;
    JaxbHtml header = JaxbHtml.build(htmlStringToObjectList(inputHtml));
    String result = getAkn(Judgment.builder().header(header).build());

    assertEquals(
        StringUtils.deleteWhitespace(AKN_START + expectedXml + AKN_END),
        StringUtils.deleteWhitespace(result));
  }

  @Test
  void keyword() {

    String keyword = "keyword1";

    String expectedXml =
        """
              <akn:meta>
                 <akn:classification source="attributsemantik-noch-undefiniert">
                    <akn:keyword dictionary="attributsemantik-noch-undefiniert"
                                 showAs="attributsemantik-noch-undefiniert"
                                 value="keyword1"/>
                 </akn:classification>
              </akn:meta>
              """;
    AknKeyword aknKeyword = new AknKeyword(keyword);
    String result =
        getAkn(
            Judgment.builder()
                .meta(
                    Meta.builder()
                        .classification(
                            Classification.builder().keyword(List.of(aknKeyword)).build())
                        .build())
                .build());

    assertEquals(
        StringUtils.deleteWhitespace(AKN_START + expectedXml + AKN_END),
        StringUtils.deleteWhitespace(result));
  }

  private static List<Object> htmlStringToObjectList(String html) {
    try {
      String wrapped = "<wrapper>" + html + "</wrapper>";

      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document doc = builder.parse(new InputSource(new StringReader(wrapped)));

      NodeList childNodes = doc.getDocumentElement().getChildNodes();

      return XmlUtilService.toList(childNodes).stream().map(e -> (Object) e).toList();
    } catch (ParserConfigurationException | IOException | SAXException e) {
      throw new MappingException(e.getMessage());
    }
  }
}
