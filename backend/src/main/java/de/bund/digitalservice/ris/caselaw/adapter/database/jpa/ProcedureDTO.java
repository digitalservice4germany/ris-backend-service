package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "procedure", schema = "incremental_migration")
public class ProcedureDTO {
  @Id @GeneratedValue private UUID id;

  @Include
  @Column(name = "name")
  String label;

  @Column(name = "created_at", updatable = false)
  @CreationTimestamp
  Instant createdAt;

  @Include
  @ManyToOne()
  @JoinColumn(name = "documentation_office_id")
  @NotNull
  DocumentationOfficeDTO documentationOffice;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "documentation_unit_procedure",
      schema = "incremental_migration",
      inverseJoinColumns = @JoinColumn(name = "documentation_unit_id"),
      joinColumns = @JoinColumn(name = "procedure_id"))
  List<DocumentationUnitDTO> documentationUnits;

  @Include
  @ManyToOne()
  @JoinColumn(name = "documentation_office_user_group_id")
  UserGroupDTO userGroupDTO;
}
