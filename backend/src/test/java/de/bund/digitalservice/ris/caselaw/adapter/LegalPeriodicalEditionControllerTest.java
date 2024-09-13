package de.bund.digitalservice.ris.caselaw.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseApiKeyRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = LegalPeriodicalEditionController.class)
@Import({SecurityConfig.class, TestConfig.class, AuthService.class, KeycloakUserService.class})
class LegalPeriodicalEditionControllerTest {
  @Autowired private RisWebTestClient risWebTestClient;
  @Autowired private AuthService authService;
  @Autowired private KeycloakUserService keycloakUserService;
  @MockBean private LegalPeriodicalEditionService service;

  @MockBean private HandoverService handoverService;
  @MockBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private DatabaseApiKeyRepository databaseApiKeyRepository;
  @MockBean private DatabaseDocumentationOfficeRepository databaseDocumentationOfficeRepository;
  @MockBean private UserGroupService userGroupService;
  @MockBean private DocumentationUnitService documentationUnitService;
  @MockBean private ProcedureService procedureService;

  private final String EDITION_ENDPOINT = "/api/v1/caselaw/legalperiodicaledition";

  @Test
  void testGetLegalPeriodicalEditionById_shouldReturnValue() {
    UUID uuid = UUID.randomUUID();
    var edition =
        LegalPeriodicalEdition.builder()
            .id(UUID.randomUUID())
            .legalPeriodical(LegalPeriodical.builder().uuid(uuid).abbreviation("ABC").build())
            .name("2024 Sonderheft 1")
            .prefix("2024,")
            .suffix("- Sonderheft 1")
            .build();

    when(service.getById(uuid)).thenReturn(Optional.ofNullable(edition));

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri(EDITION_ENDPOINT + "/" + uuid)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void testGetLegalPeriodicalEditions_withNoEditions_shouldCallService() {
    UUID uuid = UUID.randomUUID();
    when(service.getLegalPeriodicalEditions(uuid)).thenReturn(null);

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri(EDITION_ENDPOINT + "?legal_periodical_id=" + uuid)
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getLegalPeriodicalEditions(uuid);
  }

  @Test
  void testGetLegalPeriodicalEditions_withNoLegalPeriodicalId_shouldReturnError() {
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri(EDITION_ENDPOINT)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  void testGetLegalPeriodicalEditions_withEditions_shouldCallServiceAndReturnResult() {
    UUID uuid = UUID.randomUUID();
    List<LegalPeriodicalEdition> editions =
        List.of(
            LegalPeriodicalEdition.builder()
                .id(UUID.randomUUID())
                .legalPeriodical(LegalPeriodical.builder().uuid(uuid).abbreviation("ABC").build())
                .name("2024")
                .prefix("2024,")
                .build(),
            LegalPeriodicalEdition.builder()
                .id(UUID.randomUUID())
                .legalPeriodical(LegalPeriodical.builder().uuid(uuid).abbreviation("ABC").build())
                .name("2024 Sonderheft 1")
                .prefix("2024,")
                .suffix("- Sonderheft 1")
                .build());
    when(service.getLegalPeriodicalEditions(uuid)).thenReturn(editions);

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri(EDITION_ENDPOINT + "?legal_periodical_id=" + uuid)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(LegalPeriodicalEdition[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).hasSize(2);
              assertThat(response.getResponseBody())
                  .containsExactlyInAnyOrder(editions.get(0), editions.get(1));
            });

    verify(service, times(1)).getLegalPeriodicalEditions(uuid);
  }

  @Test
  void testSaveLegalPeriodicalEditions_shouldCallService() {
    UUID uuid = UUID.randomUUID();
    var edition =
        LegalPeriodicalEdition.builder()
            .id(UUID.randomUUID())
            .legalPeriodical(LegalPeriodical.builder().uuid(uuid).abbreviation("ABC").build())
            .name("2024 Sonderheft 1")
            .prefix("2024,")
            .suffix("- Sonderheft 1")
            .build();
    when(service.saveLegalPeriodicalEdition(edition)).thenReturn(edition);

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri(EDITION_ENDPOINT)
        .bodyValue(edition)
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).saveLegalPeriodicalEdition(edition);
  }

  @Test
  void testSaveLegalPeriodicalEditions_withoutEdition_shouldReturn4xx() {
    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri(EDITION_ENDPOINT)
        .bodyValue(null)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  void testSaveLegalPeriodicalEditions_withExternalUser_shouldBeForbidden() {
    UUID uuid = UUID.randomUUID();
    var edition =
        LegalPeriodicalEdition.builder()
            .id(UUID.randomUUID())
            .legalPeriodical(LegalPeriodical.builder().uuid(uuid).abbreviation("ABC").build())
            .name("2024 Sonderheft 1")
            .prefix("2024,")
            .suffix("- Sonderheft 1")
            .build();

    risWebTestClient
        .withExternalLogin()
        .put()
        .uri(EDITION_ENDPOINT)
        .bodyValue(edition)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void testDeleteLegalPeriodicalEdition_withoutReferences_shouldSucceed() {
    UUID uuid = UUID.randomUUID();
    when(service.delete(uuid)).thenReturn(true);

    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri(EDITION_ENDPOINT + "/" + uuid)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void testDeleteLegalPeriodicalEdition_withReferences_shouldReturnUnchanged() {
    UUID uuid = UUID.randomUUID();
    when(service.delete(uuid)).thenReturn(false);

    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri(EDITION_ENDPOINT + "/" + uuid)
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  @Test
  void testDeleteLegalPeriodicalEditions_withExternalUser_shouldBeForbidden() {
    UUID uuid = UUID.randomUUID();
    when(service.delete(uuid)).thenReturn(false);

    risWebTestClient
        .withExternalLogin()
        .delete()
        .uri(EDITION_ENDPOINT + "/" + uuid)
        .exchange()
        .expectStatus()
        .isForbidden();
  }
}
