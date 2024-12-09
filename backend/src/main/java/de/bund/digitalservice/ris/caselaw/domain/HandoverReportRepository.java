package de.bund.digitalservice.ris.caselaw.domain;

import java.util.List;
import java.util.UUID;

/** Repository for handover reports. */
public interface HandoverReportRepository {

  List<HandoverReport> saveAll(List<HandoverReport> reports);

  List<HandoverReport> getAllByEntityId(UUID entityId);
}
