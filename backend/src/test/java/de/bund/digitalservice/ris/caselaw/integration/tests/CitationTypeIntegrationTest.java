package de.bund.digitalservice.ris.caselaw.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.CitationTypeController;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseCitationTypeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresCitationTypeRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentTypeRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.CitationTypeService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.citation.CitationType;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@RISIntegrationTest(
    imports = {
      CitationTypeService.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      PostgresCitationTypeRepositoryImpl.class,
      PostgresDocumentTypeRepositoryImpl.class,
      SecurityConfig.class,
      AuthService.class,
      TestConfig.class,
    },
    controllers = {CitationTypeController.class})
@Sql(scripts = {"classpath:citation_types_init.sql"})
@Sql(
    scripts = {"classpath:citation_types_cleanup.sql"},
    executionPhase = AFTER_TEST_METHOD)
class CitationTypeIntegrationTest {
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
  @Autowired private DatabaseCitationTypeRepository citationTypeRepository;

  @MockBean UserService userService;
  @MockBean private DocumentationUnitService documentationUnitService;
  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private ProcedureService procedureService;

  @AfterEach
  void cleanUp() {
    citationTypeRepository.deleteAll();
  }

  @Test
  void testGetAllCitationTypes() {
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/citationtypes")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(CitationType[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).hasSize(2);
              assertThat(response.getResponseBody()[0].label()).isEqualTo("Abgrenzung");
              assertThat(response.getResponseBody()[1].label()).isEqualTo("Anschluss");
            });
  }

  @Test
  void testGetCitationTypeBySearchString() {
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/citationtypes?q=abg")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(CitationType[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).hasSize(1);
              assertThat(response.getResponseBody()[0].label()).isEqualTo("Abgrenzung");
            });
  }
}
