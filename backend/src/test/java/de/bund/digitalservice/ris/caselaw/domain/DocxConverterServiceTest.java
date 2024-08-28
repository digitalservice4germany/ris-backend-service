package de.bund.digitalservice.ris.caselaw.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.adapter.DocxConverterService;
import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocumentConverterException;
import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocxConverter;
import de.bund.digitalservice.ris.caselaw.config.ConverterConfig;
import de.bund.digitalservice.ris.caselaw.domain.docx.BorderNumber;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocumentationUnitDocx;
import de.bund.digitalservice.ris.caselaw.domain.docx.Docx2Html;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocxImagePart;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocxMetadataProperty;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingList.DocumentationUnitNumberingListNumberFormat;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingListEntry;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingListEntryIndex;
import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunTextElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.TableCellElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.TableElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.TableRowElement;
import jakarta.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.docx4j.docProps.custom.Properties;
import org.docx4j.model.structure.DocumentModel;
import org.docx4j.model.structure.HeaderFooterPolicy;
import org.docx4j.model.structure.SectionWrapper;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.DocPropsCustomPart;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.Parts;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.ImageJpegPart;
import org.docx4j.openpackaging.parts.WordprocessingML.ImagePngPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MetafileEmfPart;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.JcEnumeration;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Style;
import org.docx4j.wml.Styles;
import org.docx4j.wml.Text;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@ExtendWith(SpringExtension.class)
@Import({DocxConverterService.class, ConverterConfig.class})
class DocxConverterServiceTest {

  @Autowired DocxConverterService service;

  @MockBean S3Client client;

  @SpyBean DocumentBuilderFactory documentBuilderFactory;

  @Mock WordprocessingMLPackage mlPackage;

  @Mock ResponseBytes<GetObjectResponse> responseBytes;

  @MockBean DocxConverter converter;

  @Captor ArgumentCaptor<Map<String, Style>> styleMapCaptor;
  @Captor ArgumentCaptor<List<ParagraphElement>> footerCaptor;

  @Captor ArgumentCaptor<Map<String, DocxImagePart>> imageMapCaptor;

  @Test
  void testGetOriginalText() {
    MainDocumentPart mockedMainDocumentPart = mock(MainDocumentPart.class);
    when(mlPackage.getMainDocumentPart()).thenReturn(mockedMainDocumentPart);
    when(mlPackage.getMainDocumentPart().getXML())
        .thenReturn("<document><p><t>text</t></p></document>");
    var result = service.getOriginalText(mlPackage);

    assertEquals("text", result);
  }

  @Test
  void testGetOriginalText_withNoMlPackage() {
    mlPackage = null;
    var result = service.getOriginalText(mlPackage);
    assertEquals("<no word file selected>", result);
  }

  @Test
  void testGetOriginalText_throwsException() throws ParserConfigurationException {
    MainDocumentPart mockedMainDocumentPart = mock(MainDocumentPart.class);
    when(mlPackage.getMainDocumentPart()).thenReturn(mockedMainDocumentPart);
    when(mlPackage.getMainDocumentPart().getXML())
        .thenReturn("<document><p><t>text</t></p></document>");

    when(documentBuilderFactory.newDocumentBuilder()).thenThrow(new ParserConfigurationException());

    assertThatThrownBy(
            () -> {
              service.getOriginalText(mlPackage);
            })
        .isInstanceOf(DocumentConverterException.class)
        .hasMessageContaining("Couldn't read all text elements of docx xml!");
  }

  @Test
  void testGetHtml() {
    new TestDocumentGenerator(client, responseBytes, mlPackage, converter)
        .addContent("1", generateText("test"))
        .addContent("2", generateBorderNumber("1"))
        .addContent("3", generateText("border number 1"))
        .addContent("4", generateBorderNumber("2"))
        .addContent("5", generateText("border number 2"))
        .addContent("6", generateTable("table content"))
        .generate();

    try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
        mockStatic(WordprocessingMLPackage.class)) {
      mockedMLPackageStatic
          .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
          .thenReturn(mlPackage);

      Docx2Html docx2Html = service.getConvertedObject("test.docx");
      assertNotNull(docx2Html);

      assertEquals(
          "<p>test</p>"
              + "<border-number><number>1</number><content><p>border number 1</p></content></border-number>"
              + "<border-number><number>2</number><content><p>border number 2</p></content></border-number>"
              + "<table style=\"border-collapse: collapse;\"><tr><td style=\"min-width: 5px; padding: 5px;\"><p>table content</p></td></tr></table>",
          docx2Html.html());
    }
  }

  @Test
  void testGetHtml_WithNoFilename() {
    Assertions.assertNull(service.getConvertedObject(null));
  }

  @Test
  void testGetHtml_withStyleInformation() {
    when(client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
        .thenReturn(responseBytes);
    when(responseBytes.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
    MainDocumentPart mainDocumentPart = mock(MainDocumentPart.class);
    StyleDefinitionsPart styleDefinitionsPart = mock(StyleDefinitionsPart.class);
    Styles styles = mock(Styles.class);
    Style style = mock(Style.class);
    when(style.getStyleId()).thenReturn("test-style");
    when(styles.getStyle()).thenReturn(List.of(style));
    when(styleDefinitionsPart.getJaxbElement()).thenReturn(styles);
    when(mainDocumentPart.getStyleDefinitionsPart()).thenReturn(styleDefinitionsPart);
    when(mlPackage.getMainDocumentPart()).thenReturn(mainDocumentPart);
    when(mainDocumentPart.getContent()).thenReturn(Collections.emptyList());

    try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
        mockStatic(WordprocessingMLPackage.class)) {
      mockedMLPackageStatic
          .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
          .thenReturn(mlPackage);

      Docx2Html docx2Html = service.getConvertedObject("test.docx");
      Assertions.assertNotNull(docx2Html);

      verify(converter).setStyles(styleMapCaptor.capture());
      assertTrue(styleMapCaptor.getValue().containsKey("test-style"));
      assertEquals(style, styleMapCaptor.getValue().get("test-style"));
    }
  }

  @Test
  void testGetHtml_withImages() throws InvalidFormatException, IOException {
    when(client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
        .thenReturn(responseBytes);
    when(responseBytes.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
    MainDocumentPart mainDocumentPart = mock(MainDocumentPart.class);
    Parts parts = mock(Parts.class);
    HashMap<PartName, Part> partMap = new HashMap<>();
    PartName partName = new PartName("/emfPart");
    BinaryPartAbstractImage part = new MetafileEmfPart(partName);
    Relationship relationship = new Relationship();
    relationship.setId("emfPart");
    part.getSourceRelationships().add(relationship);
    InputStream emfStream =
        DocxConverterServiceTest.class.getClassLoader().getResourceAsStream("test.emf");
    part.setBinaryData(emfStream.readAllBytes());
    partMap.put(partName, part);
    partName = new PartName("/jpegPart");
    part = new ImageJpegPart(partName);
    relationship = new Relationship();
    relationship.setId("jpegPart");
    part.getSourceRelationships().add(relationship);
    part.setBinaryData(new byte[] {});
    partMap.put(partName, part);
    partName = new PartName("/pngPart");
    part = new ImagePngPart(partName);
    relationship = new Relationship();
    relationship.setId("pngPart");
    part.getSourceRelationships().add(relationship);
    part.setBinaryData(new byte[] {});
    partMap.put(partName, part);
    when(parts.getParts()).thenReturn(partMap);
    when(mlPackage.getParts()).thenReturn(parts);
    when(mlPackage.getMainDocumentPart()).thenReturn(mainDocumentPart);
    when(mainDocumentPart.getContent()).thenReturn(Collections.emptyList());

    try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
        mockStatic(WordprocessingMLPackage.class)) {
      mockedMLPackageStatic
          .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
          .thenReturn(mlPackage);

      Docx2Html docx2Html = service.getConvertedObject("test.docx");
      Assertions.assertNotNull(docx2Html);

      verify(converter).setImages(imageMapCaptor.capture());
      Map<String, DocxImagePart> imageMapValue = imageMapCaptor.getValue();
      assertEquals(3, imageMapValue.values().size());
      assertTrue(imageMapValue.containsKey("emfPart"));
      assertEquals("image/x-emf", imageMapValue.get("emfPart").contentType());
      assertTrue(imageMapValue.containsKey("jpegPart"));
      assertEquals("image/jpeg", imageMapValue.get("jpegPart").contentType());
      assertTrue(imageMapValue.containsKey("pngPart"));
      assertEquals("image/png", imageMapValue.get("pngPart").contentType());
    }
  }

  @Test
  void testGetHtml_withFooters() {
    when(client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
        .thenReturn(responseBytes);
    when(responseBytes.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
    MainDocumentPart mainDocumentPart = mock(MainDocumentPart.class);
    when(mlPackage.getMainDocumentPart()).thenReturn(mainDocumentPart);

    P paragraph = new P();
    RPr rPr = new RPr();
    R run = new R();
    run.setRPr(rPr);
    Text text = new Text();
    text.setValue("ECLI:65432:87654:4321:4321");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    List<Object> content = List.of(paragraph);
    FooterPart defaultFooter = mock(FooterPart.class);
    FooterPart firstFooter = mock(FooterPart.class);
    FooterPart evenFooter = mock(FooterPart.class);
    HeaderFooterPolicy headerFooterPolicy = mock(HeaderFooterPolicy.class);
    SectionWrapper section = mock(SectionWrapper.class);
    List<SectionWrapper> sections = List.of(section);
    DocumentModel documentModel = mock(DocumentModel.class);
    when(defaultFooter.getContent()).thenReturn(content);
    when(firstFooter.getContent()).thenReturn(content);
    when(evenFooter.getContent()).thenReturn(content);
    when(headerFooterPolicy.getDefaultFooter()).thenReturn(defaultFooter);
    when(headerFooterPolicy.getFirstFooter()).thenReturn(defaultFooter);
    when(headerFooterPolicy.getFirstFooter()).thenReturn(firstFooter);
    when(headerFooterPolicy.getEvenFooter()).thenReturn(evenFooter);
    when(section.getHeaderFooterPolicy()).thenReturn(headerFooterPolicy);
    when(documentModel.getSections()).thenReturn(sections);
    when(mlPackage.getDocumentModel()).thenReturn(documentModel);

    try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
        mockStatic(WordprocessingMLPackage.class)) {
      mockedMLPackageStatic
          .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
          .thenReturn(mlPackage);

      Docx2Html docx2Html = service.getConvertedObject("test.docx");
      Assertions.assertNotNull(docx2Html);
    }
  }

  @Test
  void testGetHtml_withEmptyBorderNumber() {
    new TestDocumentGenerator(client, responseBytes, mlPackage, converter)
        .addContent("1", generateText("test"))
        .addContent("2", generateBorderNumber("1"))
        .addContent("3", generateBorderNumber("2"))
        .addContent("4", generateText("border number 2"))
        .generate();

    try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
        mockStatic(WordprocessingMLPackage.class)) {
      mockedMLPackageStatic
          .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
          .thenReturn(mlPackage);

      Docx2Html docx2Html = service.getConvertedObject("test.docx");

      assertNotNull(docx2Html);
      assertEquals(
          "<p>test</p><border-number><number>1</number></border-number>"
              + "<border-number><number>2</number><content><p>border number 2</p>"
              + "</content></border-number>",
          docx2Html.html());
    }
  }

  @Nested
  @DisplayName("Test render List HTML")
  class TestRenderListHTML {
    private String lvlText;
    private String startVal;
    private String restartNumberingAfterBreak;
    private String color;
    private String fontStyle;
    private String fontSize;
    private boolean lvlPicBullet;
    private boolean isLgl;
    private DocumentationUnitNumberingListNumberFormat numberFormat;
    private String iLvl;
    private JcEnumeration lvlJc;
    private String suff;
    private List<NumberingListEntry> entries;

    @BeforeEach
    void setUp() {
      lvlText = "";
      startVal = "1";
      restartNumberingAfterBreak = "";
      color = "";
      fontStyle = "";
      fontSize = "";
      lvlPicBullet = false;
      isLgl = false;
      numberFormat = DocumentationUnitNumberingListNumberFormat.BULLET;
      iLvl = "0";
      lvlJc = JcEnumeration.RIGHT;
      suff = "space";
      entries = new ArrayList<>();
    }

    NumberingListEntryIndex createNumberingListEntryIndex() {
      return new NumberingListEntryIndex(
          lvlText,
          startVal,
          restartNumberingAfterBreak,
          color,
          fontStyle,
          fontSize,
          lvlPicBullet,
          isLgl,
          numberFormat,
          iLvl,
          lvlJc,
          suff);
    }

    @Test
    void testGetHtml_withListEntries_shouldRenderTwoLists_withDifferentStyles() {
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("bullet list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("bullet list entry 2", createNumberingListEntryIndex()));
      numberFormat = DocumentationUnitNumberingListNumberFormat.DECIMAL;
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("decimal list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("decimal list entry 2", createNumberingListEntryIndex()));

      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ul style=\"list-style-type:disc;\">"
                + "<li>"
                + "<p>bullet list entry 1</p>"
                + "</li>"
                + "<li>"
                + "<p>bullet list entry 2</p>"
                + "</li>"
                + "</ul>"
                + "<ol style=\"list-style-type:decimal;\">"
                + "<li>"
                + "<p>decimal list entry 1</p>"
                + "</li>"
                + "<li>"
                + "<p>decimal list entry 2</p>"
                + "</li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withListEntries_shouldHaveLowerLatinStyle() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.LOWER_LETTER;
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2", createNumberingListEntryIndex()));
      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:lower-latin;\">"
                + "<li><p>list entry 1</p></li>"
                + "<li><p>list entry 2</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withListEntries_shouldHaveUpperLatinStyle() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.UPPER_LETTER;
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2", createNumberingListEntryIndex()));
      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:upper-latin;\">"
                + "<li><p>list entry 1</p></li>"
                + "<li><p>list entry 2</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withListEntries_shouldHaveLowerRomanStyle() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.LOWER_ROMAN;
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2", createNumberingListEntryIndex()));
      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:lower-roman;\">"
                + "<li><p>list entry 1</p></li>"
                + "<li><p>list entry 2</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withListEntries_shouldHaveUpperRomanStyle() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.UPPER_ROMAN;
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2", createNumberingListEntryIndex()));
      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:upper-roman;\">"
                + "<li><p>list entry 1</p></li>"
                + "<li><p>list entry 2</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_LglList() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.DECIMAL;
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 1", createNumberingListEntryIndex()));
      iLvl = "1";
      isLgl = true;
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 1.1", createNumberingListEntryIndex()));

      iLvl = "0";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("bullet list entry 2", createNumberingListEntryIndex()));
      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:decimal;\">"
                + "<li><p>list entry 1</p></li>"
                + "<ol style=\"list-style-type:decimal;\">"
                + "<li style=\"list-style-type:decimal\"><p>list entry 1.1</p></li>"
                + "</ol>"
                + "<li style=\"list-style-type:decimal\"><p>bullet list entry 2</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_emptyStartValue() {
      startVal = "";
      lvlText = "%1.";
      numberFormat = DocumentationUnitNumberingListNumberFormat.DECIMAL;
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("bullet list entry 1", createNumberingListEntryIndex()));
      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:decimal;\">"
                + "<li><p>bullet list entry 1</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withThreeLvlOfNumberingListEntriesBullet() {
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("bullet list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("bullet list entry 2", createNumberingListEntryIndex()));
      iLvl = "1";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("bullet list entry 2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("bullet list entry 2.2", createNumberingListEntryIndex()));
      iLvl = "2";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "bullet list entry 2.2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "bullet list entry 2.2.2", createNumberingListEntryIndex()));
      iLvl = "1";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("bullet list entry 2.3", createNumberingListEntryIndex()));
      iLvl = "0";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("bullet list entry 3", createNumberingListEntryIndex()));

      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ul style=\"list-style-type:disc;\">"
                + "<li><p>bullet list entry 1</p></li>"
                + "<li><p>bullet list entry 2</p></li>"
                + "<ul style=\"list-style-type:disc;\">"
                + "<li><p>bullet list entry 2.1</p></li>"
                + "<li><p>bullet list entry 2.2</p></li>"
                + "<ul style=\"list-style-type:disc;\">"
                + "<li><p>bullet list entry 2.2.1</p></li>"
                + "<li><p>bullet list entry 2.2.2</p></li>"
                + "</ul><li><p>bullet list entry 2.3</p></li>"
                + "</ul>"
                + "<li><p>bullet list entry 3</p></li>"
                + "</ul>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withThreeLvlOfNumberingListEntriesLowerRoman() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.LOWER_ROMAN;
      lvlText = "%1.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower roman list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower roman list entry 2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower roman list entry 2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower roman list entry 2.2", createNumberingListEntryIndex()));
      iLvl = "2";
      lvlText = "%3.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower roman list entry 2.2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower roman list entry 2.2.2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower roman list entry 2.3", createNumberingListEntryIndex()));
      iLvl = "0";
      lvlText = "%1.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower roman list entry 3", createNumberingListEntryIndex()));

      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:lower-roman;\">"
                + "<li><p>lower roman list entry 1</p></li>"
                + "<li><p>lower roman list entry 2</p></li>"
                + "<ol style=\"list-style-type:lower-roman;\">"
                + "<li><p>lower roman list entry 2.1</p></li>"
                + "<li><p>lower roman list entry 2.2</p></li>"
                + "<ol style=\"list-style-type:lower-roman;\">"
                + "<li><p>lower roman list entry 2.2.1</p></li>"
                + "<li><p>lower roman list entry 2.2.2</p></li>"
                + "</ol>"
                + "<li><p>lower roman list entry 2.3</p></li>"
                + "</ol>"
                + "<li><p>lower roman list entry 3</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withThreeLvlOfNumberingListEntriesUpperRoman() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.UPPER_ROMAN;
      lvlText = "%1.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper roman list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper roman list entry 2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper roman list entry 2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper roman list entry 2.2", createNumberingListEntryIndex()));
      iLvl = "2";
      lvlText = "%3.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper roman list entry 2.2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper roman list entry 2.2.2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper roman list entry 2.3", createNumberingListEntryIndex()));
      iLvl = "0";
      lvlText = "%1.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper roman list entry 3", createNumberingListEntryIndex()));

      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:upper-roman;\">"
                + "<li><p>upper roman list entry 1</p></li>"
                + "<li><p>upper roman list entry 2</p></li>"
                + "<ol style=\"list-style-type:upper-roman;\">"
                + "<li><p>upper roman list entry 2.1</p></li>"
                + "<li><p>upper roman list entry 2.2</p></li>"
                + "<ol style=\"list-style-type:upper-roman;\">"
                + "<li><p>upper roman list entry 2.2.1</p></li>"
                + "<li><p>upper roman list entry 2.2.2</p></li></ol>"
                + "<li><p>upper roman list entry 2.3</p></li></ol>"
                + "<li><p>upper roman list entry 3</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withThreeLvlOfNumberingListEntriesLowerLetter() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.LOWER_LETTER;
      lvlText = "%1)";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower letter list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower letter list entry 2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower letter list entry 2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower letter list entry 2.2", createNumberingListEntryIndex()));
      iLvl = "2";
      lvlText = "(%3)";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower letter list entry 2.2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower letter list entry 2.2.2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower letter list entry 2.3", createNumberingListEntryIndex()));
      iLvl = "0";
      lvlText = "%1)";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "lower letter list entry 3", createNumberingListEntryIndex()));

      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:lower-latin;\">"
                + "<li><p>lower letter list entry 1</p></li>"
                + "<li><p>lower letter list entry 2</p></li>"
                + "<ol style=\"list-style-type:lower-latin;\">"
                + "<li><p>lower letter list entry 2.1</p></li>"
                + "<li><p>lower letter list entry 2.2</p></li>"
                + "<ol style=\"list-style-type:lower-latin;\">"
                + "<li><p>lower letter list entry 2.2.1</p></li>"
                + "<li><p>lower letter list entry 2.2.2</p></li>"
                + "</ol>"
                + "<li><p>lower letter list entry 2.3</p></li>"
                + "</ol>"
                + "<li><p>lower letter list entry 3</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withThreeLvlOfNumberingListEntriesUpperLetter() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.UPPER_LETTER;
      lvlText = "%1)";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper letter list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper letter list entry 2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper letter list entry 2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper letter list entry 2.2", createNumberingListEntryIndex()));
      iLvl = "2";
      lvlText = "(%3)";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper letter list entry 2.2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper letter list entry 2.2.2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper letter list entry 2.3", createNumberingListEntryIndex()));
      iLvl = "0";
      lvlText = "%1)";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "upper letter list entry 3", createNumberingListEntryIndex()));

      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:upper-latin;\">"
                + "<li><p>upper letter list entry 1</p></li>"
                + "<li><p>upper letter list entry 2</p></li>"
                + "<ol style=\"list-style-type:upper-latin;\">"
                + "<li><p>upper letter list entry 2.1</p></li>"
                + "<li><p>upper letter list entry 2.2</p></li>"
                + "<ol style=\"list-style-type:upper-latin;\">"
                + "<li><p>upper letter list entry 2.2.1</p></li>"
                + "<li><p>upper letter list entry 2.2.2</p></li>"
                + "</ol>"
                + "<li><p>upper letter list entry 2.3</p></li>"
                + "</ol>"
                + "<li><p>upper letter list entry 3</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withThreeLvlOfNumberingListEntriesDecimal() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.DECIMAL;
      lvlText = "%1.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("decimal list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("decimal list entry 2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%1.%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "decimal list entry 2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "decimal list entry 2.2", createNumberingListEntryIndex()));
      iLvl = "2";
      lvlText = "%1.%2.%3.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "decimal list entry 2.2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "decimal list entry 2.2.2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%1.%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry(
                  "decimal list entry 2.3", createNumberingListEntryIndex()));
      iLvl = "0";
      lvlText = "%1.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("decimal list entry 3", createNumberingListEntryIndex()));

      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:decimal;\">"
                + "<li><p>decimal list entry 1</p></li>"
                + "<li><p>decimal list entry 2</p></li>"
                + "<ol style=\"list-style-type:decimal;\">"
                + "<li><p>decimal list entry 2.1</p></li>"
                + "<li><p>decimal list entry 2.2</p></li>"
                + "<ol style=\"list-style-type:decimal;\">"
                + "<li><p>decimal list entry 2.2.1</p></li>"
                + "<li><p>decimal list entry 2.2.2</p></li>"
                + "</ol>"
                + "<li><p>decimal list entry 2.3</p></li>"
                + "</ol>"
                + "<li><p>decimal list entry 3</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }

    @Test
    void testGetHtml_withThreeLvlOfNumberingListEntriesMixed() {
      numberFormat = DocumentationUnitNumberingListNumberFormat.DECIMAL;
      lvlText = "%1.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2", createNumberingListEntryIndex()));
      iLvl = "1";
      numberFormat = DocumentationUnitNumberingListNumberFormat.BULLET;
      fontStyle = "Symbol";
      fontSize = "18";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2.2", createNumberingListEntryIndex()));
      iLvl = "2";
      numberFormat = DocumentationUnitNumberingListNumberFormat.DECIMAL;
      lvlText = "%1.%2.%3.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2.2.1", createNumberingListEntryIndex()));
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2.2.2", createNumberingListEntryIndex()));
      iLvl = "1";
      lvlText = "%1.%2.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2.3", createNumberingListEntryIndex()));
      iLvl = "2";
      numberFormat = DocumentationUnitNumberingListNumberFormat.BULLET;
      fontStyle = "Symbol";
      fontSize = "18";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 2.3.1", createNumberingListEntryIndex()));
      numberFormat = DocumentationUnitNumberingListNumberFormat.DECIMAL;
      iLvl = "0";
      lvlText = "%1.";
      entries.add(
          (NumberingListEntry)
              generateNumberingListEntry("list entry 3", createNumberingListEntryIndex()));

      TestDocumentGenerator generator =
          new TestDocumentGenerator(client, responseBytes, mlPackage, converter);
      int index = 0;
      for (DocumentationUnitDocx entry : entries) {
        generator.addContent(String.valueOf(++index), entry);
      }
      generator.generate();

      try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
          mockStatic(WordprocessingMLPackage.class)) {
        mockedMLPackageStatic
            .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
            .thenReturn(mlPackage);

        Docx2Html docx2Html = service.getConvertedObject("test.docx");

        assertNotNull(docx2Html);
        assertEquals(
            "<ol style=\"list-style-type:decimal;\">"
                + "<li><p>list entry 1</p></li>"
                + "<li><p>list entry 2</p></li>"
                + "<ul style=\"list-style-type:disc;\">"
                + "<li><p>list entry 2.1</p></li>"
                + "<li><p>list entry 2.2</p></li>"
                + "<ol style=\"list-style-type:decimal;\">"
                + "<li><p>list entry 2.2.1</p></li>"
                + "<li><p>list entry 2.2.2</p></li>"
                + "</ol>"
                + "<li><p>list entry 2.3</p></li>"
                + "<ul style=\"list-style-type:disc;\">"
                + "<li><p>list entry 2.3.1</p></li>"
                + "</ul>"
                + "</ul>"
                + "<li><p>list entry 3</p></li>"
                + "</ol>",
            docx2Html.html());
      }
    }
  }

  @Test
  void testGetHtml_withInputStreamIsNull() {
    when(client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
        .thenReturn(responseBytes);
    when(responseBytes.asInputStream()).thenReturn(null);

    Docx2Html docx2Html = service.getConvertedObject("test.docx");
    assertEquals(Docx2Html.EMPTY, docx2Html);
  }

  @Test
  void testGetHtml_withLoadDocxThrowsException() {
    when(client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
        .thenReturn(responseBytes);
    when(responseBytes.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));

    try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
        mockStatic(WordprocessingMLPackage.class)) {
      mockedMLPackageStatic
          .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
          .thenThrow(Docx4JException.class);

      // TODO throwable.getMessage().equals("Couldn't load docx file!"))
      Assertions.assertThrows(
          DocumentConverterException.class, () -> service.getConvertedObject("test.docx"));
    }
  }

  @Test
  void testGetHtml_withProperties() {
    when(client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
        .thenReturn(responseBytes);
    when(responseBytes.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));

    MainDocumentPart mainDocumentPart = mock(MainDocumentPart.class);
    when(mainDocumentPart.getContent()).thenReturn(Collections.emptyList());

    DocPropsCustomPart docPropsCustomPart = mock(DocPropsCustomPart.class);

    Properties.Property courtTypeProperty = mock(Properties.Property.class);
    when(courtTypeProperty.getName()).thenReturn("Gerichtstyp");
    when(courtTypeProperty.getLpwstr()).thenReturn("BGH");

    Properties.Property fileNumberProperty = mock(Properties.Property.class);
    when(fileNumberProperty.getName()).thenReturn("Aktenzeichen");
    when(fileNumberProperty.getLpwstr()).thenReturn("VI ZR 20/23");

    Properties.Property legalEffectProperty = mock(Properties.Property.class);
    when(legalEffectProperty.getName()).thenReturn("Rechtskraft");
    when(legalEffectProperty.getLpwstr()).thenReturn("ja");

    Properties.Property appraisalBodyProperty = mock(Properties.Property.class);
    when(appraisalBodyProperty.getName()).thenReturn("Spruchkoerper");
    when(appraisalBodyProperty.getLpwstr()).thenReturn("1. Senat");

    Properties.Property randomProperty = mock(Properties.Property.class);
    when(randomProperty.getName()).thenReturn("Random");
    when(randomProperty.getLpwstr()).thenReturn("foo");

    Properties jaxbElement = mock(Properties.class);
    when(docPropsCustomPart.getJaxbElement()).thenReturn(jaxbElement);
    when(jaxbElement.getProperty())
        .thenReturn(
            List.of(
                fileNumberProperty,
                courtTypeProperty,
                legalEffectProperty,
                appraisalBodyProperty,
                randomProperty));

    when(mlPackage.getDocPropsCustomPart()).thenReturn(docPropsCustomPart);
    when(mlPackage.getMainDocumentPart()).thenReturn(mainDocumentPart);

    try (MockedStatic<WordprocessingMLPackage> mockedMLPackageStatic =
        mockStatic(WordprocessingMLPackage.class)) {
      mockedMLPackageStatic
          .when(() -> WordprocessingMLPackage.load(any(InputStream.class)))
          .thenReturn(mlPackage);

      Docx2Html docx2Html = service.getConvertedObject("test.docx");
      Assertions.assertNotNull(docx2Html);

      assertEquals(4, docx2Html.properties().size());
      assertEquals("VI ZR 20/23", docx2Html.properties().get(DocxMetadataProperty.FILE_NUMBER));
      assertEquals("BGH", docx2Html.properties().get(DocxMetadataProperty.COURT_TYPE));
      assertEquals("1. Senat", docx2Html.properties().get(DocxMetadataProperty.APPRAISAL_BODY));
      assertEquals("ja", docx2Html.properties().get(DocxMetadataProperty.LEGAL_EFFECT));
    }
  }

  private ParagraphElement generateText(String text) {
    var textElement = new ParagraphElement();
    var runTextElement = new RunTextElement();
    runTextElement.setText(text);
    textElement.addRunElement(runTextElement);
    return textElement;
  }

  private BorderNumber generateBorderNumber(String text) {
    BorderNumber borderNumber = new BorderNumber();
    borderNumber.addNumberText(text);
    return borderNumber;
  }

  private TableElement generateTable(String text) {
    List<DocumentationUnitDocx> paragraphElements = List.of(generateText(text));
    List<TableCellElement> cells = List.of(new TableCellElement(paragraphElements, null));
    List<TableRowElement> rows = List.of(new TableRowElement(cells));

    return new TableElement(rows);
  }

  private DocumentationUnitDocx generateNumberingListEntry(
      String text, NumberingListEntryIndex numberingListEntryIndex) {
    var paragraphElement = generateText(text);

    return new NumberingListEntry(paragraphElement, numberingListEntryIndex);
  }

  private static class TestDocumentGenerator {

    private final List<Object> ids = new ArrayList<>();
    private final S3Client client;
    private final ResponseBytes<GetObjectResponse> responseBytes;
    private final WordprocessingMLPackage mlPackage;
    private final DocxConverter converter;

    public TestDocumentGenerator(
        S3Client client,
        ResponseBytes<GetObjectResponse> responseBytes,
        WordprocessingMLPackage mlPackage,
        DocxConverter converter) {

      this.client = client;
      this.responseBytes = responseBytes;
      this.mlPackage = mlPackage;
      this.converter = converter;
    }

    private TestDocumentGenerator addContent(
        String id, DocumentationUnitDocx documentationUnitDocx) {
      ids.add(id);
      when(converter.convert(id)).thenReturn(documentationUnitDocx);

      return this;
    }

    private void generate() {
      when(client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class)))
          .thenReturn(responseBytes);
      when(responseBytes.asInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
      MainDocumentPart mainDocumentPart = mock(MainDocumentPart.class);
      when(mlPackage.getMainDocumentPart()).thenReturn(mainDocumentPart);
      when(mainDocumentPart.getContent()).thenReturn(ids);
    }
  }
}
