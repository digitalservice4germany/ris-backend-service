package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for reports (responses from the mail API) of performed jDV handover operations. */
public interface DatabaseHandoverReportRepository extends JpaRepository<HandoverReportDTO, UUID> {
  List<HandoverReportDTO> findAllByEntityId(UUID entityId);
}
