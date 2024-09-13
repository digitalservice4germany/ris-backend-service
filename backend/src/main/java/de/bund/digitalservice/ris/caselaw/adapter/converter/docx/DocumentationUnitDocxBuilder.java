package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import de.bund.digitalservice.ris.caselaw.domain.docx.BorderNumber;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocumentationUnitDocx;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingListEntry;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingListEntryIndex;
import jakarta.xml.bind.JAXBElement;
import java.util.List;
import org.docx4j.model.listnumbering.AbstractListNumberingDefinition;
import org.docx4j.model.listnumbering.ListLevel;
import org.docx4j.model.listnumbering.ListNumberingDefinition;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase.NumPr;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;

public class DocumentationUnitDocxBuilder extends DocxBuilder {
  public static final String SOFT_HYPHEN = "\u00AD";
  public static final String NON_BREAKING_SPACE = "\u00A0";
  P paragraph;

  private DocumentationUnitDocxBuilder() {}

  public static DocumentationUnitDocxBuilder newInstance() {
    return new DocumentationUnitDocxBuilder();
  }

  public DocumentationUnitDocxBuilder setParagraph(P paragraph) {
    this.paragraph = paragraph;
    replaceSoftHyphenNonBreakingSpaceCombination();
    return this;
  }

  public DocumentationUnitDocx build() {
    if (isBorderNumber()) {
      return convertToBorderNumber();
    } else if (isNumberingListEntry()) {
      return convertToNumberingListEntry();
    } else if (isParagraph()) {
      return ParagraphConverter.convert(paragraph, converter);
    }

    return null;
  }

  /**
   * We have cases where a combination of soft hyphen (SHY) and non-breaking space (NBSP) should be
   * (and in word is) rendered as a regular hyphen. The docx XML looks e.g. like this:
   * <w:r><w:t> </w:t></w:r><w:r><w:rPr></w:rPr><w:t>­</w:t></w:r>
   */
  private void replaceSoftHyphenNonBreakingSpaceCombination() {
    Text previousSoftHyphenText = null;
    Text previousNonBreakingSpaceText = null;

    for (Object paragraphContent : paragraph.getContent()) {
      if (!(paragraphContent instanceof R run)) {
        continue;
      }

      for (Object runContent : run.getContent()) {
        if (!(runContent instanceof JAXBElement<?> element)
            || element.getDeclaredType() != Text.class) {
          continue;
        }
        Text currentText = (Text) element.getValue();
        String updatedTextValue = currentText.getValue();

        // white-space preserve = NBSP
        if (currentText.getSpace() != null && currentText.getSpace().equals("preserve")) {
          updatedTextValue = updatedTextValue.replace(SOFT_HYPHEN + " ", "-" + NON_BREAKING_SPACE);
          updatedTextValue = updatedTextValue.replace(" " + SOFT_HYPHEN, NON_BREAKING_SPACE + "-");
        }
        // soft hyphen node + non-breaking space node = hyphen
        if (previousSoftHyphenText != null
            && currentText.getValue().startsWith(NON_BREAKING_SPACE)) {
          previousSoftHyphenText.setValue(
              // strip last chat because we know it ends with soft hyphen
              previousSoftHyphenText
                      .getValue()
                      .substring(0, previousSoftHyphenText.getValue().length() - 1)
                  // append normal hyphen
                  + "-");
          // non-breaking space node + soft hyphen node = hyphen
        } else if (previousNonBreakingSpaceText != null
            && currentText.getValue().startsWith(SOFT_HYPHEN)) {
          updatedTextValue = currentText.getValue().replaceFirst(SOFT_HYPHEN, "-");
        } else {
          // soft hyphen node + non-breaking space in either order in same node = hyphen
          updatedTextValue =
              updatedTextValue.replace(SOFT_HYPHEN + NON_BREAKING_SPACE, "-" + NON_BREAKING_SPACE);
          updatedTextValue =
              updatedTextValue.replace(NON_BREAKING_SPACE + SOFT_HYPHEN, NON_BREAKING_SPACE + "-");
        }

        currentText.setValue(updatedTextValue);

        // Remember is the direct previous element has been a soft hyphen or non-breaking space
        previousSoftHyphenText = currentText.getValue().endsWith(SOFT_HYPHEN) ? currentText : null;
        previousNonBreakingSpaceText =
            currentText.getValue().endsWith(NON_BREAKING_SPACE) ? currentText : null;
      }
    }
  }

  private boolean isText() {
    if (!isParagraph()) {
      return false;
    }

    var hasRElement = paragraph.getContent().stream().anyMatch(R.class::isInstance);

    if (!hasRElement) {
      return paragraph.getPPr() != null;
    }

    return paragraph.getContent().stream()
        .anyMatch(
            tag -> {
              if (tag instanceof R r) {
                return r.getContent().stream()
                    .anyMatch(
                        subTag -> {
                          if (subTag instanceof JAXBElement<?> element) {
                            return element.getDeclaredType() == Text.class;
                          }

                          return false;
                        });
              }

              return false;
            });
  }

  private boolean isBorderNumber() {
    if (!isText()) {
      return false;
    }

    PPr ppr = paragraph.getPPr();
    if (ppr == null) {
      return false;
    }

    if (ppr.getPStyle() != null
        && ppr.getPStyle().getVal().equals("Listenabsatz")
        && ppr.getFramePr() != null) {
      return true;
    }

    if (ppr.getPStyle() != null
        && List.of("RandNummer", "ListParagraph", "Randziffern")
            .contains(ppr.getPStyle().getVal())) {
      return true;
    }

    // Found in some BGH documents: the border numbers have no dedicated style element in
    // document.xml. So we decided to combine 3 characteristics that hopefully match these kind
    // of border numbers in all cases
    // --> keepNext exists (=true), line in spacing to be 240 and the text to be only one integer
    return ppr.getKeepNext() != null
        && ppr.getKeepNext().isVal()
        && ppr.getSpacing() != null
        && ppr.getSpacing().getLine() != null
        && ppr.getSpacing().getLine().intValue() == 240
        && contentIsOnlyInteger();
  }

  private boolean contentIsOnlyInteger() {
    StringBuilder content = new StringBuilder();
    paragraph.getContent().stream()
        .filter(R.class::isInstance)
        .map(R.class::cast)
        .forEach(r -> content.append(RunElementConverter.parseTextFromRun(r)));
    try {
      Integer.parseInt(content.toString());
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private BorderNumber convertToBorderNumber() {
    BorderNumber borderNumber = new BorderNumber();

    paragraph.getContent().stream()
        .filter(R.class::isInstance)
        .map(R.class::cast)
        .forEach(r -> borderNumber.addNumberText(RunElementConverter.parseTextFromRun(r)));

    PPr ppr = paragraph.getPPr();
    if (ppr != null && ppr.getNumPr() != null && ppr.getNumPr().getNumId() != null) {
      borderNumber.setNumId(paragraph.getPPr().getNumPr().getNumId().getVal().intValue());
    }
    return borderNumber;
  }

  private boolean isNumberingListEntry() {
    if (!isParagraph() || paragraph.getPPr() == null) {
      return false;
    }

    return paragraph.getPPr().getNumPr() != null;
  }

  private DocumentationUnitDocx convertToNumberingListEntry() {
    NumPr numPr = paragraph.getPPr().getNumPr();
    String numId = null;
    String iLvl = null;

    ListNumberingDefinition listNumberingDefinition = null;
    if (numPr != null && numPr.getNumId() != null && numPr.getNumId().getVal() != null) {
      numId = numPr.getNumId().getVal().toString();
      listNumberingDefinition = converter.getListNumberingDefinitions().get(numId);
    }

    if (numPr != null && numPr.getIlvl() != null && numPr.getIlvl().getVal() != null) {
      iLvl = numPr.getIlvl().getVal().toString();
    }

    ListLevel listLevel = null;
    if (listNumberingDefinition != null) {
      AbstractListNumberingDefinition abstractListDefinition =
          listNumberingDefinition.getAbstractListDefinition();

      if (abstractListDefinition != null && iLvl != null) {
        listLevel = abstractListDefinition.getListLevels().get(iLvl);
      }
    }

    // Unless we find a counter example, we treat the presence of a numPr element with numId 0
    // as a list entry that brings its own numbering symbol inside the paragraph part,
    // --> we therefore convert it as a paragraph instead of a list
    if (numId == null || numId.equals("0") || listLevel == null) {
      return ParagraphConverter.convert(paragraph, converter);
    } else {
      NumberingListEntryIndex numberingListEntryIndex = setNumberingListEntryIndex(listLevel, iLvl);
      return new NumberingListEntry(
          ParagraphConverter.convert(paragraph, converter), numberingListEntryIndex);
    }
  }

  private NumberingListEntryIndex setNumberingListEntryIndex(ListLevel listLevel, String iLvl) {
    return NumberingListEntryIndexGenerator.generate(listLevel, iLvl);
  }

  private boolean isParagraph() {
    return paragraph != null;
  }
}
