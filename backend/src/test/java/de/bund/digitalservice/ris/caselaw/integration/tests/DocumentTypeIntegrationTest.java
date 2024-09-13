package de.bund.digitalservice.ris.caselaw.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentTypeController;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentTypeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentTypeRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.DocumentTypeService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@RISIntegrationTest(
    imports = {
      DocumentTypeService.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      PostgresDocumentTypeRepositoryImpl.class,
      SecurityConfig.class,
      AuthService.class,
      TestConfig.class
    },
    controllers = {DocumentTypeController.class})
@Sql(
    scripts = {"classpath:document_types.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
class DocumentTypeIntegrationTest {
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
  @Autowired private DatabaseDocumentTypeRepository repository;
  @MockBean private UserService userService;
  @MockBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private DocumentationUnitService service;
  @MockBean private ProcedureService procedureService;

  @Test
  void testGetAllDocumentTypes() {
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documenttypes")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<List<DocumentType>>() {})
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody())
                  .extracting("jurisShortcut", "label")
                  .containsExactly(
                      Tuple.tuple("Amtsrechtliche Anordnung", "AmA"),
                      Tuple.tuple("Anordnung", "Ao"),
                      Tuple.tuple("Beschluss", "Bes"),
                      Tuple.tuple("Urteil", "Ur"));
            });
  }

  @Test
  void testGetDocumentTypesWithQuery() {
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documenttypes?q=Anord")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<List<DocumentType>>() {})
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody())
                  .extracting("jurisShortcut", "label")
                  .containsExactly(
                      Tuple.tuple("Anordnung", "Ao"),
                      Tuple.tuple("Amtsrechtliche Anordnung", "AmA"));
            });
  }
}
