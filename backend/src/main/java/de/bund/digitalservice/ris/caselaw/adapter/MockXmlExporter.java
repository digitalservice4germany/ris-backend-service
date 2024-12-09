package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import de.bund.digitalservice.ris.caselaw.domain.XmlExporter;
import de.bund.digitalservice.ris.caselaw.domain.XmlTransformationResult;
import java.time.Instant;
import java.util.List;

/**
 * Mock implementation of the {@link XmlExporter} interface. This implementation is used for testing
 * purposes.
 */
public class MockXmlExporter implements XmlExporter {

  /**
   * Generates a mock XML export result.
   *
   * @param documentationUnit the document unit
   * @return the XML export result
   */
  @Override
  public XmlTransformationResult transformToXml(DocumentationUnit documentationUnit) {
    return new XmlTransformationResult(
        "xml",
        documentationUnit.coreData().decisionDate() != null,
        List.of("message 1", "message 2"),
        "test.xml",
        Instant.now());
  }

  @Override
  public String generateEncryptedXMLString(DocumentationUnit documentationUnit) {
    return "";
  }

  /**
   * Generates a mock XML export result.
   *
   * @param edition the edition
   * @return the XML export result
   */
  @Override
  public List<XmlTransformationResult> transformToXml(LegalPeriodicalEdition edition) {
    return edition.references().stream()
        .map(
            reference ->
                new XmlTransformationResult(
                    "citation: "
                        + reference.citation()
                        + " docunit: "
                        + reference.documentationUnit().getDocumentNumber(),
                    true,
                    List.of("message 1", "message 2"),
                    reference.documentationUnit().getDocumentNumber() + ".xml",
                    Instant.now()))
        .toList();
  }
}
