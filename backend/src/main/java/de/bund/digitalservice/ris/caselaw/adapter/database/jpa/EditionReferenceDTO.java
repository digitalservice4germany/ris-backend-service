package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "edition_reference", schema = "incremental_migration")
public class EditionReferenceDTO {
  @Id @GeneratedValue private UUID id;

  @Column(name = "rank")
  private Integer rank;

  @Column(name = "dtype")
  private String dtype;

  @ManyToOne
  @JoinColumn(name = "edition_id", nullable = false)
  private LegalPeriodicalEditionDTO edition;

  @Column(name = "reference_id")
  private UUID referenceId;
}
