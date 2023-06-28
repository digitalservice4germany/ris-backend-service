package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDefaultDocOffice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import de.bund.digitalservice.ris.caselaw.RisWebTestClient;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentUnitStatusService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentUnitController;
import de.bund.digitalservice.ris.caselaw.adapter.DocxConverterService;
import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.adapter.ProceedingDecisionController;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DatabaseDocumentUnitMetadataRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DatabaseDocumentUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DatabaseDocumentUnitStatusRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DatabaseDocumentationUnitLinkRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DatabasePublicationReportRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DocumentUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DocumentUnitMetadataDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DocumentUnitStatusDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DocumentationUnitLinkDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.FileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.FileNumberRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.PostgresDocumentUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.PostgresPublishReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.DatabaseDocumentTypeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.DataSource;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitStatus;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitLinkType;
import de.bund.digitalservice.ris.caselaw.domain.EmailPublishService;
import de.bund.digitalservice.ris.caselaw.domain.ProceedingDecision;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient.BodyContentSpec;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@RISIntegrationTest(
    imports = {
      DocumentUnitService.class,
      KeycloakUserService.class,
      DatabaseDocumentUnitStatusService.class,
      DatabaseDocumentNumberService.class,
      PostgresDocumentUnitRepositoryImpl.class,
      PostgresPublishReportRepositoryImpl.class,
      FlywayConfig.class,
      PostgresConfig.class,
      SecurityConfig.class,
      AuthService.class,
      TestConfig.class
    },
    controllers = {ProceedingDecisionController.class, DocumentUnitController.class})
class ProceedingDecisionIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:12");

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("database.user", () -> postgreSQLContainer.getUsername());
    registry.add("database.password", () -> postgreSQLContainer.getPassword());
    registry.add("database.host", () -> postgreSQLContainer.getHost());
    registry.add("database.port", () -> postgreSQLContainer.getFirstMappedPort());
    registry.add("database.database", () -> postgreSQLContainer.getDatabaseName());
  }

  @Autowired private RisWebTestClient risWebTestClient;
  @Autowired private DatabaseDocumentUnitRepository repository;
  @Autowired private DatabaseDocumentUnitMetadataRepository metadataRepository;
  @Autowired private DatabaseDocumentationUnitLinkRepository linkRepository;
  @Autowired private FileNumberRepository fileNumberRepository;
  @Autowired private DatabaseDocumentTypeRepository databaseDocumentTypeRepository;
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  @Autowired private DatabaseDocumentUnitStatusRepository statusRepository;

  @Autowired private DatabasePublicationReportRepository databasePublishReportRepository;

  @MockBean UserService userService;
  @MockBean ReactiveClientRegistrationRepository clientRegistrationRepository;
  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private EmailPublishService publishService;
  @MockBean DocxConverterService docxConverterService;

  private final DocumentationOffice docOffice = buildDefaultDocOffice();
  private UUID documentationOfficeUuid;

  @BeforeEach
  void setUp() {
    documentationOfficeUuid =
        documentationOfficeRepository.findByLabel(docOffice.label()).block().getId();
    doReturn(Mono.just(docOffice)).when(userService).getDocumentationOffice(any(OidcUser.class));
  }

  @AfterEach
  void cleanUp() {
    // has to be cleaned first to avoid foreign key constraint violation in the following deletions
    linkRepository.deleteAll().block();
    repository.deleteAll().block();
    metadataRepository.deleteAll().block();
    fileNumberRepository.deleteAll().block();
    databaseDocumentTypeRepository.deleteAll().block();
    statusRepository.deleteAll().block();
    databasePublishReportRepository.deleteAll().block();
  }

  // This test is flaky if executed locally, but reliable in the pipeline
  @Test
  void testAddProceedingDecisionLink() {
    UUID parentUuid = UUID.randomUUID();
    DocumentUnitDTO parentDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(parentUuid)
            .documentationOfficeId(documentationOfficeUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("documntnumber")
            .dataSource(DataSource.NEURIS)
            .build();
    repository.save(parentDocumentUnitDTO).block();

    ProceedingDecision proceedingDecision =
        ProceedingDecision.builder().dataSource(DataSource.PROCEEDING_DECISION).build();

    assertThat(
            linkRepository
                .findAllByParentDocumentationUnitUuidAndTypeOrderByIdAsc(
                    parentDocumentUnitDTO.getUuid(), DocumentationUnitLinkType.PREVIOUS_DECISION)
                .collectList()
                .block())
        .isEmpty();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + parentUuid + "/proceedingdecisions")
        .bodyValue(proceedingDecision)
        .exchange()
        .expectStatus()
        .is2xxSuccessful();

    assertThat(
            linkRepository
                .findAllByParentDocumentationUnitUuidAndTypeOrderByIdAsc(
                    parentDocumentUnitDTO.getUuid(), DocumentationUnitLinkType.PREVIOUS_DECISION)
                .collectList()
                .block())
        .hasSize(1);

    List<UUID> childUuids =
        linkRepository
            .findAllByParentDocumentationUnitUuidAndTypeOrderByIdAsc(
                parentDocumentUnitDTO.getUuid(), DocumentationUnitLinkType.PREVIOUS_DECISION)
            .map(DocumentationUnitLinkDTO::getChildDocumentationUnitUuid)
            .collectList()
            .block();

    childUuids.stream()
        .map(childUuid -> assertThat(repository.findByUuid(childUuid).block()).isNotNull());
  }

  @Test
  void testAddProceedingDecisionLink_alsoAppendsPreviousDecisionsToDocumentUnit() {
    UUID parentUuid = UUID.randomUUID();
    DocumentUnitDTO parentDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(parentUuid)
            .documentationOfficeId(documentationOfficeUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("documntnumber")
            .dataSource(DataSource.NEURIS)
            .build();
    parentDocumentUnitDTO = repository.save(parentDocumentUnitDTO).block();

    UUID childUuid = UUID.randomUUID();
    DocumentUnitDTO childDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(childUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("abcdefghjikl")
            .dataSource(DataSource.NEURIS)
            .build();
    childDocumentUnitDTO = repository.save(childDocumentUnitDTO).block();

    DocumentationUnitLinkDTO linkDTO =
        DocumentationUnitLinkDTO.builder()
            .parentDocumentationUnitUuid(parentDocumentUnitDTO.getUuid())
            .childDocumentationUnitUuid(childDocumentUnitDTO.getUuid())
            .type(DocumentationUnitLinkType.PREVIOUS_DECISION)
            .build();
    linkDTO = linkRepository.save(linkDTO).block();
    assertThat(linkDTO).isNotNull();

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/documntnumber")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentUnit.class)
        .consumeWith(
            response -> {
              DocumentUnit responseBody = response.getResponseBody();
              assertThat(responseBody.proceedingDecisions()).hasSize(1);
            });
  }

  @Test
  void testLinkExistingProceedingDecision() {
    UUID parentUuid = UUID.randomUUID();
    DocumentUnitDTO parentDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(parentUuid)
            .documentationOfficeId(documentationOfficeUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("1234567890123")
            .dataSource(DataSource.NEURIS)
            .build();
    parentDocumentUnitDTO = repository.save(parentDocumentUnitDTO).block();

    UUID childUuid = UUID.randomUUID();
    DocumentUnitDTO childDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(childUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("abcdefghjikl")
            .dataSource(DataSource.NEURIS)
            .build();
    childDocumentUnitDTO = repository.save(childDocumentUnitDTO).block();

    assertThat(
            linkRepository
                .findByParentDocumentationUnitUuidAndChildDocumentationUnitUuidAndType(
                    parentDocumentUnitDTO.getUuid(),
                    childDocumentUnitDTO.getUuid(),
                    DocumentationUnitLinkType.PREVIOUS_DECISION)
                .block())
        .isNull();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + parentUuid + "/proceedingdecisions/" + childUuid)
        .exchange()
        .expectStatus()
        .is2xxSuccessful();

    assertThat(
            linkRepository
                .findByParentDocumentationUnitUuidAndChildDocumentationUnitUuidAndType(
                    parentDocumentUnitDTO.getUuid(),
                    childDocumentUnitDTO.getUuid(),
                    DocumentationUnitLinkType.PREVIOUS_DECISION)
                .block())
        .isNotNull();
  }

  @Test
  void testRemoveProceedingDecisionLink() {
    UUID parentUuid = UUID.randomUUID();
    DocumentUnitDTO parentDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(parentUuid)
            .documentationOfficeId(documentationOfficeUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("1234567890123")
            .dataSource(DataSource.NEURIS)
            .build();
    parentDocumentUnitDTO = repository.save(parentDocumentUnitDTO).block();

    UUID childUuid = UUID.randomUUID();
    DocumentUnitDTO childDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(childUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("abcdefghjikl")
            .dataSource(DataSource.NEURIS)
            .build();
    childDocumentUnitDTO = repository.save(childDocumentUnitDTO).block();

    DocumentationUnitLinkDTO linkDTO =
        DocumentationUnitLinkDTO.builder()
            .parentDocumentationUnitUuid(parentDocumentUnitDTO.getUuid())
            .childDocumentationUnitUuid(childDocumentUnitDTO.getUuid())
            .type(DocumentationUnitLinkType.PREVIOUS_DECISION)
            .build();
    linkDTO = linkRepository.save(linkDTO).block();
    assertThat(linkDTO).isNotNull();

    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/" + parentUuid + "/proceedingdecisions/" + childUuid)
        .exchange()
        .expectStatus()
        .is2xxSuccessful();

    assertThat(linkRepository.findById(linkDTO.getId()).block()).isNull();
    assertThat(repository.findById(childDocumentUnitDTO.getId()).block()).isNotNull();
  }

  @Test
  void testRemoveProceedingDecisionLink_alsoRemovesProceedingDecisionFromDocumentUnit() {
    UUID parentUuid = UUID.randomUUID();
    DocumentUnitDTO parentDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(parentUuid)
            .documentationOfficeId(documentationOfficeUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("documntnumber")
            .dataSource(DataSource.NEURIS)
            .build();
    parentDocumentUnitDTO = repository.save(parentDocumentUnitDTO).block();

    UUID childUuid = UUID.randomUUID();
    DocumentUnitDTO childDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(childUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("abcdefghjikl")
            .dataSource(DataSource.NEURIS)
            .build();
    childDocumentUnitDTO = repository.save(childDocumentUnitDTO).block();

    DocumentationUnitLinkDTO linkDTO =
        DocumentationUnitLinkDTO.builder()
            .parentDocumentationUnitUuid(parentDocumentUnitDTO.getUuid())
            .childDocumentationUnitUuid(childDocumentUnitDTO.getUuid())
            .type(DocumentationUnitLinkType.PREVIOUS_DECISION)
            .build();
    linkDTO = linkRepository.save(linkDTO).block();
    assertThat(linkDTO).isNotNull();

    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/" + parentUuid + "/proceedingdecisions/" + childUuid)
        .exchange()
        .expectStatus()
        .is2xxSuccessful();

    assertThat(linkRepository.findById(linkDTO.getId()).block()).isNull();
    assertThat(repository.findById(childDocumentUnitDTO.getId()).block()).isNotNull();

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/documntnumber")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentUnit.class)
        .consumeWith(
            response -> {
              DocumentUnit responseBody = response.getResponseBody();
              assertThat(responseBody.proceedingDecisions()).isEmpty();
            });
  }

  @Test
  void testRemoveProceedingDecisionLinkAndDeleteOrphanedDocumentUnit() {
    UUID parentUuid = UUID.randomUUID();
    DocumentUnitDTO parentDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(parentUuid)
            .documentationOfficeId(documentationOfficeUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("1234567890123")
            .dataSource(DataSource.NEURIS)
            .build();
    parentDocumentUnitDTO = repository.save(parentDocumentUnitDTO).block();

    UUID childUuid = UUID.randomUUID();
    DocumentUnitDTO childDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(childUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("abcdefghjikl")
            .dataSource(DataSource.PROCEEDING_DECISION)
            .build();
    childDocumentUnitDTO = repository.save(childDocumentUnitDTO).block();

    DocumentationUnitLinkDTO linkDTO =
        DocumentationUnitLinkDTO.builder()
            .parentDocumentationUnitUuid(parentDocumentUnitDTO.getUuid())
            .childDocumentationUnitUuid(childDocumentUnitDTO.getUuid())
            .type(DocumentationUnitLinkType.PREVIOUS_DECISION)
            .build();
    linkDTO = linkRepository.save(linkDTO).block();
    assertThat(linkDTO).isNotNull();

    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/" + parentUuid + "/proceedingdecisions/" + childUuid)
        .exchange()
        .expectStatus()
        .is2xxSuccessful();

    assertThat(linkRepository.findById(linkDTO.getId()).block()).isNull();
    assertThat(repository.findById(childDocumentUnitDTO.getId()).block()).isNull();
  }

  @Test
  void testRemoveProceedingDecisionLinkAndKeepLinkedProceedingDecision() {
    UUID parentUuid1 = UUID.randomUUID();
    DocumentUnitDTO parentDocumentUnitDTO1 =
        DocumentUnitDTO.builder()
            .uuid(parentUuid1)
            .documentationOfficeId(documentationOfficeUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("1234567890123")
            .dataSource(DataSource.NEURIS)
            .build();
    parentDocumentUnitDTO1 = repository.save(parentDocumentUnitDTO1).block();

    UUID parentUuid2 = UUID.randomUUID();
    DocumentUnitDTO parentDocumentUnitDTO2 =
        DocumentUnitDTO.builder()
            .uuid(parentUuid2)
            .creationtimestamp(Instant.now())
            .documentnumber("1234567890124")
            .dataSource(DataSource.NEURIS)
            .build();
    parentDocumentUnitDTO2 = repository.save(parentDocumentUnitDTO2).block();

    UUID childUuid = UUID.randomUUID();
    DocumentUnitDTO childDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(childUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("abcdefghjikl")
            .dataSource(DataSource.PROCEEDING_DECISION)
            .build();
    childDocumentUnitDTO = repository.save(childDocumentUnitDTO).block();

    DocumentationUnitLinkDTO linkDTO1 =
        DocumentationUnitLinkDTO.builder()
            .parentDocumentationUnitUuid(parentDocumentUnitDTO1.getUuid())
            .childDocumentationUnitUuid(childDocumentUnitDTO.getUuid())
            .type(DocumentationUnitLinkType.PREVIOUS_DECISION)
            .build();
    linkDTO1 = linkRepository.save(linkDTO1).block();
    assertThat(linkDTO1).isNotNull();

    DocumentationUnitLinkDTO linkDTO2 =
        DocumentationUnitLinkDTO.builder()
            .parentDocumentationUnitUuid(parentDocumentUnitDTO2.getUuid())
            .childDocumentationUnitUuid(childDocumentUnitDTO.getUuid())
            .type(DocumentationUnitLinkType.PREVIOUS_DECISION)
            .build();
    linkDTO2 = linkRepository.save(linkDTO2).block();
    assertThat(linkDTO2).isNotNull();

    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/" + parentUuid1 + "/proceedingdecisions/" + childUuid)
        .exchange()
        .expectStatus()
        .is2xxSuccessful();

    assertThat(linkRepository.findById(linkDTO1.getId()).block()).isNull();
    assertThat(linkRepository.findById(linkDTO2.getId()).block()).isNotNull();
    assertThat(repository.findById(childDocumentUnitDTO.getId()).block()).isNotNull();
  }

  @Test
  void testRemoveNonExistingProceedingDecisionLink() {
    UUID parentUuid = UUID.randomUUID();
    DocumentUnitDTO parentDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(parentUuid)
            .documentationOfficeId(documentationOfficeUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("1234567890123")
            .dataSource(DataSource.NEURIS)
            .build();
    repository.save(parentDocumentUnitDTO).block();

    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri(
            "/api/v1/caselaw/documentunits/" + parentUuid + "/proceedingdecisions/" + "invalidUUID")
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  void testLinkTwoExistingDocumentUnits() {
    UUID parentUuid = UUID.randomUUID();
    DocumentUnitDTO parentDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(parentUuid)
            .documentationOfficeId(documentationOfficeUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("1234567890123")
            .dataSource(DataSource.NEURIS)
            .build();
    parentDocumentUnitDTO = repository.save(parentDocumentUnitDTO).block();

    UUID childUuid = UUID.randomUUID();
    DocumentUnitDTO childDocumentUnitDTO =
        DocumentUnitDTO.builder()
            .uuid(childUuid)
            .creationtimestamp(Instant.now())
            .documentnumber("abcdefghjikl")
            .dataSource(DataSource.NEURIS)
            .build();
    childDocumentUnitDTO = repository.save(childDocumentUnitDTO).block();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + parentUuid + "/proceedingdecisions/" + childUuid)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(DocumentUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().proceedingDecisions().size()).isEqualTo(1);
              assertThat(response.getResponseBody().proceedingDecisions().get(0).getUuid())
                  .isEqualTo(childUuid);
            });

    List<DocumentationUnitLinkDTO> list = linkRepository.findAll().collectList().block();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getParentDocumentationUnitUuid())
        .isEqualTo(parentDocumentUnitDTO.getUuid());
    assertThat(list.get(0).getChildDocumentationUnitUuid())
        .isEqualTo(childDocumentUnitDTO.getUuid());
  }

  @Test
  void testSearchForDocumentUnitsByProceedingDecisionInput_noSearchCriteria_shouldMatchAll() {
    prepareDocumentUnitMetadataDTOs();
    simulateAPICall(ProceedingDecision.builder().build())
        .jsonPath("$.content")
        .isNotEmpty()
        .jsonPath("$.content.length()")
        .isEqualTo(3);
  }

  @Test
  void testSearchForDocumentUnitsByProceedingDecisionInput_onlyDate_shouldMatchOne() {
    Instant date1 = prepareDocumentUnitMetadataDTOs();
    simulateAPICall(ProceedingDecision.builder().decisionDate(date1).build())
        .jsonPath("$.content")
        .isNotEmpty()
        .jsonPath("$.content.length()")
        .isEqualTo(1)
        .jsonPath("$.content[0].decisionDate")
        .isEqualTo(date1.toString());
  }

  @Test
  void testSearchForDocumentUnitsByProceedingDecisionInput_onlyCourt_shouldMatchOne() {
    prepareDocumentUnitMetadataDTOs();
    simulateAPICall(
            ProceedingDecision.builder().court(Court.builder().type("SomeCourt").build()).build())
        .jsonPath("$.content")
        .isNotEmpty()
        .jsonPath("$.content.length()")
        .isEqualTo(1)
        .jsonPath("$.content[0].court.type")
        .isEqualTo("SomeCourt");
  }

  @Test
  void testSearchForDocumentUnitsByProceedingDecisionInput_onlyFileNumber_shouldMatchTwo() {
    prepareDocumentUnitMetadataDTOs();
    simulateAPICall(ProceedingDecision.builder().fileNumber("AkteX").build())
        .jsonPath("$.content")
        .isNotEmpty()
        .jsonPath("$.content.length()")
        .isEqualTo(2)
        .jsonPath("$.content[0].fileNumber")
        .isEqualTo("AkteX");
  }

  @Test
  void testSearchForDocumentUnitsByProceedingDecisionInput_onlyDocumentType_shouldMatchOne() {
    prepareDocumentUnitMetadataDTOs();
    simulateAPICall(
            ProceedingDecision.builder()
                .documentType(DocumentType.builder().jurisShortcut("GH").build())
                .build())
        .jsonPath("$.content")
        .isArray()
        .jsonPath("$.content.length()")
        .isEqualTo(1)
        .jsonPath("$.content[0].documentType.jurisShortcut")
        .isEqualTo("GH");
  }

  @Test
  void
      testSearchForDocumentUnitsByProceedingDecisionInput_threeMatchingOneDoesNot_shouldMatchNothing() {
    Instant date1 = prepareDocumentUnitMetadataDTOs();
    simulateAPICall(
            ProceedingDecision.builder()
                .decisionDate(date1)
                .court(Court.builder().type("SomeCourt").build())
                .fileNumber("AkteX")
                .documentType(DocumentType.builder().jurisShortcut("XY").build())
                .build())
        .jsonPath("$.content.length()")
        .isEqualTo(0);
  }

  @Test
  void testSearchForDocumentUnitsByProceedingDecisionInput_shouldOnlyFindPublished() {
    Instant date = Instant.parse("2023-02-02T00:00:00.00Z");

    var du1 =
        createDocumentUnit(
            "Court1",
            "Berlin",
            date,
            List.of("AkteZ"),
            "EF",
            "DigitalService",
            DocumentUnitStatus.UNPUBLISHED);
    var du2 =
        createDocumentUnit(
            "Court2",
            "Berlin",
            date,
            List.of("AkteZ"),
            "EF",
            "DigitalService",
            DocumentUnitStatus.PUBLISHED);
    var du3 =
        createDocumentUnit(
            "Court3", "Berlin", date, List.of("AkteZ"), "EF", "DigitalService", null);

    var du4 =
        createDocumentUnit(
            "Court4",
            "Berlin",
            date,
            List.of("AkteZ"),
            "EF",
            "CC-RIS",
            DocumentUnitStatus.UNPUBLISHED);
    var du5 =
        createDocumentUnit(
            "Court5",
            "Berlin",
            date,
            List.of("AkteZ"),
            "EF",
            "CC-RIS",
            DocumentUnitStatus.PUBLISHED);
    var du6 = createDocumentUnit("Court6", "Berlin", date, List.of("AkteZ"), "EF", "CC-RIS", null);

    simulateAPICall(ProceedingDecision.builder().fileNumber("AkteZ").build())
        .jsonPath("$.content.length()")
        .isEqualTo(4)
        .jsonPath("$.content[?(@.uuid=='" + du1.getUuid() + "')]")
        .isEmpty()
        .jsonPath("$.content[?(@.uuid=='" + du2.getUuid() + "')]")
        .isArray()
        .jsonPath("$.content[?(@.uuid=='" + du3.getUuid() + "')]")
        .isArray()
        .jsonPath("$.content[?(@.uuid=='" + du4.getUuid() + "')]")
        .isEmpty()
        .jsonPath("$.content[?(@.uuid=='" + du5.getUuid() + "')]")
        .isArray()
        .jsonPath("$.content[?(@.uuid=='" + du6.getUuid() + "')]")
        .isArray();
  }

  private Instant prepareDocumentUnitMetadataDTOs() {
    Instant date1 = Instant.parse("2023-01-02T00:00:00.00Z");
    DocumentUnitMetadataDTO documentUnit1 =
        createDocumentUnit(
            "SomeCourt",
            "Berlin",
            date1,
            List.of("AkteX", "AkteY"),
            "CD",
            "DigitalService",
            DocumentUnitStatus.PUBLISHED);

    Instant date2 = Instant.parse("2023-02-03T00:00:00.00Z");
    DocumentUnitMetadataDTO documentUnit2 =
        createDocumentUnit(
            "AnotherCourt",
            "Hamburg",
            date2,
            null,
            "EF",
            "DigitalService",
            DocumentUnitStatus.PUBLISHED);

    Instant date3 = Instant.parse("2023-03-04T00:00:00.00Z");
    DocumentUnitMetadataDTO documentUnit3 =
        createDocumentUnit(
            "YetAnotherCourt",
            "Munich",
            date3,
            List.of("AkteX"),
            "GH",
            "DigitalService",
            DocumentUnitStatus.PUBLISHED);
    return date1;
  }

  private BodyContentSpec simulateAPICall(ProceedingDecision proceedingDecisionSearchInput) {
    return risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/search?pg=0&sz=30")
        .bodyValue(proceedingDecisionSearchInput)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody();
  }

  private DocumentUnitMetadataDTO createDocumentUnit(
      String courtType,
      String courtLocation,
      Instant decisionDate,
      List<String> fileNumbers,
      String documentTypeJurisShortcut,
      String documentOfficeLabel,
      DocumentUnitStatus status) {

    Long documentTypeId = null;
    if (documentTypeJurisShortcut != null) {
      DocumentTypeDTO documentTypeDTO =
          DocumentTypeDTO.builder()
              .changeIndicator('a')
              .documentType('b')
              .label("ABC123")
              .jurisShortcut(documentTypeJurisShortcut)
              .build();
      documentTypeId = databaseDocumentTypeRepository.save(documentTypeDTO).block().getId();
    }

    DocumentationOfficeDTO documentOffice =
        documentationOfficeRepository.findByLabel(documentOfficeLabel).block();
    assertThat(documentOffice).isNotNull();

    DocumentUnitMetadataDTO documentUnitMetadataDTO =
        DocumentUnitMetadataDTO.builder()
            .uuid(UUID.randomUUID())
            .documentationOfficeId(documentOffice.getId())
            .documentnumber(RandomStringUtils.randomAlphanumeric(13))
            .creationtimestamp(Instant.now())
            .courtType(courtType)
            .courtLocation(courtLocation)
            .decisionDate(decisionDate)
            .documentTypeId(documentTypeId)
            .dataSource(DataSource.NEURIS)
            .documentationOfficeId(documentOffice.getId())
            .build();
    Long id = metadataRepository.save(documentUnitMetadataDTO).block().getId();

    List<FileNumberDTO> fileNumberDTOs;
    if (fileNumbers != null) {
      fileNumberDTOs =
          fileNumbers.stream()
              .map(fn -> FileNumberDTO.builder().fileNumber(fn).documentUnitId(id).build())
              .collect(Collectors.toList());
      fileNumberRepository.saveAll(fileNumberDTOs).collectList().block();
    }

    if (status == null) {
      return documentUnitMetadataDTO;
    }

    assertThat(
            statusRepository
                .save(
                    DocumentUnitStatusDTO.builder()
                        .id(UUID.randomUUID())
                        .documentUnitId(documentUnitMetadataDTO.getUuid())
                        .status(status)
                        .newEntry(true)
                        .build())
                .block())
        .isNotNull();
    return documentUnitMetadataDTO;
  }
}
