package de.bund.digitalservice.ris.caselaw.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.CourtController;
import de.bund.digitalservice.ris.caselaw.adapter.OAuthService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.CourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseCourtRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.JurisdictionTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresCourtRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentTypeRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.CourtService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
      CourtService.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      PostgresCourtRepositoryImpl.class,
      PostgresDocumentTypeRepositoryImpl.class,
      SecurityConfig.class,
      OAuthService.class,
      TestConfig.class,
    },
    controllers = {CourtController.class})
@Sql(
    scripts = {"classpath:doc_office_init.sql"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourtIntegrationTest {
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
  @Autowired private DatabaseCourtRepository databaseCourtRepository;

  @MockBean UserService userService;
  @MockBean private DocumentationUnitService documentationUnitService;
  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private ProcedureService procedureService;

  @AfterEach
  void cleanUp() {
    databaseCourtRepository.deleteAll();
  }

  @Test
  void testGetAllCourts() {
    CourtDTO courtDTO1 =
        CourtDTO.builder()
            .jurisId(0)
            .type("AB")
            .location("Berlin")
            .isSuperiorCourt(false)
            .isForeignCourt(false)
            .additionalInformation("- aufgehoben: 1975-02-01 -")
            .jurisdictionType(
                JurisdictionTypeDTO.builder()
                    .id(
                        UUID.fromString(
                            "27e099f9-5b47-4ce9-ac58-b84ca4643bc2")) // see doc_office_init.sql
                    .build())
            .build();
    databaseCourtRepository.save(courtDTO1);
    CourtDTO courtDTO2 =
        CourtDTO.builder()
            .jurisId(1)
            .type("BGH")
            .location("Karlsruhe")
            .isSuperiorCourt(true)
            .isForeignCourt(false)
            .build();
    databaseCourtRepository.save(courtDTO2);
    CourtDTO courtDTO3 =
        CourtDTO.builder()
            .jurisId(2)
            .type("LG")
            .location("Glückstadt")
            .isSuperiorCourt(false)
            .isForeignCourt(false)
            .additionalInformation("aufgehoben")
            .build();
    databaseCourtRepository.save(courtDTO3);
    CourtDTO courtDTO4 =
        CourtDTO.builder()
            .jurisId(3)
            .type("OLG")
            .location("Altdorf")
            .isSuperiorCourt(false)
            .isForeignCourt(false)
            .additionalInformation("andere Information")
            .build();
    databaseCourtRepository.save(courtDTO4);

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/courts")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(Court[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).hasSize(4);
              var court1 = response.getResponseBody()[0];
              assertThat(court1.label()).isEqualTo("AB Berlin");
              assertThat(court1.revoked()).isEqualTo("aufgehoben seit: 1975");
              assertThat(court1.responsibleDocOffice().abbreviation()).isEqualTo("BGH");

              var court2 = response.getResponseBody()[1];
              assertThat(court2.label()).isEqualTo("BGH");
              assertThat(court2.revoked()).isNull();

              var court3 = response.getResponseBody()[2];
              assertThat(court3.label()).isEqualTo("LG Glückstadt");
              assertThat(court3.revoked()).isEqualTo("aufgehoben");

              var court4 = response.getResponseBody()[3];
              assertThat(court4.label()).isEqualTo("OLG Altdorf");
              assertThat(court4.revoked()).isNull();
            });
  }

  @Test
  void testGetCourtsBySearchString() {
    String[][] courtData = {
      {"Kammer für Baulandsachen", "Ulm"},
      {"Gericht", "Potsdam"}, // not expected to be in results
      {"Landsitzungskammer", "Hamburg"},
      {"Verwaltungsgericht der Landeskirche", "Frankfurt"},
      {"England", "Court"},
      {"Landgericht", "Amberg"},
      {"Jugendgericht des Haupt-Landes", "München"},
    };

    for (int i = 0; i < courtData.length; i++) {
      String[] court = courtData[i];
      databaseCourtRepository.save(
          CourtDTO.builder()
              .jurisId(i)
              .type(court[0])
              .location(court[1])
              .isSuperiorCourt(false)
              .isForeignCourt(false)
              .build());
    }

    // expected order: alphabetically within 3 priority classes:
    List<String> expectedOrder =
        Arrays.asList(
            "Landgericht Amberg", // [1]
            "Landsitzungskammer Hamburg", // [1]
            "Jugendgericht des Haupt-Landes München", // [2]
            "Verwaltungsgericht der Landeskirche Frankfurt", // [2]
            "England Court", // [3]
            "Kammer für Baulandsachen Ulm" // [3]
            );

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/courts?q=land")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(Court[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).hasSize(6);
              for (int i = 0; i < expectedOrder.size(); i++) {
                assertThat(response.getResponseBody()[i].label()).isEqualTo(expectedOrder.get(i));
              }
            });
  }
}
