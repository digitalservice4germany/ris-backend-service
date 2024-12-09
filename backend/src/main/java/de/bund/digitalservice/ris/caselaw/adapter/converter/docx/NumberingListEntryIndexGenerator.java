package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingList.DocumentationUnitNumberingListNumberFormat;
import de.bund.digitalservice.ris.caselaw.domain.docx.NumberingListEntryIndex;
import lombok.extern.slf4j.Slf4j;
import org.docx4j.model.listnumbering.ListLevel;
import org.docx4j.wml.JcEnumeration;

@Slf4j
public class NumberingListEntryIndexGenerator {

  private NumberingListEntryIndexGenerator() {}

  public static NumberingListEntryIndex generate(ListLevel listLevel, String iLvl) {
    return NumberingListEntryIndex.builder()
        .numberFormat(numberFormat(listLevel))
        .suff(suff(listLevel))
        .lvlJc(lvlJc(listLevel))
        .lvlPicBullet(hasLvlPickBulletId(listLevel))
        .startVal(startVal(listLevel))
        .lvlText(lvlText(listLevel))
        .restartNumberingAfterBreak(restartNumberingAfterBreak(listLevel))
        .isLgl(isLgl(listLevel))
        .color(fontColor(listLevel))
        .fontSize(fontSize(listLevel))
        .fontStyle(fontStyle(listLevel))
        .iLvl(iLvl)
        .build();
  }

  private static DocumentationUnitNumberingListNumberFormat numberFormat(ListLevel listLevel) {
    if (listLevel == null || listLevel.getNumFmt() == null) {
      return DocumentationUnitNumberingListNumberFormat.BULLET;
    }

    DocumentationUnitNumberingListNumberFormat numberFormat;
    switch (listLevel.getNumFmt()) {
      case BULLET -> numberFormat = DocumentationUnitNumberingListNumberFormat.BULLET;
      case DECIMAL -> numberFormat = DocumentationUnitNumberingListNumberFormat.DECIMAL;
      case UPPER_LETTER -> numberFormat = DocumentationUnitNumberingListNumberFormat.UPPER_LETTER;
      case LOWER_LETTER -> numberFormat = DocumentationUnitNumberingListNumberFormat.LOWER_LETTER;
      case UPPER_ROMAN -> numberFormat = DocumentationUnitNumberingListNumberFormat.UPPER_ROMAN;
      case LOWER_ROMAN -> numberFormat = DocumentationUnitNumberingListNumberFormat.LOWER_ROMAN;
      default -> {
        log.error(
            "not implemented number format ({}) in list. use default bullet list",
            listLevel.getNumFmt());
        numberFormat = DocumentationUnitNumberingListNumberFormat.BULLET;
      }
    }

    return numberFormat;
  }

  private static String suff(ListLevel listLevel) {
    if (listLevel == null
        || listLevel.getJaxbAbstractLvl() == null
        || listLevel.getJaxbAbstractLvl().getSuff() == null) {

      return "tab";
    }

    return listLevel.getJaxbAbstractLvl().getSuff().getVal();
  }

  private static JcEnumeration lvlJc(ListLevel listLevel) {
    if (listLevel == null
        || listLevel.getJaxbAbstractLvl() == null
        || listLevel.getJaxbAbstractLvl().getLvlJc() == null) {

      return JcEnumeration.RIGHT;
    }

    return listLevel.getJaxbAbstractLvl().getLvlJc().getVal();
  }

  private static boolean hasLvlPickBulletId(ListLevel listLevel) {
    return listLevel != null
        && listLevel.IsBullet()
        && listLevel.getJaxbAbstractLvl() != null
        && listLevel.getJaxbAbstractLvl().getLvlPicBulletId() != null;
  }

  private static String startVal(ListLevel listLevel) {
    if (listLevel == null
        || listLevel.getJaxbAbstractLvl() == null
        || listLevel.getJaxbAbstractLvl().getStart() == null
        || listLevel.getJaxbAbstractLvl().getStart().getVal() != null) {

      return "1";
    }

    return listLevel.getJaxbAbstractLvl().getStart().getVal().toString();
  }

  private static String lvlText(ListLevel listLevel) {
    if (listLevel == null
        || listLevel.getLevelText() == null
        || listLevel.getLevelText().isBlank()) {
      return "";
    }

    return listLevel.getLevelText();
  }

  private static String restartNumberingAfterBreak(ListLevel listLevel) {
    if (listLevel == null
        || listLevel.getJaxbAbstractLvl() == null
        || listLevel.getJaxbAbstractLvl().getLvlRestart() == null
        || listLevel.getJaxbAbstractLvl().getLvlRestart().getVal() != null) {

      return "";
    }

    return listLevel.getJaxbAbstractLvl().getLvlRestart().getVal().toString();
  }

  private static boolean isLgl(ListLevel listLevel) {
    if (listLevel == null
        || listLevel.getJaxbAbstractLvl() == null
        || listLevel.getJaxbAbstractLvl().getIsLgl() == null) {

      return false;
    }

    return listLevel.getJaxbAbstractLvl().getIsLgl().isVal();
  }

  private static String fontColor(ListLevel listLevel) {
    if (listLevel == null
        || listLevel.getJaxbAbstractLvl() == null
        || listLevel.getJaxbAbstractLvl().getRPr() == null
        || listLevel.getJaxbAbstractLvl().getRPr().getColor() == null
        || listLevel.getJaxbAbstractLvl().getRPr().getColor().getVal() == null) {

      return "";
    }

    return listLevel.getJaxbAbstractLvl().getRPr().getColor().getVal();
  }

  private static String fontSize(ListLevel listLevel) {
    if (listLevel == null
        || listLevel.getJaxbAbstractLvl() == null
        || listLevel.getJaxbAbstractLvl().getRPr() == null
        || listLevel.getJaxbAbstractLvl().getRPr().getSz() == null
        || listLevel.getJaxbAbstractLvl().getRPr().getSz().getVal() == null) {

      return "";
    }

    return listLevel.getJaxbAbstractLvl().getRPr().getSz().getVal().toString();
  }

  private static String fontStyle(ListLevel listLevel) {
    if (listLevel == null
        || listLevel.getJaxbAbstractLvl() == null
        || listLevel.getJaxbAbstractLvl().getRPr() == null
        || listLevel.getJaxbAbstractLvl().getRPr().getRFonts() == null
        || listLevel.getJaxbAbstractLvl().getRPr().getRFonts().getAscii() == null) {

      return "";
    }

    return listLevel.getJaxbAbstractLvl().getRPr().getRFonts().getAscii();
  }
}
