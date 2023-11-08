package de.bund.digitalservice.ris.caselaw.adapter;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.RisWebTestClient;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = FieldOfLawController.class)
@Import({SecurityConfig.class, TestConfig.class})
class FieldOfLawControllerTest {
  @Autowired private RisWebTestClient risWebTestClient;

  @MockBean private FieldOfLawService service;
  @MockBean private ReactiveClientRegistrationRepository clientRegistrationRepository;

  @Test
  void testGetFieldsOfLaw_withoutQuery_shouldCallServiceWithoutValue() {
    Pageable pageable = PageRequest.of(0, 10);
    when(service.getFieldsOfLawBySearchQuery(Optional.empty(), pageable)).thenReturn(Mono.empty());

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/fieldsoflaw?pg=0&sz=10")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getFieldsOfLawBySearchQuery(Optional.empty(), pageable);
  }

  @Test
  void testGetFieldsOfLaw_withQuery_shouldCallServiceWithValue() {
    Pageable pageable = PageRequest.of(0, 10);
    when(service.getFieldsOfLawBySearchQuery(Optional.of("root"), pageable))
        .thenReturn(Mono.empty());

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/fieldsoflaw?q=root&pg=0&sz=10")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getFieldsOfLawBySearchQuery(Optional.of("root"), pageable);
  }

  @Test
  void testGetChildrenOfFieldOfLaw() {
    when(service.getChildrenOfFieldOfLaw("root")).thenReturn(Mono.empty());

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/fieldsoflaw/root/children")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getChildrenOfFieldOfLaw("root");
  }

  @Test
  void testGetTreeForFieldOfLaw() {
    when(service.getTreeForFieldOfLaw("root")).thenReturn(Mono.empty());

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/fieldsoflaw/root/tree")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getTreeForFieldOfLaw("root");
  }
}
