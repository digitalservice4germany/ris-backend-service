package de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@Deprecated
public interface DatabaseKeywordRepository extends R2dbcRepository<KeywordDTO, Long> {
  Flux<KeywordDTO> findAllByDocumentUnitIdOrderById(Long documentUnitId);

  Mono<KeywordDTO> findByDocumentUnitIdAndKeyword(Long documentUnitId, String keyword);
}
