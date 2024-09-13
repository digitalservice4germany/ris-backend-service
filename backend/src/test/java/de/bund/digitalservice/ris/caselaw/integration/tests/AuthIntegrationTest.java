package de.bund.digitalservice.ris.caselaw.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthController;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.User;
import de.bund.digitalservice.ris.caselaw.domain.UserGroup;
import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@RISIntegrationTest(
    imports = {
      PostgresJPAConfig.class,
      FlywayConfig.class,
      KeycloakUserService.class,
      SecurityConfig.class,
      TestConfig.class,
      AuthService.class
    },
    controllers = {AuthController.class})
class AuthIntegrationTest {
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
  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean DocumentationUnitService documentationUnitService;
  @MockBean UserGroupService userGroupService;
  @MockBean private ProcedureService procedureService;

  @BeforeEach
  public void beforeEach() {
    doReturn(
            List.of(
                UserGroup.builder()
                    .docOffice(DocumentationOffice.builder().abbreviation("CC-RIS").build())
                    .userGroupPathName("/CC-RIS")
                    .build(),
                UserGroup.builder()
                    .docOffice(DocumentationOffice.builder().abbreviation("DS").build())
                    .userGroupPathName("/DS")
                    .build()))
        .when(userGroupService)
        .getAllUserGroups();
  }

  @Test
  void testGetUser() {
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/auth/me")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(User.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody().name()).isEqualTo("testUser");
              assertThat(response.getResponseBody().documentationOffice().abbreviation())
                  .isEqualTo("DS");
            });
  }

  @Test
  void testGetUserForOtherOffice() {
    risWebTestClient
        .withLogin("/CC-RIS")
        .get()
        .uri("/api/v1/auth/me")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(User.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().name()).isEqualTo("testUser");
              assertThat(response.getResponseBody().documentationOffice().abbreviation())
                  .isEqualTo("CC-RIS");
            });
  }

  @Test
  void testGetUserNameWithoutKnownGroup() {
    risWebTestClient
        .withLogin("foo")
        .get()
        .uri("/api/v1/auth/me")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(User.class)
        .consumeWith(
            response -> assertThat(response.getResponseBody().name()).isEqualTo("testUser"));
  }
}
