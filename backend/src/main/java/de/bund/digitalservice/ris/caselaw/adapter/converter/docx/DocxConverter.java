package de.bund.digitalservice.ris.caselaw.adapter.converter.docx;

import de.bund.digitalservice.ris.caselaw.domain.Converter;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocumentationUnitDocx;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocxImagePart;
import de.bund.digitalservice.ris.caselaw.domain.docx.ErrorElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.ParagraphElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.UnhandledElement;
import jakarta.xml.bind.JAXBElement;
import java.util.List;
import java.util.Map;
import org.docx4j.model.listnumbering.ListNumberingDefinition;
import org.docx4j.wml.P;
import org.docx4j.wml.Style;
import org.docx4j.wml.Tbl;

public class DocxConverter implements Converter<DocumentationUnitDocx> {
  private Map<String, Style> styles;
  private Map<String, DocxImagePart> images;
  private List<ParagraphElement> footers;
  private Map<String, ListNumberingDefinition> listNumberingDefinitions;

  public Map<String, DocxImagePart> getImages() {
    return images;
  }

  public void setImages(Map<String, DocxImagePart> images) {
    this.images = images;
  }

  public Map<String, Style> getStyles() {
    return styles;
  }

  public void setStyles(Map<String, Style> styles) {
    this.styles = styles;
  }

  public Map<String, ListNumberingDefinition> getListNumberingDefinitions() {
    return listNumberingDefinitions;
  }

  public void setListNumberingDefinitions(
      Map<String, ListNumberingDefinition> listNumberingDefinitions) {
    this.listNumberingDefinitions = listNumberingDefinitions;
  }

  public List<ParagraphElement> getFooters() {
    return footers;
  }

  public void setFooters(List<ParagraphElement> footers) {
    this.footers = footers;
  }

  public DocumentationUnitDocx convert(Object part, List<UnhandledElement> unhandledElements) {
    DocxBuilder builder;

    if (part instanceof P p) {
      builder = convertP(p);
    } else if (part instanceof JAXBElement<?> element && element.getDeclaredType() == Tbl.class) {
      builder = convertTbl((Tbl) element.getValue());
    } else {
      return new ErrorElement(part.getClass().getName());
    }

    builder.setConverter(this);

    return builder.build(unhandledElements);
  }

  private DocxBuilder convertP(P part) {
    return DocumentationUnitDocxBuilder.newInstance().setParagraph(part);
  }

  private DocxBuilder convertTbl(Tbl part) {
    return DocxTableBuilder.newInstance().setTable(part);
  }
}
