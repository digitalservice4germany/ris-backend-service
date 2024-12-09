package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "incremental_migration", name = "status")
public class StatusDTO {
  @Id @GeneratedValue UUID id;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "publication_status")
  @NotNull
  @Enumerated(EnumType.STRING)
  private PublicationStatus publicationStatus;

  @Column(name = "with_error")
  @NotNull
  private boolean withError;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "documentation_unit_id")
  private DocumentationUnitDTO documentationUnit;
}
