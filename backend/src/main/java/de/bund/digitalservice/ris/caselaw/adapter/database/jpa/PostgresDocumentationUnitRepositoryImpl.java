package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentTypeTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationUnitListItemTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationUnitTransformer;
import de.bund.digitalservice.ris.caselaw.domain.ContentRelatedIndexing;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitListItem;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitSearchInput;
import de.bund.digitalservice.ris.caselaw.domain.Procedure;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationType;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.Status;
import de.bund.digitalservice.ris.caselaw.domain.StringUtils;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.repository.query.Param;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Implementation of the DocumentationUnitRepository for the Postgres database */
@Repository
@Slf4j
@Primary
public class PostgresDocumentationUnitRepositoryImpl implements DocumentationUnitRepository {
  private final DatabaseDocumentationUnitRepository repository;
  private final DatabaseCourtRepository databaseCourtRepository;
  private final DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  private final DatabaseKeywordRepository keywordRepository;
  private final DatabaseFieldOfLawRepository fieldOfLawRepository;
  private final DatabaseProcedureRepository procedureRepository;
  private final DatabaseRelatedDocumentationRepository relatedDocumentationRepository;
  private final UserService userService;

  public PostgresDocumentationUnitRepositoryImpl(
      DatabaseDocumentationUnitRepository repository,
      DatabaseCourtRepository databaseCourtRepository,
      DatabaseDocumentationOfficeRepository documentationOfficeRepository,
      DatabaseRelatedDocumentationRepository relatedDocumentationRepository,
      DatabaseKeywordRepository keywordRepository,
      DatabaseProcedureRepository procedureRepository,
      DatabaseFieldOfLawRepository fieldOfLawRepository,
      UserService userService) {

    this.repository = repository;
    this.databaseCourtRepository = databaseCourtRepository;
    this.documentationOfficeRepository = documentationOfficeRepository;
    this.keywordRepository = keywordRepository;
    this.relatedDocumentationRepository = relatedDocumentationRepository;
    this.fieldOfLawRepository = fieldOfLawRepository;
    this.procedureRepository = procedureRepository;
    this.userService = userService;
  }

  @Override
  @Transactional(transactionManager = "jpaTransactionManager")
  public DocumentationUnit findByDocumentNumber(String documentNumber)
      throws DocumentationUnitNotExistsException {
    var documentationUnit =
        repository
            .findByDocumentNumber(documentNumber)
            .orElseThrow(() -> new DocumentationUnitNotExistsException(documentNumber));
    return DocumentationUnitTransformer.transformToDomain(documentationUnit);
  }

  @Override
  @Transactional(transactionManager = "jpaTransactionManager")
  public DocumentationUnit findByUuid(UUID uuid) throws DocumentationUnitNotExistsException {
    var documentationUnit =
        repository.findById(uuid).orElseThrow(() -> new DocumentationUnitNotExistsException(uuid));
    return DocumentationUnitTransformer.transformToDomain(documentationUnit);
  }

  @Override
  public DocumentationUnit createNewDocumentationUnit(
      String documentNumber, DocumentationOffice documentationOffice) {

    var documentationOfficeDTO =
        documentationOfficeRepository.findByAbbreviation(documentationOffice.abbreviation());
    var documentationUnitDTO =
        DocumentationUnitDTO.builder()
            .documentNumber(documentNumber)
            .documentationOffice(documentationOfficeDTO)
            .legalEffect(LegalEffectDTO.KEINE_ANGABE)
            .version(0L)
            .build();

    documentationUnitDTO
        .getStatus()
        .add(
            StatusDTO.builder()
                .createdAt(Instant.now())
                .documentationUnitDTO(documentationUnitDTO)
                .publicationStatus(PublicationStatus.UNPUBLISHED)
                .withError(false)
                .build());

    documentationUnitDTO = repository.save(documentationUnitDTO);

    return DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  @Override
  public void save(DocumentationUnit documentationUnit) {

    DocumentationUnitDTO documentationUnitDTO =
        repository.findById(documentationUnit.uuid()).orElse(null);
    if (documentationUnitDTO == null) {
      log.info("Can't save non-existing docUnit with id = " + documentationUnit.uuid());
      return;
    }

    // ---
    // Doing database-related (pre) transformation

    if (documentationUnit.coreData() != null) {
      documentationUnitDTO.getRegions().clear();
      if (documentationUnit.coreData().court() != null
          && documentationUnit.coreData().court().id() != null) {
        Optional<CourtDTO> court =
            databaseCourtRepository.findById(documentationUnit.coreData().court().id());
        if (court.isPresent() && court.get().getRegion() != null) {
          documentationUnitDTO.getRegions().add(court.get().getRegion());
        }
        // delete leading decision norm references if court is not BGH
        if (court.isPresent() && !court.get().getType().equals("BGH")) {
          documentationUnit =
              documentationUnit.toBuilder()
                  .coreData(
                      documentationUnit.coreData().toBuilder()
                          .leadingDecisionNormReferences(List.of())
                          .build())
                  .build();
        }
      }
    }
    // ---

    // Transform non-database-related properties
    documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(documentationUnitDTO, documentationUnit);
    repository.save(documentationUnitDTO);
  }

  @Override
  public void saveKeywords(DocumentationUnit documentationUnit) {
    if (documentationUnit == null || documentationUnit.contentRelatedIndexing() == null) {
      return;
    }

    repository
        .findById(documentationUnit.uuid())
        .ifPresent(
            documentationUnitDTO -> {
              ContentRelatedIndexing contentRelatedIndexing =
                  documentationUnit.contentRelatedIndexing();

              if (contentRelatedIndexing.keywords() != null) {
                List<DocumentationUnitKeywordDTO> documentationUnitKeywordDTOs = new ArrayList<>();

                List<String> keywords = contentRelatedIndexing.keywords();
                for (int i = 0; i < keywords.size(); i++) {
                  String value = StringUtils.normalizeSpace(keywords.get(i));

                  KeywordDTO keywordDTO =
                      keywordRepository
                          .findByValue(value)
                          .orElseGet(
                              () ->
                                  keywordRepository.save(
                                      KeywordDTO.builder().value(value).build()));

                  DocumentationUnitKeywordDTO documentationUnitKeywordDTO =
                      DocumentationUnitKeywordDTO.builder()
                          .primaryKey(
                              new DocumentationUnitKeywordId(
                                  documentationUnitDTO.getId(), keywordDTO.getId()))
                          .documentationUnit(documentationUnitDTO)
                          .keyword(keywordDTO)
                          .rank(i + 1)
                          .build();

                  documentationUnitKeywordDTOs.add(documentationUnitKeywordDTO);
                }

                documentationUnitDTO.setDocumentationUnitKeywordDTOs(documentationUnitKeywordDTOs);

                repository.save(documentationUnitDTO);
              }
            });
  }

  @Override
  public void saveFieldsOfLaw(DocumentationUnit documentationUnit) {
    if (documentationUnit == null || documentationUnit.contentRelatedIndexing() == null) {
      return;
    }

    repository
        .findById(documentationUnit.uuid())
        .ifPresent(
            documentationUnitDTO -> {
              ContentRelatedIndexing contentRelatedIndexing =
                  documentationUnit.contentRelatedIndexing();

              if (contentRelatedIndexing.fieldsOfLaw() != null) {
                List<DocumentationUnitFieldOfLawDTO> documentationUnitFieldOfLawDTOs =
                    new ArrayList<>();

                List<FieldOfLaw> fieldsOfLaw = contentRelatedIndexing.fieldsOfLaw();
                for (int i = 0; i < fieldsOfLaw.size(); i++) {
                  FieldOfLaw fieldOfLaw = fieldsOfLaw.get(i);

                  Optional<FieldOfLawDTO> fieldOfLawDTOOptional =
                      fieldOfLawRepository.findById(fieldOfLaw.id());

                  if (fieldOfLawDTOOptional.isPresent()) {
                    DocumentationUnitFieldOfLawDTO documentationUnitFieldOfLawDTO =
                        DocumentationUnitFieldOfLawDTO.builder()
                            .primaryKey(
                                new DocumentationUnitFieldOfLawId(
                                    documentationUnitDTO.getId(),
                                    fieldOfLawDTOOptional.get().getId()))
                            .rank(i + 1)
                            .build();
                    documentationUnitFieldOfLawDTO.setDocumentationUnit(documentationUnitDTO);
                    documentationUnitFieldOfLawDTO.setFieldOfLaw(fieldOfLawDTOOptional.get());

                    documentationUnitFieldOfLawDTOs.add(documentationUnitFieldOfLawDTO);
                  } else {
                    throw new DocumentationUnitException(
                        "field of law with id: '" + fieldOfLaw.id() + "' not found.");
                  }
                }

                documentationUnitDTO.setDocumentationUnitFieldsOfLaw(
                    documentationUnitFieldOfLawDTOs);

                repository.save(documentationUnitDTO);
              }
            });
  }

  @Override
  @Transactional(transactionManager = "jpaTransactionManager")
  public void saveProcedures(DocumentationUnit documentationUnit) {
    if (documentationUnit == null
        || documentationUnit.coreData() == null
        || documentationUnit.coreData().procedure() == null) {
      return;
    }

    var documentationUnitDTOOptional = repository.findById(documentationUnit.uuid());
    if (documentationUnitDTOOptional.isEmpty()) {
      return;
    }
    var documentationUnitDTO = documentationUnitDTOOptional.get();
    Procedure procedure = documentationUnit.coreData().procedure();

    List<DocumentationUnitProcedureDTO> documentationUnitProcedureDTOs = new ArrayList<>();

    ProcedureDTO procedureDTO =
        getOrCreateProcedure(documentationUnitDTO.getDocumentationOffice(), procedure);

    boolean sameAsLast =
        !documentationUnitDTO.getProcedures().isEmpty()
            && documentationUnitDTO
                .getProcedures()
                .get(documentationUnitDTO.getProcedures().size() - 1)
                .getProcedure()
                .equals(procedureDTO);

    documentationUnitDTO
        .getProcedures()
        .forEach(
            documentationUnitProcedureDTO -> {
              DocumentationUnitProcedureDTO newLink =
                  DocumentationUnitProcedureDTO.builder()
                      .primaryKey(
                          new DocumentationUnitProcedureId(
                              documentationUnitDTO.getId(),
                              documentationUnitProcedureDTO.getProcedure().getId()))
                      .documentationUnit(documentationUnitDTO)
                      .procedure(documentationUnitProcedureDTO.getProcedure())
                      .build();
              documentationUnitProcedureDTOs.add(newLink);
            });

    if (procedureDTO != null && !sameAsLast) {
      DocumentationUnitProcedureDTO documentationUnitProcedureDTO =
          DocumentationUnitProcedureDTO.builder()
              .primaryKey(
                  new DocumentationUnitProcedureId(
                      documentationUnitDTO.getId(), procedureDTO.getId()))
              .documentationUnit(documentationUnitDTO)
              .procedure(procedureDTO)
              .build();
      documentationUnitProcedureDTOs.add(documentationUnitProcedureDTO);
    }

    updateProcedureRank(documentationUnitProcedureDTOs);

    documentationUnitDTO.getProcedures().clear();
    documentationUnitDTO.getProcedures().addAll(documentationUnitProcedureDTOs);

    repository.save(documentationUnitDTO);
  }

  private ProcedureDTO getOrCreateProcedure(
      DocumentationOfficeDTO documentationOfficeDTO, Procedure procedure) {
    if (procedure.id() == null) {
      Optional<ProcedureDTO> existingProcedure =
          procedureRepository.findAllByLabelAndDocumentationOffice(
              StringUtils.normalizeSpace(procedure.label()), documentationOfficeDTO);

      return existingProcedure.orElseGet(
          () ->
              procedureRepository.save(
                  ProcedureDTO.builder()
                      .label(StringUtils.normalizeSpace(procedure.label()))
                      .createdAt(Instant.now())
                      .documentationOffice(documentationOfficeDTO)
                      .build()));
    }
    return procedureRepository.findById(procedure.id()).orElse(null);
  }

  private void updateProcedureRank(
      List<DocumentationUnitProcedureDTO> documentationUnitProcedureDTOs) {
    for (int i = 0; i < documentationUnitProcedureDTOs.size(); i++) {
      DocumentationUnitProcedureDTO documentationUnitProcedureDTO =
          documentationUnitProcedureDTOs.get(i);
      documentationUnitProcedureDTO.setRank(i + 1);
    }
  }

  @Override
  public void delete(DocumentationUnit documentationUnit) {
    repository.deleteById(documentationUnit.uuid());
  }

  @Override
  @Transactional(transactionManager = "jpaTransactionManager")
  public Slice<RelatedDocumentationUnit> searchLinkableDocumentationUnits(
      RelatedDocumentationUnit relatedDocumentationUnit,
      DocumentationOffice documentationOffice,
      String documentNumberToExclude,
      Pageable pageable) {
    String courtType =
        Optional.ofNullable(relatedDocumentationUnit.getCourt()).map(Court::type).orElse(null);
    String courtLocation =
        Optional.ofNullable(relatedDocumentationUnit.getCourt()).map(Court::location).orElse(null);

    DocumentationOfficeDTO documentationOfficeDTO =
        documentationOfficeRepository.findByAbbreviation(documentationOffice.abbreviation());

    Slice<DocumentationUnitListItemDTO> allResults =
        getDocumentationUnitSearchResultDTOS(
            pageable,
            courtType,
            courtLocation,
            null,
            documentNumberToExclude,
            relatedDocumentationUnit.getFileNumber(),
            relatedDocumentationUnit.getDecisionDate(),
            null,
            null,
            false,
            false,
            relatedDocumentationUnit.getDocumentType(),
            documentationOfficeDTO);

    return allResults.map(DocumentationUnitListItemTransformer::transformToRelatedDocumentation);
  }

  @NotNull
  @SuppressWarnings("java:S107")
  private Slice<DocumentationUnitListItemDTO> getDocumentationUnitSearchResultDTOS(
      Pageable pageable,
      String courtType,
      String courtLocation,
      String documentNumber,
      String documentNumberToExclude,
      String fileNumber,
      LocalDate decisionDate,
      LocalDate decisionDateEnd,
      PublicationStatus status,
      Boolean withError,
      Boolean myDocOfficeOnly,
      DocumentType documentType,
      DocumentationOfficeDTO documentationOfficeDTO) {
    if ((fileNumber == null || fileNumber.trim().isEmpty())) {
      return repository.searchByDocumentationUnitSearchInput(
          documentationOfficeDTO.getId(),
          documentNumber,
          documentNumberToExclude,
          courtType,
          courtLocation,
          decisionDate,
          decisionDateEnd,
          status,
          withError,
          myDocOfficeOnly,
          DocumentTypeTransformer.transformToDTO(documentType),
          pageable);
    }

    // The highest possible number of results - For page 0: 30, for page 1: 60, for page 2: 90, etc.
    int maxResultsUpToCurrentPage = (pageable.getPageNumber() + 1) * pageable.getPageSize();

    // We need to start with index 0 because we collect 3 results sets, each of the desired size of
    // the page. Then we sort the full ist and cut it to the page size (possibly leaving 2x page
    // size results behind). If we don't always start with index 0, we might miss results.
    // This approach could even be better if we replace the next/previous with a "load more" button
    Pageable fixedPageRequest = PageRequest.of(0, maxResultsUpToCurrentPage);

    Slice<DocumentationUnitListItemDTO> fileNumberResults = new SliceImpl<>(List.of());
    Slice<DocumentationUnitListItemDTO> deviatingFileNumberResults = new SliceImpl<>(List.of());

    if (!fileNumber.trim().isEmpty()) {
      fileNumberResults =
          repository.searchByDocumentationUnitSearchInputFileNumber(
              documentationOfficeDTO.getId(),
              documentNumber,
              documentNumberToExclude,
              fileNumber.trim(),
              courtType,
              courtLocation,
              decisionDate,
              decisionDateEnd,
              status,
              withError,
              myDocOfficeOnly,
              DocumentTypeTransformer.transformToDTO(documentType),
              fixedPageRequest);

      deviatingFileNumberResults =
          repository.searchByDocumentationUnitSearchInputDeviatingFileNumber(
              documentationOfficeDTO.getId(),
              documentNumber,
              documentNumberToExclude,
              fileNumber.trim(),
              courtType,
              courtLocation,
              decisionDate,
              decisionDateEnd,
              status,
              withError,
              myDocOfficeOnly,
              DocumentTypeTransformer.transformToDTO(documentType),
              fixedPageRequest);
    }

    Set<DocumentationUnitListItemDTO> allResults = new HashSet<>();

    allResults.addAll(fileNumberResults.getContent());
    allResults.addAll(deviatingFileNumberResults.getContent());

    // We can provide entries for a next page if ...
    // A) we already have collected more results than fit on the current page, or
    // B) at least one of the queries has more results
    boolean hasNext =
        allResults.size() >= maxResultsUpToCurrentPage
            || fileNumberResults.hasNext()
            || deviatingFileNumberResults.hasNext();

    return new SliceImpl<>(
        allResults.stream()
            .sorted(
                (o1, o2) -> {
                  if (o1.getDecisionDate() != null && o2.getDecisionDate() != null) {
                    return o1.getDocumentNumber().compareTo(o2.getDocumentNumber());
                  }
                  return 0;
                })
            .toList()
            .subList(
                pageable.getPageNumber() * pageable.getPageSize(),
                Math.min(allResults.size(), maxResultsUpToCurrentPage)),
        pageable,
        hasNext);
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public Slice<DocumentationUnitListItem> searchByDocumentationUnitSearchInput(
      Pageable pageable,
      OidcUser oidcUser,
      @Param("searchInput") DocumentationUnitSearchInput searchInput) {

    DocumentationOffice documentationOffice = userService.getDocumentationOffice(oidcUser);
    log.debug("Find by overview search: {}, {}", documentationOffice.abbreviation(), searchInput);

    DocumentationOfficeDTO documentationOfficeDTO =
        documentationOfficeRepository.findByAbbreviation(documentationOffice.abbreviation());

    Boolean withError =
        Optional.ofNullable(searchInput.status()).map(Status::withError).orElse(false);

    Slice<DocumentationUnitListItemDTO> allResults =
        getDocumentationUnitSearchResultDTOS(
            pageable,
            searchInput.courtType(),
            searchInput.courtLocation(),
            searchInput.documentNumber(),
            null,
            searchInput.fileNumber(),
            searchInput.decisionDate(),
            searchInput.decisionDateEnd(),
            searchInput.status() != null ? searchInput.status().publicationStatus() : null,
            withError,
            searchInput.myDocOfficeOnly(),
            null,
            documentationOfficeDTO);

    var userGroup = userService.getUserGroup(oidcUser);
    boolean isInternalUser = userService.isInternal(oidcUser);
    List<ProcedureDTO> assignedProcedures;
    if (userGroup.isPresent() && !isInternalUser) {
      assignedProcedures = procedureRepository.findAllByUserGroupDTO_Id(userGroup.get().id());
    } else {
      assignedProcedures = List.of();
    }
    return allResults.map(
        item ->
            DocumentationUnitListItemTransformer.transformToDomain(item).toBuilder()
                .isDeletable(
                    hasSameDocumentationOffice(item, documentationOffice) && isInternalUser)
                .isEditable(
                    hasSameDocumentationOffice(item, documentationOffice)
                        && (isInternalUser || isUserAssigned(assignedProcedures, item)))
                .build());
  }

  private boolean hasSameDocumentationOffice(
      DocumentationUnitListItemDTO item, DocumentationOffice documentationOffice) {
    return item.getDocumentationOffice().getId().equals(documentationOffice.uuid());
  }

  private boolean isUserAssigned(
      List<ProcedureDTO> assignedProcedures, DocumentationUnitListItemDTO item) {
    if (!item.getProcedures().isEmpty()) {
      var docUnitProcedureId = item.getProcedures().get(0).getProcedure().getId();
      return assignedProcedures.stream()
          .anyMatch(procedure -> procedure.getId().equals(docUnitProcedureId));
    } else {
      return false;
    }
  }

  @Override
  public Map<RelatedDocumentationType, Long> getAllDocumentationUnitWhichLink(
      UUID documentationUnitId) {
    return relatedDocumentationRepository
        .findAllByReferencedDocumentationUnitId(documentationUnitId)
        .stream()
        .collect(Collectors.groupingBy(RelatedDocumentationDTO::getType, Collectors.counting()));
  }
}
