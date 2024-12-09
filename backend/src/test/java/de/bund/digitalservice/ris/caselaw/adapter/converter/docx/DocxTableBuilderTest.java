package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.bund.digitalservice.ris.caselaw.domain.docx.DocumentationUnitDocx;
import de.bund.digitalservice.ris.caselaw.domain.docx.TableElement;
import jakarta.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.CTCnf;
import org.docx4j.wml.CTShd;
import org.docx4j.wml.CTTblPrBase;
import org.docx4j.wml.CTTblPrBase.TblStyle;
import org.docx4j.wml.CTTblStylePr;
import org.docx4j.wml.CTVerticalAlignRun;
import org.docx4j.wml.Color;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.RPr;
import org.docx4j.wml.STBorder;
import org.docx4j.wml.STShd;
import org.docx4j.wml.STTblStyleOverrideType;
import org.docx4j.wml.STVerticalAlignRun;
import org.docx4j.wml.Style;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblBorders;
import org.docx4j.wml.TblPr;
import org.docx4j.wml.Tc;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.TcPrInner;
import org.docx4j.wml.TcPrInner.TcBorders;
import org.docx4j.wml.Tr;
import org.docx4j.wml.U;
import org.docx4j.wml.UnderlineEnumeration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DocxTableBuilderTest {
  @Test
  void testBuild_withTable() {
    // check every possible field because correct converting is not ready yet
    DocxTableBuilder builder = DocxTableBuilder.newInstance();
    Tbl table =
        generateTable(
            List.of(
                List.of("cell r1c1", "cell r1c2", "cell r1c3"),
                List.of("cell r2c1", "cell r2c2", "cell r2c3"),
                List.of("cell r3c1", "cell r3c2", "cell r3c3")));
    table.setTblPr(new TblPr());
    var result = builder.setTable(table).build(new ArrayList<>());

    assertTrue(result instanceof TableElement);
    TableElement tableElement = (TableElement) result;
    assertEquals(3, tableElement.rows.size());
    var cells = tableElement.rows.get(0).cells();
    assertEquals(3, cells.size());
    assertEquals(1, cells.get(0).paragraphElements.size());
    assertEquals("<p>cell r1c1</p>", cells.get(0).paragraphElements.get(0).toHtmlString());
    assertEquals(1, cells.get(1).paragraphElements.size());
    assertEquals("<p>cell r1c2</p>", cells.get(1).paragraphElements.get(0).toHtmlString());
    assertEquals(1, cells.get(2).paragraphElements.size());
    assertEquals("<p>cell r1c3</p>", cells.get(2).paragraphElements.get(0).toHtmlString());
    cells = tableElement.rows.get(1).cells();
    assertEquals(3, cells.size());
    assertEquals(1, cells.get(0).paragraphElements.size());
    assertEquals("<p>cell r2c1</p>", cells.get(0).paragraphElements.get(0).toHtmlString());
    assertEquals(1, cells.get(1).paragraphElements.size());
    assertEquals("<p>cell r2c2</p>", cells.get(1).paragraphElements.get(0).toHtmlString());
    assertEquals(1, cells.get(2).paragraphElements.size());
    assertEquals("<p>cell r2c3</p>", cells.get(2).paragraphElements.get(0).toHtmlString());
    cells = tableElement.rows.get(2).cells();
    assertEquals(3, cells.size());
    assertEquals(1, cells.get(0).paragraphElements.size());
    assertEquals("<p>cell r3c1</p>", cells.get(0).paragraphElements.get(0).toHtmlString());
    assertEquals(1, cells.get(1).paragraphElements.size());
    assertEquals("<p>cell r3c2</p>", cells.get(1).paragraphElements.get(0).toHtmlString());
    assertEquals(1, cells.get(2).paragraphElements.size());
    assertEquals("<p>cell r3c3</p>", cells.get(2).paragraphElements.get(0).toHtmlString());
  }

  @Test
  void testBuild_withEmptyTable() {
    DocxTableBuilder builder = DocxTableBuilder.newInstance();
    Tbl table = new Tbl();
    table.getContent().add(new Object());

    var result = builder.setTable(table).build(new ArrayList<>());

    assertTrue(result instanceof TableElement);
    assertTrue(((TableElement) result).rows.isEmpty());
  }

  @Test
  void testBuild_withTable_withBorder() {
    var border1 = new CTBorder();
    border1.setVal(STBorder.SINGLE);
    border1.setColor("a64d79");
    border1.setSz(BigInteger.valueOf(24));

    var border2 = new CTBorder();
    border2.setVal(STBorder.SINGLE);
    border2.setColor("abc");
    border2.setSz(BigInteger.valueOf(48));

    TblBorders borders = new TblBorders();
    borders.setTop(border1);
    borders.setRight(border1);
    borders.setBottom(border2);
    borders.setLeft(border2);
    borders.setInsideH(border1);
    borders.setInsideV(border2);
    TblPr tblPr = new TblPr();
    tblPr.setTblBorders(borders);
    Tbl table = new Tbl();
    table.setTblPr(tblPr);

    DocxTableBuilder builder = DocxTableBuilder.newInstance();
    var result = builder.setTable(table).build(new ArrayList<>());
    assertTrue(result.toHtmlString().contains("border-top: 3px solid #a64d79;"));
    assertTrue(result.toHtmlString().contains("border-right: 3px solid #a64d79;"));
    assertTrue(result.toHtmlString().contains("border-bottom: 6px solid #abc;"));
    assertTrue(result.toHtmlString().contains("border-left: 6px solid #abc;"));
  }

  @Test
  void testBuild_withTable_withCell() {
    var topRightCell = generateTableCellWidthBorder("MNOPQR", 12);
    var rightCtBorder = new CTBorder();
    rightCtBorder.setVal(STBorder.SINGLE);
    rightCtBorder.setSz(BigInteger.valueOf(24));
    rightCtBorder.setColor("foo");
    topRightCell.getValue().getTcPr().getTcBorders().setRight(rightCtBorder);

    var gridSpan = new TcPrInner.GridSpan();
    gridSpan.setVal(BigInteger.valueOf(2));
    topRightCell.getValue().getTcPr().setGridSpan(gridSpan);

    var shading = new CTShd();
    shading.setVal(STShd.CLEAR);
    shading.setFill("111222");
    topRightCell.getValue().getTcPr().setShd(shading);

    var row = new Tr();
    row.getContent().add(generateTableCellWidthBorder("ABCDEF", 12));
    row.getContent().add(generateTableCellWidthBorder("GHIJKL", 12));
    row.getContent().add(topRightCell);
    row.getContent().add(generateTableCellWidthBorder("MNOPQR", 12));

    var tableCtBorder = new CTBorder();
    tableCtBorder.setVal(STBorder.SINGLE);
    tableCtBorder.setSz(BigInteger.valueOf(48));
    tableCtBorder.setColor("auto");

    var tableBorders = new TblBorders();
    tableBorders.setInsideV(tableCtBorder);
    tableBorders.setInsideH(tableCtBorder);

    var tblPr = new TblPr();
    tblPr.setTblBorders(tableBorders);
    var tbl = new Tbl();
    tbl.setTblPr(tblPr);
    tbl.getContent().add(row);

    var builder = DocxTableBuilder.newInstance();
    builder.setTable(tbl);

    var result = builder.build(new ArrayList<>()).toHtmlString();

    // cell should have colspan
    assertTrue(result.contains("colspan=\"2\""));

    // cell should take insideV from table
    assertThat(result)
        .contains(
            "<td style=\"border-left: 6px solid #000; border-right: 6px solid #000; border-top: 1.5px solid #ghijkl; min-width: 5px; padding: 5px;\">")
        .contains(
            "<td colspan=\"2\" style=\"background-color: #111222; border-left: 6px solid #000; border-right: 3px solid #foo; border-top: 1.5px solid #mnopqr; min-width: 5px; padding: 5px;\"><p>foo</p></td>");
  }

  @Test
  void testBuild_withExternalTableStyle() {
    Tbl table = generateTable(List.of(List.of("table cell")));
    TblPr tblPr = new TblPr();
    TblStyle tblStyle = new TblStyle();
    tblStyle.setVal("external-style");
    tblPr.setTblStyle(tblStyle);
    table.setTblPr(tblPr);
    Map<String, Style> styles = new HashMap<>();
    Style style = new Style();
    CTTblPrBase externalTblPr = new CTTblPrBase();
    TblBorders externalTblBorder = new TblBorders();
    CTBorder externalLeftBorder = new CTBorder();
    externalLeftBorder.setSz(new BigInteger("24"));
    externalLeftBorder.setVal(STBorder.SINGLE);
    externalTblBorder.setLeft(externalLeftBorder);
    externalTblPr.setTblBorders(externalTblBorder);
    style.setTblPr(externalTblPr);
    styles.put("external-style", style);

    var converter = new DocxConverter();
    converter.setStyles(styles);
    var result =
        DocxTableBuilder.newInstance()
            .setTable(table)
            .setConverter(converter)
            .build(new ArrayList<>());

    assertThat(result).isInstanceOf(TableElement.class);
    TableElement tableElement = (TableElement) result;
    assertThat(tableElement.getStyleString())
        .isEqualTo(" style=\"border-collapse: collapse; border-left: 3px solid #000;\"");
  }

  @Test
  void testBuild_withExternalAndInternalTableStyle() {
    Tbl table = generateTable(List.of(List.of("table cell")));
    TblPr tblPr = new TblPr();
    TblStyle tblStyle = new TblStyle();
    tblStyle.setVal("external-style");
    tblPr.setTblStyle(tblStyle);
    table.setTblPr(tblPr);
    Map<String, Style> styles = new HashMap<>();
    Style style = new Style();
    CTTblPrBase externalTblPr = new CTTblPrBase();
    TblBorders externalTblBorder = new TblBorders();
    CTBorder externalLeftBorder = new CTBorder();
    externalLeftBorder.setSz(new BigInteger("24"));
    externalLeftBorder.setVal(STBorder.SINGLE);
    externalTblBorder.setLeft(externalLeftBorder);
    externalTblPr.setTblBorders(externalTblBorder);
    style.setTblPr(externalTblPr);
    styles.put("external-style", style);
    TblBorders internalTblBorder = new TblBorders();
    CTBorder internalLeftBorder = new CTBorder();
    internalLeftBorder.setSz(new BigInteger("48"));
    internalLeftBorder.setVal(STBorder.SINGLE);
    internalLeftBorder.setColor("F00");
    internalTblBorder.setLeft(internalLeftBorder);
    tblPr.setTblBorders(internalTblBorder);

    var converter = new DocxConverter();
    converter.setStyles(styles);
    var result =
        DocxTableBuilder.newInstance()
            .setTable(table)
            .setConverter(converter)
            .build(new ArrayList<>());

    assertThat(result).isInstanceOf(TableElement.class);
    TableElement tableElement = (TableElement) result;
    assertThat(tableElement.getStyleString())
        .isEqualTo(" style=\"border-collapse: collapse; border-left: 6px solid #f00;\"");
  }

  @Test
  void testBuild_withExternalTableStyleContainsRunElementStyle() {
    Tbl table = generateTable(List.of(List.of("table cell")));
    TblPr tblPr = new TblPr();
    TblStyle tblStyle = new TblStyle();
    tblStyle.setVal("external-style");
    tblPr.setTblStyle(tblStyle);
    table.setTblPr(tblPr);

    Map<String, Style> styles = new HashMap<>();
    Style style = new Style();
    RPr styleRPr = new RPr();
    styleRPr.setB(new BooleanDefaultTrue());
    styleRPr.setI(new BooleanDefaultTrue());
    styleRPr.setStrike(new BooleanDefaultTrue());
    var verticalAlign = new CTVerticalAlignRun();
    verticalAlign.setVal(STVerticalAlignRun.SUBSCRIPT);
    styleRPr.setVertAlign(verticalAlign);
    HpsMeasure size = new HpsMeasure();
    size.setVal(new BigInteger("24"));
    styleRPr.setSz(size);
    U u = new U();
    u.setVal(UnderlineEnumeration.SINGLE);
    styleRPr.setU(u);
    Color color = new Color();
    color.setVal("FF0000");
    styleRPr.setColor(color);
    style.setRPr(styleRPr);
    styles.put("external-style", style);

    var converter = new DocxConverter();
    converter.setStyles(styles);
    var result =
        DocxTableBuilder.newInstance()
            .setTable(table)
            .setConverter(converter)
            .build(new ArrayList<>());

    assertThat(result).isInstanceOf(TableElement.class);
    TableElement tableElement = (TableElement) result;
    assertThat(tableElement.toHtmlString())
        .contains(
            "<p><span style=\"color: #ff0000; font-size: 12pt; font-style: italic; "
                + "font-weight: bold; text-decoration: line-through underline; "
                + "vertical-align: sub;\">table cell</span></p>");
  }

  @Test
  void testBuild_withExternalTableStyleContainsTableStylePropertiesAndNotPropertySet() {
    Tbl table = generateTable(List.of(List.of("table cell")));
    TblPr tblPr = new TblPr();
    TblStyle tblStyle = new TblStyle();
    tblStyle.setVal("external-style");
    tblPr.setTblStyle(tblStyle);
    table.setTblPr(tblPr);

    Map<String, Style> styles = new HashMap<>();
    Style style = new Style();
    putTableStylePrToStyle(style);
    styles.put("external-style", style);

    var converter = new DocxConverter();
    converter.setStyles(styles);
    var result =
        DocxTableBuilder.newInstance()
            .setTable(table)
            .setConverter(converter)
            .build(new ArrayList<>());

    assertThat(result).isInstanceOf(TableElement.class);
    TableElement tableElement = (TableElement) result;
    assertThat(tableElement.toHtmlString())
        .isEqualTo(
            "<table style=\"border-collapse: collapse;\">"
                + "<tr>"
                + "<td style=\"border-left: 0.25px solid #000; min-width: 5px; padding: 5px;\"><p>table cell</p></td>"
                + "</tr>"
                + "</table>");
  }

  @SuppressWarnings("java:S5976") // Disable warning for tests that could be parametrized
  @Nested
  class TestBuildWithExternalTableStyleContainsTableStyleProperties {
    private String htmlFromTableElement(float borderLeft) {
      return String.format(
          "<table style=\"border-collapse: collapse;\">"
              + "<tr>"
              + "<td style=\"border-left: %spx solid #000; min-width: 5px; padding: 5px;\"><p>table cell</p></td>"
              + "</tr>"
              + "</table>",
          borderLeft);
    }

    private DocumentationUnitDocx createDocumentationUnit(Integer ctCnfInt) {
      Tbl table = generateTable(List.of(List.of("table cell")));
      TblPr tblPr = new TblPr();
      TblStyle tblStyle = new TblStyle();
      tblStyle.setVal("external-style");
      tblPr.setTblStyle(tblStyle);
      table.setTblPr(tblPr);

      Map<String, Style> styles = new HashMap<>();
      Style style = new Style();
      putTableStylePrToStyle(style);
      Tr tr = (Tr) table.getContent().get(0);
      JAXBElement<Tc> tcJAXBElement = (JAXBElement<Tc>) tr.getContent().get(0);
      TcPr tcPr = new TcPr();
      CTCnf ctCnf = new CTCnf();
      ctCnf.setVal(Integer.toBinaryString(ctCnfInt));
      tcPr.setCnfStyle(ctCnf);
      tcJAXBElement.getValue().setTcPr(tcPr);
      styles.put("external-style", style);

      var converter = new DocxConverter();
      converter.setStyles(styles);
      return DocxTableBuilder.newInstance()
          .setTable(table)
          .setConverter(converter)
          .build(new ArrayList<>());
    }

    @Test
    void testAllPropertySet() {
      var result = createDocumentationUnit(4097);

      assertThat(result).isInstanceOf(TableElement.class);
      TableElement tableElement = (TableElement) result;
      assertThat(tableElement.toHtmlString()).isEqualTo(htmlFromTableElement(3.25f));
    }

    @Test
    void testOnePropertySetWhichDoesntMatchCell() {
      var result = createDocumentationUnit(32);

      assertThat(result).isInstanceOf(TableElement.class);
      TableElement tableElement = (TableElement) result;
      assertThat(tableElement.toHtmlString()).isEqualTo(htmlFromTableElement(0.25f));
    }

    @Test
    void testOnePropertySetWhichMatchesCell() {
      var result = createDocumentationUnit(512);

      assertThat(result).isInstanceOf(TableElement.class);
      TableElement tableElement = (TableElement) result;
      assertThat(tableElement.toHtmlString()).isEqualTo(htmlFromTableElement(1.5f));
    }
  }

  private void putTableStylePrToStyle(Style style) {
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.WHOLE_TABLE, "2", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.BAND_1_VERT, "4", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.BAND_2_VERT, "6", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.BAND_1_HORZ, "8", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.BAND_2_HORZ, "10", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.FIRST_COL, "12", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.LAST_COL, "14", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.FIRST_ROW, "16", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.LAST_ROW, "18", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.NE_CELL, "20", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.NW_CELL, "22", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.SE_CELL, "24", null, null, null));
    style
        .getTblStylePr()
        .add(generateTableStylePr(STTblStyleOverrideType.SW_CELL, "26", null, null, null));
  }

  private CTTblStylePr generateTableStylePr(
      STTblStyleOverrideType type,
      String leftSize,
      String topSize,
      String rightSize,
      String bottomSize) {
    CTTblStylePr tableStyleProperty = new CTTblStylePr();
    tableStyleProperty.setType(type);
    tableStyleProperty.setTcPr(generateTcPrWithBorder(leftSize, topSize, rightSize, bottomSize));
    return tableStyleProperty;
  }

  private TcPr generateTcPrWithBorder(
      String leftSize, String topSize, String rightSize, String bottomSize) {
    TcPr styleTcPr = new TcPr();
    TcBorders styleTcBorder = new TcBorders();
    if (leftSize != null) {
      CTBorder styleBorder = new CTBorder();
      styleBorder.setSz(new BigInteger(leftSize));
      styleTcBorder.setLeft(styleBorder);
    }
    if (topSize != null) {
      CTBorder styleBorder = new CTBorder();
      styleBorder.setSz(new BigInteger(topSize));
      styleTcBorder.setTop(styleBorder);
    }
    if (rightSize != null) {
      CTBorder styleBorder = new CTBorder();
      styleBorder.setSz(new BigInteger(rightSize));
      styleTcBorder.setRight(styleBorder);
    }
    if (bottomSize != null) {
      CTBorder styleBorder = new CTBorder();
      styleBorder.setSz(new BigInteger(bottomSize));
      styleTcBorder.setBottom(styleBorder);
    }
    styleTcPr.setTcBorders(styleTcBorder);
    return styleTcPr;
  }

  private Tbl generateTable(List<List<String>> cells) {
    Tbl table = new Tbl();

    for (List<String> rows : cells) {
      Tr row = new Tr();
      for (String cellText : rows) {
        Tc cell = new Tc();
        cell.getContent()
            .add(
                TestDocxBuilder.newParagraphBuilder()
                    .addRunElement(TestDocxBuilder.buildTextRunElement(cellText))
                    .build());
        cell.setTcPr(new TcPr());
        JAXBElement<Tc> tcElement = new JAXBElement<>(new QName("tc"), Tc.class, cell);
        row.getContent().add(tcElement);
      }
      table.getContent().add(row);
    }

    return table;
  }

  private JAXBElement<Tc> generateTableCellWidthBorder(String color, Integer width) {
    var ctBorder = new CTBorder();
    ctBorder.setVal(STBorder.SINGLE);
    ctBorder.setSz(BigInteger.valueOf(width));
    ctBorder.setColor(color);

    var tcBorders = new TcPrInner.TcBorders();
    tcBorders.setTop(ctBorder);

    var tcPr = new TcPr();
    tcPr.setTcBorders(tcBorders);

    var tc = new Tc();
    tc.setTcPr(tcPr);
    tc.getContent()
        .add(
            TestDocxBuilder.newParagraphBuilder()
                .addRunElement(TestDocxBuilder.buildTextRunElement("foo"))
                .build());
    return new JAXBElement<>(new QName("tc"), Tc.class, tc);
  }
}
