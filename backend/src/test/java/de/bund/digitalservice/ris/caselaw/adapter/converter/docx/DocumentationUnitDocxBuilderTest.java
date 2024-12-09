package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import static de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocumentationUnitDocxBuilder.NON_BREAKING_SPACE;
import static de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocumentationUnitDocxBuilder.SOFT_HYPHEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.domain.docx.AnchorImageElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.BorderNumber;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocxImagePart;
import de.bund.digitalservice.ris.caselaw.domain.docx.ErrorRunElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.InlineImageElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingList.DocumentationUnitNumberingListNumberFormat;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingListEntry;
import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunTextElement;
import jakarta.xml.bind.JAXBElement;
import java.awt.Dimension;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.docx4j.dml.CTBlip;
import org.docx4j.dml.CTBlipFillProperties;
import org.docx4j.dml.CTNonVisualDrawingProps;
import org.docx4j.dml.CTPositiveSize2D;
import org.docx4j.dml.Graphic;
import org.docx4j.dml.GraphicData;
import org.docx4j.dml.picture.Pic;
import org.docx4j.dml.wordprocessingDrawing.Anchor;
import org.docx4j.dml.wordprocessingDrawing.CTPosH;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.dml.wordprocessingDrawing.STAlignH;
import org.docx4j.model.listnumbering.AbstractListNumberingDefinition;
import org.docx4j.model.listnumbering.ListLevel;
import org.docx4j.model.listnumbering.ListNumberingDefinition;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTFramePr;
import org.docx4j.wml.CTVerticalAlignRun;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.Jc;
import org.docx4j.wml.JcEnumeration;
import org.docx4j.wml.NumberFormat;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.PPrBase.NumPr;
import org.docx4j.wml.PPrBase.NumPr.Ilvl;
import org.docx4j.wml.PPrBase.NumPr.NumId;
import org.docx4j.wml.PPrBase.Spacing;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.STVerticalAlignRun;
import org.docx4j.wml.Style;
import org.docx4j.wml.Text;
import org.docx4j.wml.U;
import org.docx4j.wml.UnderlineEnumeration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DocumentationUnitDocxBuilderTest {

  @Test
  void test_withoutConvertableElements() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    var result = builder.build(new ArrayList<>());

    assertNull(result);
  }

  @Test
  void testSetParagraph() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();

    var returnedBuilder = builder.setParagraph(paragraph);

    assertEquals(builder, returnedBuilder);
    assertEquals(paragraph, returnedBuilder.paragraph);
  }

  @Test
  void testBuild_withBorderNumber() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    PPr pPr = new PPr();
    NumPr numPr = new NumPr();
    NumId numId = new NumId();
    numId.setVal(new BigInteger("0"));
    numPr.setNumId(numId);
    pPr.setNumPr(numPr);
    PPrBase.PStyle pStyle = new PPrBase.PStyle();
    pStyle.setVal("RandNummer");
    pPr.setPStyle(pStyle);
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("1");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertTrue(result instanceof BorderNumber);
    var borderNumberElement = (BorderNumber) result;
    assertEquals("1", borderNumberElement.getNumber());
    assertEquals(numId.getVal().intValue(), borderNumberElement.getNumId());

    var htmlString = borderNumberElement.toHtmlString();
    assertEquals("<border-number><number>1</number></border-number>", htmlString);
  }

  @Test
  void testBuild_withListParagraphBorderNumber_shouldSucceed() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    PPr pPr = new PPr();
    PPrBase.PStyle pStyle = new PPrBase.PStyle();
    pStyle.setVal("ListParagraph");
    pPr.setPStyle(pStyle);
    paragraph.setPPr(pPr);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertTrue(result instanceof BorderNumber);
    // it won't have a number assigned because postprocessing is not done, see the
    // DocumentationUnitDocxListUtilsTest for testing the postprocessing of border numbers
  }

  @Test
  void testBuild_withListenabsatzBorderNumber_shouldSucceed() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    PPr pPr = new PPr();
    PPrBase.PStyle pStyle = new PPrBase.PStyle();
    pStyle.setVal("Listenabsatz");
    pPr.setPStyle(pStyle);
    paragraph.setPPr(pPr);
    CTFramePr framePr = new CTFramePr();
    pPr.setFramePr(framePr);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertTrue(result instanceof BorderNumber);
  }

  @Test
  void testBuild_withBorderNumberThatHasNoBorderNumberTemplateStyle_shouldSucceed() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    PPr pPr = new PPr();
    BooleanDefaultTrue keepNext = new BooleanDefaultTrue();
    keepNext.setVal(true);
    pPr.setKeepNext(keepNext); // indicator 1
    Spacing spacing = new Spacing();
    spacing.setLine(BigInteger.valueOf(240L)); // indicator 2
    pPr.setSpacing(spacing);
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("1"); // indicator 3: just one integer
    run.getContent().add(new JAXBElement<>(new QName("text"), Text.class, text));
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertTrue(result instanceof BorderNumber);
    var borderNumberElement = (BorderNumber) result;
    assertEquals("1", borderNumberElement.getNumber());
    assertEquals(
        "<border-number><number>1</number></border-number>", borderNumberElement.toHtmlString());
  }

  @Test
  void testBuild_withBorderNumberThatHasNoBorderNumberTemplateStyle_wrongTextShouldFail() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    PPr pPr = new PPr();
    BooleanDefaultTrue keepNext = new BooleanDefaultTrue();
    keepNext.setVal(true);
    pPr.setKeepNext(keepNext);
    Spacing spacing = new Spacing();
    spacing.setLine(BigInteger.valueOf(240L));
    pPr.setSpacing(spacing);
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("1."); // can't be parsed as integer
    run.getContent().add(new JAXBElement<>(new QName("text"), Text.class, text));
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    var paragraphElement = (ParagraphElement) result;
    assertEquals("<p>1.</p>", paragraphElement.toHtmlString());
  }

  @Test
  void testBuild_withText() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();

    R wrongRun = new R();
    wrongRun.getContent().add(new Object());
    paragraph.getContent().add(wrongRun);

    R run = new R();
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    var paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    assertEquals("text", ((RunTextElement) runElement).getText());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p>text</p>", htmlString);
  }

  @Test
  void testBuild_withParagraphWithoutText() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    paragraph.getContent().add(run);
    paragraph.getContent().add(new P.Hyperlink());

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    var paragraphElement = (ParagraphElement) result;
    assertTrue(paragraphElement.getRunElements().isEmpty());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p></p>", htmlString);
  }

  @Test
  void testBuild_withTextAndParagraphAlignmentRight() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    PPr pPr = new PPr();
    Jc jc = new Jc();
    jc.setVal(JcEnumeration.RIGHT);
    pPr.setJc(jc);
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    assertEquals("text", ((RunTextElement) runElement).getText());
    assertTrue(paragraphElement.getStyleString().contains("text-align: right"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p style=\"text-align: right;\">text</p>", htmlString);
  }

  @Test
  void testBuild_withTextAndParagraphAlignmentLeft() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    PPr pPr = new PPr();
    Jc jc = new Jc();
    jc.setVal(JcEnumeration.LEFT);
    pPr.setJc(jc);
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    assertEquals("text", ((RunTextElement) runElement).getText());
    assertTrue(paragraphElement.getStyleString().contains("text-align: left"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p style=\"text-align: left;\">text</p>", htmlString);
  }

  @Test
  void testBuild_withTextAndParagraphAlignmentJustify() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    PPr pPr = new PPr();
    Jc jc = new Jc();
    jc.setVal(JcEnumeration.BOTH);
    pPr.setJc(jc);
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    assertEquals("text", ((RunTextElement) runElement).getText());
    assertTrue(paragraphElement.getStyleString().contains("text-align: justify"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p style=\"text-align: justify;\">text</p>", htmlString);
  }

  @Test
  void testBuild_withTextAndParagraphAlignmentCenter() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    PPr pPr = new PPr();
    Jc jc = new Jc();
    jc.setVal(JcEnumeration.CENTER);
    pPr.setJc(jc);
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    assertEquals("text", ((RunTextElement) runElement).getText());
    assertTrue(paragraphElement.getStyleString().contains("text-align: center"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p style=\"text-align: center;\">text</p>", htmlString);
  }

  @Test
  void testBuild_withTextAndSize() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();

    RPr rPr = new RPr();
    HpsMeasure size = new HpsMeasure();
    size.setVal(new BigInteger("48"));
    rPr.setSz(size);
    R run = new R();
    run.setRPr(rPr);
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    rPr = new RPr();
    size = new HpsMeasure();
    size.setVal(new BigInteger("21"));
    rPr.setSz(size);
    run = new R();
    run.setRPr(rPr);
    text = new Text();
    text.setValue("text2");
    element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(2, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    var runTextElement = ((RunTextElement) runElement);
    assertEquals("text", runTextElement.getText());
    assertTrue(runTextElement.getStyleString().contains("font-size: 24pt"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals(
        "<p><span style=\"font-size: 24pt;\">text</span><span style=\"font-size: 10.5pt;\">text2</span></p>",
        htmlString);
  }

  @Test
  void testBuild_withTextAndWeight() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    RPr rPr = new RPr();
    BooleanDefaultTrue bold = new BooleanDefaultTrue();
    bold.setVal(true);
    rPr.setB(bold);
    R run = new R();
    run.setRPr(rPr);
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    var runTextElement = (RunTextElement) runElement;
    assertEquals("text", runTextElement.getText());
    assertTrue(runTextElement.getStyleString().contains("font-weight: bold"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p><span style=\"font-weight: bold;\">text</span></p>", htmlString);
  }

  @Test
  void testBuild_withTextAndItalic() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    RPr rPr = new RPr();
    BooleanDefaultTrue italic = new BooleanDefaultTrue();
    italic.setVal(true);
    rPr.setI(italic);
    R run = new R();
    run.setRPr(rPr);
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    var runTextElement = (RunTextElement) runElement;
    assertEquals("text", runTextElement.getText());
    assertTrue(runTextElement.getStyleString().contains("font-style: italic"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p><span style=\"font-style: italic;\">text</span></p>", htmlString);
  }

  @Test
  void testBuild_withTextAndStrike() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    RPr rPr = new RPr();
    BooleanDefaultTrue strike = new BooleanDefaultTrue();
    strike.setVal(true);
    rPr.setStrike(strike);
    R run = new R();
    run.setRPr(rPr);
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    var runTextElement = (RunTextElement) runElement;
    assertEquals("text", runTextElement.getText());
    assertTrue(runTextElement.getStyleString().contains("text-decoration: line-through"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p><span style=\"text-decoration: line-through;\">text</span></p>", htmlString);
  }

  @Test
  void testBuild_withTextAndUnderline() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    RPr rPr = new RPr();
    U underline = new U();
    underline.setVal(UnderlineEnumeration.SINGLE);
    rPr.setU(underline);
    R run = new R();
    run.setRPr(rPr);
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    var runTextElement = (RunTextElement) runElement;
    assertEquals("text", runTextElement.getText());
    assertTrue(runTextElement.getStyleString().contains("text-decoration: underline"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p><span style=\"text-decoration: underline;\">text</span></p>", htmlString);
  }

  @Test
  void testBuild_withTextAndSubscript() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();

    RPr rPr = new RPr();
    CTVerticalAlignRun vertAlign = new CTVerticalAlignRun();
    vertAlign.setVal(STVerticalAlignRun.SUBSCRIPT);
    rPr.setVertAlign(vertAlign);
    R run = new R();
    run.setRPr(rPr);
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    var runTextElement = ((RunTextElement) runElement);
    assertEquals("text", runTextElement.getText());
    assertTrue(runTextElement.getStyleString().contains("vertical-align: sub"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p><span style=\"vertical-align: sub;\">text</span></p>", htmlString);
  }

  @Test
  void testBuild_withTextAndSuperscript() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();

    RPr rPr = new RPr();
    CTVerticalAlignRun vertAlign = new CTVerticalAlignRun();
    vertAlign.setVal(STVerticalAlignRun.SUPERSCRIPT);
    rPr.setVertAlign(vertAlign);
    R run = new R();
    run.setRPr(rPr);
    Text text = new Text();
    text.setValue("text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var result = builder.setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    var runTextElement = ((RunTextElement) runElement);
    assertEquals("text", runTextElement.getText());
    assertTrue(runTextElement.getStyleString().contains("vertical-align: super"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p><span style=\"vertical-align: super;\">text</span></p>", htmlString);
  }

  @Test
  void testBuild_withMultipleTextDecoration() {

    RPr rPr = new RPr();
    BooleanDefaultTrue strike = new BooleanDefaultTrue();
    strike.setVal(true);
    rPr.setStrike(strike);
    U underline = new U();
    underline.setVal(UnderlineEnumeration.SINGLE);
    rPr.setU(underline);

    P paragraph =
        TestDocxBuilder.newParagraphBuilder()
            .setRunElementStyle(rPr)
            .addRunElement(TestDocxBuilder.buildTextRunElement("text"))
            .build();

    var result =
        DocumentationUnitDocxBuilder.newInstance().setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    assertEquals("text", ((RunTextElement) runElement).getText());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals(
        "<p><span style=\"text-decoration: line-through underline;\">text</span></p>", htmlString);
  }

  @Test
  void testBuild_withRunElementStyles() {

    RPr rPr = new RPr();
    BooleanDefaultTrue strike = new BooleanDefaultTrue();
    strike.setVal(true);
    rPr.setStrike(strike);

    P paragraph =
        TestDocxBuilder.newParagraphBuilder()
            .setRunElementStyle(rPr)
            .addRunElement(TestDocxBuilder.buildTextRunElement("text"))
            .build();

    var result =
        DocumentationUnitDocxBuilder.newInstance().setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    var runTextElement = (RunTextElement) runElement;
    assertEquals("text", runTextElement.getText());
    assertTrue(runTextElement.getStyleString().contains("text-decoration: line-through"));

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p><span style=\"text-decoration: line-through;\">text</span></p>", htmlString);
  }

  @Test
  void testBuild_withExternalStyles() {
    Map<String, Style> styles = new HashMap<>();
    Style style = new Style();

    PPr pPr = new PPr();
    RPr rPr = new RPr();
    U underline = new U();
    underline.setVal(UnderlineEnumeration.SINGLE);
    rPr.setU(underline);

    style.setRPr(rPr);
    styles.put("external", style);
    PPrBase.PStyle pStyle = new PPrBase.PStyle();
    pStyle.setVal("external");
    pPr.setPStyle(pStyle);

    P paragraph =
        TestDocxBuilder.newParagraphBuilder()
            .addRunElement(TestDocxBuilder.buildTextRunElement("text"))
            .setParagraphStyle(pPr)
            .build();

    var converter = new DocxConverter();
    converter.setStyles(styles);
    var result =
        DocumentationUnitDocxBuilder.newInstance()
            .setParagraph(paragraph)
            .setConverter(converter)
            .build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    var runTextElement = (RunTextElement) runElement;
    assertEquals("text", runTextElement.getText());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p><span style=\"text-decoration: underline;\">text</span></p>", htmlString);
  }

  @Test
  void testBuild_withMultipleTextBlocks() {
    P paragraph =
        TestDocxBuilder.newParagraphBuilder()
            .addRunElement(TestDocxBuilder.buildTextRunElement("run text 1"))
            .addRunElement(TestDocxBuilder.buildTextRunElement("run text 2"))
            .build();

    var result =
        DocumentationUnitDocxBuilder.newInstance().setParagraph(paragraph).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(2, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());
    var runTextElement = (RunTextElement) runElement;
    assertEquals("run text 1", runTextElement.getText());
    runElement = paragraphElement.getRunElements().get(1);
    assertEquals(RunTextElement.class, runElement.getClass());
    runTextElement = (RunTextElement) runElement;
    assertEquals("run text 2", runTextElement.getText());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p>run text 1run text 2</p>", htmlString);
  }

  @Test
  void testBuild_withInlineImage() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    Drawing drawing = new Drawing();
    drawing.getAnchorOrInline().add(generateInline(null, null, null));
    JAXBElement<Drawing> element = new JAXBElement<>(new QName("drawing"), Drawing.class, drawing);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    HashMap<String, DocxImagePart> images = new HashMap<>();
    DocxImagePart image = new DocxImagePart("content-extension", new byte[] {1, 2});
    images.put("image-ref", image);

    var converter = new DocxConverter();
    converter.setImages(images);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(InlineImageElement.class, runElement.getClass());
    var runImageElement = (InlineImageElement) runElement;
    assertEquals("content-extension", runImageElement.getContentType());
    assertEquals("AQI=", runImageElement.getBase64Representation());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p><img src=\"data:content-extension;base64, AQI=\" /></p>", htmlString);
  }

  @Test
  void testBuild_withInlineImage_withAlternateText() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    Drawing drawing = new Drawing();
    drawing.getAnchorOrInline().add(generateInline("name", "description", null));
    JAXBElement<Drawing> element = new JAXBElement<>(new QName("drawing"), Drawing.class, drawing);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    HashMap<String, DocxImagePart> images = new HashMap<>();
    DocxImagePart image = new DocxImagePart("content-extension", new byte[] {1, 2});
    images.put("image-ref", image);

    var converter = new DocxConverter();
    converter.setImages(images);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(InlineImageElement.class, runElement.getClass());
    var runImageElement = (InlineImageElement) runElement;
    assertEquals("content-extension", runImageElement.getContentType());
    assertEquals("AQI=", runImageElement.getBase64Representation());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals(
        "<p><img src=\"data:content-extension;base64, AQI=\" alt=\"namedescription\" /></p>",
        htmlString);
  }

  @Test
  void testBuild_withInlineImage_withSize() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    Drawing drawing = new Drawing();
    drawing.getAnchorOrInline().add(generateInline(null, null, new Dimension(10, 10)));
    JAXBElement<Drawing> element = new JAXBElement<>(new QName("drawing"), Drawing.class, drawing);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    HashMap<String, DocxImagePart> images = new HashMap<>();
    DocxImagePart image = new DocxImagePart("content-extension", new byte[] {1, 2});
    images.put("image-ref", image);

    var converter = new DocxConverter();
    converter.setImages(images);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(InlineImageElement.class, runElement.getClass());
    var runImageElement = (InlineImageElement) runElement;
    assertEquals("content-extension", runImageElement.getContentType());
    assertEquals("AQI=", runImageElement.getBase64Representation());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals(
        "<p><img src=\"data:content-extension;base64, AQI=\" width=\"10\" height=\"10\" /></p>",
        htmlString);
  }

  @Test
  void testBuild_withInlineImage_withAlternateTextAndSize() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    Drawing drawing = new Drawing();
    drawing.getAnchorOrInline().add(generateInline("name", "description", new Dimension(10, 10)));
    JAXBElement<Drawing> element = new JAXBElement<>(new QName("drawing"), Drawing.class, drawing);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    HashMap<String, DocxImagePart> images = new HashMap<>();
    DocxImagePart image = new DocxImagePart("content-extension", new byte[] {1, 2});
    images.put("image-ref", image);

    var converter = new DocxConverter();
    converter.setImages(images);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(InlineImageElement.class, runElement.getClass());
    var runImageElement = (InlineImageElement) runElement;
    assertEquals("content-extension", runImageElement.getContentType());
    assertEquals("AQI=", runImageElement.getBase64Representation());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals(
        "<p><img src=\"data:content-extension;base64, AQI=\" alt=\"namedescription\" width=\"10\" height=\"10\" /></p>",
        htmlString);
  }

  @Test
  void testBuild_withAnchorGraphic() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    Drawing drawing = new Drawing();
    drawing.getAnchorOrInline().add(generateAnchor(null, null, null, null));
    JAXBElement<Drawing> element = new JAXBElement<>(new QName("drawing"), Drawing.class, drawing);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    HashMap<String, DocxImagePart> images = new HashMap<>();
    DocxImagePart image = new DocxImagePart("content-extension", new byte[] {1, 2});
    images.put("image-ref", image);

    var converter = new DocxConverter();
    converter.setImages(images);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(AnchorImageElement.class, runElement.getClass());
    var runImageElement = (AnchorImageElement) runElement;
    assertEquals("content-extension", runImageElement.getContentType());
    assertEquals("AQI=", runImageElement.getBase64Representation());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals("<p><img src=\"data:content-extension;base64, AQI=\" /></p>", htmlString);
  }

  @Test
  void testBuild_withAnchorGraphic_withLeftFloating() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    Drawing drawing = new Drawing();
    drawing.getAnchorOrInline().add(generateAnchor(null, null, null, STAlignH.LEFT));
    JAXBElement<Drawing> element = new JAXBElement<>(new QName("drawing"), Drawing.class, drawing);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    HashMap<String, DocxImagePart> images = new HashMap<>();
    DocxImagePart image = new DocxImagePart("content-extension", new byte[] {1, 2});
    images.put("image-ref", image);

    var converter = new DocxConverter();
    converter.setImages(images);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(AnchorImageElement.class, runElement.getClass());
    var runImageElement = (AnchorImageElement) runElement;
    assertEquals("content-extension", runImageElement.getContentType());
    assertEquals("AQI=", runImageElement.getBase64Representation());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals(
        "<p class=\"clearfix\"><img src=\"data:content-extension;base64, AQI=\" style=\"float: left;\" /></p>",
        htmlString);
  }

  @Test
  void testBuild_withAnchorGraphic_withRightFloating() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    Drawing drawing = new Drawing();
    drawing.getAnchorOrInline().add(generateAnchor(null, null, null, STAlignH.RIGHT));
    JAXBElement<Drawing> element = new JAXBElement<>(new QName("drawing"), Drawing.class, drawing);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    HashMap<String, DocxImagePart> images = new HashMap<>();
    DocxImagePart image = new DocxImagePart("content-extension", new byte[] {1, 2});
    images.put("image-ref", image);

    var converter = new DocxConverter();
    converter.setImages(images);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(AnchorImageElement.class, runElement.getClass());
    var runImageElement = (AnchorImageElement) runElement;
    assertEquals("content-extension", runImageElement.getContentType());
    assertEquals("AQI=", runImageElement.getBase64Representation());

    var htmlString = paragraphElement.toHtmlString();
    assertEquals(
        "<p class=\"clearfix\"><img src=\"data:content-extension;base64, AQI=\" style=\"float: right;\" /></p>",
        htmlString);
  }

  @Test
  void testBuild_withAnchorGraphic_withUnknownFloating() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    Drawing drawing = new Drawing();
    drawing.getAnchorOrInline().add(generateAnchor(null, null, null, STAlignH.CENTER));
    JAXBElement<Drawing> element = new JAXBElement<>(new QName("drawing"), Drawing.class, drawing);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    HashMap<String, DocxImagePart> images = new HashMap<>();
    DocxImagePart image = new DocxImagePart("content-extension", new byte[] {1, 2});
    images.put("image-ref", image);

    var converter = new DocxConverter();
    converter.setImages(images);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertInstanceOf(ParagraphElement.class, result);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(ErrorRunElement.class, runElement.getClass());
    var errorElement = (ErrorRunElement) runElement;

    assertEquals(
        "unknown run element: anchor image with unknown alignment: center",
        errorElement.toString());
  }

  @Test
  void testBuild_withMultipleGraphicObjects() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    Drawing drawing = new Drawing();
    drawing.getAnchorOrInline().add(generateAnchor(null, null, null, null));
    drawing.getAnchorOrInline().add(generateInline(null, null, null));
    JAXBElement<Drawing> element = new JAXBElement<>(new QName("drawing"), Drawing.class, drawing);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    builder = builder.setParagraph(paragraph);
    DocumentationUnitDocxBuilder finalBuilder = builder;
    Exception exception =
        assertThrows(DocxConverterException.class, () -> finalBuilder.build(new ArrayList<>()));

    assertEquals("more than one graphic data in a drawing", exception.getMessage());
  }

  @Test
  void testBuild_withInlineImageWithoutGraphicData() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P paragraph = new P();
    R run = new R();
    Drawing drawing = new Drawing();
    drawing.getAnchorOrInline().add(new Inline());
    JAXBElement<Drawing> element = new JAXBElement<>(new QName("drawing"), Drawing.class, drawing);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    builder = builder.setParagraph(paragraph);
    DocumentationUnitDocxBuilder finalBuilder = builder;
    Exception exception =
        assertThrows(DocxConverterException.class, () -> finalBuilder.build(new ArrayList<>()));

    assertEquals("no graphic data", exception.getMessage());
  }

  @Test
  void testBuild_withNumberingList() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    PPr pPr = new PPr();
    NumPr numPr = new NumPr();
    NumId numId = new NumId();
    numId.setVal(new BigInteger("1"));
    numPr.setNumId(numId);
    Ilvl ilvl = new Ilvl();
    ilvl.setVal(new BigInteger("0"));
    numPr.setIlvl(ilvl);
    pPr.setNumPr(numPr);
    P paragraph = new P();
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("test text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var listNumberingDefinitions = new HashMap<String, ListNumberingDefinition>();
    var listNumberingDefinition = mock(ListNumberingDefinition.class);
    var abstractListDefinition = mock(AbstractListNumberingDefinition.class);
    var listLevel = mock(ListLevel.class);
    when(listLevel.getNumFmt()).thenReturn(NumberFormat.DECIMAL);
    when(listNumberingDefinition.getAbstractListDefinition()).thenReturn(abstractListDefinition);
    HashMap<String, ListLevel> listLevels = new HashMap<>();
    listLevels.put("0", listLevel);
    when(abstractListDefinition.getListLevels()).thenReturn(listLevels);
    listNumberingDefinitions.put("1", listNumberingDefinition);

    var converter = new DocxConverter();
    converter.setListNumberingDefinitions(listNumberingDefinitions);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertTrue(result instanceof NumberingListEntry);
    var numberingListEntry = (NumberingListEntry) result;
    assertNotNull(numberingListEntry.paragraphElement());
    ParagraphElement paragraphElement = (ParagraphElement) numberingListEntry.paragraphElement();
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());

    var htmlString = numberingListEntry.toHtmlString();
    assertEquals("<p>test text</p>", htmlString);
  }

  @Test
  void testBuild_withNumberingList_havingBullets() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    PPr pPr = new PPr();
    NumPr numPr = new NumPr();
    NumId numId = new NumId();
    numId.setVal(new BigInteger("1"));
    Ilvl ilvl = new Ilvl();
    ilvl.setVal(new BigInteger("0"));
    numPr.setNumId(numId);
    numPr.setIlvl(ilvl);
    pPr.setNumPr(numPr);
    P paragraph = new P();
    paragraph.setPPr(pPr);

    var listNumberingDefinitions = new HashMap<String, ListNumberingDefinition>();
    var listNumberingDefinition = mock(ListNumberingDefinition.class);
    listNumberingDefinitions.put("1", listNumberingDefinition);
    var abstractListDefinition = mock(AbstractListNumberingDefinition.class);
    var listLevel = mock(ListLevel.class);
    when(listLevel.getNumFmt()).thenReturn(NumberFormat.BULLET);
    when(listNumberingDefinition.getAbstractListDefinition()).thenReturn(abstractListDefinition);
    HashMap<String, ListLevel> listLevels = new HashMap<>();
    listLevels.put("0", listLevel);
    when(abstractListDefinition.getListLevels()).thenReturn(listLevels);
    listNumberingDefinitions.put("1", listNumberingDefinition);
    var converter = new DocxConverter();
    converter.setListNumberingDefinitions(listNumberingDefinitions);

    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());
    assertTrue(result instanceof NumberingListEntry);
    var numberingListEntry = (NumberingListEntry) result;
    assertEquals(
        DocumentationUnitNumberingListNumberFormat.BULLET,
        numberingListEntry.numberingListEntryIndex().numberFormat());
  }

  @Test
  void testBuild_hasTheRightSorting() {
    P paragraph =
        TestDocxBuilder.newParagraphBuilder()
            .addRunElement(TestDocxBuilder.buildTextRunElement("test"))
            .addRunElement(TestDocxBuilder.buildAnchorImageElement())
            .addRunElement(TestDocxBuilder.buildTextRunElement("test 2"))
            .addRunElement(TestDocxBuilder.buildInlineImageElement())
            .addRunElement(TestDocxBuilder.buildTextRunElement("test 3"))
            .build();

    var converter = new DocxConverter();
    converter.setImages(TestDocxBuilder.getImageMap());
    var result =
        DocumentationUnitDocxBuilder.newInstance()
            .setParagraph(paragraph)
            .setConverter(converter)
            .build(new ArrayList<>());

    assertThat(result).isInstanceOf(ParagraphElement.class);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertThat(paragraphElement.getRunElements()).hasSize(5);
    assertThat(paragraphElement.getRunElements().get(0)).isInstanceOf(AnchorImageElement.class);
    assertThat(paragraphElement.getRunElements().get(1))
        .hasFieldOrPropertyWithValue("text", "test");
    assertThat(paragraphElement.getRunElements().get(2))
        .hasFieldOrPropertyWithValue("text", "test 2");
    assertThat(paragraphElement.getRunElements().get(3)).isInstanceOf(InlineImageElement.class);
    assertThat(paragraphElement.getRunElements().get(4))
        .hasFieldOrPropertyWithValue("text", "test 3");
  }

  @Test
  void testBuild_withVmlShape() {
    P paragraph =
        TestDocxBuilder.newParagraphBuilder()
            .addRunElement(TestDocxBuilder.buildVmlImage())
            .build();

    var converter = new DocxConverter();
    converter.setImages(TestDocxBuilder.getImageMap());
    var result =
        DocumentationUnitDocxBuilder.newInstance()
            .setParagraph(paragraph)
            .setConverter(converter)
            .build(new ArrayList<>());

    assertThat(result).isInstanceOf(ParagraphElement.class);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertThat(paragraphElement.getRunElements()).hasSize(1);
    assertThat(paragraphElement.getRunElements().get(0)).isInstanceOf(AnchorImageElement.class);
    AnchorImageElement imageElement = (AnchorImageElement) paragraphElement.getRunElements().get(0);
    assertThat(imageElement.getBase64Representation()).isEqualTo("dm1s");
    assertThat(imageElement.getContentType()).isEqualTo("vml-content-extension");
  }

  @Test
  void testBuild_withUnknownRunElement_shouldHaveAnErrorRunElement() {
    R runElement = new R();
    runElement.getContent().add(new JAXBElement<>(new QName("error run"), String.class, "error"));
    P paragraph = TestDocxBuilder.newParagraphBuilder().addRunElement(runElement).build();

    var result =
        DocumentationUnitDocxBuilder.newInstance().setParagraph(paragraph).build(new ArrayList<>());

    assertThat(result).isInstanceOf(ParagraphElement.class);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertThat(paragraphElement.getRunElements()).hasSize(1);
    assertThat(paragraphElement.getRunElements().get(0)).isInstanceOf(ErrorRunElement.class);
  }

  @Test
  void testBuild_withNumberingList_withNotAllowedNumberingFormat() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    PPr pPr = new PPr();
    NumPr numPr = new NumPr();
    NumId numId = new NumId();
    numId.setVal(new BigInteger("0"));
    numPr.setNumId(numId);
    Ilvl ilvl = new Ilvl();
    ilvl.setVal(new BigInteger("0"));
    numPr.setIlvl(ilvl);
    pPr.setNumPr(numPr);
    P paragraph = new P();
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("test text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var listNumberingDefinitions = new HashMap<String, ListNumberingDefinition>();
    var listNumberingDefinition = mock(ListNumberingDefinition.class);
    var abstractListDefinition = mock(AbstractListNumberingDefinition.class);
    var listLevel = mock(ListLevel.class);
    when(listLevel.getNumFmt()).thenReturn(NumberFormat.CHICAGO);
    when(listNumberingDefinition.getAbstractListDefinition()).thenReturn(abstractListDefinition);
    HashMap<String, ListLevel> listLevels = new HashMap<>();
    listLevels.put("0", listLevel);
    when(abstractListDefinition.getListLevels()).thenReturn(listLevels);
    listNumberingDefinitions.put("0", listNumberingDefinition);

    var converter = new DocxConverter();
    converter.setListNumberingDefinitions(listNumberingDefinitions);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertTrue(result instanceof ParagraphElement);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());

    var htmlString = result.toHtmlString();
    assertEquals("<p>test text</p>", htmlString);
  }

  @Test
  void testBuild_withNumberingList_withoutNumId() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    PPr pPr = new PPr();
    NumPr numPr = new NumPr();
    NumId numId = new NumId();
    numId.setVal(new BigInteger("0"));
    numPr.setNumId(numId);
    Ilvl ilvl = new Ilvl();
    ilvl.setVal(new BigInteger("0"));
    numPr.setIlvl(ilvl);
    pPr.setNumPr(numPr);
    P paragraph = new P();
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("test text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var listNumberingDefinitions = new HashMap<String, ListNumberingDefinition>();
    var listNumberingDefinition = mock(ListNumberingDefinition.class);
    var abstractListDefinition = mock(AbstractListNumberingDefinition.class);
    var listLevel = mock(ListLevel.class);
    when(listLevel.getNumFmt()).thenReturn(NumberFormat.DECIMAL);
    when(listNumberingDefinition.getAbstractListDefinition()).thenReturn(abstractListDefinition);
    HashMap<String, ListLevel> listLevels = new HashMap<>();
    listLevels.put("0", listLevel);
    when(abstractListDefinition.getListLevels()).thenReturn(listLevels);
    listNumberingDefinitions.put("0", listNumberingDefinition);

    var converter = new DocxConverter();
    converter.setListNumberingDefinitions(listNumberingDefinitions);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertTrue(result instanceof ParagraphElement);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());

    var htmlString = result.toHtmlString();
    assertEquals("<p>test text</p>", htmlString);
  }

  @Test
  void testBuild_withNumberingList_withoutListLevel() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    PPr pPr = new PPr();
    NumPr numPr = new NumPr();
    NumId numId = new NumId();
    numId.setVal(new BigInteger("1"));
    numPr.setNumId(numId);
    Ilvl ilvl = new Ilvl();
    ilvl.setVal(new BigInteger("0"));
    numPr.setIlvl(ilvl);
    pPr.setNumPr(numPr);
    P paragraph = new P();
    paragraph.setPPr(pPr);
    R run = new R();
    Text text = new Text();
    text.setValue("test text");
    JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, text);
    run.getContent().add(element);
    paragraph.getContent().add(run);

    var listNumberingDefinitions = new HashMap<String, ListNumberingDefinition>();
    var listNumberingDefinition = mock(ListNumberingDefinition.class);
    var abstractListDefinition = mock(AbstractListNumberingDefinition.class);
    when(listNumberingDefinition.getAbstractListDefinition()).thenReturn(abstractListDefinition);
    listNumberingDefinitions.put("1", listNumberingDefinition);

    var converter = new DocxConverter();
    converter.setListNumberingDefinitions(listNumberingDefinitions);
    var result = builder.setParagraph(paragraph).setConverter(converter).build(new ArrayList<>());

    assertTrue(result instanceof ParagraphElement);
    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(1, paragraphElement.getRunElements().size());
    var runElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, runElement.getClass());

    var htmlString = result.toHtmlString();
    assertEquals("<p>test text</p>", htmlString);
  }

  public static Stream<Arguments> nodesThatShouldTurnIntoAHyphen() {
    return Stream.of(
        Arguments.of(
            List.of(SOFT_HYPHEN + NON_BREAKING_SPACE), false, List.of("-" + NON_BREAKING_SPACE)),
        Arguments.of(
            List.of(NON_BREAKING_SPACE + SOFT_HYPHEN), false, List.of(NON_BREAKING_SPACE + "-")),
        Arguments.of(
            List.of(NON_BREAKING_SPACE, SOFT_HYPHEN), false, List.of(NON_BREAKING_SPACE, "-")),
        Arguments.of(
            List.of(SOFT_HYPHEN, NON_BREAKING_SPACE), false, List.of("-", NON_BREAKING_SPACE)),
        Arguments.of(
            List.of("131/16" + NON_BREAKING_SPACE + SOFT_HYPHEN),
            false,
            List.of("131/16" + NON_BREAKING_SPACE + "-")),
        Arguments.of(
            List.of("131/16", NON_BREAKING_SPACE, SOFT_HYPHEN),
            false,
            List.of("131/16", NON_BREAKING_SPACE, "-")),
        Arguments.of(
            List.of(NON_BREAKING_SPACE, SOFT_HYPHEN + " ABC"),
            false,
            List.of(NON_BREAKING_SPACE, "- ABC")),

        // with whitespace: preserve
        Arguments.of(List.of(SOFT_HYPHEN + " "), true, List.of("-" + NON_BREAKING_SPACE)),
        Arguments.of(List.of(" " + SOFT_HYPHEN), true, List.of(NON_BREAKING_SPACE + "-")));
  }

  public static Stream<Arguments> nodesThatShouldNotTurnIntoHyphen() {
    return Stream.of(
        Arguments.of(List.of(SOFT_HYPHEN), false, null), // only soft hyphen
        Arguments.of(List.of(NON_BREAKING_SPACE), false, null), // only non-breaking space
        Arguments.of(
            List.of(NON_BREAKING_SPACE, " ", SOFT_HYPHEN),
            false,
            null), // space text node in between
        Arguments.of(
            List.of(NON_BREAKING_SPACE + " " + SOFT_HYPHEN), false, null)); // space char in between
  }

  /**
   * @param inputTextNodes the list of text nodes to be transformed into a paragraph element
   * @param expectedValues the list of expected HTML String values. If null, we expect the same as
   *     the input inputTextNodes
   */
  @ParameterizedTest
  @MethodSource({"nodesThatShouldTurnIntoAHyphen", "nodesThatShouldNotTurnIntoHyphen"})
  void testBuild_paragraphWithNBSPAndSHYCombination_shouldBeTransformedIntoHyphen(
      List<String> inputTextNodes, boolean preserveWhitespace, List<String> expectedValues) {
    if (expectedValues == null) {
      expectedValues = inputTextNodes;
    }

    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P parentParagraph = new P();
    for (String textNodeValue : inputTextNodes) {
      R run = new R();
      Text textNode = new Text();
      textNode.setValue(textNodeValue);
      textNode.setSpace(preserveWhitespace ? "preserve" : null);
      JAXBElement<Text> element = new JAXBElement<>(new QName("text"), Text.class, textNode);
      run.getContent().add(element);
      parentParagraph.getContent().add(run);
    }
    var result = builder.setParagraph(parentParagraph).build(new ArrayList<>());
    assertInstanceOf(ParagraphElement.class, result);

    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(expectedValues.size(), paragraphElement.getRunElements().size());

    for (int i = 0; i < expectedValues.size(); i++) {
      RunElement runElement = paragraphElement.getRunElements().get(i);
      assertEquals(RunTextElement.class, runElement.getClass());
      assertEquals(expectedValues.get(i), runElement.toHtmlString());
    }
  }

  @Test
  void testBuild_paragraphWithSpecialHyphenRunElements_shouldBeTransformed() {
    DocumentationUnitDocxBuilder builder = DocumentationUnitDocxBuilder.newInstance();
    P parentParagraph = new P();

    // SoftHyphen
    R softHyphenRun = new R();
    R.SoftHyphen softHyphen = new R.SoftHyphen();
    JAXBElement<R.SoftHyphen> softHyphenElement =
        new JAXBElement<>(new QName("text"), R.SoftHyphen.class, softHyphen);
    softHyphenRun.getContent().add(softHyphenElement);

    // NoBreakHyphen
    R noBreakHyphenRun = new R();
    R.NoBreakHyphen noBreakHyphen = new R.NoBreakHyphen();
    noBreakHyphenRun.getContent().add(noBreakHyphen);
    JAXBElement<R.NoBreakHyphen> noBreakHyphenElement =
        new JAXBElement<>(new QName("text"), R.NoBreakHyphen.class, noBreakHyphen);
    softHyphenRun.getContent().add(noBreakHyphenElement);

    parentParagraph.getContent().add(softHyphenRun);
    parentParagraph.getContent().add(noBreakHyphenRun);

    var result = builder.setParagraph(parentParagraph).build(new ArrayList<>());
    assertInstanceOf(ParagraphElement.class, result);

    ParagraphElement paragraphElement = (ParagraphElement) result;
    assertEquals(2, paragraphElement.getRunElements().size());

    // expect soft hyphen and no break hyphen to be transformed
    RunElement firstRunElement = paragraphElement.getRunElements().get(0);
    assertEquals(RunTextElement.class, firstRunElement.getClass());
    assertEquals("\u00AD", firstRunElement.toHtmlString());

    RunElement secondRunElement = paragraphElement.getRunElements().get(1);
    assertEquals(RunTextElement.class, secondRunElement.getClass());
    assertEquals("\u2011", secondRunElement.toHtmlString());
  }

  private Inline generateInline(String name, String description, Dimension size) {
    Inline inline = new Inline();

    if (name != null || description != null) {
      CTNonVisualDrawingProps nvDrawingProps = new CTNonVisualDrawingProps();
      nvDrawingProps.setName(name);
      nvDrawingProps.setDescr(description);
      inline.setDocPr(nvDrawingProps);
    }

    if (size != null) {
      CTPositiveSize2D positionSize2D = new CTPositiveSize2D();
      positionSize2D.setCx(size.width * 9525L);
      positionSize2D.setCy(size.height * 9525L);
      inline.setExtent(positionSize2D);
    }

    Graphic graphic = new Graphic();
    GraphicData graphicData = new GraphicData();
    Pic pic = new Pic();
    CTBlipFillProperties blibFill = new CTBlipFillProperties();
    CTBlip blib = new CTBlip();
    blib.setEmbed("image-ref");
    blibFill.setBlip(blib);
    pic.setBlipFill(blibFill);
    graphicData.getAny().add(pic);
    graphic.setGraphicData(graphicData);
    inline.setGraphic(graphic);

    return inline;
  }

  private Anchor generateAnchor(String name, String description, Dimension size, STAlignH alignH) {
    Anchor anchor = new Anchor();

    if (name != null || description != null) {
      CTNonVisualDrawingProps nvDrawingProps = new CTNonVisualDrawingProps();
      nvDrawingProps.setName(name);
      nvDrawingProps.setDescr(description);
      anchor.setDocPr(nvDrawingProps);
    }

    if (size != null) {
      CTPositiveSize2D positionSize2D = new CTPositiveSize2D();
      positionSize2D.setCx(size.width * 9525L);
      positionSize2D.setCy(size.height * 9525L);
      anchor.setExtent(positionSize2D);
    }

    if (alignH != null) {
      CTPosH posH = new CTPosH();
      posH.setAlign(alignH);
      anchor.setPositionH(posH);
    }

    Graphic graphic = new Graphic();
    GraphicData graphicData = new GraphicData();
    Pic pic = new Pic();
    CTBlipFillProperties blibFill = new CTBlipFillProperties();
    CTBlip blib = new CTBlip();
    blib.setEmbed("image-ref");
    blibFill.setBlip(blib);
    pic.setBlipFill(blibFill);
    graphicData.getAny().add(pic);
    graphic.setGraphicData(graphicData);
    anchor.setGraphic(graphic);

    return anchor;
  }
}
