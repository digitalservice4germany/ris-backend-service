package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "jurisdiction_type", schema = "incremental_migration")
public class JurisdictionTypeDTO {
  @Id private UUID id;

  @Column private String label;

  @OneToOne(mappedBy = "jurisdictionType")
  private DocumentationOfficeDTO documentationOffice;
}
