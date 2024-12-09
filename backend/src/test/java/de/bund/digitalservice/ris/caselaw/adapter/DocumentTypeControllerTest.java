package de.bund.digitalservice.ris.caselaw.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.DocumentTypeService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = DocumentTypeController.class)
@Import({SecurityConfig.class, TestConfig.class})
class DocumentTypeControllerTest {

  @Autowired private RisWebTestClient risWebTestClient;

  @MockBean private DocumentTypeService service;
  @MockBean private ClientRegistrationRepository clientRegistrationRepository;

  private DocumentType documentType1;
  private DocumentType documentType2;

  @BeforeEach
  void setUp() {
    documentType1 =
        DocumentType.builder()
            .uuid(UUID.randomUUID())
            .label("label1")
            .jurisShortcut("abbreviation1")
            .build();
    documentType2 =
        DocumentType.builder()
            .uuid(UUID.randomUUID())
            .label("label2")
            .jurisShortcut("abbreviation2")
            .build();
  }

  @Test
  void shouldReturnListOfDocumentTypes_whenGetDocumentTypesIsCalled() {
    List<DocumentType> documentTypes = List.of(documentType1, documentType2);

    when(service.getDocumentTypes(any(Optional.class))).thenReturn(documentTypes);

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documenttypes")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getDocumentTypes(any(Optional.class));
  }

  @Test
  void
      shouldReturnListOfDependentLiteratureDocumentTypes_whenGetDependentLiteratureDocumentTypesIsCalled() {
    List<DocumentType> documentTypes = List.of(documentType1, documentType2);

    when(service.getDependentLiteratureDocumentTypes(any(Optional.class)))
        .thenReturn(documentTypes);

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documenttypes/dependent-literature")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getDependentLiteratureDocumentTypes(any(Optional.class));
  }

  @Test
  void shouldCallServiceWithSearchQueryParameter_whenGetDocumentTypesIsCalledWithSearchString() {
    List<DocumentType> documentTypes = List.of(documentType1, documentType2);

    when(service.getDocumentTypes(any(Optional.class))).thenReturn(documentTypes);

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documenttypes?q=label1")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getDocumentTypes(Optional.of("label1"));
  }

  @Test
  void
      shouldCallServiceWithSearchQueryParameter_whenGetDependentLiteratureDocumentTypesIsCalledWithSearchString() {
    List<DocumentType> documentTypes = List.of(documentType1, documentType2);

    when(service.getDependentLiteratureDocumentTypes(any(Optional.class)))
        .thenReturn(documentTypes);

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documenttypes/dependent-literature?q=label2")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service, times(1)).getDependentLiteratureDocumentTypes(Optional.of("label2"));
  }
}
