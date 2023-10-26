package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "court_synonym", schema = "incremental_migration")
public class SynonymDTO {
  @Id @GeneratedValue private UUID id;

  @Column(nullable = false)
  @NotBlank
  private String type;

  @Column(nullable = false)
  @NotBlank
  private String label;

  @ManyToOne @NotNull private CourtDTO court;
}
