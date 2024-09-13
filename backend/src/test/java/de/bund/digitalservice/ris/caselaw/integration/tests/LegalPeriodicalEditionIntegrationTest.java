package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDSDocOffice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.LegalPeriodicalEditionController;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresFieldOfLawRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresLegalPeriodicalEditionRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresLegalPeriodicalRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionRepository;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionService;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalRepository;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
      LegalPeriodicalEditionService.class,
      PostgresLegalPeriodicalEditionRepositoryImpl.class,
      PostgresLegalPeriodicalRepositoryImpl.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      PostgresFieldOfLawRepositoryImpl.class,
      SecurityConfig.class,
      AuthService.class,
      TestConfig.class
    },
    controllers = {LegalPeriodicalEditionController.class})
@Sql(scripts = {"classpath:legal_periodical_init.sql"})
@Sql(
    scripts = {"classpath:legal_periodical_cleanup.sql"},
    executionPhase = AFTER_TEST_METHOD)
class LegalPeriodicalEditionIntegrationTest {
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
  @Autowired private LegalPeriodicalEditionRepository repository;
  @Autowired private LegalPeriodicalRepository legalPeriodicalRepository;

  @MockBean private UserService userService;
  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private DocumentationUnitService service;
  @MockBean private DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  @MockBean private ProcedureService procedureService;

  @MockBean private HandoverService handoverService;

  private static final String EDITION_ENDPOINT = "/api/v1/caselaw/legalperiodicaledition";
  private final DocumentationOffice docOffice = buildDSDocOffice();

  @BeforeEach
  void setUp() {
    when(userService.getDocumentationOffice(any())).thenReturn(docOffice);
  }

  @Test
  void testGetLegalPeriodicalEditions_byLegalPerodical_shouldReturnValue() {

    var legalPeriodical =
        legalPeriodicalRepository.findAllBySearchStr(Optional.of("ABC")).stream()
            .findAny()
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "Legal periodical not found, check legal_periodical_init.sql"));

    repository.save(
        LegalPeriodicalEdition.builder()
            .id(UUID.randomUUID())
            .legalPeriodical(legalPeriodical)
            .name("2024 Sonderheft 1")
            .prefix("2024,")
            .suffix("- Sonderheft 1")
            .build());

    var editionList =
        Arrays.stream(
                risWebTestClient
                    .withDefaultLogin()
                    .get()
                    .uri(EDITION_ENDPOINT + "?legal_periodical_id=" + legalPeriodical.uuid())
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(LegalPeriodicalEdition[].class)
                    .returnResult()
                    .getResponseBody())
            .toList();

    Assertions.assertFalse(editionList.isEmpty(), "List should not be empty");
    Assertions.assertEquals("2024 Sonderheft 1", editionList.get(0).name());
  }

  @Test
  void testGetEditionById() {
    var legalPeriodical =
        legalPeriodicalRepository.findAllBySearchStr(Optional.of("ABC")).stream()
            .findAny()
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "Legal periodical not found, check legal_periodical_init.sql"));

    var saved =
        repository.save(
            LegalPeriodicalEdition.builder()
                .id(UUID.randomUUID())
                .legalPeriodical(legalPeriodical)
                .name("2024 Sonderheft 1")
                .prefix("2024,")
                .suffix("- Sonderheft 1")
                .build());

    Assertions.assertNotNull(saved.createdAt());

    var result =
        risWebTestClient
            .withDefaultLogin()
            .get()
            .uri(EDITION_ENDPOINT + "/" + saved.id())
            .exchange()
            .expectBody(LegalPeriodicalEdition.class)
            .returnResult()
            .getResponseBody();

    Assertions.assertEquals(saved, result);
  }

  @Test
  void testDeleteEditionWithoutReferences() {
    var legalPeriodical =
        legalPeriodicalRepository.findAllBySearchStr(Optional.of("ABC")).stream()
            .findAny()
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "Legal periodical not found, check legal_periodical_init.sql"));

    var legalPeriodicalEdition =
        LegalPeriodicalEdition.builder()
            .id(UUID.randomUUID())
            .legalPeriodical(legalPeriodical)
            .prefix("2024, ")
            .build();
    legalPeriodicalEdition = repository.save(legalPeriodicalEdition);
    assertThat(repository.findAllByLegalPeriodicalId(legalPeriodical.uuid())).hasSize(1);
    repository.delete(legalPeriodicalEdition);

    assertThat(repository.findAllByLegalPeriodicalId(legalPeriodical.uuid())).isEmpty();
  }
}
