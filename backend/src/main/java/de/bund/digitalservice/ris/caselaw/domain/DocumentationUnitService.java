package de.bund.digitalservice.ris.caselaw.domain;

import static de.bund.digitalservice.ris.caselaw.domain.StringUtils.normalizeSpace;

import com.gravity9.jsonpatch.JsonPatch;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitDeletionException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitPatchException;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DocumentationUnitService {

  private final DocumentationUnitRepository repository;
  private final DocumentNumberService documentNumberService;
  private final DocumentationUnitStatusService statusService;
  private final AttachmentService attachmentService;
  private final DocumentNumberRecyclingService documentNumberRecyclingService;
  private final PatchMapperService patchMapperService;
  private final AuthService authService;
  private final Validator validator;

  public DocumentationUnitService(
      DocumentationUnitRepository repository,
      DocumentNumberService documentNumberService,
      DocumentationUnitStatusService statusService,
      DocumentNumberRecyclingService documentNumberRecyclingService,
      Validator validator,
      AttachmentService attachmentService,
      @Lazy AuthService authService,
      PatchMapperService patchMapperService) {

    this.repository = repository;
    this.documentNumberService = documentNumberService;
    this.documentNumberRecyclingService = documentNumberRecyclingService;
    this.validator = validator;
    this.attachmentService = attachmentService;
    this.patchMapperService = patchMapperService;
    this.statusService = statusService;
    this.authService = authService;
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public DocumentationUnit generateNewDocumentationUnit(
      DocumentationOffice userDocOffice, Optional<DocumentationUnitCreationParameters> parameters)
      throws DocumentationUnitException {

    // default office is user office
    DocumentationUnitCreationParameters params =
        parameters.orElse(
            DocumentationUnitCreationParameters.builder()
                .documentationOffice(userDocOffice)
                .build());
    if (params.documentationOffice() == null) {
      params = params.toBuilder().documentationOffice(userDocOffice).build();
    }

    DocumentationUnit docUnit =
        DocumentationUnit.builder()
            .version(0L)
            .documentNumber(generateDocumentNumber(params.documentationOffice()))
            .coreData(
                CoreData.builder()
                    .documentationOffice(params.documentationOffice())
                    .fileNumbers(params.fileNumber() == null ? null : List.of(params.fileNumber()))
                    .documentType(params.documentType())
                    .decisionDate(params.decisionDate())
                    .court(params.court())
                    .creatingDocOffice(
                        params.documentationOffice() == null
                                || userDocOffice.uuid().equals(params.documentationOffice().uuid())
                            ? null
                            : userDocOffice)
                    .legalEffect(
                        LegalEffect.deriveFrom(params.court(), true)
                            .orElse(LegalEffect.NOT_SPECIFIED)
                            .getLabel())
                    .build())
            .build();

    Status status =
        Status.builder()
            .publicationStatus(
                userDocOffice.uuid().equals(docUnit.coreData().documentationOffice().uuid())
                    ? PublicationStatus.UNPUBLISHED
                    : PublicationStatus.EXTERNAL_HANDOVER_PENDING)
            .withError(false)
            .build();

    return repository.createNewDocumentationUnit(
        docUnit,
        status,
        params.reference(),
        params.reference() != null && params.reference().legalPeriodical() != null
            ? params.reference().legalPeriodical().abbreviation()
                + " "
                + params.reference().citation()
            : null);
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
      OidcUser oidcUser,
      Optional<String> documentNumber,
      Optional<String> fileNumber,
      Optional<String> courtType,
      Optional<String> courtLocation,
      Optional<LocalDate> decisionDate,
      Optional<LocalDate> decisionDateEnd,
      Optional<LocalDate> publicationDate,
      Optional<Boolean> scheduledOnly,
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
            .publicationDate(publicationDate.orElse(null))
            .scheduledOnly(scheduledOnly.orElse(false))
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

    Slice<DocumentationUnitListItem> documentationUnitListItems =
        repository.searchByDocumentationUnitSearchInput(
            PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
            oidcUser,
            searchInput);

    return documentationUnitListItems.map(item -> addPermissions(oidcUser, item));
  }

  public DocumentationUnitListItem takeOverDocumentationUnit(
      String documentNumber, OidcUser oidcUser) throws DocumentationUnitNotExistsException {

    statusService.update(
        documentNumber,
        Status.builder().publicationStatus(PublicationStatus.UNPUBLISHED).withError(false).build());

    return addPermissions(
        oidcUser, repository.findDocumentationUnitListItemByDocumentNumber(documentNumber));
  }

  public void setPublicationDateTime(UUID uuid) {
    repository.saveLastPublicationDateTime(uuid);
  }

  private DocumentationUnitListItem addPermissions(
      OidcUser oidcUser, DocumentationUnitListItem listItem) {

    boolean hasWriteAccess =
        authService.userHasWriteAccess(
            oidcUser,
            listItem.creatingDocumentationOffice(),
            listItem.documentationOffice(),
            listItem.status());
    boolean isInternalUser = authService.userIsInternal().apply(oidcUser);

    return listItem.toBuilder()
        .isDeletable(hasWriteAccess && isInternalUser)
        .isEditable(
            (hasWriteAccess
                && (isInternalUser || authService.isAssignedViaProcedure().apply(listItem.uuid()))))
        .build();
  }

  public DocumentationUnit getByDocumentNumber(String documentNumber)
      throws DocumentationUnitNotExistsException {
    return repository.findByDocumentNumber(documentNumber);
  }

  public DocumentationUnit getByUuid(UUID documentationUnitId)
      throws DocumentationUnitNotExistsException {
    return repository.findByUuid(documentationUnitId);
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public String deleteByUuid(UUID documentationUnitId) throws DocumentationUnitNotExistsException {

    Map<RelatedDocumentationType, Long> relatedEntities =
        repository.getAllDocumentationUnitWhichLink(documentationUnitId);

    if (!(relatedEntities == null
        || relatedEntities.isEmpty()
        || relatedEntities.values().stream().mapToLong(Long::longValue).sum() == 0)) {

      log.debug(
          "Could not delete document unit {} cause of related entities: {}",
          documentationUnitId,
          relatedEntities);

      throw new DocumentationUnitDeletionException(
          "Die Dokumentationseinheit konnte nicht gelöscht werden, da", relatedEntities);
    }

    DocumentationUnit documentationUnit = repository.findByUuid(documentationUnitId);

    log.debug("Deleting DocumentationUnitDTO " + documentationUnitId);

    if (documentationUnit.attachments() != null && !documentationUnit.attachments().isEmpty())
      attachmentService.deleteAllObjectsFromBucketForDocumentationUnit(documentationUnitId);

    saveForRecycling(documentationUnit);
    repository.delete(documentationUnit);
    return "Dokumentationseinheit gelöscht: " + documentationUnitId;
  }

  /**
   * Update a documenation unit with a {@link RisJsonPatch}.
   *
   * @param documentationUnitId id of the documentation unit
   * @param patch patch to update the documentation unit
   * @return a patch with changes the client not know yet (automatically set fields, fields update
   *     by other user)
   * @throws DocumentationUnitNotExistsException if the documentation unit not exist
   * @throws DocumentationUnitPatchException if the documentation unit couldn't updated
   */
  public RisJsonPatch updateDocumentationUnit(UUID documentationUnitId, RisJsonPatch patch)
      throws DocumentationUnitNotExistsException, DocumentationUnitPatchException {

    /*
     next iteration:
       * handle add operation with null values (remove of values from two users at the same time)
       * handle unique following operation (sometimes by add and remove operations at the same time)
    */

    DocumentationUnit existingDocumentationUnit = getByUuid(documentationUnitId);

    long newVersion = 1L;
    if (existingDocumentationUnit.version() != null) {
      newVersion = existingDocumentationUnit.version() + 1;
    }

    JsonPatch newPatch =
        patchMapperService.calculatePatch(
            existingDocumentationUnit.uuid(), patch.documentationUnitVersion());

    if (!patch.patch().getOperations().isEmpty() || !newPatch.getOperations().isEmpty()) {
      log.debug(
          "documentation unit '{}' with patch '{}' for version '{}'",
          documentationUnitId,
          patch.documentationUnitVersion(),
          patch.patch());
      log.debug("new version is {}", newVersion);
      log.debug("version {} - patch in database: {}", patch.documentationUnitVersion(), newPatch);
    }

    JsonPatch toFrontendJsonPatch = new JsonPatch(Collections.emptyList());
    RisJsonPatch toFrontend;
    if (!patch.patch().getOperations().isEmpty()) {
      JsonPatch toUpdate = patchMapperService.removePatchForSamePath(patch.patch(), newPatch);

      log.debug("version {} - update patch: {}", patch.documentationUnitVersion(), toUpdate);

      if (!toUpdate.getOperations().isEmpty()) {
        DocumentationUnit patchedDocumentationUnit =
            patchMapperService.applyPatchToEntity(toUpdate, existingDocumentationUnit);
        patchedDocumentationUnit = patchedDocumentationUnit.toBuilder().version(newVersion).build();
        DocumentationUnit updatedDocumentationUnit =
            updateDocumentationUnit(patchedDocumentationUnit);

        toFrontendJsonPatch =
            patchMapperService.getDiffPatch(patchedDocumentationUnit, updatedDocumentationUnit);

        log.debug(
            "version {} - raw to frontend patch: {}",
            patch.documentationUnitVersion(),
            toFrontendJsonPatch);

        JsonPatch toSaveJsonPatch =
            patchMapperService.addUpdatePatch(toUpdate, toFrontendJsonPatch);

        log.debug(
            "version {} - to save patch: {}", patch.documentationUnitVersion(), toSaveJsonPatch);

        patchMapperService.savePatch(
            toSaveJsonPatch, existingDocumentationUnit.uuid(), existingDocumentationUnit.version());
      }

      toFrontend =
          patchMapperService.handlePatchForSamePath(
              existingDocumentationUnit, toFrontendJsonPatch, patch.patch(), newPatch);

      log.debug(
          "version {} - cleaned to frontend patch: {}",
          patch.documentationUnitVersion(),
          toFrontend);

      if (toFrontend.errorPaths().isEmpty()) {
        toFrontend = toFrontend.toBuilder().documentationUnitVersion(newVersion).build();
      } else {
        toFrontend =
            toFrontend.toBuilder()
                .documentationUnitVersion(existingDocumentationUnit.version())
                .build();
      }

      log.debug(
          "version {} - second cleaned to frontend patch: {}",
          patch.documentationUnitVersion(),
          toFrontend);
    } else {
      if (newPatch == null) {
        newPatch = new JsonPatch(Collections.emptyList());
      }
      toFrontend =
          new RisJsonPatch(existingDocumentationUnit.version(), newPatch, Collections.emptyList());
    }

    return toFrontend;
  }

  public DocumentationUnit updateDocumentationUnit(DocumentationUnit documentationUnit)
      throws DocumentationUnitNotExistsException {
    repository.saveKeywords(documentationUnit);
    repository.saveFieldsOfLaw(documentationUnit);
    repository.saveProcedures(documentationUnit);

    repository.save(documentationUnit);

    return repository.findByUuid(documentationUnit.uuid());
  }

  public Slice<RelatedDocumentationUnit> searchLinkableDocumentationUnits(
      RelatedDocumentationUnit relatedDocumentationUnit,
      DocumentationOffice documentationOffice,
      Optional<String> documentNumberToExclude,
      Pageable pageable) {

    if (relatedDocumentationUnit.getFileNumber() != null) {
      relatedDocumentationUnit.setFileNumber(
          normalizeSpace(relatedDocumentationUnit.getFileNumber()));
    }
    return repository.searchLinkableDocumentationUnits(
        relatedDocumentationUnit,
        documentationOffice,
        documentNumberToExclude.orElse(null),
        pageable);
  }

  public String validateSingleNorm(SingleNormValidationInfo singleNormValidationInfo) {
    Set<ConstraintViolation<SingleNormValidationInfo>> violations =
        validator.validate(singleNormValidationInfo);

    if (violations.isEmpty()) {
      return "Ok";
    }
    return "Validation error";
  }

  private void saveForRecycling(DocumentationUnit documentationUnit) {
    try {
      documentNumberRecyclingService.addForRecycling(
          documentationUnit.uuid(),
          documentationUnit.documentNumber(),
          documentationUnit.coreData().documentationOffice().abbreviation());

    } catch (Exception e) {
      log.info("Did not save for recycling", e);
    }
  }
}
