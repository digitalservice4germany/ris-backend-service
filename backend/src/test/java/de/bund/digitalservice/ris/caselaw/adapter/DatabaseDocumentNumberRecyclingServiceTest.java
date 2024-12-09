package de.bund.digitalservice.ris.caselaw.adapter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDeletedDocumentationIdsRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DeletedDocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.StatusDTO;
import de.bund.digitalservice.ris.caselaw.domain.DateUtil;
import de.bund.digitalservice.ris.caselaw.domain.EventRecord;
import de.bund.digitalservice.ris.caselaw.domain.HandoverEntityType;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentNumberPatternException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentNumberRecyclingException;
import java.time.Instant;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = DocumentNumberPatternConfig.class)
@Import(DatabaseDocumentNumberRecyclingService.class)
class DatabaseDocumentNumberRecyclingServiceTest {

  @MockBean DatabaseDeletedDocumentationIdsRepository repository;

  @MockBean DatabaseDocumentationUnitRepository documentationUnitRepository;

  @Autowired DatabaseDocumentNumberRecyclingService service;

  @MockBean HandoverService handoverService;

  private static final String DEFAULT_DOCUMENTATION_OFFICE = "BGH";

  private static String generateDefaultDocumentNumber() {
    return "KORE70001" + DateUtil.getYear();
  }

  @Test
  void addForRecycling_shouldNotSave_ifPrefixIsInvalid() {

    var documentationUnitDTO =
        DocumentationUnitDTO.builder()
            .id(UUID.randomUUID())
            .documentNumber("KORE2" + Year.now() + "00037")
            .build();

    var unpublished = generateStatus(documentationUnitDTO, PublicationStatus.UNPUBLISHED);

    var outdatedDeletedId =
        DeletedDocumentationUnitDTO.builder()
            .documentNumber(documentationUnitDTO.getDocumentNumber())
            .year(Year.now())
            .abbreviation(DEFAULT_DOCUMENTATION_OFFICE)
            .build();

    when(repository.findFirstByAbbreviationAndYear(DEFAULT_DOCUMENTATION_OFFICE, Year.now()))
        .thenReturn(Optional.of(outdatedDeletedId));
    when(documentationUnitRepository.findById(documentationUnitDTO.getId()))
        .thenReturn(
            Optional.of(
                DocumentationUnitDTO.builder().statusHistory(List.of(unpublished)).build()));
    when(repository.save(any())).thenReturn(generateDeletedDocumentationUnitDTO());

    assertThrows(
        DocumentNumberPatternException.class,
        () ->
            service.addForRecycling(
                documentationUnitDTO.getId(),
                documentationUnitDTO.getDocumentNumber(),
                DEFAULT_DOCUMENTATION_OFFICE));
  }

  @Test
  void recycleFromDeletedDocumentationUnit_shouldNotOfferInvalidPrefix() {
    var outdatedDeletedId =
        DeletedDocumentationUnitDTO.builder()
            .documentNumber("KORE2" + Year.now() + "0037")
            .year(Year.now())
            .abbreviation(DEFAULT_DOCUMENTATION_OFFICE)
            .build();

    when(repository.findFirstByAbbreviationAndYear(DEFAULT_DOCUMENTATION_OFFICE, Year.now()))
        .thenReturn(Optional.of(outdatedDeletedId));

    assertThrows(
        DocumentNumberPatternException.class,
        () ->
            service.recycleFromDeletedDocumentationUnit(DEFAULT_DOCUMENTATION_OFFICE, Year.now()));
  }

  @Test
  void recycleFromDeletedDocumentationUnit_shouldNotOfferUsedDocumentationUnit() {
    var usedDocumentationUnit =
        DeletedDocumentationUnitDTO.builder()
            .documentNumber(generateDefaultDocumentNumber())
            .year(Year.now())
            .abbreviation(DEFAULT_DOCUMENTATION_OFFICE)
            .build();

    when(repository.findFirstByAbbreviationAndYear(DEFAULT_DOCUMENTATION_OFFICE, Year.now()))
        .thenReturn(Optional.of(usedDocumentationUnit));

    when(documentationUnitRepository.findByDocumentNumber(
            usedDocumentationUnit.getDocumentNumber()))
        .thenReturn(Optional.of(generateDocumentationUnitDto()));

    var exception =
        assertThrows(
            DocumentNumberRecyclingException.class,
            () ->
                service.recycleFromDeletedDocumentationUnit(
                    DEFAULT_DOCUMENTATION_OFFICE, Year.now()));

    Assertions.assertEquals(
        "Saved documentation number is currently in use", exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = PublicationStatus.class,
      names = {"UNPUBLISHED", "EXTERNAL_HANDOVER_PENDING"})
  void addForRecycling_shouldSaveIfOnly_unpublished(PublicationStatus publicationStatus) {
    var documentationUnitDTO =
        DocumentationUnitDTO.builder()
            .id(UUID.randomUUID())
            .documentNumber(generateDefaultDocumentNumber())
            .build();

    documentationUnitDTO.setStatus(generateStatus(documentationUnitDTO, publicationStatus));

    when(documentationUnitRepository.findById(documentationUnitDTO.getId()))
        .thenReturn(Optional.of(documentationUnitDTO));

    when(repository.save(any())).thenReturn(generateDeletedDocumentationUnitDTO());

    DocumentNumberRecyclingException exception =
        assertThrows(
            DocumentNumberRecyclingException.class,
            () ->
                service.addForRecycling(
                    documentationUnitDTO.getId(),
                    documentationUnitDTO.getDocumentNumber(),
                    DEFAULT_DOCUMENTATION_OFFICE));

    Assertions.assertEquals(
        "Status has been changed once or neither unpublished nor handover pending",
        exception.getMessage());
  }

  @Test
  void addForRecycling_shouldNotSave_ifHadedOverOrMigrated() {
    var documentationUnitDTO = generateDocumentationUnitDto();
    //    var unpublished = generateStatus(documentationUnitDTO, PublicationStatus.UNPUBLISHED);

    when(handoverService.getEventLog(
            documentationUnitDTO.getId(), HandoverEntityType.DOCUMENTATION_UNIT))
        .thenReturn(List.of(mock(EventRecord.class)));

    when(documentationUnitRepository.findById(documentationUnitDTO.getId()))
        .thenReturn(
            Optional.of(
                DocumentationUnitDTO.builder().statusHistory(Collections.emptyList()).build()));

    DocumentNumberRecyclingException exception =
        assertThrows(
            DocumentNumberRecyclingException.class,
            () ->
                service.addForRecycling(
                    documentationUnitDTO.getId(),
                    documentationUnitDTO.getDocumentNumber(),
                    DEFAULT_DOCUMENTATION_OFFICE));

    Assertions.assertEquals(
        "This Documentation unit has already been handed over or migrated and therefore cannot be reused",
        exception.getMessage());
  }

  @Test
  void addForRecycling_shouldNotSave_IfDocumentationUnitHasBeenPublished() {
    var documentationUnitDTO = generateDocumentationUnitDto();
    var published = generateStatus(documentationUnitDTO, PublicationStatus.PUBLISHED);

    when(documentationUnitRepository.findById(documentationUnitDTO.getId()))
        .thenReturn(
            Optional.of(
                DocumentationUnitDTO.builder()
                    .status(published)
                    .statusHistory(List.of(published))
                    .build()));

    when(repository.save(any())).thenReturn(generateDeletedDocumentationUnitDTO());

    DocumentNumberRecyclingException exception =
        assertThrows(
            DocumentNumberRecyclingException.class,
            () ->
                service.addForRecycling(
                    documentationUnitDTO.getId(),
                    documentationUnitDTO.getDocumentNumber(),
                    DEFAULT_DOCUMENTATION_OFFICE));

    Assertions.assertEquals(
        "Status has been changed once or neither unpublished nor handover pending",
        exception.getMessage());
  }

  @Test
  void addForRecycling_shouldNotSave_ifMultipleStatusExists() {
    var documentationUnitDto = generateDocumentationUnitDto();

    var statusList =
        List.of(
            generateStatus(documentationUnitDto, PublicationStatus.PUBLISHED),
            generateStatus(documentationUnitDto, PublicationStatus.UNPUBLISHED));
    documentationUnitDto.setStatus(statusList.get(1));
    documentationUnitDto.setStatusHistory(statusList);

    when(documentationUnitRepository.findById(documentationUnitDto.getId()))
        .thenReturn(Optional.of(documentationUnitDto));

    when(repository.save(any())).thenReturn(generateDeletedDocumentationUnitDTO());

    DocumentNumberRecyclingException exception =
        assertThrows(
            DocumentNumberRecyclingException.class,
            () ->
                service.addForRecycling(
                    documentationUnitDto.getId(),
                    documentationUnitDto.getDocumentNumber(),
                    DEFAULT_DOCUMENTATION_OFFICE));

    Assertions.assertEquals(
        "Status has been changed once or neither unpublished nor handover pending",
        exception.getMessage());
  }

  @Test
  void assertStatusHasNeverBeenPublished_shouldReturnErrorIfDocumentHasBeenPublishedOrHandedOver() {
    var documentationUnitDTO = generateDocumentationUnitDto();

    when(handoverService.getEventLog(
            documentationUnitDTO.getId(), HandoverEntityType.DOCUMENTATION_UNIT))
        .thenReturn(List.of(mock(EventRecord.class)));

    assertThrows(
        DocumentNumberRecyclingException.class,
        () ->
            service.assertDocumentationUnitHasNeverBeenHandedOverOrMigrated(
                documentationUnitDTO.getId()));
  }

  private static StatusDTO generateStatus(
      DocumentationUnitDTO documentationUnitDTO, PublicationStatus publicationStatus) {
    return StatusDTO.builder()
        .documentationUnit(documentationUnitDTO)
        .publicationStatus(publicationStatus)
        .createdAt(Instant.now())
        .withError(false)
        .build();
  }

  private static DocumentationUnitDTO generateDocumentationUnitDto() {
    return DocumentationUnitDTO.builder()
        .id(UUID.randomUUID())
        .documentNumber(generateDefaultDocumentNumber())
        .build();
  }

  private static DeletedDocumentationUnitDTO generateDeletedDocumentationUnitDTO() {
    return DeletedDocumentationUnitDTO.builder()
        .documentNumber(generateDefaultDocumentNumber())
        .year(Year.now())
        .abbreviation(DEFAULT_DOCUMENTATION_OFFICE)
        .build();
  }
}
