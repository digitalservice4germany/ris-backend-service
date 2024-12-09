package de.bund.digitalservice.ris.caselaw.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.bund.digitalservice.ris.caselaw.adapter.JurisXmlExporterWrapper;
import de.bund.digitalservice.ris.caselaw.adapter.MockXmlExporter;
import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocxConverter;
import de.bund.digitalservice.ris.caselaw.domain.XmlExporter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import org.docx4j.org.apache.xalan.processor.TransformerFactoryImpl;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ConverterConfig {
  @Bean
  public DocxConverter docxConverter() {
    return new DocxConverter();
  }

  @Bean
  public DocumentBuilderFactory documentBuilderFactory() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    try {
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    } catch (ParserConfigurationException e) {
      throw new BeanCreationException(
          "DocumentBuilderFactory", "Couldn't create DocumentBuilderFactory!", e);
    }

    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    return factory;
  }

  @Bean(name = "saxon")
  public TransformerFactory transformerFactorySaxon() {
    return new net.sf.saxon.TransformerFactoryImpl();
  }

  @Bean
  @Primary
  public TransformerFactory transformerFactory() {
    return new TransformerFactoryImpl();
  }

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    var objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.registerModule(YearJsonConverter.yearJsonConverter());
    return objectMapper;
  }

  @Bean
  public XmlExporter jurisXmlExporter() {
    return new JurisXmlExporterWrapper(objectMapper(), transformerFactory());
  }

  // @Bean
  // use the mock if you don't have access to JurisXmlExporter
  public XmlExporter mockXmlExporter() {
    return new MockXmlExporter();
  }
}
