package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "procedure_link")
public class ProcedureLinkDTO {
  @Id @GeneratedValue private UUID id;

  @Column(name = "documentation_unit_id")
  UUID documentationUnitId;

  @Column(name = "rank")
  Integer rank;

  @ManyToOne()
  @JoinColumn(name = "procedure_id", referencedColumnName = "id")
  ProcedureDTO procedureDTO;
}
