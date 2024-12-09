package de.bund.digitalservice.ris.caselaw.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import de.bund.digitalservice.ris.caselaw.domain.Reference;
import de.bund.digitalservice.ris.caselaw.domain.XmlExporter;
import de.bund.digitalservice.ris.caselaw.domain.XmlExporterException;
import de.bund.digitalservice.ris.caselaw.domain.XmlTransformationResult;
import de.bund.digitalservice.ris.domain.export.juris.JurisXmlExporter;
import de.bund.digitalservice.ris.domain.export.juris.ResultObject;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

/** Wraps the Juris XML exporter to provide a common interface for XML export. */
public class JurisXmlExporterWrapper implements XmlExporter {
  private final JurisXmlExporter jurisXmlExporter;

  public JurisXmlExporterWrapper(ObjectMapper objectMapper, TransformerFactory transformerFactory) {
    this.jurisXmlExporter = new JurisXmlExporter(objectMapper, transformerFactory);
  }

  /**
   * Generates juris XML from a documentation unit.
   *
   * @param documentationUnit the documentation unit
   * @return the XML export result that may be unsuccessful and may contain error messages
   * @throws ParserConfigurationException if the XML generation fails due to a configuration error
   * @throws TransformerException if the XML generation fails due to failed transformation
   */
  @Override
  public XmlTransformationResult transformToXml(DocumentationUnit documentationUnit)
      throws ParserConfigurationException, TransformerException {
    ResultObject resultObject = jurisXmlExporter.generateXml(documentationUnit);
    return new XmlTransformationResult(
        resultObject.xml(),
        resultObject.status().statusCode().equals("200"),
        resultObject.status().statusMessages(),
        resultObject.fileName(),
        resultObject.publishDate());
  }

  @Override
  public String generateEncryptedXMLString(DocumentationUnit documentationUnit)
      throws XmlExporterException {
    String resultObject;
    try {
      resultObject = jurisXmlExporter.generateEncryptedXMLString(documentationUnit);
    } catch (Exception e) {
      throw new XmlExporterException("Failed to generate encrypted XML string", e);
    }
    return resultObject;
  }

  /**
   * Generates juris XML from a documentation unit.
   *
   * @param edition the edition
   * @return the XML export results that may be unsuccessful and may contain error messages
   * @throws ParserConfigurationException if the XML generation fails due to a configuration error
   * @throws TransformerException if the XML generation fails due to failed transformation
   */
  @Override
  public List<XmlTransformationResult> transformToXml(LegalPeriodicalEdition edition)
      throws ParserConfigurationException, TransformerException {

    List<XmlTransformationResult> results = new ArrayList<>();
    for (Reference reference : edition.references()) {
      ResultObject resultObject =
          jurisXmlExporter.generateXml(
              DocumentationUnit.builder()
                  .documentNumber(reference.documentationUnit().getDocumentNumber())
                  .references(List.of(reference))
                  .build(),
              false);
      results.add(
          new XmlTransformationResult(
              resultObject.xml(),
              resultObject.status().statusCode().equals("200"),
              resultObject.status().statusMessages(),
              resultObject.fileName(),
              resultObject.publishDate()));
    }
    return results;
  }
}
