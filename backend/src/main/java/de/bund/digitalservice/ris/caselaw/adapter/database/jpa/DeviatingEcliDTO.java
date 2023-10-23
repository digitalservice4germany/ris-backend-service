package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
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
@Table(schema = "incremental_migration")
public class DeviatingEcliDTO {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false)
  @NotBlank
  private String value;

  // @ManyToOne @NotNull private DocumentationUnit documentationUnit;
  @Column(name = "documentation_unit_id")
  private UUID documentationUnitId;

  public DeviatingEcliDTO(String value) {
    this.value = value;
  }
}
