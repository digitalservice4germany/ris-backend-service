package de.bund.digitalservice.ris.caselaw.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnit;
import de.bund.digitalservice.ris.caselaw.domain.XmlExporter;
import de.bund.digitalservice.ris.caselaw.domain.XmlResultObject;
import de.bund.digitalservice.ris.domain.export.juris.JurisXmlExporter;
import de.bund.digitalservice.ris.domain.export.juris.ResultObject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class JurisXmlExporterWrapper implements XmlExporter {
  private final JurisXmlExporter jurisXmlExporter;

  public JurisXmlExporterWrapper(ObjectMapper objectMapper) {
    this.jurisXmlExporter = new JurisXmlExporter(objectMapper);
  }

  @Override
  public XmlResultObject generateXml(DocumentUnit documentUnit)
      throws ParserConfigurationException, TransformerException {
    ResultObject resultObject = jurisXmlExporter.generateXml(documentUnit);
    return new XmlResultObject(
        resultObject.xml(),
        resultObject.status().statusCode(),
        resultObject.status().statusMessages(),
        resultObject.fileName(),
        resultObject.publishDate());
  }

  @Override
  public String generateEncryptedXMLString(DocumentUnit documentUnit) throws Exception {
    String resultObject = jurisXmlExporter.generateEncryptedXMLString(documentUnit);
    return resultObject;
  }
}
