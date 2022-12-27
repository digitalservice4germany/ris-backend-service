package de.bund.digitalservice.ris.caselaw.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.domain.lookuptable.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.court.CourtDTO;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.court.CourtRepository;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentTypeRepository;
import java.util.List;
import java.util.Optional;
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
  @MockBean private CourtRepository courtRepository;

  @Test
  void testGetDocumentTypes() {
    DocumentTypeDTO documentTypeDTO = DocumentTypeDTO.EMPTY;
    documentTypeDTO.setId(3L);
    when(documentTypeRepository.findAllByDocumentTypeOrderByJurisShortcutAscLabelAsc('R'))
        .thenReturn(Flux.just(documentTypeDTO));

    StepVerifier.create(service.getCaselawDocumentTypes(Optional.empty()))
        .consumeNextWith(
            documentType -> {
              assertThat(documentType).isInstanceOf(DocumentType.class);
              assertThat(documentType.id()).isEqualTo(3L);
            })
        .verifyComplete();

    verify(documentTypeRepository).findAllByDocumentTypeOrderByJurisShortcutAscLabelAsc('R');
  }

  @Test
  void testGetTwoDifferentCourts() {
    // court where the location will be intentionally dropped
    CourtDTO courtA = new CourtDTO();
    courtA.setCourttype("ABC");
    courtA.setCourtlocation("Berlin");
    courtA.setSuperiorcourt("Ja");
    courtA.setForeigncountry("Nein");

    // court where the location will be kept
    CourtDTO courtB = new CourtDTO();
    courtB.setCourttype("XYZ");
    courtB.setCourtlocation("Hamburg");
    courtB.setSuperiorcourt("Nein");
    courtB.setForeigncountry("Nein");

    when(courtRepository.findAllByOrderByCourttypeAscCourtlocationAsc())
        .thenReturn(Flux.fromIterable(List.of(courtA, courtB)));

    StepVerifier.create(service.getCourts(Optional.empty()))
        .expectNext(new Court("ABC", null, "ABC", null))
        .expectNext(new Court("XYZ", "Hamburg", "XYZ Hamburg", null))
        .verifyComplete();

    verify(courtRepository).findAllByOrderByCourttypeAscCourtlocationAsc();
  }
}
