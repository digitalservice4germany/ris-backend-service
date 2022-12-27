package de.bund.digitalservice.ris.caselaw.adapter;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import de.bund.digitalservice.ris.caselaw.domain.LookupTableService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = LookupTableController.class)
@WithMockUser
class LookupTableControllerTest {
  @Autowired private WebTestClient webClient;

  @MockBean private LookupTableService service;

  @Test
  void testGetDocumentTypes() {
    when(service.getCaselawDocumentTypes(Optional.empty())).thenReturn(Flux.empty());

    webClient
        .mutateWith(csrf())
        .get()
        .uri("/api/v1/caselaw/lookuptable/documentTypes")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getCaselawDocumentTypes(Optional.empty());
  }

  @Test
  void testGetCourts() {
    when(service.getCourts(Optional.empty())).thenReturn(Flux.empty());

    webClient
        .mutateWith(csrf())
        .get()
        .uri("/api/v1/caselaw/lookuptable/courts")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getCourts(Optional.empty());
  }
}
