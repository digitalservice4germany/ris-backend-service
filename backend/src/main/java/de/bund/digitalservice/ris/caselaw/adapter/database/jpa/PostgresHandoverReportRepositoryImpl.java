package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.domain.HandoverReport;
import de.bund.digitalservice.ris.caselaw.domain.HandoverReportRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Postgres Repository for reports (responses from the mail API) of performed jDV handover
 * operations.
 */
@Repository
public class PostgresHandoverReportRepositoryImpl implements HandoverReportRepository {

  private final DatabaseHandoverReportRepository repository;

  public PostgresHandoverReportRepositoryImpl(DatabaseHandoverReportRepository repository) {

    this.repository = repository;
  }

  /**
   * Saves all handover reports to the database.
   *
   * @param reports the handover reports to save
   * @return the saved handover reports
   */
  @Override
  public List<HandoverReport> saveAll(List<HandoverReport> reports) {
    List<HandoverReportDTO> handoverReportDTOS =
        reports.stream()
            .map(
                report ->
                    HandoverReportDTO.builder()
                        .entityId(report.entityId())
                        .receivedDate(report.receivedDate())
                        .content(report.content())
                        .build())
            .filter(Objects::nonNull)
            .toList();

    return repository.saveAll(handoverReportDTOS).stream()
        .map(
            report ->
                HandoverReport.builder()
                    .receivedDate(report.getReceivedDate())
                    .content(report.getContent())
                    .build())
        .toList();
  }

  /**
   * Retrieves all handover reports for a given entity (documentation unit or edition).
   *
   * @param entityId the entity UUID
   * @return the handover reports
   */
  @Override
  public List<HandoverReport> getAllByEntityId(UUID entityId) {
    return repository.findAllByEntityId(entityId).stream()
        .map(
            report ->
                HandoverReport.builder()
                    .receivedDate(report.getReceivedDate())
                    .content(report.getContent())
                    .build())
        .toList();
  }
}
