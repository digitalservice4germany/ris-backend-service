package de.bund.digitalservice.ris.caselaw.adapter.authorization;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDocOffice;
import static de.bund.digitalservice.ris.caselaw.AuthUtils.setUpDocumentationOfficeMocks;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.RisWebTestClient;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.adapter.ProceedingDecisionController;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitLinkType;
import de.bund.digitalservice.ris.caselaw.domain.ProceedingDecision;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = ProceedingDecisionController.class)
@Import({SecurityConfig.class, AuthService.class, TestConfig.class})
class ProceedingDecisionControllerAuthTest {
  @Autowired private RisWebTestClient risWebTestClient;

  @MockBean private DocumentUnitService service;
  @MockBean private KeycloakUserService userService;
  @MockBean ReactiveClientRegistrationRepository clientRegistrationRepository;

  private static final UUID TEST_UUID = UUID.fromString("88888888-4444-4444-4444-121212121212");
  private static final UUID CHILD_UUID = UUID.fromString("99999999-5555-5555-5555-232323232323");
  private final String docOffice1Group = "/CC-RIS";
  private final String docOffice2Group = "/caselaw/BGH";
  private final DocumentationOffice docOffice1 = buildDocOffice("CC-RIS");
  private final DocumentationOffice docOffice2 = buildDocOffice("BGH");

  @BeforeEach
  void setUp() {
    setUpDocumentationOfficeMocks(
        userService, docOffice1, docOffice1Group, docOffice2, docOffice2Group);
  }

  @Test
  void testCreateProceedingDecision() {
    mockDocumentUnit(docOffice1);
    when(service.createLinkedDocumentationUnit(
            any(UUID.class),
            any(ProceedingDecision.class),
            any(DocumentationOffice.class),
            eq(DocumentationUnitLinkType.PREVIOUS_DECISION)))
        .thenReturn(Mono.empty());

    String uri = "/api/v1/caselaw/documentunits/" + TEST_UUID + "/proceedingdecisions";

    risWebTestClient
        .withLogin(docOffice1Group)
        .put()
        .uri(uri)
        .bodyValue(ProceedingDecision.builder().build())
        .exchange()
        .expectStatus()
        .isOk();

    risWebTestClient
        .withLogin(docOffice2Group)
        .put()
        .uri(uri)
        .bodyValue(ProceedingDecision.builder().build())
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void testLinkProceedingDecision() {
    mockDocumentUnit(docOffice2);
    when(service.linkLinkedDocumentationUnit(
            TEST_UUID, CHILD_UUID, DocumentationUnitLinkType.PREVIOUS_DECISION))
        .thenReturn(Mono.empty());

    String uri =
        "/api/v1/caselaw/documentunits/" + TEST_UUID + "/proceedingdecisions/" + CHILD_UUID;

    risWebTestClient
        .withLogin(docOffice1Group)
        .put()
        .uri(uri)
        .exchange()
        .expectStatus()
        .isForbidden();

    risWebTestClient.withLogin(docOffice2Group).put().uri(uri).exchange().expectStatus().isOk();
  }

  @Test
  void testRemoveProceedingDecision() {
    mockDocumentUnit(docOffice1);
    when(service.removeLinkedDocumentationUnit(
            TEST_UUID, CHILD_UUID, DocumentationUnitLinkType.PREVIOUS_DECISION))
        .thenReturn(Mono.empty());

    String uri =
        "/api/v1/caselaw/documentunits/" + TEST_UUID + "/proceedingdecisions/" + CHILD_UUID;

    risWebTestClient.withLogin(docOffice1Group).delete().uri(uri).exchange().expectStatus().isOk();

    risWebTestClient
        .withLogin(docOffice2Group)
        .delete()
        .uri(uri)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  private void mockDocumentUnit(DocumentationOffice docOffice) {
    when(service.getByUuid(TEST_UUID))
        .thenReturn(
            Mono.just(
                DocumentUnit.builder()
                    .uuid(TEST_UUID)
                    .coreData(CoreData.builder().documentationOffice(docOffice).build())
                    .build()));
  }
}
