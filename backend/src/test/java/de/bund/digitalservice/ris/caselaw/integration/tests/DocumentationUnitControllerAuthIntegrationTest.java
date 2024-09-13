package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.mockUserGroups;
import static de.bund.digitalservice.ris.caselaw.domain.PublicationStatus.PUBLISHED;
import static de.bund.digitalservice.ris.caselaw.domain.PublicationStatus.PUBLISHING;
import static de.bund.digitalservice.ris.caselaw.domain.PublicationStatus.UNPUBLISHED;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import de.bund.digitalservice.ris.caselaw.SliceTestImpl;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberGeneratorService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentationUnitStatusService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentNumberPatternConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentationUnitController;
import de.bund.digitalservice.ris.caselaw.adapter.DocxConverterService;
import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseStatusRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDeltaMigrationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.StatusDTO;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitDocxMetadataInitializationService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitListItem;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.MailService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.Status;
import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Slice;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@RISIntegrationTest(
    imports = {
      DocumentationUnitService.class,
      PostgresDeltaMigrationRepositoryImpl.class,
      AuthService.class,
      KeycloakUserService.class,
      DatabaseDocumentNumberGeneratorService.class,
      DatabaseDocumentNumberRecyclingService.class,
      DatabaseDocumentationUnitStatusService.class,
      PostgresDocumentationUnitRepositoryImpl.class,
      PostgresHandoverReportRepositoryImpl.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      SecurityConfig.class,
      AuthService.class,
      TestConfig.class,
      DocumentNumberPatternConfig.class
    },
    controllers = {DocumentationUnitController.class})
class DocumentationUnitControllerAuthIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>("postgres:14").withInitScript("init_db.sql");

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
  @Autowired private DatabaseStatusRepository statusRepository;
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;

  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private MailService mailService;
  @MockBean DocxConverterService docxConverterService;
  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean AttachmentService attachmentService;
  @MockBean private PatchMapperService patchMapperService;

  @MockBean private HandoverService handoverService;
  @MockBean UserGroupService userGroupService;
  @MockBean private ProcedureService procedureService;

  @MockBean
  private DocumentationUnitDocxMetadataInitializationService
      documentationUnitDocxMetadataInitializationService;

  static Stream<Arguments> getUnauthorizedCases() {
    return Stream.of(
        Arguments.of("CC-RIS", "/BGH", List.of(UNPUBLISHED)),
        Arguments.of("BGH", "/CC-RIS", List.of(UNPUBLISHED)));
  }

  static Stream<Arguments> getAuthorizedCases() {
    return Stream.of(
        Arguments.of("CC-RIS", "/BGH", List.of(PUBLISHED)),
        Arguments.of("CC-RIS", "/BGH", List.of(PUBLISHING)),
        Arguments.of("BGH", "/BGH", List.of(UNPUBLISHED)),
        Arguments.of("BGH", "/BGH", List.of(PUBLISHED)),
        Arguments.of("BGH", "/BGH", List.of(UNPUBLISHED, PUBLISHED)),
        Arguments.of("BGH", "/BGH", List.of(PUBLISHING)),
        Arguments.of("BGH", "/BGH", List.of(UNPUBLISHED, PUBLISHING)),
        Arguments.of("BGH", "/BGH", List.of(UNPUBLISHED, PUBLISHED, UNPUBLISHED)));
  }

  private DocumentationOfficeDTO ccRisOffice;
  private DocumentationOfficeDTO bghOffice;

  @BeforeEach
  void setUp() {
    // created via db migration V0_79__caselaw_insert_default_documentation_offices
    ccRisOffice = documentationOfficeRepository.findByAbbreviation("CC-RIS");
    bghOffice = documentationOfficeRepository.findByAbbreviation("BGH");

    mockUserGroups(userGroupService);
  }

  @AfterEach
  void cleanUp() {
    repository.deleteAll();
  }

  @ParameterizedTest
  @MethodSource("getAuthorizedCases")
  void testGetAll_shouldBeAccessible(
      String docUnitOfficeString, String userGroupPath, List<PublicationStatus> publicationStatus) {

    DocumentationOfficeDTO docUnitOffice;
    if (docUnitOfficeString.equals(bghOffice.getAbbreviation())) {
      docUnitOffice = bghOffice;
    } else {
      docUnitOffice = ccRisOffice;
    }

    DocumentationUnitDTO documentationUnitDTO =
        createNewDocumentationUnitDTO(UUID.randomUUID(), docUnitOffice);
    for (int i = 0; i < publicationStatus.size(); i++) {
      saveToStatusRepository(
          documentationUnitDTO,
          Instant.now().plusSeconds(60 + i),
          Status.builder().publicationStatus(publicationStatus.get(i)).build());
    }

    Slice<DocumentationUnitListItem> docUnitsSearchResult =
        risWebTestClient
            .withLogin(userGroupPath)
            .get()
            .uri("/api/v1/caselaw/documentunits/search?pg=0&sz=10")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
            .returnResult()
            .getResponseBody();

    var docUnit =
        docUnitsSearchResult.stream()
            .filter(unit -> unit.uuid().equals(documentationUnitDTO.getId()))
            .findFirst()
            .get();
    assertThat(docUnit.status().publicationStatus()).isEqualTo(getResultStatus(publicationStatus));

    risWebTestClient
        .withLogin(userGroupPath)
        .get()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDTO.getDocumentNumber())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .returnResult();
  }

  @ParameterizedTest
  @MethodSource("getUnauthorizedCases")
  void testGetAll_shouldNotBeAccessible(
      String docUnitOfficeAbbreviation,
      String userGroupPath,
      List<PublicationStatus> publicationStatus) {

    DocumentationOfficeDTO docUnitOffice;
    if (docUnitOfficeAbbreviation.equals(bghOffice.getAbbreviation())) {
      docUnitOffice = bghOffice;
    } else {
      docUnitOffice = ccRisOffice;
    }

    DocumentationUnitDTO documentationUnitDTO =
        createNewDocumentationUnitDTO(UUID.randomUUID(), docUnitOffice);
    for (int i = 0; i < publicationStatus.size(); i++) {
      saveToStatusRepository(
          documentationUnitDTO,
          Instant.now().plusSeconds(60 + i),
          Status.builder().publicationStatus(publicationStatus.get(i)).build());
    }

    Slice<DocumentationUnitListItem> docUnitsSearchResult =
        risWebTestClient
            .withLogin(userGroupPath)
            .get()
            .uri("/api/v1/caselaw/documentunits/search?pg=0&sz=10")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
            .returnResult()
            .getResponseBody();

    assertThat(docUnitsSearchResult)
        .noneMatch(result -> result.uuid().equals(documentationUnitDTO.getId()));

    risWebTestClient
        .withLogin(userGroupPath)
        .get()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDTO.getDocumentNumber())
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void testUnpublishedDocumentationUnitIsForbiddenForOtherOffice() {
    DocumentationUnitDTO documentationUnitDTO =
        createNewDocumentationUnitDTO(UUID.randomUUID(), ccRisOffice);
    saveToStatusRepository(
        documentationUnitDTO,
        Instant.now(),
        Status.builder().publicationStatus(UNPUBLISHED).build());

    // Documentation Office 1
    risWebTestClient
        .withLogin(("/CC-RIS"))
        .get()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDTO.getDocumentNumber())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody().uuid()).isEqualTo(documentationUnitDTO.getId());
            });

    // Documentation Office 2
    risWebTestClient
        .withLogin("/BGH")
        .get()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDTO.getDocumentNumber())
        .exchange()
        .expectStatus()
        .isForbidden();

    saveToStatusRepository(
        documentationUnitDTO,
        Instant.now().plus(1, ChronoUnit.DAYS),
        Status.builder().publicationStatus(PUBLISHING).build());

    risWebTestClient
        .withLogin(("/BGH"))
        .get()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDTO.getDocumentNumber())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().uuid())
                    .isEqualTo(documentationUnitDTO.getId()));

    saveToStatusRepository(
        documentationUnitDTO,
        Instant.now().plus(2, ChronoUnit.DAYS),
        Status.builder().publicationStatus(PUBLISHED).build());

    risWebTestClient
        .withLogin(("/BGH"))
        .get()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDTO.getDocumentNumber())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().uuid())
                    .isEqualTo(documentationUnitDTO.getId()));
  }

  private PublicationStatus getResultStatus(List<PublicationStatus> publicationStatus) {
    if (publicationStatus.isEmpty()) {
      return null;
    }

    return publicationStatus.get(publicationStatus.size() - 1);
  }

  private DocumentationUnitDTO createNewDocumentationUnitDTO(
      UUID documentationUnitUuid, DocumentationOfficeDTO documentationOffice) {
    String documentNumber =
        new Random().ints(13, 0, 10).mapToObj(Integer::toString).collect(Collectors.joining());
    return repository.save(
        DocumentationUnitDTO.builder()
            .id(documentationUnitUuid)
            .documentNumber(documentNumber)
            .documentationOffice(documentationOffice)
            .build());
  }

  private void saveToStatusRepository(
      DocumentationUnitDTO documentationUnitDTO, Instant createdAt, Status status) {
    statusRepository.save(
        StatusDTO.builder()
            .documentationUnitDTO(documentationUnitDTO)
            .publicationStatus(status.publicationStatus())
            .withError(status.withError())
            .createdAt(createdAt)
            .build());
  }
}
