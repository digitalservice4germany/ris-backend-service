package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DecisionNameDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DeviatingCourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DeviatingDateDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DeviatingEcliDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DeviatingFileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO.DocumentationUnitDTOBuilder;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.EnsuingDecisionDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.FileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.InputTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.JobProfileDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LeadingDecisionNormReferenceDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalEffectDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.NormReferenceDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PendingDecisionDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.StatusDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.YearOfDisputeDTO;
import de.bund.digitalservice.ris.caselaw.domain.ContentRelatedIndexing;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.CoreData.CoreDataBuilder;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.EnsuingDecision;
import de.bund.digitalservice.ris.caselaw.domain.LegalEffect;
import de.bund.digitalservice.ris.caselaw.domain.NormReference;
import de.bund.digitalservice.ris.caselaw.domain.PreviousDecision;
import de.bund.digitalservice.ris.caselaw.domain.SingleNorm;
import de.bund.digitalservice.ris.caselaw.domain.StringUtils;
import de.bund.digitalservice.ris.caselaw.domain.Texts;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * This class is responsible for transforming a documentation unit object from its domain
 * representation into a database object and back
 */
@Slf4j
public class DocumentationUnitTransformer {
  private DocumentationUnitTransformer() {}

  /**
   * Transforms a documentation unit object from its domain representation into a database object
   *
   * @param currentDto the current database documentation unit
   * @param updatedDomainObject the updated domain object, e.g. by a REST call
   * @return a transformed database object containing the changes from the @param
   *     updatedDomainObject
   */
  public static DocumentationUnitDTO transformToDTO(
      DocumentationUnitDTO currentDto, DocumentationUnit updatedDomainObject) {

    log.debug("transform database documentation unit '{}'", currentDto.getId());

    DocumentationUnitDTO.DocumentationUnitDTOBuilder builder =
        currentDto.toBuilder()
            .id(updatedDomainObject.uuid())
            .documentNumber(updatedDomainObject.documentNumber())
            .note(updatedDomainObject.note())
            .version(updatedDomainObject.version());

    if (updatedDomainObject.coreData() != null) {
      var coreData = updatedDomainObject.coreData();

      builder
          .ecli(StringUtils.normalizeSpace(coreData.ecli()))
          .judicialBody(StringUtils.normalizeSpace(coreData.appraisalBody()))
          .decisionDate(coreData.decisionDate())
          .documentType(
              coreData.documentType() != null
                  ? DocumentTypeTransformer.transformToDTO(coreData.documentType())
                  : null)
          .court(CourtTransformer.transformToDTO(coreData.court()));

      addInputTypes(builder, coreData);
      addFileNumbers(currentDto, builder, coreData);
      addDeviationCourts(builder, coreData);
      addDeviatingDecisionDates(builder, coreData);
      addDeviatingFileNumbers(currentDto, builder, coreData);
      addDeviatingEclis(builder, coreData);
      addLegalEffect(currentDto, updatedDomainObject, builder);
      addLeadingDecisionNormReferences(updatedDomainObject, builder);
      addYearsOfDisputeToDTO(builder, coreData);

    } else {
      builder
          .procedures(Collections.emptyList())
          .ecli(null)
          .judicialBody(null)
          .decisionDate(null)
          .court(null)
          .documentType(null)
          .documentationOffice(null)
          .yearsOfDispute(null);
    }

    addPreviousDecisions(updatedDomainObject, builder);
    addEnsuingAndPendingDecisions(updatedDomainObject, builder);

    if (updatedDomainObject.contentRelatedIndexing() != null) {
      ContentRelatedIndexing contentRelatedIndexing = updatedDomainObject.contentRelatedIndexing();

      addActiveCitations(builder, contentRelatedIndexing);
      addNormReferences(builder, contentRelatedIndexing);
      addJobProfiles(builder, contentRelatedIndexing);
    }

    if (updatedDomainObject.texts() != null) {
      addTexts(updatedDomainObject, builder);
    } else {
      builder
          .decisionNames(Collections.emptyList())
          .headline(null)
          .guidingPrinciple(null)
          .headnote(null)
          .otherHeadnote(null)
          .tenor(null)
          .grounds(null)
          .caseFacts(null)
          .decisionGrounds(null)
          .dissentingOpinion(null)
          .otherLongText(null)
          .outline(null);
    }

    addReferences(updatedDomainObject, builder);

    return builder.build();
  }

  private static void addReferences(
      DocumentationUnit updatedDomainObject, DocumentationUnitDTOBuilder builder) {
    AtomicInteger i = new AtomicInteger(1);
    builder.references(
        updatedDomainObject.references() == null
            ? Collections.emptyList()
            : updatedDomainObject.references().stream()
                .map(ReferenceTransformer::transformToDTO)
                .map(
                    referenceDTO -> {
                      // TODO why is this necessary?
                      referenceDTO.setDocumentationUnit(builder.build());
                      referenceDTO.setRank(i.getAndIncrement());
                      return referenceDTO;
                    })
                .toList());
  }

  private static void addTexts(
      DocumentationUnit updatedDomainObject, DocumentationUnitDTOBuilder builder) {
    Texts texts = updatedDomainObject.texts();

    builder
        .headline(texts.headline())
        .guidingPrinciple(texts.guidingPrinciple())
        .headnote(texts.headnote())
        .otherHeadnote(texts.otherHeadnote())
        .tenor(texts.tenor())
        .grounds(texts.reasons())
        .caseFacts(texts.caseFacts())
        .decisionGrounds(texts.decisionReasons())
        .dissentingOpinion(texts.dissentingOpinion())
        .otherLongText(texts.otherLongText())
        .outline(texts.outline());

    if (texts.decisionName() != null) {
      // Todo multiple decision names?
      builder.decisionNames(List.of(DecisionNameDTO.builder().value(texts.decisionName()).build()));
    } else {
      builder.decisionNames(Collections.emptyList());
    }
  }

  /**
   * Adds norm references to the documentation unit builder based on the provided content-related
   * indexing information. Each {@link de.bund.digitalservice.ris.caselaw.domain.SingleNorm} are
   * grouped in a list of single norms, according to the associated norm abbreviation and packed
   * into a {@link NormReference}. When converting into a DTO object, each single norm in the
   * normReference is converted into its own {@link NormReferenceDTO}. In order for JPA to be able
   * to correctly link the legal force of each NormReferenceDTO, it must be explicitly set again.
   * (Because legal force is the owning side of the one to one connection, it is not implicitly
   * linked by jpa, when a norm with legal force is saved.)
   *
   * @param builder The builder for constructing the documentation unit DTO.
   * @param contentRelatedIndexing The content-related indexing information containing the norms.
   */
  private static void addNormReferences(
      DocumentationUnitDTOBuilder builder, ContentRelatedIndexing contentRelatedIndexing) {
    if (contentRelatedIndexing.norms() == null) {
      return;
    }

    AtomicInteger i = new AtomicInteger(1);
    List<NormReferenceDTO> flattenNormReferenceDTOs = new ArrayList<>();
    contentRelatedIndexing
        .norms()
        .forEach(
            norm -> {
              List<NormReferenceDTO> normReferenceDTOs =
                  NormReferenceTransformer.transformToDTO(norm);
              normReferenceDTOs.forEach(
                  normReferenceDTO -> normReferenceDTO.setRank(i.getAndIncrement()));
              flattenNormReferenceDTOs.addAll(normReferenceDTOs);
            });

    flattenNormReferenceDTOs.forEach(
        normReferenceDTO -> {
          if (normReferenceDTO.getLegalForce() != null)
            normReferenceDTO.getLegalForce().setNormReference(normReferenceDTO);
        });

    builder.normReferences(flattenNormReferenceDTOs);
  }

  private static void addActiveCitations(
      DocumentationUnitDTOBuilder builder, ContentRelatedIndexing contentRelatedIndexing) {
    if (contentRelatedIndexing.activeCitations() == null) {
      return;
    }

    AtomicInteger i = new AtomicInteger(1);
    builder.activeCitations(
        contentRelatedIndexing.activeCitations().stream()
            .map(ActiveCitationTransformer::transformToDTO)
            .filter(Objects::nonNull)
            .map(
                activeCitationDTO -> {
                  activeCitationDTO.setRank(i.getAndIncrement());
                  return activeCitationDTO;
                })
            .toList());
  }

  private static void addJobProfiles(
      DocumentationUnitDTOBuilder builder, ContentRelatedIndexing contentRelatedIndexing) {
    if (contentRelatedIndexing.jobProfiles() == null) {
      return;
    }

    List<JobProfileDTO> jobProfileDTOs = new ArrayList<>();
    List<String> jobProfiles = contentRelatedIndexing.jobProfiles().stream().distinct().toList();

    for (int i = 0; i < jobProfiles.size(); i++) {
      jobProfileDTOs.add(JobProfileDTO.builder().value(jobProfiles.get(i)).rank(i + 1L).build());
    }

    builder.jobProfiles(jobProfileDTOs);
  }

  private static void addEnsuingAndPendingDecisions(
      DocumentationUnit updatedDomainObject, DocumentationUnitDTOBuilder builder) {
    List<EnsuingDecision> ensuingDecisions = updatedDomainObject.ensuingDecisions();
    if (ensuingDecisions != null) {

      List<EnsuingDecisionDTO> ensuingDecisionDTOs = new ArrayList<>();
      List<PendingDecisionDTO> pendingDecisionDTOs = new ArrayList<>();

      AtomicInteger i = new AtomicInteger(1);
      for (EnsuingDecision ensuingDecision : ensuingDecisions) {
        if (ensuingDecision.isPending()) {
          PendingDecisionDTO pendingDecisionDTO =
              PendingDecisionTransformer.transformToDTO(ensuingDecision);
          if (pendingDecisionDTO != null) {
            pendingDecisionDTO.setRank(i.getAndIncrement());
            pendingDecisionDTOs.add(pendingDecisionDTO);
          }
        } else {
          EnsuingDecisionDTO ensuingDecisionDTO =
              EnsuingDecisionTransformer.transformToDTO(ensuingDecision);
          if (ensuingDecisionDTO != null) {
            ensuingDecisionDTO.setRank(i.getAndIncrement());
            ensuingDecisionDTOs.add(ensuingDecisionDTO);
          }
        }
      }

      builder.ensuingDecisions(ensuingDecisionDTOs.stream().toList());
      builder.pendingDecisions(pendingDecisionDTOs.stream().toList());
    }
  }

  private static void addPreviousDecisions(
      DocumentationUnit updatedDomainObject, DocumentationUnitDTOBuilder builder) {
    List<PreviousDecision> previousDecisions = updatedDomainObject.previousDecisions();
    if (previousDecisions != null) {
      AtomicInteger i = new AtomicInteger(1);
      builder.previousDecisions(
          previousDecisions.stream()
              .map(PreviousDecisionTransformer::transformToDTO)
              .filter(Objects::nonNull)
              .map(
                  previousDecisionDTO -> {
                    previousDecisionDTO.setRank(i.getAndIncrement());
                    return previousDecisionDTO;
                  })
              .toList());
    }
  }

  private static void addLegalEffect(
      DocumentationUnitDTO currentDto,
      DocumentationUnit updatedDomainObject,
      DocumentationUnitDTOBuilder builder) {

    boolean courtWasAdded =
        currentDto.getCourt() == null
            && updatedDomainObject.coreData() != null
            && updatedDomainObject.coreData().court() != null;
    boolean courtWasDeleted =
        currentDto.getCourt() != null
            && (updatedDomainObject.coreData() == null
                || updatedDomainObject.coreData().court() == null);
    boolean courtHasChanged =
        currentDto.getCourt() != null
            && updatedDomainObject.coreData() != null
            && updatedDomainObject.coreData().court() != null
            && !updatedDomainObject.coreData().court().id().equals(currentDto.getCourt().getId());

    var legalEffect =
        LegalEffect.deriveFrom(
            updatedDomainObject, courtWasAdded || courtWasDeleted || courtHasChanged);

    LegalEffectDTO legalEffectDTO = LegalEffectDTO.FALSCHE_ANGABE;
    if (legalEffect != null) {
      switch (legalEffect) {
        case NO -> legalEffectDTO = LegalEffectDTO.NEIN;
        case YES -> legalEffectDTO = LegalEffectDTO.JA;
        case NOT_SPECIFIED -> legalEffectDTO = LegalEffectDTO.KEINE_ANGABE;
      }
    }

    builder.legalEffect(legalEffectDTO);
  }

  private static void addLeadingDecisionNormReferences(
      DocumentationUnit updatedDomainObject, DocumentationUnitDTOBuilder builder) {

    List<String> leadingDecisionNormReferences =
        updatedDomainObject.coreData().leadingDecisionNormReferences();
    if (leadingDecisionNormReferences != null) {
      AtomicInteger i = new AtomicInteger(1);
      builder.leadingDecisionNormReferences(
          leadingDecisionNormReferences.stream()
              .map(
                  normReference ->
                      LeadingDecisionNormReferenceDTO.builder()
                          .normReference(StringUtils.normalizeSpace(normReference))
                          .rank(i.getAndIncrement())
                          .build())
              .toList());
    } else {
      builder.leadingDecisionNormReferences(Collections.emptyList());
    }
  }

  private static void addDeviatingEclis(DocumentationUnitDTOBuilder builder, CoreData coreData) {
    if (coreData.deviatingEclis() == null) {
      return;
    }

    List<DeviatingEcliDTO> deviatingEcliDTOs = new ArrayList<>();
    List<String> deviatingEclis = coreData.deviatingEclis();

    for (int i = 0; i < deviatingEclis.size(); i++) {
      deviatingEcliDTOs.add(
          DeviatingEcliDTO.builder()
              .value(StringUtils.normalizeSpace(deviatingEclis.get(i)))
              .rank(i + 1L)
              .build());
    }

    builder.deviatingEclis(deviatingEcliDTOs);
  }

  private static void addDeviatingFileNumbers(
      DocumentationUnitDTO currentDto, DocumentationUnitDTOBuilder builder, CoreData coreData) {
    if (coreData.deviatingFileNumbers() == null) {
      return;
    }

    List<DeviatingFileNumberDTO> deviatingFileNumberDTOs = new ArrayList<>();
    List<String> deviatingFileNumbers = coreData.deviatingFileNumbers();

    for (int i = 0; i < deviatingFileNumbers.size(); i++) {
      deviatingFileNumberDTOs.add(
          DeviatingFileNumberDTO.builder()
              .value(StringUtils.normalizeSpace(deviatingFileNumbers.get(i)))
              .rank(i + 1L)
              .documentationUnit(currentDto)
              .build());
    }

    builder.deviatingFileNumbers(deviatingFileNumberDTOs);
  }

  private static void addDeviatingDecisionDates(
      DocumentationUnitDTOBuilder builder, CoreData coreData) {
    if (coreData.deviatingDecisionDates() == null) {
      return;
    }

    List<DeviatingDateDTO> deviatingDateDTOs = new ArrayList<>();
    List<LocalDate> deviatingDecisionDates = coreData.deviatingDecisionDates();

    for (int i = 0; i < deviatingDecisionDates.size(); i++) {
      deviatingDateDTOs.add(
          DeviatingDateDTO.builder().value(deviatingDecisionDates.get(i)).rank(i + 1L).build());
    }

    builder.deviatingDates(deviatingDateDTOs);
  }

  private static void addDeviationCourts(DocumentationUnitDTOBuilder builder, CoreData coreData) {
    if (coreData.deviatingCourts() == null) {
      return;
    }

    List<DeviatingCourtDTO> deviatingCourtDTOs = new ArrayList<>();
    List<String> deviatingCourts = coreData.deviatingCourts();

    for (int i = 0; i < deviatingCourts.size(); i++) {
      deviatingCourtDTOs.add(
          DeviatingCourtDTO.builder()
              .value(StringUtils.normalizeSpace(deviatingCourts.get(i)))
              .rank(i + 1L)
              .build());
    }

    builder.deviatingCourts(deviatingCourtDTOs);
  }

  private static void addInputTypes(DocumentationUnitDTOBuilder builder, CoreData coreData) {
    if (coreData.inputTypes() == null) {
      return;
    }

    List<InputTypeDTO> inputTypeDTOs = new ArrayList<>();
    List<String> inputTypes = coreData.inputTypes();

    for (int i = 0; i < inputTypes.size(); i++) {
      inputTypeDTOs.add(
          InputTypeDTO.builder()
              .value(StringUtils.normalizeSpace(inputTypes.get(i)))
              .rank(i + 1L)
              .build());
    }

    builder.inputTypes(inputTypeDTOs);
  }

  private static void addFileNumbers(
      DocumentationUnitDTO currentDto, DocumentationUnitDTOBuilder builder, CoreData coreData) {
    if (coreData.fileNumbers() == null) {
      return;
    }

    List<FileNumberDTO> fileNumberDTOs = new ArrayList<>();
    List<String> fileNumbers = coreData.fileNumbers();

    for (int i = 0; i < fileNumbers.size(); i++) {
      fileNumberDTOs.add(
          FileNumberDTO.builder()
              .value(StringUtils.normalizeSpace(fileNumbers.get(i)))
              .rank(i + 1L)
              .documentationUnit(currentDto)
              .build());
    }

    builder.fileNumbers(fileNumberDTOs);
  }

  /**
   * Transforms a documentation unit object from its database representation into a domain object
   * that is suitable to be consumed by clients of the REST service.
   *
   * @param documentationUnitDTO the database documentation unit
   * @return a transformed domain object, or an empty domain object if the input is null
   */
  public static DocumentationUnit transformToDomain(DocumentationUnitDTO documentationUnitDTO) {
    if (documentationUnitDTO == null) {
      throw new DocumentationUnitTransformerException("Document unit is null and won't transform");
    }

    log.debug(
        "transfer database documentation unit '{}' to domain object", documentationUnitDTO.getId());

    LegalEffect legalEffect = getLegalEffectForDomain(documentationUnitDTO);

    DocumentationUnit.DocumentationUnitBuilder builder =
        DocumentationUnit.builder()
            .note(documentationUnitDTO.getNote())
            .version(documentationUnitDTO.getVersion());
    CoreDataBuilder coreDataBuilder =
        CoreData.builder()
            .court(CourtTransformer.transformToDomain((documentationUnitDTO.getCourt())))
            .procedure(
                ProcedureTransformer.transformFirstToDomain(documentationUnitDTO.getProcedures()))
            .previousProcedures(
                ProcedureTransformer.transformPreviousProceduresToLabel(
                    documentationUnitDTO.getProcedures()))
            .documentationOffice(
                DocumentationOfficeTransformer.transformToDomain(
                    documentationUnitDTO.getDocumentationOffice()))
            // TODO multiple regions
            .region(
                documentationUnitDTO.getRegions() == null
                        || documentationUnitDTO.getRegions().isEmpty()
                        || documentationUnitDTO.getRegions().get(0) == null
                    ? null
                    : documentationUnitDTO.getRegions().get(0).getCode())
            .ecli(documentationUnitDTO.getEcli())
            .decisionDate(documentationUnitDTO.getDecisionDate())
            .appraisalBody(documentationUnitDTO.getJudicialBody())
            .legalEffect(legalEffect == null ? null : legalEffect.getLabel());

    addInputTypesToDomain(documentationUnitDTO, coreDataBuilder);
    addFileNumbersToDomain(documentationUnitDTO, coreDataBuilder);
    addDeviatingFileNumbersToDomain(documentationUnitDTO, coreDataBuilder);
    addDeviatingCourtsToDomain(documentationUnitDTO, coreDataBuilder);
    addDeviatingEclisToDomain(documentationUnitDTO, coreDataBuilder);
    addDeviatingDecisionDatesToDomain(documentationUnitDTO, coreDataBuilder);
    addLeadingDecisionNormReferencesToDomain(documentationUnitDTO, coreDataBuilder);
    addYearsOfDisputeToDomain(documentationUnitDTO, coreDataBuilder);
    DocumentTypeDTO documentTypeDTO = documentationUnitDTO.getDocumentType();
    if (documentTypeDTO != null) {
      coreDataBuilder.documentType(DocumentTypeTransformer.transformToDomain(documentTypeDTO));
    }

    CoreData coreData = coreDataBuilder.build();

    ContentRelatedIndexing.ContentRelatedIndexingBuilder contentRelatedIndexingBuilder =
        ContentRelatedIndexing.builder();

    if (documentationUnitDTO.getDocumentationUnitKeywordDTOs() != null) {
      List<String> keywords =
          documentationUnitDTO.getDocumentationUnitKeywordDTOs().stream()
              .map(
                  documentationUnitKeywordDTO ->
                      documentationUnitKeywordDTO.getKeyword().getValue())
              .toList();
      contentRelatedIndexingBuilder.keywords(keywords);
    }

    if (documentationUnitDTO.getNormReferences() != null) {
      List<NormReference> norms = addNormReferencesToDomain(documentationUnitDTO);
      contentRelatedIndexingBuilder.norms(norms);
    }

    if (documentationUnitDTO.getActiveCitations() != null) {
      contentRelatedIndexingBuilder.activeCitations(
          documentationUnitDTO.getActiveCitations().stream()
              .map(ActiveCitationTransformer::transformToDomain)
              .toList());
    }

    if (documentationUnitDTO.getDocumentationUnitFieldsOfLaw() != null) {
      List<FieldOfLaw> fieldOfLaws =
          documentationUnitDTO.getDocumentationUnitFieldsOfLaw().stream()
              .map(
                  documentationUnitFieldOfLawDTO ->
                      FieldOfLawTransformer.transformToDomain(
                          documentationUnitFieldOfLawDTO.getFieldOfLaw(), false, false))
              .toList();

      contentRelatedIndexingBuilder.fieldsOfLaw(fieldOfLaws);
    }

    if (documentationUnitDTO.getJobProfiles() != null) {
      List<String> jobProfiles =
          documentationUnitDTO.getJobProfiles().stream().map(JobProfileDTO::getValue).toList();
      contentRelatedIndexingBuilder.jobProfiles(jobProfiles);
    }

    ContentRelatedIndexing contentRelatedIndexing = contentRelatedIndexingBuilder.build();

    Texts texts =
        Texts.builder()
            // TODO multiple decisionNames
            .decisionName(
                (documentationUnitDTO.getDecisionNames() == null
                        || documentationUnitDTO.getDecisionNames().isEmpty())
                    ? null
                    : documentationUnitDTO.getDecisionNames().stream().findFirst().get().getValue())
            .headline(documentationUnitDTO.getHeadline())
            .guidingPrinciple(documentationUnitDTO.getGuidingPrinciple())
            .headnote(documentationUnitDTO.getHeadnote())
            .otherHeadnote(documentationUnitDTO.getOtherHeadnote())
            .tenor(documentationUnitDTO.getTenor())
            .reasons(documentationUnitDTO.getGrounds())
            .caseFacts(documentationUnitDTO.getCaseFacts())
            .decisionReasons(documentationUnitDTO.getDecisionGrounds())
            .dissentingOpinion(documentationUnitDTO.getDissentingOpinion())
            .otherLongText(documentationUnitDTO.getOtherLongText())
            .outline(documentationUnitDTO.getOutline())
            .build();

    List<String> borderNumbers =
        extractBorderNumbers(
            documentationUnitDTO.getTenor(),
            documentationUnitDTO.getGrounds(),
            documentationUnitDTO.getCaseFacts(),
            documentationUnitDTO.getDecisionGrounds());

    addOriginalFileDocuments(documentationUnitDTO, builder);
    addPreviousDecisionsToDomain(documentationUnitDTO, builder);
    addEnsuingDecisionsToDomain(documentationUnitDTO, builder);

    builder
        .uuid(documentationUnitDTO.getId())
        .documentNumber(documentationUnitDTO.getDocumentNumber())
        .coreData(coreData)
        .texts(texts)
        .borderNumbers(borderNumbers)
        .contentRelatedIndexing(contentRelatedIndexing);

    addStatusToDomain(documentationUnitDTO, builder);
    addReferencesToDomain(documentationUnitDTO, builder);

    return builder.build();
  }

  private static void addReferencesToDomain(
      DocumentationUnitDTO documentationUnitDTO,
      DocumentationUnit.DocumentationUnitBuilder builder) {
    builder.references(
        documentationUnitDTO.getReferences() == null
            ? Collections.emptyList()
            : documentationUnitDTO.getReferences().stream()
                .map(ReferenceTransformer::transformToDomain)
                .toList());
  }

  /**
   * Adds norm references to the domain object based on the provided documentation unit DTO. A list
   * of NormReferenceDTOs with the same normAbbreviation are grouped into one NormReference, with a
   * list of {@link SingleNorm}.
   *
   * @param documentationUnitDTO The documentation unit DTO containing norm references to be added.
   * @return A list of NormReference objects representing the added norm references.
   */
  private static List<NormReference> addNormReferencesToDomain(
      DocumentationUnitDTO documentationUnitDTO) {
    List<NormReference> normReferences = new ArrayList<>();

    documentationUnitDTO.getNormReferences().stream()
        .filter(normReferenceDTO -> normReferenceDTO.getNormAbbreviation() != null)
        .forEach(
            normReferenceDTO -> {
              NormReference normReference =
                  NormReferenceTransformer.transformToDomain(normReferenceDTO);
              List<NormReference> existingNormReferences =
                  normReferences.stream()
                      .filter(
                          existingNormReference ->
                              existingNormReference
                                  .normAbbreviation()
                                  .id()
                                  .equals(normReferenceDTO.getNormAbbreviation().getId()))
                      .toList();
              if (existingNormReferences.size() > 1) {
                log.error(
                    "More than one norm references for norm abbreviation ({}, {})",
                    normReferenceDTO.getNormAbbreviation().getId(),
                    normReferenceDTO.getNormAbbreviation().getAbbreviation());
                throw new DocumentationUnitTransformerException(
                    "More than one norm references with the same norm abbreviation.");
              } else if (existingNormReferences.isEmpty()) {
                normReferences.add(normReference);
              } else {
                existingNormReferences
                    .get(0)
                    .singleNorms()
                    .add(SingleNormTransformer.transformToDomain(normReferenceDTO));
              }
            });

    normReferences.addAll(
        documentationUnitDTO.getNormReferences().stream()
            .filter(normReferenceDTO -> normReferenceDTO.getNormAbbreviation() == null)
            .map(NormReferenceTransformer::transformToDomain)
            .toList());

    return normReferences;
  }

  private static LegalEffect getLegalEffectForDomain(DocumentationUnitDTO documentationUnitDTO) {
    LegalEffect legalEffect = null;

    if (documentationUnitDTO.getLegalEffect() != null) {
      if (documentationUnitDTO.getLegalEffect() == LegalEffectDTO.NEIN) {
        legalEffect = LegalEffect.NO;
      } else if (documentationUnitDTO.getLegalEffect() == LegalEffectDTO.JA) {
        legalEffect = LegalEffect.YES;
      } else if (documentationUnitDTO.getLegalEffect() == LegalEffectDTO.KEINE_ANGABE) {
        legalEffect = LegalEffect.NOT_SPECIFIED;
      }
    }

    return legalEffect;
  }

  private static void addOriginalFileDocuments(
      DocumentationUnitDTO documentationUnitDTO,
      DocumentationUnit.DocumentationUnitBuilder builder) {
    builder.attachments(
        documentationUnitDTO.getAttachments().stream()
            .map(AttachmentTransformer::transformToDomain)
            .toList());
  }

  private static void addStatusToDomain(
      DocumentationUnitDTO documentationUnitDTO,
      DocumentationUnit.DocumentationUnitBuilder builder) {
    if (documentationUnitDTO.getStatus() == null || documentationUnitDTO.getStatus().isEmpty()) {
      return;
    }
    builder.status(
        StatusTransformer.transformToDomain(
            documentationUnitDTO.getStatus().stream()
                .max(Comparator.comparing(StatusDTO::getCreatedAt))
                .orElse(null)));
  }

  private static void addPreviousDecisionsToDomain(
      DocumentationUnitDTO documentationUnitDTO,
      DocumentationUnit.DocumentationUnitBuilder builder) {
    if (documentationUnitDTO.getPreviousDecisions() == null) {
      return;
    }

    builder.previousDecisions(
        documentationUnitDTO.getPreviousDecisions().stream()
            .map(PreviousDecisionTransformer::transformToDomain)
            .toList());
  }

  private static void addDeviatingDecisionDatesToDomain(
      DocumentationUnitDTO documentationUnitDTO, CoreDataBuilder coreDataBuilder) {
    if (documentationUnitDTO.getDeviatingDates() == null) {
      return;
    }

    List<LocalDate> deviatingDecisionDates =
        documentationUnitDTO.getDeviatingDates().stream().map(DeviatingDateDTO::getValue).toList();
    coreDataBuilder.deviatingDecisionDates(deviatingDecisionDates);
  }

  private static void addDeviatingEclisToDomain(
      DocumentationUnitDTO documentationUnitDTO, CoreDataBuilder coreDataBuilder) {
    if (documentationUnitDTO.getDeviatingEclis() == null) {
      return;
    }

    List<String> deviatingEclis =
        documentationUnitDTO.getDeviatingEclis().stream().map(DeviatingEcliDTO::getValue).toList();
    coreDataBuilder.deviatingEclis(deviatingEclis);
  }

  private static void addDeviatingCourtsToDomain(
      DocumentationUnitDTO documentationUnitDTO, CoreDataBuilder coreDataBuilder) {
    if (documentationUnitDTO.getDeviatingCourts() == null) {
      return;
    }

    List<String> deviatingCourts =
        documentationUnitDTO.getDeviatingCourts().stream()
            .map(DeviatingCourtDTO::getValue)
            .toList();
    coreDataBuilder.deviatingCourts(deviatingCourts);
  }

  private static void addDeviatingFileNumbersToDomain(
      DocumentationUnitDTO documentationUnitDTO, CoreDataBuilder coreDataBuilder) {
    if (documentationUnitDTO.getDeviatingFileNumbers() == null) {
      return;
    }

    List<String> deviatingFileNumbers =
        documentationUnitDTO.getDeviatingFileNumbers().stream()
            .map(DeviatingFileNumberDTO::getValue)
            .toList();
    coreDataBuilder.deviatingFileNumbers(deviatingFileNumbers);
  }

  private static void addInputTypesToDomain(
      DocumentationUnitDTO documentationUnitDTO, CoreDataBuilder coreDataBuilder) {
    if (documentationUnitDTO.getInputTypes() == null) {
      return;
    }

    List<String> inputTypes =
        documentationUnitDTO.getInputTypes().stream().map(InputTypeDTO::getValue).toList();
    coreDataBuilder.inputTypes(inputTypes);
  }

  private static void addFileNumbersToDomain(
      DocumentationUnitDTO documentationUnitDTO, CoreDataBuilder coreDataBuilder) {
    if (documentationUnitDTO.getFileNumbers() == null) {
      return;
    }

    List<String> fileNumbers =
        documentationUnitDTO.getFileNumbers().stream().map(FileNumberDTO::getValue).toList();
    coreDataBuilder.fileNumbers(fileNumbers);
  }

  private static void addLeadingDecisionNormReferencesToDomain(
      DocumentationUnitDTO documentationUnitDTO, CoreDataBuilder coreDataBuilder) {
    if (documentationUnitDTO.getLeadingDecisionNormReferences() == null) {
      return;
    }
    coreDataBuilder.leadingDecisionNormReferences(
        documentationUnitDTO.getLeadingDecisionNormReferences().stream()
            .map(LeadingDecisionNormReferenceDTO::getNormReference)
            .toList());
  }

  private static void addEnsuingDecisionsToDomain(
      DocumentationUnitDTO documentationUnitDTO,
      DocumentationUnit.DocumentationUnitBuilder builder) {

    List<EnsuingDecisionDTO> ensuingDecisionDTOs = documentationUnitDTO.getEnsuingDecisions();
    List<PendingDecisionDTO> pendingDecisionDTOs = documentationUnitDTO.getPendingDecisions();

    if (pendingDecisionDTOs == null && ensuingDecisionDTOs == null) {
      return;
    }

    int size = getEnsuingDecisionListSize(ensuingDecisionDTOs, pendingDecisionDTOs);

    List<EnsuingDecision> withoutRank = new ArrayList<>();
    EnsuingDecision[] ensuingDecisions = new EnsuingDecision[size];

    addEnsuingDecisionToDomain(ensuingDecisionDTOs, withoutRank, ensuingDecisions);
    addPendingDecisionsToDomain(pendingDecisionDTOs, withoutRank, ensuingDecisions);

    handleEnsuingDecisionsWithoutRank(withoutRank, ensuingDecisions);

    builder.ensuingDecisions(Arrays.stream(ensuingDecisions).toList());
  }

  private static void handleEnsuingDecisionsWithoutRank(
      List<EnsuingDecision> withoutRank, EnsuingDecision[] ensuingDecisions) {
    if (withoutRank.isEmpty()) {
      return;
    }

    int j = 0;
    for (int i = 0; i < ensuingDecisions.length; i++) {
      if (ensuingDecisions[i] == null) {
        ensuingDecisions[i] = withoutRank.get(j++);
      }
    }

    if (j < withoutRank.size()) {
      log.error(
          "ensuing decision - adding ensuing decisions without rank has more elements than expected.");
    }
  }

  private static void addPendingDecisionsToDomain(
      List<PendingDecisionDTO> pendingDecisionDTOs,
      List<EnsuingDecision> withoutRank,
      EnsuingDecision[] ensuingDecisions) {
    if (pendingDecisionDTOs == null) {
      return;
    }

    for (PendingDecisionDTO currentDTO : pendingDecisionDTOs) {
      if (currentDTO.getRank() > 0) {
        ensuingDecisions[currentDTO.getRank() - 1] =
            PendingDecisionTransformer.transformToDomain(currentDTO);
      } else {
        withoutRank.add(PendingDecisionTransformer.transformToDomain(currentDTO));
      }
    }
  }

  private static void addEnsuingDecisionToDomain(
      List<EnsuingDecisionDTO> ensuingDecisionDTOs,
      List<EnsuingDecision> withoutRank,
      EnsuingDecision[] ensuingDecisions) {
    if (ensuingDecisionDTOs == null) {
      return;
    }

    for (EnsuingDecisionDTO currentDTO : ensuingDecisionDTOs) {
      if (currentDTO.getRank() > 0) {
        ensuingDecisions[currentDTO.getRank() - 1] =
            EnsuingDecisionTransformer.transformToDomain(currentDTO);
      } else {
        withoutRank.add(EnsuingDecisionTransformer.transformToDomain(currentDTO));
      }
    }
  }

  private static int getEnsuingDecisionListSize(
      List<EnsuingDecisionDTO> ensuingDecisionDTOs, List<PendingDecisionDTO> pendingDecisionDTOs) {
    int size = 0;

    if (ensuingDecisionDTOs != null) {
      size += ensuingDecisionDTOs.size();
    }

    if (pendingDecisionDTOs != null) {
      size += pendingDecisionDTOs.size();
    }

    return size;
  }

  /**
   * Extracts all border numbers from the passed strings and returns them as a list based on the
   * following rules: For all <border-number> elements that contain a single <number> element with
   * non-blank content, that content will be added to the list of border numbers.
   *
   * @param input the strings to be searched for border numbers
   * @return a list of found border numbers or an empty list, if the input is null
   */
  private static List<String> extractBorderNumbers(String... input) {
    if (Objects.isNull(input)) {
      return new ArrayList<>();
    }
    List<String> borderNumbers = new ArrayList<>();
    Arrays.stream(input)
        .forEach(
            longText -> {
              if (Objects.isNull(longText)) {
                return;
              }
              Document doc = Jsoup.parse(longText);
              var borderNumberElements = doc.getElementsByTag("border-number");
              borderNumberElements.forEach(
                  element -> {
                    var numberElement = element.getElementsByTag("number");
                    if (numberElement.size() == 1) {
                      var number = numberElement.get(0).text();
                      if (org.apache.commons.lang3.StringUtils.isNotBlank(number)) {
                        borderNumbers.add(numberElement.text());
                      }
                    }
                  });
            });
    return borderNumbers;
  }

  private static void addYearsOfDisputeToDTO(
      DocumentationUnitDTO.DocumentationUnitDTOBuilder builder, CoreData coreData) {

    if (coreData.yearsOfDispute() == null || coreData.yearsOfDispute().isEmpty()) {
      builder.yearsOfDispute(new LinkedHashSet<>());
      return;
    }

    Set<YearOfDisputeDTO> yearOfDisputeDTOS = new LinkedHashSet<>();

    for (int i = 0; i < coreData.yearsOfDispute().size(); i++) {
      yearOfDisputeDTOS.add(
          YearOfDisputeTransformer.transformToDTO(coreData.yearsOfDispute().get(i), i + 1));
    }
    builder.yearsOfDispute(yearOfDisputeDTOS);
  }

  static void addYearsOfDisputeToDomain(
      DocumentationUnitDTO currentDto, CoreData.CoreDataBuilder coreDataBuilder) {

    coreDataBuilder.yearsOfDispute(
        currentDto.getYearsOfDispute().stream()
            .map(YearOfDisputeTransformer::transformToDomain)
            .toList());
  }
}
