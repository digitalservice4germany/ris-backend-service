package de.bund.digitalservice.ris.caselaw.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bund.digitalservice.ris.caselaw.adapter.caselawldml.CaseLawLdml;
import de.bund.digitalservice.ris.caselaw.adapter.exception.BucketException;
import de.bund.digitalservice.ris.caselaw.adapter.exception.LdmlTransformationException;
import de.bund.digitalservice.ris.caselaw.adapter.exception.PublishException;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationUnitToLdmlTransformer;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import jakarta.xml.bind.JAXB;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.MappingException;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
@Slf4j
public class LdmlExporterService {

  private static final Logger logger = LogManager.getLogger(LdmlExporterService.class);

  private final DocumentationUnitRepository documentationUnitRepository;
  private final DocumentBuilderFactory documentBuilderFactory;
  private final LdmlBucket ldmlBucket;
  private final ObjectMapper objectMapper;
  private final Templates htmlToAknHtml;
  private final Schema schema;

  @Autowired
  public LdmlExporterService(
      DocumentationUnitRepository documentationUnitRepository,
      XmlUtilService xmlUtilService,
      DocumentBuilderFactory documentBuilderFactory,
      LdmlBucket ldmlBucket,
      ObjectMapper objectMapper) {

    this.documentationUnitRepository = documentationUnitRepository;
    this.documentBuilderFactory = documentBuilderFactory;
    this.ldmlBucket = ldmlBucket;
    this.objectMapper = objectMapper;
    this.htmlToAknHtml = xmlUtilService.getTemplates("caselawhandover/htmlToAknHtml.xslt");
    this.schema = xmlUtilService.getSchema("caselawhandover/shared/akomantoso30.xsd");
  }

  public void exportMultipleRandomDocumentationUnits() {
    logger.info("Export to LDML process has started");
    List<DocumentationUnit> documentationUnitsToTransform = new ArrayList<>();

    List<UUID> idsToTransform = documentationUnitRepository.getRandomDocumentationUnitIds();
    idsToTransform.forEach(
        id -> {
          try {
            documentationUnitsToTransform.add(documentationUnitRepository.findByUuid(id));
          } catch (DocumentationUnitNotExistsException ex) {
            log.debug(ex.getMessage());
          }
        });

    List<String> transformedDocUnits = new ArrayList<>();
    if (!documentationUnitsToTransform.isEmpty()) {
      for (DocumentationUnit documentationUnit : documentationUnitsToTransform) {
        var documentNumber = transformAndSaveDocumentationUnit(documentationUnit);
        if (documentNumber != null) {
          transformedDocUnits.add(documentNumber);
        }
      }
    }

    if (!transformedDocUnits.isEmpty()) {
      Changelog changelog = new Changelog(transformedDocUnits, null);

      try {
        String changelogString = objectMapper.writeValueAsString(changelog);
        ldmlBucket.save(
            "changelogs/" + DateUtils.toDateTimeString(LocalDateTime.now()) + ".json",
            changelogString);
      } catch (IOException e) {
        log.error("Could not write changelog file. {}", e.getMessage());
      }
    }

    logger.info("Export to LDML process is done");
  }

  /**
   * Publish the documentation unit by transforming it to valid LDML and putting the resulting XML
   * file into a bucket together with a changelog file, specifying which documentation unit has been
   * added or updated.
   *
   * @param documentationUnitId the id of the documentation unit that should be published
   * @throws DocumentationUnitNotExistsException if the documentation unit with the given id could
   *     not be found in the database
   * @throws LdmlTransformationException if the documentation unit could not be transformed to valid
   *     LDML
   * @throws PublishException if the changelog file could not be created or either of the files
   *     could not be saved in the bucket
   */
  public void publishDocumentationUnit(UUID documentationUnitId)
      throws DocumentationUnitNotExistsException {
    DocumentationUnit documentationUnit =
        documentationUnitRepository.findByUuid(documentationUnitId);
    Optional<CaseLawLdml> ldml =
        DocumentationUnitToLdmlTransformer.transformToLdml(
            documentationUnit, documentBuilderFactory);

    if (ldml.isEmpty()) {
      throw new LdmlTransformationException(
          "Could not transform documentation unit to LDML.", null);
    }

    Optional<String> fileContent = ldmlToString(ldml.get());
    if (fileContent.isEmpty()) {
      throw new LdmlTransformationException(
          "Could not transform documentation unit to valid LDML.", null);
    }

    Changelog changelog = new Changelog(List.of(ldml.get().getUniqueId()), null);
    String changelogJson;
    try {
      changelogJson = objectMapper.writeValueAsString(changelog);
    } catch (IOException e) {
      log.error("Could not write changelog file. {}", e.getMessage());
      throw new PublishException(
          "Could not publish documentation unit to portal, because changelog file could not be created.",
          null);
    }

    try {
      ldmlBucket.save(
          "changelogs/" + DateUtils.toDateTimeString(LocalDateTime.now()) + ".json", changelogJson);
    } catch (BucketException e) {
      log.error("Could not save changelog to bucket", e);
      throw new PublishException("Could not save changelog to bucket.", e);
    }

    try {
      ldmlBucket.save(ldml.get().getUniqueId() + ".xml", fileContent.get());
      log.info("LDML for documentation unit {} successfully published.", ldml.get().getUniqueId());
    } catch (BucketException e) {
      log.error("Could not save LDML to bucket", e);
      throw new PublishException("Could not save LDML to bucket.", e);
    }
  }

  public String transformAndSaveDocumentationUnit(DocumentationUnit documentationUnit) {
    Optional<CaseLawLdml> ldml =
        DocumentationUnitToLdmlTransformer.transformToLdml(
            documentationUnit, documentBuilderFactory);

    if (ldml.isPresent()) {
      Optional<String> fileContent = ldmlToString(ldml.get());
      if (fileContent.isPresent()) {
        ldmlBucket.save(ldml.get().getUniqueId() + ".xml", fileContent.get());
        return ldml.get().getUniqueId();
      }
    }
    return null;
  }

  public Optional<String> ldmlToString(CaseLawLdml ldml) {
    StringWriter jaxbOutput = new StringWriter();
    JAXB.marshal(ldml, jaxbOutput);

    try {
      String ldmlAsXmlString = XmlUtilService.xsltTransform(htmlToAknHtml, jaxbOutput.toString());
      if (ldmlAsXmlString.contains("akn:unknownUseCaseDiscovered")) {
        int hintStart = Math.max(0, ldmlAsXmlString.indexOf("akn:unknownUseCaseDiscovered") - 10);
        int hintEnd = Math.min(ldmlAsXmlString.length(), hintStart + 60);
        String hint =
            "\"..." + ldmlAsXmlString.substring(hintStart, hintEnd).replace("\n", "") + "...\"";
        logger.error(
            "Invalid ldml produced for {}. A new unsupported attribute or elements was discovered."
                + " It is either an error or needs to be added to the allow list. hint : {}",
            ldml.getUniqueId(),
            hint);
        return Optional.empty();
      }

      schema.newValidator().validate(new StreamSource(new StringReader(ldmlAsXmlString)));
      return Optional.of(ldmlAsXmlString);
    } catch (SAXException | MappingException | IOException e) {
      logXsdError(ldml.getUniqueId(), jaxbOutput.toString(), e);
      return Optional.empty();
    }
  }

  private void logXsdError(String caseLawId, String beforeXslt, Exception e) {
    String hint = "";
    if (beforeXslt.contains("<akn:judgmentBody/>")) {
      hint = "Ldml contained <judgementBody/>. An empty judgementBody isn't allowed.";
    } else if (beforeXslt.contains("KARE600062214")) {
      hint = "KARE600062214 contains an invalid width (escaping issue)";
    } else if (beforeXslt.contains("JURE200002538")) {
      hint = "JURE200002538 contains an invalid href (invalid whitespace in the middle of the url)";
    }
    logger.error("Error: {} Case Law {} does not match akomantoso30.xsd. {}", hint, caseLawId, e);
  }
}
