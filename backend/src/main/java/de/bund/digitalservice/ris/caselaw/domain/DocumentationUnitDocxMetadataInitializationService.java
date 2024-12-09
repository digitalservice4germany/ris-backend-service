package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.court.CourtRepository;
import de.bund.digitalservice.ris.caselaw.domain.docx.Docx2Html;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocxMetadataProperty;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DocumentationUnitDocxMetadataInitializationService {

  private final DocumentationUnitRepository repository;
  private final CourtRepository courtRepository;
  private final DocumentTypeRepository documentTypeRepository;

  public DocumentationUnitDocxMetadataInitializationService(
      DocumentationUnitRepository repository,
      CourtRepository courtRepository,
      DocumentTypeRepository documentTypeRepository) {
    this.repository = repository;
    this.courtRepository = courtRepository;
    this.documentTypeRepository = documentTypeRepository;
  }

  public void initializeCoreData(UUID uuid, Docx2Html docx2html) {
    try {
      DocumentationUnit documentationUnit = repository.findByUuid(uuid);
      CoreData.CoreDataBuilder builder = documentationUnit.coreData().toBuilder();

      initializeFieldsFromProperties(docx2html.properties(), documentationUnit, builder);

      if (docx2html.ecliList().size() == 1) {
        handleEcli(documentationUnit, builder, docx2html.ecliList().get(0));
      }

      DocumentationUnit updatedDocumentationUnit =
          documentationUnit.toBuilder().coreData(builder.build()).build();
      repository.saveProcedures(updatedDocumentationUnit);
      // save new court first to avoid override of legal effect
      repository.save(
          documentationUnit.toBuilder()
              .coreData(
                  documentationUnit.coreData().toBuilder().court(builder.build().court()).build())
              .build());
      repository.save(updatedDocumentationUnit);
    } catch (DocumentationUnitNotExistsException ex) {
      log.error(
          "Initialize core data failed, because documentation unit '{}' doesn't exist!", uuid);
    }
  }

  private void initializeFieldsFromProperties(
      Map<DocxMetadataProperty, String> properties,
      DocumentationUnit documentationUnit,
      CoreData.CoreDataBuilder builder) {

    properties.forEach(
        (key, value) -> {
          switch (key) {
            case FILE_NUMBER -> handleFileNumber(documentationUnit, builder, value);
            case DECISION_DATE -> handleDecisionDate(documentationUnit, builder, value);
            case COURT_TYPE, COURT_LOCATION, COURT ->
                handleCourt(properties, documentationUnit, builder);
            case APPRAISAL_BODY -> handleAppraisalBody(documentationUnit, builder, value);
            case DOCUMENT_TYPE -> handleDocumentType(documentationUnit, builder, value);
            case ECLI -> handleEcli(documentationUnit, builder, value);
            case PROCEDURE -> handleProcedure(documentationUnit, builder, value);
            case LEGAL_EFFECT -> handleLegalEffect(documentationUnit, builder, value);
          }
        });
  }

  private void handleFileNumber(
      DocumentationUnit documentationUnit, CoreData.CoreDataBuilder builder, String value) {
    if (documentationUnit.coreData().fileNumbers().isEmpty()) {
      builder.fileNumbers(Collections.singletonList(value));
    }
  }

  private void handleDecisionDate(
      DocumentationUnit documentationUnit, CoreData.CoreDataBuilder builder, String value) {
    if (documentationUnit.coreData().decisionDate() == null) {
      builder.decisionDate(LocalDate.parse(value, DateTimeFormatter.ofPattern("dd.MM.yyyy")));
    }
  }

  private void handleCourt(
      Map<DocxMetadataProperty, String> properties,
      DocumentationUnit documentationUnit,
      CoreData.CoreDataBuilder builder) {
    if (documentationUnit.coreData().court() == null && builder.build().court() == null) {
      String courtTypeProperty = properties.get(DocxMetadataProperty.COURT_TYPE);
      String courtLocationProperty = properties.get(DocxMetadataProperty.COURT_LOCATION);
      String courtProperty = properties.get(DocxMetadataProperty.COURT);

      Optional<Court> court = Optional.empty();
      if (courtTypeProperty != null) {
        court = courtRepository.findByTypeAndLocation(courtTypeProperty, courtLocationProperty);
      }
      if (court.isEmpty() && courtProperty != null) {
        court = courtRepository.findUniqueBySearchString(courtProperty);
      }

      builder.court(court.orElse(null));
    }
  }

  private void handleAppraisalBody(
      DocumentationUnit documentationUnit, CoreData.CoreDataBuilder builder, String value) {
    if (documentationUnit.coreData().appraisalBody() == null) {
      builder.appraisalBody(value);
    }
  }

  private void handleDocumentType(
      DocumentationUnit documentationUnit, CoreData.CoreDataBuilder builder, String value) {
    if (documentationUnit.coreData().documentType() == null) {
      Optional<DocumentType> documentType =
          documentTypeRepository.findUniqueCaselawBySearchStr(value);
      builder.documentType(documentType.orElse(null));
    }
  }

  private void handleEcli(
      DocumentationUnit documentationUnit, CoreData.CoreDataBuilder builder, String value) {
    if (documentationUnit.coreData().ecli() == null && builder.build().ecli() == null) {
      builder.ecli(value);
    }
  }

  private void handleProcedure(
      DocumentationUnit documentationUnit, CoreData.CoreDataBuilder builder, String value) {
    if (documentationUnit.coreData().procedure() == null) {
      builder.procedure(Procedure.builder().label(value).build());
    }
  }

  private void handleLegalEffect(
      DocumentationUnit documentationUnit, CoreData.CoreDataBuilder builder, String value) {
    if (documentationUnit.coreData().legalEffect() == null
        || documentationUnit
            .coreData()
            .legalEffect()
            .equals(LegalEffect.NOT_SPECIFIED.getLabel())) {
      builder.legalEffect(value);
    }
  }
}
