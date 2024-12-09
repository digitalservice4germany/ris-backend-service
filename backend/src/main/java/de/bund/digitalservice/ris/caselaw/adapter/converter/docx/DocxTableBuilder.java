package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import de.bund.digitalservice.ris.caselaw.domain.docx.BlockElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.Border;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocumentationUnitDocx;
import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.RunTextElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.TableCellElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.TableElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.TableRowElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.UnhandledElement;
import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.docx4j.sharedtypes.STOnOff;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.CTCnf;
import org.docx4j.wml.CTShd;
import org.docx4j.wml.CTTblLook;
import org.docx4j.wml.CTTblPrBase;
import org.docx4j.wml.CTTblStylePr;
import org.docx4j.wml.P;
import org.docx4j.wml.STTblStyleOverrideType;
import org.docx4j.wml.Style;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblBorders;
import org.docx4j.wml.Tc;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.Tr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocxTableBuilder extends DocxBuilder {
  private static final Logger LOGGER = LoggerFactory.getLogger(DocxTableBuilder.class);
  private Tbl table;

  private DocxTableBuilder() {}

  public static DocxTableBuilder newInstance() {
    return new DocxTableBuilder();
  }

  public DocxTableBuilder setTable(Tbl table) {
    this.table = table;

    return this;
  }

  public DocumentationUnitDocx build(List<UnhandledElement> unhandledElements) {
    var tableElement = new TableElement(parseTable(table, unhandledElements));
    addTableStyleProperties(tableElement);
    addTableProperties(tableElement);

    return tableElement;
  }

  private void addTableStyleProperties(TableElement tableElement) {
    if (table == null
        || table.getTblPr() == null
        || table.getTblPr().getTblStyle() == null
        || table.getTblPr().getTblStyle().getVal() == null) {

      return;
    }

    Style style = converter.getStyles().get(table.getTblPr().getTblStyle().getVal());
    if (style == null) {
      return;
    }

    if (style.getRPr() != null) {
      addRunElementStyles(tableElement, style);
    }

    AtomicInteger tblLookValue = getTableLookValue(table.getTblPr().getTblLook());

    Map<STTblStyleOverrideType, Integer> orderMap = generateTableStyleOrderMap();
    style.getTblStylePr().stream()
        .sorted(
            (o1, o2) -> {
              int value1 = Integer.MIN_VALUE;
              if (o1 != null && o1.getType() != null && orderMap.containsKey(o1.getType())) {
                value1 = orderMap.get(o1.getType());
              }
              int value2 = Integer.MAX_VALUE;
              if (o2 != null && o2.getType() != null && orderMap.containsKey(o2.getType())) {
                value2 = orderMap.get(o2.getType());
              }

              return value1 - value2;
            })
        .forEach(
            tblStylePr -> addTableStyleProperties(tableElement, tblStylePr, tblLookValue.get()));
  }

  private AtomicInteger getTableLookValue(CTTblLook tblLook) {
    if (table.getTblPr().getTblLook() == null) {
      return new AtomicInteger(0);
    }

    int value = 0;
    if (tblLook.getFirstColumn() != null && tblLook.getFirstColumn().equals(STOnOff.ONE)) {
      value += 512;
    }
    if (tblLook.getLastColumn() != null && tblLook.getLastColumn().equals(STOnOff.ONE)) {
      value += 256;
    }
    if (tblLook.getFirstRow() != null && tblLook.getFirstRow().equals(STOnOff.ONE)) {
      value += 2048;
    }
    if (tblLook.getLastRow() != null && tblLook.getLastRow().equals(STOnOff.ONE)) {
      value += 1024;
    }

    return new AtomicInteger(value);
  }

  private void addRunElementStyles(TableElement tableElement, Style style) {
    if (tableElement.rows == null) {
      return;
    }

    tableElement.rows.forEach(rowElement -> addRunElementStyles(rowElement, style));
  }

  private void addRunElementStyles(TableRowElement rowElement, Style style) {
    if (rowElement.cells() == null) {
      return;
    }

    rowElement
        .cells()
        .forEach(
            cellElement -> {
              if (cellElement.paragraphElements != null) {
                cellElement.paragraphElements.forEach(
                    element -> {
                      if (element instanceof ParagraphElement paragraphElement) {
                        paragraphElement.getRunElements().stream()
                            .filter(RunTextElement.class::isInstance)
                            .forEach(
                                runElement ->
                                    RunElementStyleAdapter.addStyles(
                                        (RunTextElement) runElement, style.getRPr()));
                      }
                    });
              }
            });
  }

  private Map<STTblStyleOverrideType, Integer> generateTableStyleOrderMap() {
    EnumMap<STTblStyleOverrideType, Integer> orderMap = new EnumMap<>(STTblStyleOverrideType.class);
    orderMap.put(STTblStyleOverrideType.WHOLE_TABLE, 1);
    orderMap.put(STTblStyleOverrideType.BAND_1_VERT, 2);
    orderMap.put(STTblStyleOverrideType.BAND_2_VERT, 3);
    orderMap.put(STTblStyleOverrideType.BAND_1_HORZ, 4);
    orderMap.put(STTblStyleOverrideType.BAND_2_HORZ, 5);
    orderMap.put(STTblStyleOverrideType.FIRST_ROW, 6);
    orderMap.put(STTblStyleOverrideType.LAST_ROW, 7);
    orderMap.put(STTblStyleOverrideType.FIRST_COL, 8);
    orderMap.put(STTblStyleOverrideType.LAST_COL, 9);
    orderMap.put(STTblStyleOverrideType.NE_CELL, 10);
    orderMap.put(STTblStyleOverrideType.NW_CELL, 11);
    orderMap.put(STTblStyleOverrideType.SE_CELL, 12);
    orderMap.put(STTblStyleOverrideType.SW_CELL, 13);
    return orderMap;
  }

  private void addTableStyleProperties(
      TableElement tableElement, CTTblStylePr tblStylePr, Integer tblLookValue) {
    if (tableElement.rows == null || tableElement.rows.isEmpty()) {
      return;
    }

    switch (tblStylePr.getType()) {
      case WHOLE_TABLE -> addTableStyleForWholeTable(tableElement, tblStylePr, tblLookValue);
      case BAND_1_VERT -> addTableStyleForOddColumns(tableElement, tblStylePr, tblLookValue);
      case BAND_2_VERT -> addTableStyleForEvenColumns(tableElement, tblStylePr, tblLookValue);
      case BAND_1_HORZ -> addTableStyleForOddRows(tableElement, tblStylePr, tblLookValue);
      case BAND_2_HORZ -> addTableStyleForEvenRows(tableElement, tblStylePr, tblLookValue);
      case FIRST_ROW -> addStyleForFirstRow(tableElement, tblStylePr, tblLookValue);
      case LAST_ROW -> addStyleForLastRow(tableElement, tblStylePr, tblLookValue);
      case FIRST_COL -> addStyleForFirstCol(tableElement, tblStylePr, tblLookValue);
      case LAST_COL -> addStyleForLastCol(tableElement, tblStylePr, tblLookValue);
      case NE_CELL -> {
        TableRowElement row = tableElement.rows.get(0);
        if (row.cells() != null && !row.cells().isEmpty()) {
          addStyleForCell(row.cells().get(row.cells().size() - 1), tblStylePr, tblLookValue);
        }
      }
      case NW_CELL -> {
        TableRowElement row = tableElement.rows.get(0);
        if (row.cells() != null && !row.cells().isEmpty()) {
          addStyleForCell(row.cells().get(0), tblStylePr, tblLookValue);
        }
      }
      case SE_CELL -> {
        TableRowElement row = tableElement.rows.get(tableElement.rows.size() - 1);
        if (row.cells() != null && !row.cells().isEmpty()) {
          addStyleForCell(row.cells().get(row.cells().size() - 1), tblStylePr, tblLookValue);
        }
      }
      case SW_CELL -> {
        TableRowElement row = tableElement.rows.get(tableElement.rows.size() - 1);
        if (row.cells() != null && !row.cells().isEmpty()) {
          addStyleForCell(row.cells().get(0), tblStylePr, tblLookValue);
        }
      }
      default -> LOGGER.error("unsupported table style property: {}", tblStylePr.getType());
    }
  }

  private void addTableStyleForWholeTable(
      TableElement tableElement, CTTblStylePr tblStylePr, Integer tblLookValue) {
    tableElement.rows.forEach(rowElement -> addStyleForRow(rowElement, tblStylePr, tblLookValue));
  }

  private void addTableStyleForOddRows(
      TableElement tableElement, CTTblStylePr tblStylePr, Integer tblLookValue) {
    for (int i = 0; i < tableElement.rows.size(); i++) {
      if (i % 2 == 0) {
        continue;
      }

      addStyleForRow(tableElement.rows.get(i), tblStylePr, tblLookValue);
    }
  }

  private void addTableStyleForOddColumns(
      TableElement tableElement, CTTblStylePr tblStylePr, Integer tblLookValue) {
    tableElement.rows.forEach(
        row -> {
          if (row.cells() != null) {
            for (int i = 0; i < row.cells().size(); i++) {
              if (i % 2 == 0) {
                continue;
              }

              addStyleForCell(row.cells().get(i), tblStylePr, tblLookValue);
            }
          }
        });
  }

  private void addTableStyleForEvenRows(
      TableElement tableElement, CTTblStylePr tblStylePr, Integer tblLookValue) {
    for (int i = 0; i < tableElement.rows.size(); i++) {
      if (i % 2 == 1) {
        continue;
      }

      addStyleForRow(tableElement.rows.get(i), tblStylePr, tblLookValue);
    }
  }

  private void addTableStyleForEvenColumns(
      TableElement tableElement, CTTblStylePr tblStylePr, Integer tblLookValue) {
    tableElement.rows.forEach(
        row -> {
          if (row.cells() != null) {
            for (int i = 0; i < row.cells().size(); i++) {
              if (i % 2 == 1) {
                continue;
              }

              addStyleForCell(row.cells().get(i), tblStylePr, tblLookValue);
            }
          }
        });
  }

  private void addStyleForFirstCol(
      TableElement tableElement, CTTblStylePr tblStylePr, Integer tblLookValue) {
    tableElement.rows.forEach(
        row -> {
          if (row.cells() == null || row.cells().isEmpty()) {
            return;
          }

          addStyleForCell(row.cells().get(0), tblStylePr, tblLookValue);
        });
  }

  private void addStyleForLastCol(
      TableElement tableElement, CTTblStylePr tblStylePr, Integer tblLookValue) {
    tableElement.rows.forEach(
        row -> {
          if (row.cells() == null || row.cells().isEmpty()) {
            return;
          }

          addStyleForCell(row.cells().get(row.cells().size() - 1), tblStylePr, tblLookValue);
        });
  }

  private void addStyleForLastRow(
      TableElement tableElement, CTTblStylePr tblStylePr, Integer tblLookValue) {
    TableRowElement lastRow = tableElement.rows.get(tableElement.rows.size() - 1);
    addStyleForRow(lastRow, tblStylePr, tblLookValue);
  }

  private void addStyleForFirstRow(
      TableElement tableElement, CTTblStylePr tblStylePr, Integer tblLookValue) {
    TableRowElement firstRow = tableElement.rows.get(0);
    addStyleForRow(firstRow, tblStylePr, tblLookValue);
  }

  private void addStyleForRow(TableRowElement row, CTTblStylePr tblStylePr, Integer tblLookValue) {
    row.cells().forEach(tableCell -> addStyleForCell(tableCell, tblStylePr, tblLookValue));
  }

  private void addStyleForCell(
      TableCellElement tableCell, CTTblStylePr tblStylePr, Integer tblLookValue) {
    if (!useStyleForCell(tableCell, tblStylePr, tblLookValue)) {
      return;
    }

    addExternalTcStyle(tableCell, tblStylePr.getTcPr());
    tableCell.paragraphElements.forEach(
        element -> {
          if (element instanceof ParagraphElement paragraphElement) {
            paragraphElement.getRunElements().stream()
                .filter(RunTextElement.class::isInstance)
                .forEach(
                    runElement ->
                        RunElementStyleAdapter.addStyles(
                            (RunTextElement) runElement, tblStylePr.getRPr()));
          }
        });
  }

  private boolean useStyleForCell(
      TableCellElement tableCell, CTTblStylePr tblStylePr, Integer tblLookValue) {
    if (tableCell.getUsedStyles() == null) {
      return false;
    }

    int usedStyles = 0;
    if (tblLookValue != null) {
      usedStyles = tblLookValue;
    }
    usedStyles |= tableCell.getUsedStyles();

    return switch (tblStylePr.getType()) {
      case FIRST_ROW -> (2048 & usedStyles) != 0;
      case LAST_ROW -> (1024 & usedStyles) != 0;
      case FIRST_COL -> (512 & usedStyles) != 0;
      case LAST_COL -> (256 & usedStyles) != 0;
      case BAND_1_VERT -> (128 & usedStyles) != 0;
      case BAND_2_VERT -> (64 & usedStyles) != 0;
      case BAND_1_HORZ -> (32 & usedStyles) != 0;
      case BAND_2_HORZ -> (16 & usedStyles) != 0;
      case NE_CELL -> (8 & usedStyles) != 0;
      case NW_CELL -> (4 & usedStyles) != 0;
      case SE_CELL -> (2 & usedStyles) != 0;
      case SW_CELL -> (1 & usedStyles) != 0;
      case WHOLE_TABLE -> true;
    };
  }

  private void addTableProperties(TableElement tableElement) {
    if (table.getTblPr() == null) {
      return;
    }

    if (table.getTblPr().getTblStyle() != null) {
      var tblStyleKey = table.getTblPr().getTblStyle().getVal();
      Style style = converter.getStyles().get(tblStyleKey);
      addTableProperties(tableElement, style.getTblPr());
    }

    addTableProperties(tableElement, table.getTblPr());
  }

  private void addTableProperties(TableElement tableElement, CTTblPrBase tblPr) {
    if (tblPr == null) {
      return;
    }

    if (tblPr.getTblBorders() != null) {
      var topBorder = tblPr.getTblBorders().getTop();
      tableElement.setTopBorder(parseCtBorder(topBorder));

      var rightBorder = tblPr.getTblBorders().getRight();
      tableElement.setRightBorder(parseCtBorder(rightBorder));

      var bottomBorder = tblPr.getTblBorders().getBottom();
      tableElement.setBottomBorder(parseCtBorder(bottomBorder));

      var leftBorder = tblPr.getTblBorders().getLeft();
      tableElement.setLeftBorder(parseCtBorder(leftBorder));
    }

    if (tblPr.getShd() != null) {
      tableElement.setBackgroundColor(parseCTShd(tblPr.getShd()));
    }
  }

  private Border parseCtBorder(CTBorder border) {
    if (border == null) return null;

    var color = border.getColor();
    if (color != null) {
      color = color.equals("auto") ? "000" : color;
      color = "#" + color.toLowerCase();
    } else {
      color = "#000";
    }

    var width = border.getSz() != null ? DocxUnitConverter.convertPointToPixel(border.getSz()) : 0;

    var type = "solid";
    if (border.getVal() != null) {
      switch (border.getVal()) {
        case SINGLE:
          break;
        case DASHED:
          type = "dashed";
          break;
        case NONE, NIL:
          type = "none";
          break;
        default:
          LOGGER.error("unsupported table border style: {}", border.getVal());
          break;
      }
    }

    return new Border(color, width, type);
  }

  private String parseCTShd(CTShd ctShd) {
    switch (ctShd.getVal()) {
      case CLEAR -> {
        return "#" + ctShd.getFill();
      }
      case SOLID -> {
        return "#" + ctShd.getColor();
      }
      default -> LOGGER.error("unsupported shading value: {}", ctShd.getVal());
    }
    return "#ffffff";
  }

  private List<TableRowElement> parseTable(Tbl table, List<UnhandledElement> unhandledElements) {
    List<TableRowElement> rows = new ArrayList<>();

    table
        .getContent()
        .forEach(
            element -> {
              if (element instanceof Tr tr) {
                rows.add(parseTr(tr, unhandledElements));
              } else {
                LOGGER.error("unknown table element: {}", element.getClass());
              }
            });

    if (!rows.isEmpty()) {
      rows.get(0).cells().forEach(BlockElement::removeTopBorder);
      rows.get(rows.size() - 1).cells().forEach(BlockElement::removeBottomBorder);
    }

    return rows;
  }

  private TableRowElement parseTr(Tr tr, List<UnhandledElement> unhandledElements) {
    List<TableCellElement> cells = new ArrayList<>();

    AtomicInteger usedStyles = new AtomicInteger();
    if (tr.getTrPr() != null
        && tr.getTrPr().getCnfStyleOrDivIdOrGridBefore() != null
        && !tr.getTrPr().getCnfStyleOrDivIdOrGridBefore().isEmpty()) {
      Optional<String> result =
          tr.getTrPr().getCnfStyleOrDivIdOrGridBefore().stream()
              .filter(jaxbElement -> jaxbElement.getDeclaredType() == CTCnf.class)
              .map(jaxbElement -> ((CTCnf) jaxbElement.getValue()).getVal())
              .findFirst();
      result.ifPresent(s -> usedStyles.set(Integer.parseInt(s, 2)));
    }

    tr.getContent()
        .forEach(
            element -> {
              if (element instanceof JAXBElement<?> jaxbElement) {
                if (jaxbElement.getDeclaredType() == Tc.class) {
                  cells.add(
                      parseTc((Tc) jaxbElement.getValue(), usedStyles.get(), unhandledElements));
                } else {
                  LOGGER.error("unknown tc element: {}", jaxbElement.getDeclaredType());
                }
              } else {
                LOGGER.error("unknown tc element: {}", element.getClass());
              }
            });

    addBordersToCells(cells);

    return new TableRowElement(cells);
  }

  private void addBordersToCells(List<TableCellElement> cells) {
    if (cells.isEmpty() || table.getTblPr() == null) {
      return;
    }

    if (table.getTblPr().getTblStyle() != null) {
      String tableStyleKey = table.getTblPr().getTblStyle().getVal();
      Style style = converter.getStyles().get(tableStyleKey);
      if (style.getTblPr() != null) {
        addBordersToCells(cells, style.getTblPr().getTblBorders());
      }
    }

    addBordersToCells(cells, table.getTblPr().getTblBorders());
  }

  private void addBordersToCells(List<TableCellElement> cells, TblBorders tblBorders) {
    if (tblBorders == null) {
      return;
    }

    var verticalCtBorder = tblBorders.getInsideV();
    var horizontalCtBorder = tblBorders.getInsideH();

    var verticalBorder = parseCtBorder(verticalCtBorder);
    var horizontalBorder = parseCtBorder(horizontalCtBorder);

    cells.forEach(
        cell -> {
          cell.setTopBorder(horizontalBorder);
          cell.setRightBorder(verticalBorder);
          cell.setBottomBorder(horizontalBorder);
          cell.setLeftBorder(verticalBorder);
        });
    cells.get(0).setLeftBorder(null);
    cells.get(cells.size() - 1).setRightBorder(null);
  }

  private TableCellElement parseTc(
      Tc tc, Integer trUsedStyles, List<UnhandledElement> unhandledElements) {
    Integer usedStyles = trUsedStyles;
    if (tc.getTcPr() != null && tc.getTcPr().getCnfStyle() != null) {
      int value = Integer.parseInt(tc.getTcPr().getCnfStyle().getVal(), 2);
      if (usedStyles == null) {
        usedStyles = value;
      } else {
        usedStyles |= value;
      }
    }

    List<DocumentationUnitDocx> paragraphElements = new ArrayList<>();
    tc.getContent()
        .forEach(
            element -> {
              if (element instanceof P p) {
                paragraphElements.add(ParagraphConverter.convert(p, converter, unhandledElements));
              } else {
                LOGGER.error("unknown tr element: {}", element);
              }
            });

    var cell = new TableCellElement(paragraphElements, usedStyles);
    addTcStyle(cell, tc);

    return cell;
  }

  private void addTcStyle(TableCellElement cellElement, Tc tc) {
    if (table.getTblPr().getTblStyle() != null) {
      var tblStyleKey = table.getTblPr().getTblStyle().getVal();
      Style style = converter.getStyles().get(tblStyleKey);
      addTcStyle(cellElement, style.getTcPr());
    }

    addTcStyle(cellElement, tc.getTcPr());
  }

  private void addExternalTcStyle(TableCellElement cellElement, TcPr tcPr) {
    if (tcPr == null) {
      return;
    }

    if (tcPr.getTcBorders() != null) {
      if (tcPr.getTcBorders().getBottom() != null) {
        cellElement.setBottomBorder(parseCtBorder(tcPr.getTcBorders().getBottom()));
      }
      if (tcPr.getTcBorders().getTop() != null) {
        cellElement.setTopBorder(parseCtBorder(tcPr.getTcBorders().getTop()));
      }
      if (tcPr.getTcBorders().getLeft() != null) {
        cellElement.setLeftBorder(parseCtBorder(tcPr.getTcBorders().getLeft()));
      }
      if (tcPr.getTcBorders().getRight() != null) {
        cellElement.setRightBorder(parseCtBorder(tcPr.getTcBorders().getRight()));
      }
    }

    if (tcPr.getGridSpan() != null) {
      cellElement.setColumnSpan(tcPr.getGridSpan().getVal().intValue());
    }

    if (tcPr.getShd() != null) {
      cellElement.setBackgroundColor(parseCTShd(tcPr.getShd()));
    }
  }

  private void addTcStyle(TableCellElement cellElement, TcPr tcPr) {
    if (tcPr != null) {
      if (tcPr.getTcBorders() != null) {
        var tcBorders = tcPr.getTcBorders();
        cellElement.setInitialBorders(
            parseCtBorder(tcBorders.getTop()),
            parseCtBorder(tcBorders.getRight()),
            parseCtBorder(tcBorders.getBottom()),
            parseCtBorder(tcBorders.getLeft()));
      }

      if (tcPr.getGridSpan() != null) {
        cellElement.setColumnSpan(tcPr.getGridSpan().getVal().intValue());
      }

      if (tcPr.getShd() != null) {
        cellElement.setBackgroundColor(parseCTShd(tcPr.getShd()));
      }
    }
  }
}
