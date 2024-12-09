package de.bund.digitalservice.ris.caselaw.adapter;

import static de.bund.digitalservice.ris.caselaw.domain.DateUtil.getYear;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDeletedDocumentationIdsRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DeletedDocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.domain.DocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverEntityType;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentNumberPatternException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentNumberRecyclingException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import java.time.Year;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service to handle deleted unpublished documentation units that can be recycled */
@Service
@Slf4j
public class DatabaseDocumentNumberRecyclingService implements DocumentNumberRecyclingService {

  private final DatabaseDeletedDocumentationIdsRepository repository;
  private final DatabaseDocumentationUnitRepository documentationUnitRepository;

  private final DocumentNumberPatternConfig documentNumberPatternConfig;

  private final HandoverService handoverService;

  public DatabaseDocumentNumberRecyclingService(
      DatabaseDeletedDocumentationIdsRepository repository,
      DocumentNumberPatternConfig documentNumberPatternConfig,
      HandoverService handoverService,
      DatabaseDocumentationUnitRepository documentationUnitRepository) {
    this.repository = repository;
    this.documentNumberPatternConfig = documentNumberPatternConfig;
    this.handoverService = handoverService;
    this.documentationUnitRepository = documentationUnitRepository;
  }

  /**
   * A method to track deleted unpublished document for reuse
   *
   * @param documentationUnitId uuid of the documentation unit
   * @param documentationUnitNumber following by the validation pattern of juris
   * @param documentationOfficeAbbreviation office abbreviation
   */
  @Override
  public void addForRecycling(
      UUID documentationUnitId,
      String documentationUnitNumber,
      String documentationOfficeAbbreviation)
      throws DocumentNumberPatternException,
          DocumentationUnitNotExistsException,
          DocumentNumberRecyclingException {

    try {

      assertPatternIsValid(documentationOfficeAbbreviation, documentationUnitNumber);

      var docUnit =
          documentationUnitRepository
              .findById(documentationUnitId)
              .orElseThrow(() -> new DocumentationUnitNotExistsException(documentationUnitId));

      assertDocumentationUnitHasNeverBeenHandedOverOrMigrated(documentationUnitId);

      assertStatusHasNeverBeenPublished(docUnit);

      var deleted =
          DeletedDocumentationUnitDTO.builder()
              .documentNumber(documentationUnitNumber)
              .year(getYear(docUnit.getStatus().getCreatedAt()))
              .abbreviation(documentationOfficeAbbreviation)
              .build();

      repository.save(deleted);

    } catch (Exception e) {
      log.info("Won´t reuse the document number", e);
      throw e;
    }
  }

  /**
   * Returns a reusable document number from the saved entries. It will remove it from the list in
   * case it does not meet the function condition.
   *
   * @param documentationOfficeAbbreviation the court abbreviation as it is saved in the deleted
   *     document numbers table.
   * @param year the year to search for, default is the current year.
   * @return a document number to reuse.
   */
  @Override
  public String recycleFromDeletedDocumentationUnit(
      String documentationOfficeAbbreviation, Year year)
      throws DocumentNumberPatternException, DocumentNumberRecyclingException {

    var optionalDeletedDocumentationUnitDTO =
        repository.findFirstByAbbreviationAndYear(documentationOfficeAbbreviation, year);

    try {
      if (optionalDeletedDocumentationUnitDTO.isEmpty()) {
        throw new DocumentNumberRecyclingException("No documentation number to reuse");
      }

      var deletedDocumentationUnitDTO = optionalDeletedDocumentationUnitDTO.get();

      if (documentationUnitRepository
          .findByDocumentNumber(deletedDocumentationUnitDTO.getDocumentNumber())
          .isPresent()) {
        throw new DocumentNumberRecyclingException(
            "Saved documentation number is currently in use");
      }

      assertPatternIsValid(
          documentationOfficeAbbreviation, deletedDocumentationUnitDTO.getDocumentNumber());

      return deletedDocumentationUnitDTO.getDocumentNumber();

    } catch (Exception e) {
      optionalDeletedDocumentationUnitDTO.ifPresent(
          deletedDocumentationUnitDTO ->
              repository.deleteById(deletedDocumentationUnitDTO.getDocumentNumber()));
      throw e;
    }
  }

  /**
   * Documentation unit number can be only reused if it has never been handed over or migrated from
   * jdv.
   *
   * @param documentationUnitId Uuid of the reused documentation unit
   * @throws DocumentNumberRecyclingException will throw error if has been ever used.
   */
  @Override
  public void assertDocumentationUnitHasNeverBeenHandedOverOrMigrated(UUID documentationUnitId)
      throws DocumentNumberRecyclingException {
    if (!handoverService
        .getEventLog(documentationUnitId, HandoverEntityType.DOCUMENTATION_UNIT)
        .isEmpty()) {
      throw new DocumentNumberRecyclingException(
          "This Documentation unit has already been handed over or migrated and therefore cannot be reused");
    }
  }

  /**
   * Validates that the documentationUnitNumber to be recycled meets the latest condition set in
   * documentNumberPatternConfig, that are driven from the yml format
   *
   * @param documentationOfficeAbbreviation office abbreviation
   * @param documentationUnitNumber documentation unit number
   * @throws DocumentNumberPatternException in case a pattern does not correspond to latest pattern.
   */
  @Override
  public void assertPatternIsValid(
      String documentationOfficeAbbreviation, String documentationUnitNumber)
      throws DocumentNumberPatternException {
    if (!documentNumberPatternConfig.hasValidPattern(
        documentationOfficeAbbreviation, documentationUnitNumber)) {
      throw new DocumentNumberPatternException("Pattern is invalid");
    }
  }

  /**
   * Validates that a documentation unit number has never been published or handed over by its
   * status
   *
   * @param documentationUnitDTO dto object
   * @throws DocumentNumberRecyclingException will through error if published / handed over or
   *     status history not empty.
   */
  public void assertStatusHasNeverBeenPublished(DocumentationUnitDTO documentationUnitDTO)
      throws DocumentNumberRecyclingException {
    PublicationStatus status = documentationUnitDTO.getStatus().getPublicationStatus();
    if (documentationUnitDTO.getStatusHistory().size() != 1
        || !(status.equals(PublicationStatus.UNPUBLISHED)
            || status.equals(PublicationStatus.EXTERNAL_HANDOVER_PENDING))) {
      throw new DocumentNumberRecyclingException(
          "Status has been changed once or neither unpublished nor handover pending");
    }
  }

  @Override
  public void delete(String documentNumber) {
    repository.deleteById(documentNumber);
  }
}
