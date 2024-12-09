package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.mockUserGroups;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.fasterxml.jackson.core.type.TypeReference;
import de.bund.digitalservice.ris.caselaw.EntityBuilderTestUtil;
import de.bund.digitalservice.ris.caselaw.SliceTestImpl;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberGeneratorService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentationUnitStatusService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseProcedureService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentNumberPatternConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentationUnitController;
import de.bund.digitalservice.ris.caselaw.adapter.DocxConverterService;
import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.adapter.LdmlExporterService;
import de.bund.digitalservice.ris.caselaw.adapter.OAuthService;
import de.bund.digitalservice.ris.caselaw.adapter.ProcedureController;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitProcedureRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseProcedureRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseUserGroupRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DeviatingFileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitProcedureDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.FileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDeltaMigrationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.ProcedureDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.UserGroupDTO;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitDocxMetadataInitializationService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitListItem;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.MailService;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.Status;
import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Slice;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
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
      DatabaseProcedureService.class,
      PostgresDocumentationUnitRepositoryImpl.class,
      PostgresHandoverReportRepositoryImpl.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      SecurityConfig.class,
      OAuthService.class,
      TestConfig.class,
      DocumentNumberPatternConfig.class,
      KeycloakUserService.class
    },
    controllers = {DocumentationUnitController.class, ProcedureController.class})
class DocumentationUnitSearchIntegrationTest {
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
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  @Autowired private DatabaseProcedureRepository procedureRepository;
  @Autowired private DatabaseUserGroupRepository userGroupRepository;

  @Autowired
  private DatabaseDocumentationUnitProcedureRepository documentationUnitProcedureRepository;

  @MockBean S3AsyncClient s3AsyncClient;
  @MockBean MailService mailService;
  @MockBean DocxConverterService docxConverterService;
  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean AttachmentService attachmentService;
  @MockBean private UserGroupService userGroupService;
  @MockBean private PatchMapperService patchMapperService;
  @MockBean private HandoverService handoverService;
  @MockBean private LdmlExporterService ldmlExporterService;

  @MockBean
  private DocumentationUnitDocxMetadataInitializationService
      documentationUnitDocxMetadataInitializationService;

  private DocumentationOfficeDTO docOfficeDTO;

  @BeforeEach
  void setUp() {
    docOfficeDTO = documentationOfficeRepository.findByAbbreviation("DS");
    mockUserGroups(userGroupService);
  }

  @AfterEach
  void cleanUp() {
    repository.deleteAll();
  }

  @Test
  void testForCorrectResponseWhenRequestingAll() {

    EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
        repository,
        DocumentationUnitDTO.builder()
            .id(UUID.randomUUID())
            .documentNumber("MIGR202200012")
            .documentationOffice(docOfficeDTO)
            .fileNumbers(List.of(FileNumberDTO.builder().value("AkteM").rank(0L).build())));

    EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
        repository,
        DocumentationUnitDTO.builder()
            .documentNumber("NEUR202300008")
            .documentationOffice(docOfficeDTO)
            .fileNumbers(List.of(FileNumberDTO.builder().value("AkteY").rank(0L).build())));

    Slice<DocumentationUnitListItem> responseBody =
        risWebTestClient
            .withDefaultLogin()
            .get()
            .uri("/api/v1/caselaw/documentunits/search?pg=0&sz=3")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
            .returnResult()
            .getResponseBody();
    assertThat(responseBody)
        .extracting("documentNumber", "fileNumber", "status")
        .containsExactly(
            tuple(
                "MIGR202200012",
                "AkteM",
                Status.builder().publicationStatus(PublicationStatus.PUBLISHED).build()),
            tuple(
                "NEUR202300008",
                "AkteY",
                Status.builder().publicationStatus(PublicationStatus.PUBLISHED).build()));
    assertThat(responseBody.getNumberOfElements()).isEqualTo(2);
  }

  @Test
  void testOrderedByDateDescending() {
    List<LocalDate> dates =
        Arrays.asList(
            LocalDate.of(2022, 1, 23),
            LocalDate.of(2022, 1, 23),
            null,
            LocalDate.of(2023, 3, 15),
            LocalDate.of(2023, 6, 7));

    for (LocalDate date : dates) {
      EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
          repository,
          DocumentationUnitDTO.builder()
              .documentNumber(RandomStringUtils.randomAlphabetic(13))
              .decisionDate(date)
              .documentationOffice(docOfficeDTO));
    }

    Slice<DocumentationUnitListItem> responseBody =
        risWebTestClient
            .withDefaultLogin()
            .get()
            .uri("/api/v1/caselaw/documentunits/search?pg=0&sz=10")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
            .returnResult()
            .getResponseBody();

    assertThat(responseBody)
        .extracting("decisionDate")
        .containsExactly(
            LocalDate.of(2023, 6, 7),
            LocalDate.of(2023, 3, 15),
            LocalDate.of(2022, 1, 23),
            LocalDate.of(2022, 1, 23),
            null);
  }

  @Test
  void test_searchByScheduledOnly_shouldReturnDescendingOrder() {
    List<LocalDateTime> scheduledPublicationDates =
        Arrays.asList(
            LocalDateTime.of(2022, 1, 23, 10, 5),
            LocalDateTime.of(2024, 1, 24, 10, 5),
            LocalDateTime.of(2022, 7, 24, 10, 5),
            LocalDateTime.of(2022, 1, 24, 10, 5),
            null,
            LocalDateTime.of(2022, 1, 23, 8, 5),
            LocalDateTime.of(2022, 1, 23, 10, 4));

    for (LocalDateTime date : scheduledPublicationDates) {
      EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
          repository,
          DocumentationUnitDTO.builder()
              .documentNumber(RandomStringUtils.randomAlphabetic(13))
              .scheduledPublicationDateTime(date)
              .documentationOffice(docOfficeDTO));
    }

    Slice<DocumentationUnitListItem> responseBody =
        risWebTestClient
            .withDefaultLogin()
            .get()
            .uri(
                "/api/v1/caselaw/documentunits/search?pg=0&sz=10&myDocOfficeOnly=true&scheduledOnly=true")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
            .returnResult()
            .getResponseBody();

    assertThat(responseBody)
        .extracting("scheduledPublicationDateTime")
        .containsExactly(
            LocalDateTime.of(2024, 1, 24, 10, 5),
            LocalDateTime.of(2022, 7, 24, 10, 5),
            LocalDateTime.of(2022, 1, 24, 10, 5),
            LocalDateTime.of(2022, 1, 23, 10, 5),
            LocalDateTime.of(2022, 1, 23, 10, 4),
            LocalDateTime.of(2022, 1, 23, 8, 5));
  }

  @Test
  void
      test_searchByScheduledOnlyWithPublicationDate_shouldReturnFilteredResultsInDescendingOrder() {
    List<LocalDateTime> scheduledPublicationDates =
        Arrays.asList(
            LocalDateTime.of(2022, 1, 23, 10, 5),
            LocalDateTime.of(2024, 1, 24, 10, 5),
            LocalDateTime.of(2022, 7, 24, 10, 5),
            LocalDateTime.of(2022, 1, 24, 10, 5),
            null,
            LocalDateTime.of(2022, 1, 23, 8, 5),
            LocalDateTime.of(2022, 1, 23, 10, 4));

    for (LocalDateTime date : scheduledPublicationDates) {
      EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
          repository,
          DocumentationUnitDTO.builder()
              .documentNumber(RandomStringUtils.randomAlphabetic(13))
              .scheduledPublicationDateTime(date)
              .documentationOffice(docOfficeDTO));
    }

    Slice<DocumentationUnitListItem> responseBody =
        risWebTestClient
            .withDefaultLogin()
            .get()
            .uri(
                "/api/v1/caselaw/documentunits/search?pg=0&sz=10&myDocOfficeOnly=true&publicationDate=2022-01-23&scheduledOnly=true")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
            .returnResult()
            .getResponseBody();

    assertThat(responseBody)
        .extracting("scheduledPublicationDateTime")
        .containsExactly(
            LocalDateTime.of(2022, 1, 23, 10, 5),
            LocalDateTime.of(2022, 1, 23, 10, 4),
            LocalDateTime.of(2022, 1, 23, 8, 5));
  }

  @Test
  void test_searchByPublicationDate_shouldFilterAndReturnDescendingOrder() {
    List<LocalDateTime> lastPublicationDates =
        Arrays.asList(
            LocalDateTime.of(2022, 1, 23, 10, 5),
            null,
            LocalDateTime.of(2022, 1, 23, 9, 5),
            LocalDateTime.of(2022, 1, 23, 19, 5),
            LocalDateTime.of(2022, 1, 23, 8, 5),
            LocalDateTime.of(2022, 1, 24, 10, 5),
            null);

    List<LocalDateTime> scheduledPublicationDates =
        Arrays.asList(
            null,
            LocalDateTime.of(2022, 1, 23, 5, 3),
            null,
            null,
            LocalDateTime.of(2022, 1, 23, 10, 10),
            null,
            LocalDateTime.of(2022, 1, 23, 12, 10));

    var index = 0;

    for (LocalDateTime date : lastPublicationDates) {
      EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
          repository,
          DocumentationUnitDTO.builder()
              .documentNumber(RandomStringUtils.randomAlphabetic(13))
              .lastPublicationDateTime(date)
              .scheduledPublicationDateTime(scheduledPublicationDates.get(index))
              .documentationOffice(docOfficeDTO));
      index++;
    }

    Slice<DocumentationUnitListItem> responseBody =
        risWebTestClient
            .withDefaultLogin()
            .get()
            .uri(
                "/api/v1/caselaw/documentunits/search?pg=0&sz=10&myDocOfficeOnly=true&publicationDate=2022-01-23")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
            .returnResult()
            .getResponseBody();

    /*
     * These are unlikely test conditions, because scheduled dates are in the future only and lastPublication dates in the past only.
     *
     * Ordered results visible for user (lastPublicationDate only if scheduledPublicationDate is null):
     * 2022-01-23 12:10
     * 2022-01-23 10:10
     * 2022-01-23 05:03
     * 2022-01-23 19:05
     * 2022-01-23 10:05
     * 2022-01-23 09:05
     */
    assertThat(responseBody)
        .extracting("scheduledPublicationDateTime")
        .containsExactly(
            LocalDateTime.of(2022, 1, 23, 12, 10),
            LocalDateTime.of(2022, 1, 23, 10, 10),
            LocalDateTime.of(2022, 1, 23, 5, 3),
            null,
            null,
            null);
    assertThat(responseBody)
        .extracting("lastPublicationDateTime")
        .containsExactly(
            null,
            LocalDateTime.of(2022, 1, 23, 8, 5),
            null,
            LocalDateTime.of(2022, 1, 23, 19, 5),
            LocalDateTime.of(2022, 1, 23, 10, 5),
            LocalDateTime.of(2022, 1, 23, 9, 5));
  }

  @Test
  void testForCorrectPagination() {
    EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
        repository, docOfficeDTO, "1234567801");

    EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
        repository, docOfficeDTO, "1234567802");

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/search?pg=0&sz=1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<SliceTestImpl<?>>() {})
        .consumeWith(response -> assertThat(response.getResponseBody().isLast()).isFalse());
  }

  @Test
  void testForCompleteResultListWhenSearchingForFileNumberOrDocumentNumber() {
    for (int i = 0; i < 10; i++) {
      EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
          repository,
          DocumentationUnitDTO.builder()
              // index 0-4 get a "AB" docNumber
              .documentNumber((i <= 4 ? "AB" : "GE") + "123456780" + i)
              .documentationOffice(docOfficeDTO)
              .fileNumbers(
                  // even indices get a fileNumber
                  i % 2 == 1
                      ? List.of()
                      : List.of(FileNumberDTO.builder().value("AB 34/" + i).rank(0L).build()))
              // index 4+ get a deviating fileNumber
              .deviatingFileNumbers(
                  i < 4
                      ? List.of()
                      : List.of(
                          DeviatingFileNumberDTO.builder().value("ABC 34/" + i).rank(0L).build())));
    }

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/search?pg=0&sz=4&documentNumberOrFileNumber=" + "AB")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
        .consumeWith(
            response -> {
              Slice<DocumentationUnitListItem> responseBodyFirstPage = response.getResponseBody();
              assertThat(responseBodyFirstPage.getNumberOfElements()).isEqualTo(4);
              // not to be the last page
              assertThat(responseBodyFirstPage.isFirst()).isTrue();
              assertThat(responseBodyFirstPage.isLast()).isFalse();
              // to have docNumbers "AB1234567800", "AB1234567801", "AB1234567802", "AB1234567803"
              assertThat(responseBodyFirstPage.getContent())
                  .extracting("documentNumber")
                  .containsExactly("AB1234567800", "AB1234567801", "AB1234567802", "AB1234567803");
            });

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/search?pg=1&sz=4&documentNumberOrFileNumber=" + "AB")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
        .consumeWith(
            response -> {
              // expect second page...
              Slice<DocumentationUnitListItem> responseBodySecondPage = response.getResponseBody();
              // to have 4 results
              assertThat(responseBodySecondPage.getNumberOfElements()).isEqualTo(4);
              // not to be the last or first page
              assertThat(responseBodySecondPage.isFirst()).isFalse();
              assertThat(responseBodySecondPage.isLast()).isFalse();
              // to have docNumbers "AB1234567804", "GE1234567805", "GE1234567806", "GE1234567807"
              assertThat(responseBodySecondPage.getContent())
                  .extracting("documentNumber")
                  .containsExactly("AB1234567804", "GE1234567805", "GE1234567806", "GE1234567807");
            });

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/search?pg=2&sz=4&documentNumberOrFileNumber=" + "AB")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
        .consumeWith(
            response -> {
              // expect third page...
              Slice<DocumentationUnitListItem> responseBodyThirdPage = response.getResponseBody();
              // to have 2 results
              assertThat(responseBodyThirdPage.getNumberOfElements()).isEqualTo(2);
              // not to be the first but to be the last page
              assertThat(responseBodyThirdPage.isFirst()).isFalse();
              assertThat(responseBodyThirdPage.isLast()).isTrue();
              // to have docNumbers "GE1234567808", "GE1234567809"
              assertThat(responseBodyThirdPage.getContent())
                  .extracting("documentNumber")
                  .containsExactly("GE1234567808", "GE1234567809");
            });
  }

  @Test
  void testTrim() {
    EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
        repository, docOfficeDTO, "AB1234567802");

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/search?pg=0&sz=10&documentNumberOrFileNumber=+++AB++")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().getContent())
                    .extracting("documentNumber")
                    .containsExactly("AB1234567802"));
  }

  @Test
  void testSearch_withInternalUser_shouldReturnEditableAndDeletableDocumentationUnit() {
    EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
        repository, docOfficeDTO, "AB1234567802");

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri(
            "/api/v1/caselaw/documentunits/search?pg=0&sz=10&documentNumberOrFileNumber=AB1234567802")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody().getContent())
                  .extracting("documentNumber")
                  .containsExactly("AB1234567802");
              assertThat(response.getResponseBody().getContent()).hasSize(1);
              assertThat(response.getResponseBody().getContent().get(0).isEditable()).isTrue();
              assertThat(response.getResponseBody().getContent().get(0).isDeletable()).isTrue();
            });
  }

  @Test
  void
      testSearch_withExternalUnassignedUser_shouldReturnNotEditableAndNotDeletableDocumentationUnit() {
    EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
        repository, docOfficeDTO, "AB1234567802");

    risWebTestClient
        .withExternalLogin()
        .get()
        .uri(
            "/api/v1/caselaw/documentunits/search?pg=0&sz=10&documentNumberOrFileNumber=AB1234567802")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody().getContent())
                  .extracting("documentNumber")
                  .containsExactly("AB1234567802");
              assertThat(response.getResponseBody().getContent()).hasSize(1);
              assertThat(response.getResponseBody().getContent().get(0).isEditable()).isFalse();
              assertThat(response.getResponseBody().getContent().get(0).isDeletable()).isFalse();
            });
  }

  @Test
  @Sql(
      scripts = {
        "classpath:doc_office_init.sql",
        "classpath:user_group_init.sql",
        "classpath:procedures_init.sql",
      })
  void testSearch_withExternalAssignedUser_shouldReturnEditableAndNotDeletableDocumentationUnit() {
    Optional<DocumentationUnitDTO> documentationUnitDTO =
        repository.findByDocumentNumber("docNumber00002");
    Optional<ProcedureDTO> procedureDTO =
        procedureRepository.findAllByLabelAndDocumentationOffice("procedure1", docOfficeDTO);
    Optional<UserGroupDTO> userGroupDTO =
        userGroupRepository.findById(UUID.fromString("2b733549-d2cc-40f0-b7f3-9bfa9f3c1b89"));
    DocumentationUnitProcedureDTO documentationUnitProcedureDTO =
        DocumentationUnitProcedureDTO.builder()
            .procedure(procedureDTO.get())
            .documentationUnit(documentationUnitDTO.get())
            .build();
    documentationUnitProcedureRepository.save(documentationUnitProcedureDTO);

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri(
            "/api/v1/caselaw/procedure/"
                + procedureDTO.get().getId()
                + "/assign/"
                + userGroupDTO.get().getId())
        .exchange()
        .expectStatus()
        .is2xxSuccessful();

    risWebTestClient
        .withExternalLogin()
        .get()
        .uri(
            "/api/v1/caselaw/documentunits/search?pg=0&sz=10&documentNumber="
                + documentationUnitDTO.get().getDocumentNumber())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody().getContent())
                  .extracting("documentNumber")
                  .containsExactly(documentationUnitDTO.get().getDocumentNumber());
              assertThat(response.getResponseBody().getContent()).hasSize(1);
              assertThat(response.getResponseBody().getContent().get(0).isEditable()).isTrue();
              assertThat(response.getResponseBody().getContent().get(0).isDeletable()).isFalse();
            });
  }
}
