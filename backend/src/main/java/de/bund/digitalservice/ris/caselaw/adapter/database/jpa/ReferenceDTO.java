package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(schema = "incremental_migration", name = "reference")
public abstract class ReferenceDTO {

  @Id private UUID id;

  @ManyToOne
  @NotNull
  @JoinColumn(name = "documentation_unit_id")
  private DocumentationUnitDTO documentationUnit;

  @Column(name = "rank")
  @NotNull
  private Integer documentationUnitRank;

  @Column(name = "edition_rank")
  private Integer editionRank;

  @ManyToOne
  @JoinColumn(name = "edition_id")
  private LegalPeriodicalEditionDTO edition;

  @Column @NotBlank private String citation;

  @JoinColumn(name = "legal_periodical_id")
  @ManyToOne
  private LegalPeriodicalDTO legalPeriodical;

  @Column(name = "legal_periodical_raw_value")
  @NotNull
  private String legalPeriodicalRawValue;
}
