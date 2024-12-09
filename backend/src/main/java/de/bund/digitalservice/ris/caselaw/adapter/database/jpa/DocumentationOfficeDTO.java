package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documentation_office", schema = "incremental_migration")
public class DocumentationOfficeDTO {
  @Id private UUID id;

  @Column(name = "abbreviation")
  private String abbreviation;

  @OneToOne
  @JoinColumn(name = "jurisdiction_type_id")
  private JurisdictionTypeDTO jurisdictionType;
}
