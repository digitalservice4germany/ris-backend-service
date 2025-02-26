package de.bund.digitalservice.ris.caselaw.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Templates;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.MappingException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
@Slf4j
public class LdmlExporterService {

  private final DocumentationUnitRepository documentationUnitRepository;
  private final DocumentBuilderFactory documentBuilderFactory;
  private final LdmlBucket internalPortalBucket;
  private final PortalPrototypeBucket portalPrototypeBucket;
  private final ObjectMapper objectMapper;
  private final Templates htmlToAknHtml;
  private final Schema schema;

  @Autowired
  public LdmlExporterService(
      DocumentationUnitRepository documentationUnitRepository,
      XmlUtilService xmlUtilService,
      DocumentBuilderFactory documentBuilderFactory,
      LdmlBucket internalPortalBucket,
      PortalPrototypeBucket portalPrototypeBucket,
      ObjectMapper objectMapper) {

    this.documentationUnitRepository = documentationUnitRepository;
    this.documentBuilderFactory = documentBuilderFactory;
    // TODO: Configuration for bucket (Internal/External ExporterService)
    this.internalPortalBucket = internalPortalBucket;
    this.portalPrototypeBucket = portalPrototypeBucket;
    this.objectMapper = objectMapper;
    this.htmlToAknHtml = xmlUtilService.getTemplates("caselawhandover/htmlToAknHtml.xslt");
    this.schema = xmlUtilService.getSchema("caselawhandover/shared/akomantoso30.xsd");
  }

  // FIXME: delete this method?
  public void exportMultipleRandomDocumentationUnits() {
    log.info("Export to LDML process has started");
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
        internalPortalBucket.save(
            "changelogs/" + DateUtils.toDateTimeString(LocalDateTime.now()) + ".json",
            changelogString);
      } catch (IOException e) {
        log.error("Could not write changelog file. {}", e.getMessage());
      }
    }

    log.info("Export to LDML process is done");
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
      internalPortalBucket.save(
          "changelogs/" + DateUtils.toDateTimeString(LocalDateTime.now()) + ".json", changelogJson);
    } catch (BucketException e) {
      log.error("Could not save changelog to bucket", e);
      throw new PublishException("Could not save changelog to bucket.", e);
    }

    try {
      internalPortalBucket.save(ldml.get().getUniqueId() + ".xml", fileContent.get());
      log.info("LDML for documentation unit {} successfully published.", ldml.get().getUniqueId());
    } catch (BucketException e) {
      log.error("Could not save LDML to bucket", e);
      throw new PublishException("Could not save LDML to bucket.", e);
    }
  }

  /**
   * Publish the documentation unit by transforming it to valid LDML and putting the resulting XML
   * file into a bucket together, specifying which documentation unit has been added or updated.
   *
   * @param documentNumber the documentation unit that should be published
   * @throws DocumentationUnitNotExistsException if the documentation unit with the given id could
   *     not be found in the database
   * @throws LdmlTransformationException if the documentation unit could not be transformed to valid
   *     LDML
   * @throws PublishException if the changelog file could not be created or either of the files
   *     could not be saved in the bucket
   */
  public void publishDocumentationUnit(String documentNumber)
      throws DocumentationUnitNotExistsException {
    DocumentationUnit documentationUnit =
        documentationUnitRepository.findByDocumentNumber(documentNumber);
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

    try {
      portalPrototypeBucket.save(ldml.get().getUniqueId() + ".xml", fileContent.get());
      log.info("LDML for documentation unit {} successfully published.", ldml.get().getUniqueId());
    } catch (BucketException e) {
      log.error("Could not save LDML to bucket", e);
      throw new PublishException("Could not save LDML to bucket.", e);
    }
  }

  public void deleteDocumentationUnit(String documentNumber) {
    try {
      portalPrototypeBucket.delete(documentNumber + ".xml");
    } catch (BucketException e) {
      log.error("Could not delete LDML from bucket, docNumber: {}", documentNumber, e);
    }
  }

  public void uploadChangelog(
      List<String> publishedDocumentNumbers, List<String> deletedDocumentNumbers)
      throws JsonProcessingException {
    Changelog changelog = new Changelog(publishedDocumentNumbers, deletedDocumentNumbers);

    String changelogString = objectMapper.writeValueAsString(changelog);
    portalPrototypeBucket.save(
        "changelogs/" + DateUtils.toDateTimeString(LocalDateTime.now()) + ".json", changelogString);
  }

  @Async
  public void exportSampleLdmls() throws IOException {
    List<String> documentNumbers =
        List.of(
            "KORE300422021",
            "KORE300962024",
            "KORE300712021",
            "KORE315152024",
            "KORE300452019",
            "KORE303732016",
            "KORE317912010",
            "KORE629592018",
            "KORE313312019",
            "KORE307272022",
            "KVRE427971801",
            "KVRE417211601",
            "KVRE450362201",
            "KVRE443042101",
            "KVRE407641401",
            "KVRE457652301",
            "KVRE402711301",
            "KVRE451122301",
            "KVRE400071201",
            "KVRE438112001",
            "",
            "WBRE201800180",
            "WBRE201900148",
            "WBRE201900147",
            "WBRE201800214",
            "WBRE201800217",
            "WBRE201800218",
            "WBRE201800216",
            "WBRE201800215",
            "WBRE201800307",
            "WBRE202300156",
            "",
            "STRE201150211",
            "STRE201350061",
            "STRE201450500",
            "STRE201750064",
            "STRE201550401",
            "STRE201350278",
            "STRE201250108",
            "STRE201250718",
            "STRE201350075",
            "STRE201650167",
            "",
            "KSRE126071509",
            "KSRE125951515",
            "KSRE125801515",
            "KSRE125961515",
            "KSRE128321509",
            "KSRE130411615",
            "KSRE166401506",
            "KSRE144180209",
            "KSRE131851609",
            "KSRE130181715",
            "",
            "KARE600032200",
            "KARE600035228",
            "KARE600051335",
            "KARE600051463",
            "KARE600055529",
            "KARE600055896",
            "KARE600055891",
            "KARE600055897",
            "KARE600061633",
            "KARE600061634",
            "",
            "JURE229030565",
            "JURE169016439",
            "JURE229030432",
            "JURE239031156",
            "JURE209002984",
            "JURE239030994",
            "JURE249031620",
            "JURE219029935",
            "JURE199002514",
            "JURE249031399");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);

    for (String documentNumber : documentNumbers) {
      DocumentationUnit documentationUnit;
      try {
        documentationUnit = documentationUnitRepository.findByDocumentNumber(documentNumber);
      } catch (Exception ex) {
        log.error(
            "Couldn't export (step get documentation unit) {} as LegalDocML", documentNumber, ex);
        continue;
      }

      Optional<CaseLawLdml> ldml =
          DocumentationUnitToLdmlTransformer.transformToLdml(
              documentationUnit, documentBuilderFactory);

      if (ldml.isEmpty()) {
        log.error("Couldn't export (step tranform) {} as LegalDocML", documentNumber);
        continue;
      }

      Optional<String> fileContent = ldmlToString(ldml.get());
      if (fileContent.isEmpty()) {
        log.error("Couldn't export (step generate file content) {} as LegalDocML", documentNumber);
        continue;
      }

      ByteArrayInputStream bais = new ByteArrayInputStream(fileContent.get().getBytes());

      byte[] bytes = new byte[1024];
      int length;

      ZipEntry entry = new ZipEntry(documentationUnit.documentNumber() + ".xml");
      zos.putNextEntry(entry);

      while ((length = bais.read(bytes)) >= 0) {
        zos.write(bytes, 0, length);
      }

      zos.closeEntry();

      log.info("Add {} to the zip file.", documentationUnit.documentNumber());
    }

    zos.close();
    ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
    buffer.rewind();

    internalPortalBucket.saveBytes("test_documentation_units.zip", buffer);
  }

  public String transformAndSaveDocumentationUnit(DocumentationUnit documentationUnit) {
    Optional<CaseLawLdml> ldml =
        DocumentationUnitToLdmlTransformer.transformToLdml(
            documentationUnit, documentBuilderFactory);

    if (ldml.isPresent()) {
      Optional<String> fileContent = ldmlToString(ldml.get());
      if (fileContent.isPresent()) {
        internalPortalBucket.save(ldml.get().getUniqueId() + ".xml", fileContent.get());
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
        log.error(
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
    } else if (beforeXslt.matches("(?s).*?<akn:header>.*?<div.*?>.*?</akn:header>.*")) {
      hint = "Ldml contained <div> inside title.";
    } else if (beforeXslt.matches("(?s).*?<akn:header>.*?<br.*?>.*?</akn:header>.*")) {
      hint = "Ldml contained <br> inside title.";
    }
    log.error("Error: {} Case Law {} does not match akomantoso30.xsd. {}", hint, caseLawId, e);
  }
}
