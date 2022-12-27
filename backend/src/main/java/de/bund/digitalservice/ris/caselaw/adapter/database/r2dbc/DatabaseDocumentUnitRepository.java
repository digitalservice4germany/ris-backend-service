package de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface DatabaseDocumentUnitRepository extends R2dbcRepository<DocumentUnitDTO, Long> {
  @Query("SELECT * FROM doc_unit WHERE documentnumber = $1")
  Mono<DocumentUnitDTO> findByDocumentnumber(String documentnumber);

  @Query("SELECT * FROM doc_unit WHERE uuid = $1")
  Mono<DocumentUnitDTO> findByUuid(UUID uuid);
}
