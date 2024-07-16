package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for performed jDV handover operations. */
public interface DatabaseXmlHandoverMailRepository extends JpaRepository<HandoverMailDTO, Long> {

  HandoverMailDTO findTopByDocumentUnitIdOrderBySentDateDesc(UUID documentUnitId);

  List<HandoverMailDTO> findAllByDocumentUnitIdOrderBySentDateDesc(UUID documentUnitId);
}
