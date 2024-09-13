package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDSDocOffice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import ch.qos.logback.classic.Level;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.TestMemoryAppender;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseUserGroupService;
import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.adapter.UserGroupController;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitProcedureRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseProcedureRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseUserGroupRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentNumberService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserGroup;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.util.List;
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
      DocumentationUnitService.class,
      PostgresDocumentationUnitRepositoryImpl.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      KeycloakUserService.class,
      DatabaseUserGroupService.class,
      SecurityConfig.class,
      AuthService.class,
      TestConfig.class,
    },
    controllers = {UserGroupController.class})
@Sql(scripts = {"classpath:doc_office_init.sql", "classpath:user_group_init.sql"})
@Sql(
    scripts = {"classpath:procedures_cleanup.sql"},
    executionPhase = AFTER_TEST_METHOD)
class UserGroupIntegrationTest {
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
  @Autowired private DatabaseDocumentationUnitRepository documentationUnitRepository;
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  @Autowired private DatabaseProcedureRepository repository;
  @Autowired private DatabaseDocumentationUnitProcedureRepository linkRepository;
  @Autowired private DatabaseUserGroupRepository userGroupRepository;

  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private DocumentNumberService numberService;
  @MockBean private DocumentNumberRecyclingService documentNumberRecyclingService;
  @MockBean private AttachmentService attachmentService;
  @MockBean private PatchMapperService patchMapperService;
  @MockBean private ProcedureService procedureService;

  @MockBean private DatabaseUserGroupService databaseUserGroupService;

  UserGroup internalUserGroup =
      UserGroup.builder()
          .docOffice(buildDSDocOffice())
          .userGroupPathName("/DS/Intern")
          .isInternal(true)
          .build();

  UserGroup externalUserGroup =
      UserGroup.builder()
          .docOffice(buildDSDocOffice())
          .userGroupPathName("/DS/Extern")
          .isInternal(false)
          .build();
  UserGroup externalUserGroup1 =
      UserGroup.builder()
          .docOffice(buildDSDocOffice())
          .userGroupPathName("/DS/Extern/Agentur1")
          .isInternal(false)
          .build();
  UserGroup externalUserGroup2 =
      UserGroup.builder()
          .docOffice(buildDSDocOffice())
          .userGroupPathName("/DS/Extern/Agentur2")
          .isInternal(false)
          .build();

  @BeforeEach()
  void beforeEach() {
    doReturn(List.of(internalUserGroup, externalUserGroup, externalUserGroup1, externalUserGroup2))
        .when(databaseUserGroupService)
        .getAllUserGroups();
  }

  @Test
  void testGetUserGroups_withInternalUser_shouldReturnExternalUserGroupsOfDocOffice() {
    doReturn(List.of(externalUserGroup, externalUserGroup1, externalUserGroup2))
        .when(databaseUserGroupService)
        .getExternalUserGroups(any());

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/user-group")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(UserGroup[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).hasSize(3);
              assertThat(response.getResponseBody()[0]).isEqualTo(externalUserGroup);
              assertThat(response.getResponseBody()[1]).isEqualTo(externalUserGroup1);
              assertThat(response.getResponseBody()[2]).isEqualTo(externalUserGroup2);
            });
  }

  @Test
  void testGetUserGroups_withExternalUser_shouldReturnExternalUserGroupsOfDocOffice() {
    doReturn(List.of(externalUserGroup, externalUserGroup1, externalUserGroup2))
        .when(databaseUserGroupService)
        .getExternalUserGroups(any());

    risWebTestClient
        .withExternalLogin()
        .get()
        .uri("/api/v1/caselaw/user-group")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(UserGroup[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).hasSize(3);
              assertThat(response.getResponseBody()[0]).isEqualTo(externalUserGroup);
              assertThat(response.getResponseBody()[1]).isEqualTo(externalUserGroup1);
              assertThat(response.getResponseBody()[2]).isEqualTo(externalUserGroup2);
            });
  }

  @Test
  void testGetUserGroups_withExternalUser_shouldReturnNoUserGroupsAndWarnings() {
    doReturn(List.of()).when(databaseUserGroupService).getExternalUserGroups(any());
    TestMemoryAppender memoryAppender = new TestMemoryAppender(KeycloakUserService.class);

    risWebTestClient
        .withLogin("/NOT-EXISTING")
        .get()
        .uri("/api/v1/caselaw/user-group")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(UserGroup[].class)
        .consumeWith(response -> assertThat(response.getResponseBody()).isEmpty());
    assertThat(memoryAppender.getMessage(Level.WARN, 0))
        .isEqualTo(
            "No doc office user group associated with given Keycloak user groups: [/NOT-EXISTING]");
  }
}
