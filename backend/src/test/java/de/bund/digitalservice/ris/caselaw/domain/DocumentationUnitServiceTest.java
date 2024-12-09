package de.bund.digitalservice.ris.caselaw.domain;

import static de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationType.ACTIVE_CITATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentationUnitStatusService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentNumberFormatterException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentNumberPatternException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitDeletionException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitExistsException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import jakarta.validation.Validator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import({DocumentationUnitService.class, DatabaseDocumentationUnitStatusService.class})
class DocumentationUnitServiceTest {
  private static final UUID TEST_UUID = UUID.fromString("88888888-4444-4444-4444-121212121212");
  @SpyBean private DocumentationUnitService service;
  @MockBean private DatabaseDocumentationUnitRepository documentationUnitRepository;
  @MockBean private DocumentationUnitRepository repository;
  @MockBean private DocumentNumberService documentNumberService;
  @MockBean private DocumentNumberRecyclingService documentNumberRecyclingService;
  @MockBean private MailService mailService;
  @MockBean private DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  @MockBean private AttachmentService attachmentService;
  @MockBean private PatchMapperService patchMapperService;
  @MockBean private Validator validator;
  @MockBean private OidcUser oidcUser;
  @MockBean private AuthService authService;
  @Captor private ArgumentCaptor<DocumentationUnitSearchInput> searchInputCaptor;
  @Captor private ArgumentCaptor<RelatedDocumentationUnit> relatedDocumentationUnitCaptor;

  @Test
  void testGenerateNewDocumentationUnit()
      throws DocumentationUnitExistsException,
          DocumentNumberPatternException,
          DocumentNumberFormatterException {
    DocumentationOffice documentationOffice =
        DocumentationOffice.builder().uuid(UUID.randomUUID()).build();
    DocumentationUnit documentationUnit = DocumentationUnit.builder().build();

    when(repository.createNewDocumentationUnit(any(), any(), any(), any()))
        .thenReturn(documentationUnit);
    when(documentNumberService.generateDocumentNumber(documentationOffice.abbreviation()))
        .thenReturn("nextDocumentNumber");
    // Can we use a captor to check if the document number was correctly created?
    // The chicken-egg-problem is, that we are dictating what happens when
    // repository.save(), so we can't just use a captor at the same time

    Assertions.assertNotNull(
        service.generateNewDocumentationUnit(documentationOffice, Optional.empty()));

    verify(documentNumberService).generateDocumentNumber(documentationOffice.abbreviation());
    verify(repository)
        .createNewDocumentationUnit(
            DocumentationUnit.builder()
                .version(0L)
                .documentNumber("nextDocumentNumber")
                .coreData(
                    CoreData.builder()
                        .legalEffect(LegalEffect.NOT_SPECIFIED.getLabel())
                        .documentationOffice(documentationOffice)
                        .build())
                .build(),
            Status.builder()
                .publicationStatus(PublicationStatus.UNPUBLISHED)
                .withError(false)
                .build(),
            null,
            null);
  }

  @Test
  void testGenerateNewDocumentationUnitWithParameters()
      throws DocumentationUnitExistsException,
          DocumentNumberPatternException,
          DocumentNumberFormatterException {
    DocumentationOffice userDocumentationOffice =
        DocumentationOffice.builder().abbreviation("BAG").uuid(UUID.randomUUID()).build();
    DocumentationOffice designatedDocumentationOffice =
        DocumentationOffice.builder().abbreviation("BGH").uuid(UUID.randomUUID()).build();
    DocumentationUnit documentationUnit = DocumentationUnit.builder().build();
    DocumentationUnitCreationParameters parameters =
        DocumentationUnitCreationParameters.builder()
            .documentationOffice(designatedDocumentationOffice)
            .fileNumber("fileNumber")
            .court(Court.builder().type("BGH").build())
            .decisionDate(LocalDate.now())
            .documentType(DocumentType.builder().label("Bes").build())
            .reference(
                Reference.builder()
                    .citation("2024, 4")
                    .legalPeriodical(LegalPeriodical.builder().abbreviation("BAG").build())
                    .build())
            .build();

    when(repository.createNewDocumentationUnit(any(), any(), any(), any()))
        .thenReturn(documentationUnit);

    when(documentNumberService.generateDocumentNumber(designatedDocumentationOffice.abbreviation()))
        .thenReturn("nextDocumentNumber");
    // Can we use a captor to check if the document number was correctly created?
    // The chicken-egg-problem is, that we are dictating what happens when
    // repository.save(), so we can't just use a captor at the same time

    Assertions.assertNotNull(
        service.generateNewDocumentationUnit(userDocumentationOffice, Optional.of(parameters)));

    verify(documentNumberService)
        .generateDocumentNumber(designatedDocumentationOffice.abbreviation());
    verify(repository)
        .createNewDocumentationUnit(
            DocumentationUnit.builder()
                .version(0L)
                .documentNumber("nextDocumentNumber")
                .coreData(
                    CoreData.builder()
                        .creatingDocOffice(userDocumentationOffice)
                        .documentationOffice(designatedDocumentationOffice)
                        .fileNumbers(List.of(parameters.fileNumber()))
                        .court(parameters.court())
                        .legalEffect(LegalEffect.YES.getLabel())
                        .decisionDate(parameters.decisionDate())
                        .documentType(parameters.documentType())
                        .build())
                .build(),
            Status.builder()
                .publicationStatus(PublicationStatus.EXTERNAL_HANDOVER_PENDING)
                .withError(false)
                .build(),
            parameters.reference(),
            parameters.reference().legalPeriodical().abbreviation()
                + " "
                + parameters.reference().citation());
  }

  @Test
  void testGetByDocumentnumber() throws DocumentationUnitNotExistsException {
    when(repository.findByDocumentNumber("ABCDE20220001"))
        .thenReturn(DocumentationUnit.builder().build());
    var documentationUnit = service.getByDocumentNumber("ABCDE20220001");
    assertEquals(documentationUnit.getClass(), DocumentationUnit.class);

    verify(repository).findByDocumentNumber("ABCDE20220001");
  }

  @Test
  void testDeleteByUuid_withoutFileAttached() throws DocumentationUnitNotExistsException {
    // I think I shouldn't have to insert a specific DocumentationUnit object here?
    // But if I don't, the test by itself succeeds, but fails if all tests in this class run
    // something flaky with the repository mock? Investigate this later
    DocumentationUnit documentationUnit = DocumentationUnit.builder().uuid(TEST_UUID).build();
    // can we also test that the fileUuid from the DocumentationUnit is used? with a captor somehow?
    when(repository.findByUuid(TEST_UUID)).thenReturn(documentationUnit);

    var string = service.deleteByUuid(TEST_UUID);
    assertNotNull(string);
    assertEquals("Dokumentationseinheit gelöscht: " + TEST_UUID, string);

    verify(attachmentService, times(0)).deleteAllObjectsFromBucketForDocumentationUnit(TEST_UUID);
  }

  @Test
  void testDeleteByUuid_withFileAttached() throws DocumentationUnitNotExistsException {
    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .uuid(TEST_UUID)
            .attachments(
                Collections.singletonList(
                    Attachment.builder().s3path(TEST_UUID.toString()).build()))
            .build();

    when(repository.findByUuid(TEST_UUID)).thenReturn(documentationUnit);

    var string = service.deleteByUuid(TEST_UUID);
    assertNotNull(string);
    assertEquals("Dokumentationseinheit gelöscht: " + TEST_UUID, string);

    verify(attachmentService).deleteAllObjectsFromBucketForDocumentationUnit(TEST_UUID);
  }

  @Test
  void testDeleteByUuid_withoutFileAttached_withExceptionFromRepository()
      throws DocumentationUnitNotExistsException {

    when(repository.findByUuid(TEST_UUID)).thenReturn(DocumentationUnit.builder().build());
    doThrow(new IllegalArgumentException())
        .when(repository)
        .delete(DocumentationUnit.builder().build());

    Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteByUuid(TEST_UUID));

    verify(repository).findByUuid(TEST_UUID);
  }

  @Test
  void testDeleteByUuid_withLinks() throws DocumentationUnitNotExistsException {
    when(repository.findByUuid(TEST_UUID)).thenReturn(DocumentationUnit.builder().build());
    when(repository.getAllDocumentationUnitWhichLink(TEST_UUID))
        .thenReturn(Map.of(ACTIVE_CITATION, 2L));
    DocumentationUnitDeletionException throwable =
        Assertions.assertThrows(
            DocumentationUnitDeletionException.class, () -> service.deleteByUuid(TEST_UUID));
    Assertions.assertTrue(
        throwable
            .getMessage()
            .contains(
                "Die Dokumentationseinheit konnte nicht gelöscht werden, da (2: Aktivzitierung,)"));
  }

  @Test
  void testUpdateDocumentationUnit() throws DocumentationUnitNotExistsException {
    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .uuid(UUID.randomUUID())
            .documentNumber("ABCDE20220001")
            .attachments(
                Collections.singletonList(
                    Attachment.builder().uploadTimestamp(Instant.now()).build()))
            .build();
    when(repository.findByUuid(documentationUnit.uuid())).thenReturn(documentationUnit);

    var du = service.updateDocumentationUnit(documentationUnit);
    assertEquals(du, documentationUnit);

    verify(repository).save(documentationUnit);
  }

  @Test
  void testSearchByDocumentationUnitListEntry() throws DocumentationUnitNotExistsException {
    DocumentationUnitSearchInput documentationUnitSearchInput =
        DocumentationUnitSearchInput.builder().build();
    DocumentationUnitListItem documentationUnitListItem =
        DocumentationUnitListItem.builder().build();
    PageRequest pageRequest = PageRequest.of(0, 10);
    when(authService.userIsInternal()).thenReturn(user -> true);
    when(authService.isAssignedViaProcedure()).thenReturn(user -> true);
    when(repository.findByDocumentNumber(any()))
        .thenReturn(DocumentationUnit.builder().uuid(UUID.randomUUID()).build());
    when(repository.searchByDocumentationUnitSearchInput(
            pageRequest, oidcUser, documentationUnitSearchInput))
        .thenReturn(new PageImpl<>(List.of(documentationUnitListItem)));

    service.searchByDocumentationUnitSearchInput(
        pageRequest,
        oidcUser,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());
    verify(repository)
        .searchByDocumentationUnitSearchInput(pageRequest, oidcUser, documentationUnitSearchInput);
  }

  @Test
  void testSearchByDocumentationUnitListEntry_shouldNormalizeSpaces()
      throws DocumentationUnitNotExistsException {
    DocumentationUnitListItem documentationUnitListItem =
        DocumentationUnitListItem.builder().build();
    PageRequest pageRequest = PageRequest.of(0, 10);
    when(authService.userIsInternal()).thenReturn(user -> true);
    when(authService.isAssignedViaProcedure()).thenReturn(user -> true);
    when(repository.findByDocumentNumber(any()))
        .thenReturn(DocumentationUnit.builder().uuid(UUID.randomUUID()).build());
    when(repository.searchByDocumentationUnitSearchInput(
            any(PageRequest.class), any(OidcUser.class), any(DocumentationUnitSearchInput.class)))
        .thenReturn(new PageImpl<>(List.of(documentationUnitListItem)));

    service.searchByDocumentationUnitSearchInput(
        pageRequest,
        oidcUser,
        Optional.of("This\u00A0is\u202Fa\uFEFFtest\u2007docnumber\u180Ewith\u2060spaces"),
        Optional.of("This\u00A0is\u202Fa\uFEFFtest\u2007filenumber\u180Ewith\u2060spaces"),
        Optional.of("This\u00A0is\u202Fa\uFEFFtest\u2007courttype\u180Ewith\u2060spaces"),
        Optional.of("This\u00A0is\u202Fa\uFEFFtest\u2007courtlocation\u180Ewith\u2060spaces"),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());

    // Capture the searchInput argument
    verify(repository)
        .searchByDocumentationUnitSearchInput(
            any(PageRequest.class), any(OidcUser.class), searchInputCaptor.capture());

    DocumentationUnitSearchInput capturedSearchInput = searchInputCaptor.getValue();

    // Verify that the searchInput fields have normalized spaces
    assertThat(capturedSearchInput.documentNumber())
        .isEqualTo("This is a test docnumber with spaces");
    assertThat(capturedSearchInput.fileNumber()).isEqualTo("This is a test filenumber with spaces");
    assertThat(capturedSearchInput.courtType()).isEqualTo("This is a test courttype with spaces");
    assertThat(capturedSearchInput.courtLocation())
        .isEqualTo("This is a test courtlocation with spaces");
  }

  @Test
  void testSearchLinkableDocumentationUnits_shouldNormalizeSpaces() {
    DocumentationOffice documentationOffice = DocumentationOffice.builder().build();
    RelatedDocumentationUnit relatedDocumentationUnit =
        RelatedDocumentationUnit.builder()
            .uuid(UUID.randomUUID())
            .fileNumber(
                "This\u00A0is\u202Fa\uFEFFtest\u2007filenumber\u180Ewith\u2060spaces.") // String
            // with
            // non-breaking space
            .build();
    PageRequest pageRequest = PageRequest.of(0, 10);
    String documentNumberToExclude = "DOC12345";

    // Configure the mock repository to return a non-null Slice object
    when(repository.searchLinkableDocumentationUnits(
            any(RelatedDocumentationUnit.class),
            any(DocumentationOffice.class),
            any(String.class),
            any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(relatedDocumentationUnit)));

    // Call the service method
    service.searchLinkableDocumentationUnits(
        relatedDocumentationUnit,
        documentationOffice,
        Optional.of(documentNumberToExclude),
        pageRequest);

    // Capture the relatedDocumentationUnit argument
    verify(repository)
        .searchLinkableDocumentationUnits(
            relatedDocumentationUnitCaptor.capture(),
            any(DocumentationOffice.class),
            any(String.class),
            any(Pageable.class));

    RelatedDocumentationUnit capturedRelatedDocumentationUnit =
        relatedDocumentationUnitCaptor.getValue();

    // Verify that the fileNumber field has normalized spaces
    assertThat(capturedRelatedDocumentationUnit.getFileNumber())
        .isEqualTo("This is a test filenumber with spaces.");
  }

  @Test
  void test_setPublicationDateTime_shouldSaveLastPublicationDateTime() {
    DocumentationUnit documentationUnit = DocumentationUnit.builder().build();

    service.setPublicationDateTime(documentationUnit.uuid());

    verify(repository, times(1)).saveLastPublicationDateTime(documentationUnit.uuid());
  }
}
