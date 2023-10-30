package de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc;

import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitLinkType;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Deprecated
public interface DatabaseDocumentationUnitLinkRepository
    extends R2dbcRepository<DocumentationUnitLinkDTO, Long> {

  Flux<DocumentationUnitLinkDTO> findAllByParentDocumentationUnitUuidAndTypeOrderByIdAsc(
      UUID parentDocumentUnitId, DocumentationUnitLinkType type);

  Mono<DocumentationUnitLinkDTO>
      findByParentDocumentationUnitUuidAndChildDocumentationUnitUuidAndType(
          UUID parentDocumentationUnitUuid,
          UUID childDocumentationUnitUuid,
          DocumentationUnitLinkType type);

  Mono<Boolean> existsByChildDocumentationUnitUuid(UUID childDocumentationUnitUuid);

  Mono<Long> countByChildDocumentationUnitUuid(UUID childDocumentationUnitUuid);
}
