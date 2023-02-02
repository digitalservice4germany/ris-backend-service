package de.bund.digitalservice.ris.caselaw.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentUnitController;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DatabaseDocumentUnitListEntryRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DatabaseDocumentUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DocumentUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.FileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.FileNumberRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.PostgresDocumentUnitListEntryRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.PostgresDocumentUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresConfig;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitListEntry;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitService;
import de.bund.digitalservice.ris.caselaw.domain.EmailPublishService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@RISIntegrationTest(
    imports = {
      DocumentUnitService.class,
      DatabaseDocumentNumberService.class,
      PostgresDocumentUnitRepositoryImpl.class,
      PostgresDocumentUnitListEntryRepositoryImpl.class,
      FlywayConfig.class,
      PostgresConfig.class
    },
    controllers = {DocumentUnitController.class})
public class DocumentUnitListEntryIntegrationTest {
  @Container
  static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer<>("postgres:12");

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("database.user", () -> postgreSQLContainer.getUsername());
    registry.add("database.password", () -> postgreSQLContainer.getPassword());
    registry.add("database.host", () -> postgreSQLContainer.getHost());
    registry.add("database.port", () -> postgreSQLContainer.getFirstMappedPort());
    registry.add("database.database", () -> postgreSQLContainer.getDatabaseName());
  }

  @MockBean S3AsyncClient s3AsyncClient;
  @MockBean EmailPublishService publishService;

  @Autowired private WebTestClient webClient;
  @Autowired private DatabaseDocumentUnitRepository repository;
  @Autowired private DatabaseDocumentUnitListEntryRepository listEntryRepository;
  @Autowired private FileNumberRepository fileNumberRepository;

  @BeforeEach
  void setUp() {
    repository.deleteAll().block();
    fileNumberRepository.deleteAll().block();
  }

  @Test
  void testForCorrectResponseWhenRequestingAll() {
    DocumentUnitDTO savedDto =
        repository
            .save(
                DocumentUnitDTO.builder()
                    .uuid(UUID.randomUUID())
                    .creationtimestamp(Instant.now())
                    .documentnumber("1234567890123")
                    .build())
            .block();

    fileNumberRepository
        .save(
            FileNumberDTO.builder()
                .documentUnitId(savedDto.getId())
                .fileNumber("AkteX")
                .isDeviating(false)
                .build())
        .block();

    webClient
        .mutateWith(csrf())
        .get()
        .uri("/api/v1/caselaw/documentunits")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentUnitListEntry[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody()).hasSize(1);
              assertThat(response.getResponseBody()[0].getDocumentNumber())
                  .isEqualTo("1234567890123");
              assertThat(response.getResponseBody()[0].getUuid()).isEqualTo(savedDto.getUuid());
              assertThat(response.getResponseBody()[0].getFileNumber()).isEqualTo("AkteX");
            });
  }
}
