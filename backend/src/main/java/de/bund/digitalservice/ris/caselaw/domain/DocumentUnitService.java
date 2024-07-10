package de.bund.digitalservice.ris.caselaw.domain;

import static de.bund.digitalservice.ris.caselaw.domain.StringUtils.normalizeSpace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatchException;
import de.bund.digitalservice.ris.caselaw.domain.docx.Docx2Html;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentUnitDeletionException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import de.bund.digitalservice.ris.caselaw.domain.exception.PatchForSamePathException;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

@Service
@Slf4j
public class DocumentUnitService {

  private final DocumentUnitRepository repository;
  private final PublicationReportRepository publicationReportRepository;
  private final DocumentNumberService documentNumberService;
  private final EmailPublishService publicationService;
  private final DeltaMigrationRepository deltaMigrationRepository;
  private final DocumentUnitStatusService documentUnitStatusService;
  private final AttachmentService attachmentService;
  private final DocumentNumberRecyclingService documentNumberRecyclingService;
  private final PatchMapperService patchMapperService;
  private final Validator validator;

  @Value("${mail.exporter.recipientAddress:neuris@example.com}")
  private String recipientAddress;

  public DocumentUnitService(
      DocumentUnitRepository repository,
      DocumentNumberService documentNumberService,
      EmailPublishService publicationService,
      DeltaMigrationRepository migrationService,
      DocumentUnitStatusService documentUnitStatusService,
      PublicationReportRepository publicationReportRepository,
      DocumentNumberRecyclingService documentNumberRecyclingService,
      Validator validator,
      AttachmentService attachmentService,
      PatchMapperService patchMapperService) {

    this.repository = repository;
    this.documentNumberService = documentNumberService;
    this.publicationService = publicationService;
    this.deltaMigrationRepository = migrationService;
    this.documentUnitStatusService = documentUnitStatusService;
    this.publicationReportRepository = publicationReportRepository;
    this.documentNumberRecyclingService = documentNumberRecyclingService;
    this.validator = validator;
    this.attachmentService = attachmentService;
    this.patchMapperService = patchMapperService;
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public DocumentUnit generateNewDocumentUnit(DocumentationOffice documentationOffice)
      throws DocumentationUnitException {
    var documentNumber = generateDocumentNumber(documentationOffice);
    return repository.createNewDocumentUnit(documentNumber, documentationOffice);
  }

  private String generateDocumentNumber(DocumentationOffice documentationOffice) {
    try {
      return documentNumberService.generateDocumentNumber(documentationOffice.abbreviation());
    } catch (Exception e) {
      throw new DocumentationUnitException("Could not generate document number", e);
    }
  }

  public Slice<DocumentationUnitListItem> searchByDocumentationUnitSearchInput(
      Pageable pageable,
      DocumentationOffice documentationOffice,
      Optional<String> documentNumber,
      Optional<String> fileNumber,
      Optional<String> courtType,
      Optional<String> courtLocation,
      Optional<LocalDate> decisionDate,
      Optional<LocalDate> decisionDateEnd,
      Optional<String> publicationStatus,
      Optional<Boolean> withError,
      Optional<Boolean> myDocOfficeOnly) {

    DocumentationUnitSearchInput searchInput =
        DocumentationUnitSearchInput.builder()
            .documentNumber(normalizeSpace(documentNumber.orElse(null)))
            .fileNumber(normalizeSpace(fileNumber.orElse(null)))
            .courtType(normalizeSpace(courtType.orElse(null)))
            .courtLocation(normalizeSpace(courtLocation.orElse(null)))
            .decisionDate(decisionDate.orElse(null))
            .decisionDateEnd(decisionDateEnd.orElse(null))
            .status(
                (publicationStatus.isPresent() || withError.isPresent())
                    ? Status.builder()
                        .publicationStatus(
                            publicationStatus.map(PublicationStatus::valueOf).orElse(null))
                        .withError(withError.orElse(false))
                        .build()
                    : null)
            .myDocOfficeOnly(myDocOfficeOnly.orElse(false))
            .build();

    return repository.searchByDocumentationUnitSearchInput(
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
        documentationOffice,
        searchInput);
  }

  public DocumentUnit getByDocumentNumber(String documentNumber) {
    try {
      var optionalDocumentUnit = repository.findByDocumentNumber(documentNumber);
      return optionalDocumentUnit.orElseThrow();
    } catch (Exception e) {
      return null;
    }
  }

  public DocumentUnit getByUuid(UUID documentUnitUuid) {
    return repository.findByUuid(documentUnitUuid).orElseThrow();
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public String deleteByUuid(UUID documentUnitUuid) throws DocumentationUnitNotExistsException {

    Map<RelatedDocumentationType, Long> relatedEntities =
        repository.getAllDocumentationUnitWhichLink(documentUnitUuid);

    if (!(relatedEntities == null
        || relatedEntities.isEmpty()
        || relatedEntities.values().stream().mapToLong(Long::longValue).sum() == 0)) {

      log.debug(
          "Could not delete document unit {} cause of related entities: {}",
          documentUnitUuid,
          relatedEntities);

      throw new DocumentUnitDeletionException(
          "Die Dokumentationseinheit konnte nicht gelöscht werden, da", relatedEntities);
    }

    DocumentUnit documentUnit =
        repository
            .findByUuid(documentUnitUuid)
            .orElseThrow(() -> new DocumentationUnitNotExistsException(documentUnitUuid));

    log.debug("Deleting DocumentUnitDTO " + documentUnitUuid);

    if (documentUnit.attachments() != null && !documentUnit.attachments().isEmpty())
      attachmentService.deleteAllObjectsFromBucketForDocumentationUnit(documentUnitUuid);

    saveForRecycling(documentUnit);
    repository.delete(documentUnit);
    return "Dokumentationseinheit gelöscht: " + documentUnitUuid;
  }

  public RisJsonPatch updateDocumentUnit(UUID documentationUnitId, RisJsonPatch patch)
      throws JsonPatchException,
          JsonProcessingException,
          DocumentationUnitNotExistsException,
          PatchForSamePathException {

    DocumentUnit existingDocumentationUnit = getByUuid(documentationUnitId);

    long newVersion = 1L;
    if (existingDocumentationUnit.version() != null) {
      newVersion = existingDocumentationUnit.version() + 1;
    }

    /*

       RisJsonPatch newPatch =
        patchMapperService.calculatePatch(
            existingDocumentationUnit.uuid(), patch.documentationUnitVersion(), newVersion);
    List<String> errorPaths =
        patchMapperService.removePatchForSamePath(patch.patch(), newPatch.patch());
    patchMapperService.savePatch(
        patch, existingDocumentationUnit.uuid(), existingDocumentationUnit.version());


     */

    DocumentUnit updatedDocumentUnit =
        updateDocumentUnit(
            patchMapperService.applyPatchToEntity(
                patch.patch(), existingDocumentationUnit, DocumentUnit.class));

    MergeableJsonPatch mergeableJsonPatch =
        patchMapperService.getDiffPatch(existingDocumentationUnit, updatedDocumentUnit);
    log.info(mergeableJsonPatch.toString());

    return RisJsonPatch.builder()
        .documentationUnitVersion(newVersion)
        .patch(mergeableJsonPatch)
        .errorPaths(Collections.emptyList()) // TODO: Rehandle how we want to collect errors.
        .build();
  }

  public DocumentUnit updateDocumentUnit(DocumentUnit documentUnit)
      throws DocumentationUnitNotExistsException {
    repository.saveKeywords(documentUnit);
    repository.saveFieldsOfLaw(documentUnit);
    repository.saveProcedures(documentUnit);

    repository.save(documentUnit);

    return repository
        .findByUuid(documentUnit.uuid())
        .orElseThrow(() -> new DocumentationUnitNotExistsException(documentUnit.documentNumber()));
  }

  public Publication publishAsEmail(UUID documentUnitUuid, String issuerAddress)
      throws DocumentationUnitNotExistsException {

    DocumentUnit documentUnit =
        repository
            .findByUuid(documentUnitUuid)
            .orElseThrow(() -> new DocumentationUnitNotExistsException(documentUnitUuid));

    XmlPublication mailResponse = publicationService.publish(documentUnit, recipientAddress);
    if (mailResponse.getStatusCode().equals(String.valueOf(HttpStatus.OK.value()))) {
      documentUnitStatusService.setToPublishing(
          documentUnit, mailResponse.getPublishDate(), issuerAddress);
      return mailResponse;
    } else {
      return mailResponse;
    }
  }

  public XmlResultObject previewPublication(UUID documentUuid)
      throws DocumentationUnitNotExistsException {
    DocumentUnit documentUnit =
        repository
            .findByUuid(documentUuid)
            .orElseThrow(() -> new DocumentationUnitNotExistsException(documentUuid));
    return publicationService.getPublicationPreview(documentUnit);
  }

  public List<PublicationHistoryRecord> getPublicationHistory(UUID documentUuid) {
    List<PublicationHistoryRecord> list =
        ListUtils.union(
            publicationService.getPublications(documentUuid),
            publicationReportRepository.getAllByDocumentUnitUuid(documentUuid));
    var migration = deltaMigrationRepository.getLatestMigration(documentUuid);
    if (migration != null) {
      list.add(
          migration.xml() != null
              ? migration.toBuilder().xml(prettifyXml(migration.xml())).build()
              : migration);
    }
    list.sort(Comparator.comparing(PublicationHistoryRecord::getDate).reversed());
    return list;
  }

  public static String prettifyXml(String xml) {
    try {
      Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

      Node node =
          DocumentBuilderFactory.newDefaultInstance()
              .newDocumentBuilder()
              .parse(new ByteArrayInputStream(xml.getBytes()))
              .getDocumentElement();

      StreamResult result = new StreamResult(new StringWriter());
      transformer.transform(new DOMSource(node), result);
      return result.getWriter().toString();

    } catch (TransformerException | IOException | ParserConfigurationException | SAXException e) {
      return "Could not prettify XML";
    }
  }

  public Slice<RelatedDocumentationUnit> searchLinkableDocumentationUnits(
      RelatedDocumentationUnit relatedDocumentationUnit,
      DocumentationOffice documentationOffice,
      String documentNumberToExclude,
      Pageable pageable) {

    if (relatedDocumentationUnit.getFileNumber() != null) {
      relatedDocumentationUnit.setFileNumber(
          normalizeSpace(relatedDocumentationUnit.getFileNumber()));
    }
    return repository.searchLinkableDocumentationUnits(
        relatedDocumentationUnit, documentationOffice, documentNumberToExclude, pageable);
  }

  public String validateSingleNorm(SingleNormValidationInfo singleNormValidationInfo) {
    Set<ConstraintViolation<SingleNormValidationInfo>> violations =
        validator.validate(singleNormValidationInfo);

    if (violations.isEmpty()) {
      return "Ok";
    }
    return "Validation error";
  }

  public void updateECLI(UUID uuid, Docx2Html docx2html) {
    if (docx2html.ecliList().size() == 1) {
      repository.updateECLI(uuid, docx2html.ecliList().get(0));
    }
  }

  private void saveForRecycling(DocumentUnit documentUnit) {
    try {
      documentNumberRecyclingService.addForRecycling(
          documentUnit.uuid(),
          documentUnit.documentNumber(),
          documentUnit.coreData().documentationOffice().abbreviation());

    } catch (Exception e) {
      log.info("Did not save for recycling", e);
    }
  }
}
