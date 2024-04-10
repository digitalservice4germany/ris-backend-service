package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDefaultDocOffice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import de.bund.digitalservice.ris.caselaw.RisWebTestClient;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberGeneratorService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentUnitStatusService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseProcedureService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentNumberPatternConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentUnitController;
import de.bund.digitalservice.ris.caselaw.adapter.DocxConverterService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseKeywordRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresPublicationReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationOfficeTransformer;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.ContentRelatedIndexing;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.EmailPublishService;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@RISIntegrationTest(
    imports = {
      DocumentUnitService.class,
      DatabaseDocumentNumberGeneratorService.class,
      DatabaseDocumentNumberRecyclingService.class,
      DatabaseDocumentUnitStatusService.class,
      DatabaseProcedureService.class,
      PostgresPublicationReportRepositoryImpl.class,
      PostgresDocumentationUnitRepositoryImpl.class,
      PostgresJPAConfig.class,
      SecurityConfig.class,
      AuthService.class,
      TestConfig.class,
      DocumentNumberPatternConfig.class
    },
    controllers = {DocumentUnitController.class})
@Sql(scripts = {"classpath:keyword_init.sql"})
@Sql(
    scripts = {"classpath:keyword_cleanup.sql"},
    executionPhase = AFTER_TEST_METHOD)
class DocumentUnitKeywordIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>("postgres:14")
          .withInitScript("db/create_migration_scheme_and_extensions.sql");

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("database.user", () -> postgreSQLContainer.getUsername());
    registry.add("database.password", () -> postgreSQLContainer.getPassword());
    registry.add("database.host", () -> postgreSQLContainer.getHost());
    registry.add("database.port", () -> postgreSQLContainer.getFirstMappedPort());
    registry.add("database.database", () -> postgreSQLContainer.getDatabaseName());
  }

  @Autowired private RisWebTestClient risWebTestClient;
  @Autowired private DatabaseKeywordRepository keywordRepository;
  @Autowired private DatabaseDocumentationUnitRepository documentUnitRepository;
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  @Autowired private DocumentUnitService documentUnitService;
  @MockBean private UserService userService;
  @MockBean ReactiveClientRegistrationRepository clientRegistrationRepository;
  @MockBean private DocxConverterService docxConverterService;
  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private EmailPublishService publishService;
  @MockBean private AttachmentService attachmentService;

  private final DocumentationOffice docOffice = buildDefaultDocOffice();

  @BeforeEach
  void setUp() {
    DocumentationOfficeDTO docOfficeDTO = documentationOfficeRepository.findByAbbreviation("DS");

    when(userService.getDocumentationOffice(any()))
        .thenReturn(Mono.just(DocumentationOfficeTransformer.transformToDomain(docOfficeDTO)));
  }

  @Test
  void testGetAllKeywordsForDocumentUnit_withoutKeywords_shouldReturnEmptyList() {
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/documentnr003")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody().contentRelatedIndexing().keywords()).isEmpty();
            });
  }

  @Test
  void testGetAllKeywordsForDocumentUnit_withKeywords_shouldReturnList() {
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/documentnr001")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentUnit.class)
        // Todo replace InAnyOrder when ordered by rank
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().keywords())
                    .containsExactlyInAnyOrder("keyword1", "keyword2"));
  }

  @Test
  void testAddKeywordForDocumentUnit_shouldReturnListWithAllKeywords() {
    UUID uuid = UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3");

    DocumentUnit documentUnitFromFrontend =
        DocumentUnit.builder()
            .uuid(uuid)
            .documentNumber("documentnr001")
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder()
                    .keywords(List.of("keyword1", "keyword2", "keyword3"))
                    .build())
            .build();

    assertThat(keywordRepository.findAll()).hasSize(2);

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + uuid)
        .bodyValue(documentUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().keywords())
                    .containsExactlyInAnyOrder("keyword1", "keyword2", "keyword3"));

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/documentnr002")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentUnit.class)
        // Todo replace InAnyOrder when ordered by rank
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().keywords())
                    .containsExactlyInAnyOrder("keyword1"));

    assertThat(keywordRepository.findAll()).hasSize(3);
  }

  @Test
  void
      testAddExistingKeywordForDocumentUnit_shouldNotAddDuplicateKeywordAndReturnListWithAllKeywords() {
    UUID uuid = UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3");

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/documentnr001")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentUnit.class)
        // Todo replace InAnyOrder when ordered by rank
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().keywords())
                    .containsExactlyInAnyOrder("keyword1", "keyword2"));

    DocumentUnit documentUnitFromFrontend =
        DocumentUnit.builder()
            .uuid(uuid)
            .documentNumber("documentnr001")
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder().keywords(List.of("keyword1", "keyword2")).build())
            .build();

    assertThat(keywordRepository.findAll()).hasSize(2);

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + uuid)
        .bodyValue(documentUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().keywords())
                    .containsExactlyInAnyOrder("keyword1", "keyword2"));

    assertThat(keywordRepository.findAll()).hasSize(2);
  }

  @Test
  void testDeleteKeywordFromDocumentUnit_shouldReturnListWithAllRemainingKeywords() {
    UUID uuid = UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3");

    DocumentUnit documentUnitFromFrontend =
        DocumentUnit.builder()
            .uuid(uuid)
            .documentNumber("documentnr001")
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder().keywords(List.of("keyword1")).build())
            .build();

    assertThat(keywordRepository.findAll()).hasSize(2);

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + uuid)
        .bodyValue(documentUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().keywords())
                    .containsExactlyInAnyOrder("keyword1"));

    // Todo delete keywords, when no reference to any documentationunitId?
  }
}
