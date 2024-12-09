package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import de.bund.digitalservice.ris.caselaw.TestMemoryAppender;
import de.bund.digitalservice.ris.caselaw.domain.docx.BorderNumber;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocumentationUnitDocx;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingList;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingList.DocumentationUnitNumberingListNumberFormat;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingListEntry;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingListEntryIndex;
import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunTextElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.Style;
import java.util.ArrayList;
import java.util.List;
import org.docx4j.wml.JcEnumeration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class DocumentationUnitDocxListUtilsTest {

  @Test
  void testPostprocessBorderNumbers_withEmptyList_shouldDoNothing() {
    List<DocumentationUnitDocx> list = new ArrayList<>();

    DocumentationUnitDocxListUtils.postProcessBorderNumbers(list);

    assertThat(list).isEmpty();
  }

  @Test
  @SuppressWarnings("java:S2699")
  void testPostprocessBorderNumbers_withNull_shouldDoNothing() {

    DocumentationUnitDocxListUtils.postProcessBorderNumbers(null);
  }

  @Test
  void testPostprocessBorderNumbers_withTwoNumberlessBorderNumbers_shouldAssignNumbers() {
    List<DocumentationUnitDocx> list = new ArrayList<>();
    list.add(createNumberlessBorderNumberWithNumId(7));
    list.add(createNumberlessBorderNumberWithNumId(7));

    DocumentationUnitDocxListUtils.postProcessBorderNumbers(list);

    assertThat(list).hasSize(2);
    assertThat(list.get(0)).isInstanceOf(BorderNumber.class);
    BorderNumber borderNumber0 = (BorderNumber) list.get(0);
    assertThat(borderNumber0.getNumber()).isEqualTo("1");
    assertThat(borderNumber0.getNumId()).isEqualTo(7);
    assertThat(list.get(0)).isInstanceOf(BorderNumber.class);
    BorderNumber borderNumber1 = (BorderNumber) list.get(1);
    assertThat(borderNumber1.getNumber()).isEqualTo("2");
    assertThat(borderNumber1.getNumId()).isEqualTo(7);
  }

  @Test
  void testPostprocessBorderNumbers_withNumberlessBorderNumbersButDifferentNumIds_shouldLogError(
      CapturedOutput output) {
    TestMemoryAppender memoryAppender =
        new TestMemoryAppender(DocumentationUnitDocxListUtils.class);
    List<DocumentationUnitDocx> list = new ArrayList<>();
    list.add(createNumberlessBorderNumberWithNumId(7));
    list.add(createNumberlessBorderNumberWithNumId(8));

    DocumentationUnitDocxListUtils.postProcessBorderNumbers(list);

    assertThat(list).hasSize(2);
    assertThat(list.get(0)).isInstanceOf(BorderNumber.class);
    BorderNumber borderNumber0 = (BorderNumber) list.get(0);
    assertThat(borderNumber0.getNumber()).isEqualTo("1");
    assertThat(borderNumber0.getNumId()).isEqualTo(7);
    assertThat(list.get(0)).isInstanceOf(BorderNumber.class);
    BorderNumber borderNumber1 = (BorderNumber) list.get(1);
    assertThat(borderNumber1.getNumber()).isEqualTo("2");
    assertThat(borderNumber1.getNumId()).isEqualTo(8);

    assertThat(memoryAppender.count(Level.ERROR)).isEqualTo(1);
    assertThat(memoryAppender.getMessage(Level.ERROR, 0))
        .isEqualTo(
            "Unexpected case of a new numId. Are there more than one border number blocks in this document? Then we need to support this case. Until then every border number block after the first one will not start at 1. Insteadit is a continuous counting up across the whole document. ");
    memoryAppender.detachLoggingTestAppender();
  }

  @Test
  void testPackList_withEmptyList_shouldReturnEmptyList() {
    List<DocumentationUnitDocx> documentationUnitDocxList = new ArrayList<>();

    List<DocumentationUnitDocx> packedList =
        DocumentationUnitDocxListUtils.packList(documentationUnitDocxList);

    assertThat(packedList).isEmpty();
  }

  @Test
  void testPackList_withNull_shouldReturnEmptyList() {

    List<DocumentationUnitDocx> packedList = DocumentationUnitDocxListUtils.packList(null);

    assertThat(packedList).isEmpty();
  }

  @Test
  void testPackList_withOneParagraph_shouldNotChangeList() {
    List<DocumentationUnitDocx> documentationUnitDocxList = new ArrayList<>();
    documentationUnitDocxList.add(createParagraphWithTextElement("paragraph text"));

    List<DocumentationUnitDocx> packedList =
        DocumentationUnitDocxListUtils.packList(documentationUnitDocxList);

    assertThat(packedList).hasSize(1);
    assertThat(packedList.get(0)).isInstanceOf(ParagraphElement.class);
    assertThat(packedList.get(0).toHtmlString()).isEqualTo("<p>paragraph text</p>");
  }

  @Test
  void testPackList_withNumberingListEntries_shouldMergeThemIntoOneNumberingList() {
    List<DocumentationUnitDocx> documentationUnitDocxList = new ArrayList<>();
    documentationUnitDocxList.add(createNumberingListEntry("entry 1", false, 0));
    documentationUnitDocxList.add(createNumberingListEntry("entry 2", false, 0));

    List<DocumentationUnitDocx> packedList =
        DocumentationUnitDocxListUtils.packList(documentationUnitDocxList);

    assertThat(packedList).hasSize(1);
    assertThat(packedList.get(0)).isInstanceOf(NumberingList.class);
    NumberingList numberingList = (NumberingList) packedList.get(0);
    assertThat(numberingList.getEntries()).hasSize(2);
    assertThat(numberingList.getEntries().get(0).toHtmlString()).isEqualTo("<p>entry 1</p>");
    assertThat(numberingList.getEntries().get(1).toHtmlString()).isEqualTo("<p>entry 2</p>");
  }

  @Test
  void
      testPackList_withOneRandnummerAndTwoParagraphs_shouldOnlyAddFirstParagraphToFinalRandnummer() {
    List<DocumentationUnitDocx> documentationUnitDocxList = new ArrayList<>();
    documentationUnitDocxList.add(
        createBorderNumber(1)); // <-- final (and only) one in the document
    documentationUnitDocxList.add(createParagraphWithTextElement("paragraph1 text"));
    documentationUnitDocxList.add(createParagraphWithTextElement("paragraph2 text"));

    List<DocumentationUnitDocx> packedList =
        DocumentationUnitDocxListUtils.packList(documentationUnitDocxList);

    assertThat(packedList).hasSize(2);
    assertThat(packedList.get(0)).isInstanceOf(BorderNumber.class);
    BorderNumber borderNumberBlock = (BorderNumber) packedList.get(0);
    assertThat(borderNumberBlock.getChildrenSize()).isEqualTo(1);
    assertThat(borderNumberBlock.getChildren().get(0)).isInstanceOf(ParagraphElement.class);
    assertThat(borderNumberBlock.getChildren().get(0).toHtmlString())
        .isEqualTo("<p>paragraph1 text</p>");
    assertThat(packedList.get(1)).isInstanceOf(ParagraphElement.class);
    ParagraphElement paragraphElement = (ParagraphElement) packedList.get(1);
    assertThat(paragraphElement.toHtmlString()).isEqualTo("<p>paragraph2 text</p>");
  }

  @Test
  void testPackList_withTwoRandnummernAndParagraphs_headlineShouldResetParentRandnummer() {
    List<DocumentationUnitDocx> documentationUnitDocxList = new ArrayList<>();
    documentationUnitDocxList.add(createBorderNumber(1));
    documentationUnitDocxList.add(createParagraphWithTextElement("paragraph before headline"));
    documentationUnitDocxList.add(createCenteredParagraphWithTextElement("||."));
    documentationUnitDocxList.add(createParagraphWithTextElement("paragraph after headline"));
    // just so that the first Randnummer is not the final one in the document:
    documentationUnitDocxList.add(createBorderNumber(2));

    List<DocumentationUnitDocx> packedList =
        DocumentationUnitDocxListUtils.packList(documentationUnitDocxList);

    assertThat(packedList).hasSize(4);
    assertThat(packedList.get(0)).isInstanceOf(BorderNumber.class);
    BorderNumber borderNumberBlock = (BorderNumber) packedList.get(0);
    assertThat(borderNumberBlock.getChildrenSize()).isEqualTo(1);
    assertThat(borderNumberBlock.getChildren().get(0)).isInstanceOf(ParagraphElement.class);
    assertThat(borderNumberBlock.getChildren().get(0).toHtmlString())
        .isEqualTo("<p>paragraph before headline</p>");
    assertThat(packedList.get(1)).isInstanceOf(ParagraphElement.class);
    ParagraphElement headlineParagraphElement = (ParagraphElement) packedList.get(1);
    assertThat(headlineParagraphElement.toHtmlString())
        .isEqualTo("<p style=\"text-align: center;\">||.</p>");
    assertThat(packedList.get(2)).isInstanceOf(ParagraphElement.class);
    ParagraphElement paragraphElement = (ParagraphElement) packedList.get(2);
    assertThat(paragraphElement.toHtmlString()).isEqualTo("<p>paragraph after headline</p>");
    assertThat(packedList.get(3)).isInstanceOf(BorderNumber.class);
  }

  @Test
  void
      testPackList_withTwoRandnummernAndAParagraphAndNumberingListEntries_shouldCreateCorrectHierarchy() {
    List<DocumentationUnitDocx> documentationUnitDocxList = new ArrayList<>();
    documentationUnitDocxList.add(createBorderNumber(1));
    documentationUnitDocxList.add(createParagraphWithTextElement("paragraph text"));
    documentationUnitDocxList.add(createNumberingListEntry("entry 1", false, 0));
    documentationUnitDocxList.add(createNumberingListEntry("entry 2", false, 0));
    documentationUnitDocxList.add(createBorderNumber(2)); // <-- final one in the document

    List<DocumentationUnitDocx> packedList =
        DocumentationUnitDocxListUtils.packList(documentationUnitDocxList);

    assertThat(packedList).hasSize(2);
    assertThat(packedList.get(0)).isInstanceOf(BorderNumber.class);
    assertThat(packedList.get(1)).isInstanceOf(BorderNumber.class);
    BorderNumber borderNumberBlock1 = (BorderNumber) packedList.get(0);
    assertThat(borderNumberBlock1.getNumber()).isEqualTo("1");
    assertThat(borderNumberBlock1.getChildrenSize()).isEqualTo(2);
    assertThat(borderNumberBlock1.getChildren().get(0)).isInstanceOf(ParagraphElement.class);
    assertThat(borderNumberBlock1.getChildren().get(0).toHtmlString())
        .isEqualTo("<p>paragraph text</p>");
    assertThat(borderNumberBlock1.getChildren().get(1)).isInstanceOf(NumberingList.class);
    NumberingList numberingList = (NumberingList) borderNumberBlock1.getChildren().get(1);
    assertThat(numberingList.getEntries()).hasSize(2);
    assertThat(numberingList.getEntries().get(0).toHtmlString()).isEqualTo("<p>entry 1</p>");
    assertThat(numberingList.getEntries().get(1).toHtmlString()).isEqualTo("<p>entry 2</p>");
  }

  private ParagraphElement createParagraphWithTextElement(String text) {
    ParagraphElement paragraph = new ParagraphElement();
    RunTextElement textElement = new RunTextElement();
    textElement.setText(text);
    paragraph.setRunElements(List.of(textElement));
    return paragraph;
  }

  private ParagraphElement createCenteredParagraphWithTextElement(String text) {
    ParagraphElement paragraph = new ParagraphElement();
    paragraph.addStyle(new Style("text-align", List.of("center")));
    RunTextElement textElement = new RunTextElement();
    textElement.setText(text);
    paragraph.setRunElements(List.of(textElement));
    return paragraph;
  }

  private BorderNumber createNumberlessBorderNumberWithNumId(int numId) {
    BorderNumber borderNumber = new BorderNumber();
    borderNumber.setNumId(numId);
    return borderNumber;
  }

  private BorderNumber createBorderNumber(int number) {
    BorderNumber borderNumber = new BorderNumber();
    borderNumber.addNumberText(String.valueOf(number));
    return borderNumber;
  }

  private NumberingListEntry createNumberingListEntry(String text, boolean isOrdered, int level) {
    String lvlText = "";
    String startVal = "1";
    String restartNumberingAfterBreak = "";
    String color = "";
    String fontStyle = "";
    String fontSize = "";
    boolean lvlPicBullet = false;
    boolean isLgl = false;
    DocumentationUnitNumberingListNumberFormat numberFormat =
        isOrdered
            ? DocumentationUnitNumberingListNumberFormat.DECIMAL
            : DocumentationUnitNumberingListNumberFormat.BULLET;
    String iLvl = String.valueOf(level);
    JcEnumeration lvlJc = JcEnumeration.RIGHT;
    String suff = "space";
    return new NumberingListEntry(
        createParagraphWithTextElement(text),
        new NumberingListEntryIndex(
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
            suff));
  }
}
