package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDSDocOffice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.fasterxml.jackson.core.type.TypeReference;
import de.bund.digitalservice.ris.caselaw.EntityBuilderTestUtil;
import de.bund.digitalservice.ris.caselaw.SliceTestImpl;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberGeneratorService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentationUnitStatusService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentNumberPatternConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentationUnitController;
import de.bund.digitalservice.ris.caselaw.adapter.DocxConverterService;
import de.bund.digitalservice.ris.caselaw.adapter.LdmlExporterService;
import de.bund.digitalservice.ris.caselaw.adapter.OAuthService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.CourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseCourtRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentCategoryRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentTypeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseFileNumberRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseHandoverReportRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentCategoryDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.FileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDeltaMigrationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PreviousDecisionDTO;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentTypeTransformer;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitDocxMetadataInitializationService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.MailService;
import de.bund.digitalservice.ris.caselaw.domain.PreviousDecision;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisBodySpec;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@RISIntegrationTest(
    imports = {
      DocumentationUnitService.class,
      PostgresDeltaMigrationRepositoryImpl.class,
      DatabaseDocumentationUnitStatusService.class,
      DatabaseDocumentNumberRecyclingService.class,
      DatabaseDocumentNumberGeneratorService.class,
      PostgresDocumentationUnitRepositoryImpl.class,
      PostgresHandoverReportRepositoryImpl.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      SecurityConfig.class,
      OAuthService.class,
      TestConfig.class,
      DocumentNumberPatternConfig.class
    },
    controllers = {DocumentationUnitController.class})
class PreviousDecisionIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>("postgres:14").withInitScript("init_db.sql");

  private DocumentCategoryDTO category;
  private CourtDTO testCourt;

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("database.user", () -> postgreSQLContainer.getUsername());
    registry.add("database.password", () -> postgreSQLContainer.getPassword());
    registry.add("database.host", () -> postgreSQLContainer.getHost());
    registry.add("database.port", () -> postgreSQLContainer.getFirstMappedPort());
    registry.add("database.database", () -> postgreSQLContainer.getDatabaseName());
  }

  @Autowired private RisWebTestClient risWebTestClient;
  @Autowired private DatabaseDocumentationUnitRepository repository;
  @Autowired private DatabaseDocumentTypeRepository databaseDocumentTypeRepository;
  @Autowired private DatabaseCourtRepository databaseCourtRepository;
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  @Autowired private DatabaseHandoverReportRepository databaseHandoverReportRepository;
  @Autowired private DatabaseDocumentCategoryRepository databaseDocumentCategoryRepository;
  @Autowired private DatabaseFileNumberRepository databaseFileNumberRepository;

  @MockBean UserService userService;
  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private MailService mailService;
  @MockBean DocxConverterService docxConverterService;
  @MockBean AttachmentService attachmentService;
  @MockBean private PatchMapperService patchMapperService;
  @MockBean private HandoverService handoverService;
  @MockBean private ProcedureService procedureService;
  @MockBean private LdmlExporterService ldmlExporterService;

  @MockBean
  private DocumentationUnitDocxMetadataInitializationService
      documentationUnitDocxMetadataInitializationService;

  private final DocumentationOffice docOffice = buildDSDocOffice();
  private DocumentationOfficeDTO documentationOfficeDTO;
  private final AtomicInteger courtJurisId = new AtomicInteger(100);

  @BeforeEach
  void setUp() {
    documentationOfficeDTO =
        documentationOfficeRepository.findByAbbreviation(docOffice.abbreviation());
    doReturn(docOffice).when(userService).getDocumentationOffice(any(OidcUser.class));

    category =
        databaseDocumentCategoryRepository.saveAndFlush(
            DocumentCategoryDTO.builder().label("R").build());

    testCourt =
        databaseCourtRepository.save(
            CourtDTO.builder()
                .type("Court1")
                .location("Berlin")
                .jurisId(courtJurisId.getAndIncrement())
                .isForeignCourt(false)
                .isSuperiorCourt(false)
                .id(UUID.randomUUID())
                .build());
  }

  @AfterEach
  void cleanUp() {
    // has to be cleaned first to avoid foreign key constraint violation in the following deletions
    repository.deleteAll();
    databaseDocumentTypeRepository.deleteAll();
    databaseHandoverReportRepository.deleteAll();
    databaseCourtRepository.deleteAll();
    databaseDocumentCategoryRepository.delete(category);
    databaseFileNumberRepository.deleteAll();
  }

  @Test
  void
      testGetDocumentationUnit_withPreviousDecision_shouldReturnDocumentationUnitWithListOfExistingPreviousDecsion() {

    EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
        repository,
        DocumentationUnitDTO.builder()
            .documentationOffice(documentationOfficeDTO)
            .documentNumber("documntnumber")
            .previousDecisions(
                List.of(
                    PreviousDecisionDTO.builder()
                        .fileNumber("test")
                        .deviatingFileNumber("deviatest")
                        .rank(1)
                        .build())));

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/documntnumber")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              DocumentationUnit responseBody = response.getResponseBody();
              assertThat(responseBody.previousDecisions())
                  .extracting("fileNumber")
                  .containsExactly("test");
              assertThat(responseBody.previousDecisions())
                  .extracting("deviatingFileNumber")
                  .containsExactly("deviatest");
            });
  }

  @Test
  void testAddPreviousDecisionToEmptyPreviousDecisionList_shouldContainTheNewEntry() {
    DocumentationUnitDTO parentDocumentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, documentationOfficeDTO, "documntnumber");

    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .uuid(parentDocumentationUnitDTO.getId())
            .documentNumber("docnr12345678")
            .previousDecisions(List.of(PreviousDecision.builder().fileNumber("test").build()))
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + parentDocumentationUnitDTO.getId())
        .bodyValue(documentationUnit)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response ->
                assertThat(Objects.requireNonNull(response.getResponseBody()).previousDecisions())
                    .extracting("fileNumber")
                    .containsExactly("test"));
  }

  @Test
  void testLinkExistingPreviousDecision() {
    DocumentationUnitDTO parentDocumentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, documentationOfficeDTO, "1234567890123");

    var childDocumentationUnitDTO =
        repository.save(
            DocumentationUnitDTO.builder()
                .documentNumber("abcdefghjikl")
                .decisionDate(LocalDate.parse("2021-01-01"))
                .documentationOffice(documentationOfficeDTO)
                .build());

    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .uuid(parentDocumentationUnitDTO.getId())
            .documentNumber("docnr12345678")
            .previousDecisions(
                List.of(
                    PreviousDecision.builder()
                        .documentNumber(childDocumentationUnitDTO.getDocumentNumber())
                        .deviatingFileNumber("deviatest")
                        .build()))
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + parentDocumentationUnitDTO.getId())
        .bodyValue(documentationUnit)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody().previousDecisions())
                  .extracting("documentNumber")
                  .containsExactly("abcdefghjikl");
              assertThat(response.getResponseBody().previousDecisions().get(0).isReferenceFound())
                  .isTrue();
              assertThat(response.getResponseBody().previousDecisions())
                  .extracting("deviatingFileNumber")
                  .containsExactly("deviatest");
            });
  }

  @Test
  void testRemovePreviousDecision_withEmptyList_shouldRemoveAllPreviousDecisions() {
    DocumentationUnitDTO parentDocumentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository,
            DocumentationUnitDTO.builder()
                .documentationOffice(documentationOfficeDTO)
                .documentNumber("1234567890123")
                .previousDecisions(
                    List.of(PreviousDecisionDTO.builder().fileNumber("test").rank(1).build())));

    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .uuid(parentDocumentationUnitDTO.getId())
            .documentNumber("docnr12345678")
            .previousDecisions(Collections.emptyList())
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + parentDocumentationUnitDTO.getId())
        .bodyValue(documentationUnit)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> assertThat(response.getResponseBody().previousDecisions()).isEmpty());
  }

  @Test
  void testLinkTheSameDocumentationUnitsTwice() {
    DocumentationUnitDTO childDocumentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, documentationOfficeDTO, "xxx");

    DocumentationUnitDTO parentDocumentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, documentationOfficeDTO, "1234567890123");

    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .uuid(parentDocumentationUnitDTO.getId())
            .documentNumber("docnr12345678")
            .previousDecisions(
                List.of(
                    PreviousDecision.builder()
                        .uuid(childDocumentationUnitDTO.getId())
                        .documentNumber("xxx")
                        .build(),
                    PreviousDecision.builder()
                        .uuid(childDocumentationUnitDTO.getId())
                        .documentNumber("xxx")
                        .build()))
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + parentDocumentationUnitDTO.getId())
        .bodyValue(documentationUnit)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody().previousDecisions()).hasSize(2);
              assertThat(response.getResponseBody().previousDecisions().get(0))
                  .extracting("documentNumber")
                  .isEqualTo("xxx");
            });
  }

  @Test
  void testSearchForDocumentationUnitsByPreviousDecisionInput_noSearchCriteria_shouldMatchAll() {
    prepareDocumentationUnitDTOs();
    List<PreviousDecision> content =
        simulateAPICall(PreviousDecision.builder().build())
            .returnResult()
            .getResponseBody()
            .getContent();
    assertThat(content).hasSize(3);
  }

  @Test
  void testSearchForDocumentationUnitsByPreviousDecisionInput_onlyDate_shouldMatchOne() {
    LocalDate date1 = prepareDocumentationUnitDTOs();
    List<PreviousDecision> content =
        simulateAPICall(PreviousDecision.builder().decisionDate(date1).build())
            .returnResult()
            .getResponseBody()
            .getContent();
    assertThat(content).hasSize(1);
    PreviousDecision documentationUnit = content.get(0);
    assertThat(documentationUnit.getDecisionDate()).isEqualTo(date1);
  }

  @Test
  void testSearchForDocumentationUnitsByPreviousDecisionInput_onlyCourt_shouldMatchThree() {
    prepareDocumentationUnitDTOs();
    List<PreviousDecision> content =
        simulateAPICall(
                PreviousDecision.builder().court(Court.builder().type("Court1").build()).build())
            .returnResult()
            .getResponseBody()
            .getContent();
    assertThat(content).hasSize(3);
    PreviousDecision documentationUnit = content.get(0);
    assertThat(documentationUnit.getCourt()).extracting("type").isEqualTo("Court1");
  }

  @Test
  void testSearchForDocumentationUnitsByPreviousDecisionInput_onlyFileNumber_shouldMatchTwo() {
    prepareDocumentationUnitDTOs();
    List<PreviousDecision> content =
        simulateAPICall(PreviousDecision.builder().fileNumber("AkteX").build())
            .returnResult()
            .getResponseBody()
            .getContent();
    assertThat(content).hasSize(2);
    PreviousDecision documentationUnit = content.get(0);
    assertThat(documentationUnit.getFileNumber()).isEqualTo("AkteX");
  }

  @Test
  void
      testSearchForDocumentationUnitsByPreviousDecisionInput_onlyFileNumber_shouldNotMatchDocNumber() {
    prepareDocumentationUnitDTOs();
    List<PreviousDecision> content =
        simulateAPICall(PreviousDecision.builder().fileNumber("XX").build())
            .returnResult()
            .getResponseBody()
            .getContent();
    assertThat(content).isEmpty();
  }

  @Test
  void testSearchForDocumentationUnitsByPreviousDecisionInput_onlyDocumentType_shouldMatchOne() {
    prepareDocumentationUnitDTOs();
    DocumentType documentType =
        DocumentTypeTransformer.transformToDomain(
            databaseDocumentTypeRepository.findFirstByAbbreviationAndCategory("GH", category));
    List<PreviousDecision> content =
        simulateAPICall(PreviousDecision.builder().documentType(documentType).build())
            .returnResult()
            .getResponseBody()
            .getContent();
    assertThat(content).hasSize(1);
    PreviousDecision documentationUnit = content.get(0);
    assertThat(documentationUnit.getDocumentType().jurisShortcut()).isEqualTo("GH");
  }

  @Test
  void testSearchForDocumentationUnitsByPreviousDecisionInput_nullDocumentType_shouldAll() {
    prepareDocumentationUnitDTOs();
    List<PreviousDecision> content =
        simulateAPICall(PreviousDecision.builder().documentType(null).build())
            .returnResult()
            .getResponseBody()
            .getContent();
    assertThat(content).hasSize(3);
  }

  @Test
  void
      testSearchForDocumentationUnitsByPreviousDecisionInput_threeMatchingOneDoesNot_shouldMatchNothing() {
    LocalDate date1 = prepareDocumentationUnitDTOs();
    List<PreviousDecision> content =
        simulateAPICall(
                PreviousDecision.builder()
                    .decisionDate(date1)
                    .court(Court.builder().type("Court1").build())
                    .fileNumber("AkteX")
                    .documentType(
                        DocumentType.builder().uuid(UUID.randomUUID()).jurisShortcut("XY").build())
                    .build())
            .returnResult()
            .getResponseBody()
            .getContent();
    assertThat(content).isEmpty();
  }

  @Test
  void testSearchForDocumentationUnitsByPreviousDecisionInput_shouldOnlyFindPublishedOrMine() {
    LocalDate date = LocalDate.parse("2023-02-02");

    var du1 =
        createDocumentationUnit(date, List.of("AkteZ"), "EF", "DS", PublicationStatus.UNPUBLISHED);
    var du2 =
        createDocumentationUnit(date, List.of("AkteZ"), "EF", "DS", PublicationStatus.PUBLISHED);
    var du3 =
        createDocumentationUnit(
            date, List.of("AkteZ"), "EF", "DS", PublicationStatus.EXTERNAL_HANDOVER_PENDING);
    var du4 =
        createDocumentationUnit(
            date, List.of("AkteZ"), "EF", "CC-RIS", PublicationStatus.UNPUBLISHED);
    var du5 =
        createDocumentationUnit(
            date, List.of("AkteZ"), "EF", "CC-RIS", PublicationStatus.PUBLISHED);
    var du6 =
        createDocumentationUnit(
            date, List.of("AkteZ"), "EF", "CC-RIS", PublicationStatus.EXTERNAL_HANDOVER_PENDING);

    List<PreviousDecision> content =
        simulateAPICall(PreviousDecision.builder().fileNumber("AkteZ").build())
            .returnResult()
            .getResponseBody()
            .getContent();
    assertThat(content).hasSize(4);
    assertThat(content)
        .extracting(RelatedDocumentationUnit::getUuid)
        .doesNotContain(du4.getId(), du6.getId())
        .containsExactlyInAnyOrder(du1.getId(), du2.getId(), du3.getId(), du5.getId());
  }

  private LocalDate prepareDocumentationUnitDTOs() {
    LocalDate date1 = LocalDate.parse("2023-01-02");

    createDocumentationUnit(
        date1, List.of("AkteX", "AkteY"), "CD", "DS", PublicationStatus.PUBLISHED);

    LocalDate date2 = LocalDate.parse("2023-02-03");

    createDocumentationUnit(date2, null, "EF", "DS", PublicationStatus.PUBLISHED);

    LocalDate date3 = LocalDate.parse("2023-03-04");

    createDocumentationUnit(date3, List.of("AkteX"), "GH", "DS", PublicationStatus.PUBLISHED);
    return date1;
  }

  private RisBodySpec<SliceTestImpl<PreviousDecision>> simulateAPICall(
      PreviousDecision previousDecision) {
    return risWebTestClient
        .withDefaultLogin()
        .put()
        .uri(
            "/api/v1/caselaw/documentunits/search-linkable-documentation-units?pg=0&sz=30&documentNumber=KORE000000000")
        .bodyValue(previousDecision)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<>() {});
  }

  private DocumentationUnitDTO createDocumentationUnit(
      LocalDate decisionDate,
      List<String> fileNumbers,
      String documentTypeJurisShortcut,
      String documentOfficeLabel,
      PublicationStatus status) {

    DocumentTypeDTO documentTypeDTO = null;
    if (documentTypeJurisShortcut != null) {

      var documentType =
          databaseDocumentTypeRepository.findFirstByAbbreviationAndCategory(
              documentTypeJurisShortcut, category);

      if (documentType == null) {
        documentTypeDTO =
            DocumentTypeDTO.builder()
                .category(category)
                .label("ABC123")
                .multiple(true)
                .abbreviation(documentTypeJurisShortcut)
                .build();
        documentTypeDTO = databaseDocumentTypeRepository.saveAndFlush(documentTypeDTO);
      }
    }

    DocumentationOfficeDTO documentOffice =
        documentationOfficeRepository.findByAbbreviation(documentOfficeLabel);
    assertThat(documentOffice).isNotNull();

    return EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
        repository,
        DocumentationUnitDTO.builder()
            .id(UUID.randomUUID())
            .documentationOffice(documentOffice)
            .documentNumber("XX" + RandomStringUtils.randomAlphanumeric(11))
            .court(testCourt)
            .decisionDate(decisionDate)
            .documentType(documentTypeDTO)
            .documentationOffice(documentOffice)
            .fileNumbers(
                fileNumbers == null
                    ? new ArrayList<>()
                    : new ArrayList<>(
                        fileNumbers.stream()
                            .map(fn -> FileNumberDTO.builder().value(fn).rank(1L).build())
                            .toList())),
        status);
  }
}
