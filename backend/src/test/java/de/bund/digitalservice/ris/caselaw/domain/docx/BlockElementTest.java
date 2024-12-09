package de.bund.digitalservice.ris.caselaw.domain.docx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class BlockElementTest {
  @Test
  void testSetTop() {
    var concreteBlock = generateConcreteBlockElement();

    concreteBlock.setInitialBorders(new Border("green", 1, "solid"), null, null, null);
    assertTrue(concreteBlock.toHtmlString().contains("border-top: 1px solid green;"));

    // should not overwrite self
    concreteBlock.setTopBorder(new Border("yellow", 1, "solid"));
    assertTrue(concreteBlock.toHtmlString().contains("border-top: 1px solid green;"));
  }

  @Test
  void testSetBottom() {
    var concreteBlock = generateConcreteBlockElement();
    concreteBlock.setInitialBorders(null, null, new Border("green", 1, "solid"), null);
    assertTrue(concreteBlock.toHtmlString().contains("border-bottom: 1px solid green;"));

    // should not overwrite self
    concreteBlock.setBottomBorder(new Border("yellow", 1, "solid"));
    assertTrue(concreteBlock.toHtmlString().contains("border-bottom: 1px solid green;"));
  }

  @Test
  void testSetRight() {
    var concreteBlock = generateConcreteBlockElement();
    concreteBlock.setInitialBorders(null, new Border("green", 1, "solid"), null, null);
    assertTrue(concreteBlock.toHtmlString().contains("border-right: 1px solid green;"));

    // should not overwrite self
    concreteBlock.setRightBorder(new Border("yellow", 1, "solid"));
    assertTrue(concreteBlock.toHtmlString().contains("border-right: 1px solid green;"));
  }

  @Test
  void testSetLeft() {
    var concreteBlock = generateConcreteBlockElement();
    concreteBlock.setInitialBorders(null, null, null, new Border("green", 1, "solid"));
    assertTrue(concreteBlock.toHtmlString().contains("border-left: 1px solid green;"));

    // should not overwrite
    concreteBlock.setLeftBorder(new Border("yellow", 1, "solid"));
    assertTrue(concreteBlock.toHtmlString().contains("border-left: 1px solid green;"));
  }

  @Test
  void testSetBackgroundColor() {
    var concreteBlock = generateConcreteBlockElement();
    concreteBlock.setBackgroundColor("#fff");
    assertTrue(concreteBlock.toHtmlString().contains("background-color: #fff;"));
  }

  @Test
  void testRemoveTop() {
    var concreteBlock = generateConcreteBlockElement();
    concreteBlock.setBottomBorder(new Border("green", 1, "solid"));
    concreteBlock.setTopBorder(new Border("yellow", 2, "solid"));
    concreteBlock.removeTopBorder();

    assertFalse(concreteBlock.toHtmlString().contains("border-top: 2px solid yellow;"));
  }

  @Test
  void testRemoveBottom() {
    var concreteBlock = generateConcreteBlockElement();
    concreteBlock.setBottomBorder(new Border("green", 1, "solid"));
    concreteBlock.setTopBorder(new Border("yellow", 2, "solid"));
    concreteBlock.removeBottomBorder();

    assertFalse(concreteBlock.toHtmlString().contains("border-bottom: 1px solid green;"));
  }

  @Test
  void testIsSetWithSelfs() {
    var concreteBlock = generateConcreteBlockElement();
    concreteBlock.setInitialBorders(new Border("green", 1, "solid"), null, null, null);
    assertTrue(concreteBlock.toHtmlString().contains("border-top: 1px solid green"));
  }

  private BlockElement generateConcreteBlockElement() {
    var someElements = new ArrayList<DocumentationUnitDocx>();
    someElements.add(new NumberingList());
    return new TableCellElement(someElements, null);
  }
}
