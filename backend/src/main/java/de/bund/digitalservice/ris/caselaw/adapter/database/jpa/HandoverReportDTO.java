package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Entity for reports (responses from the mail API) of performed jDV handover operations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "handover_report")
public class HandoverReportDTO {
  @Id @GeneratedValue private UUID id;

  @Column(name = "entity_id")
  private UUID entityId;

  private String content;

  @Column(name = "received_date")
  private Instant receivedDate;
}
