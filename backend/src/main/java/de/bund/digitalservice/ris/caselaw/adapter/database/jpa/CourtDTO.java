package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
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
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "court", schema = "incremental_migration")
public class CourtDTO {
  @Id @GeneratedValue private UUID id;

  @Column @NotBlank private String type;

  @Column private String location;

  @Column(name = "is_superior_court")
  @NotNull
  @Accessors(fluent = true)
  private Boolean isSuperiorCourt;

  @Column(name = "is_foreign_court")
  @NotNull
  @Accessors(fluent = true)
  private Boolean isForeignCourt;

  @Column(name = "additional_information")
  private String additionalInformation;

  // TODO This property has only been added to allow tests to create courts in the database, beacuse
  // this column is not nullable. We might have to ask the migration team to remove the contraint
  @Column(name = "juris_id")
  private int jurisId;

  @ManyToOne
  @JoinTable(
      name = "court_region",
      schema = "incremental_migration",
      joinColumns = @JoinColumn(name = "court_id"),
      inverseJoinColumns = @JoinColumn(name = "region_id"))
  private RegionDTO region;

  @ManyToOne
  @JoinColumn(name = "jurisdiction_type_id")
  private JurisdictionTypeDTO jurisdictionType;
}
