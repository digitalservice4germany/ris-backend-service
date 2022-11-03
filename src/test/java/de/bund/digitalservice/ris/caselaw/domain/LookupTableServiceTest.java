package de.bund.digitalservice.ris.caselaw.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.domain.lookuptable.DocumentType;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.DocumentTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
@Import(LookupTableService.class)
class LookupTableServiceTest {

  @SpyBean private LookupTableService service;

  @MockBean private DocumentTypeRepository documentTypeRepository;

  @Test
  void testGetDocumentTypes() {
    DocumentTypeDTO documentTypeDTO = DocumentTypeDTO.EMPTY;
    documentTypeDTO.setId(3L);
    when(documentTypeRepository.findAll()).thenReturn(Flux.just(documentTypeDTO));

    StepVerifier.create(service.getDocumentTypes())
        .consumeNextWith(
            documentType -> {
              assertThat(documentType).isInstanceOf(DocumentType.class);
              assertThat(documentType.id()).isEqualTo(3L);
            })
        .verifyComplete();

    verify(documentTypeRepository).findAll();
  }
}
