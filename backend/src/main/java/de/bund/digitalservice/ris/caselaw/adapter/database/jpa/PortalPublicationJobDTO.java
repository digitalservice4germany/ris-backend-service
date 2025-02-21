package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.domain.PortalPublicationTaskStatus;
import de.bund.digitalservice.ris.caselaw.domain.PortalPublicationTaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString.Include;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Entity
@Table(name = "portal_publication_job", schema = "incremental_migration")
public abstract class PortalPublicationJobDTO {

  @Id @GeneratedValue @Include private UUID id;

  @Column private LocalDate createdAt;

  @Column(nullable = false, unique = true, updatable = false, name = "document_number")
  @NotBlank
  @Include
  private String documentNumber;

  @Column(name = "publication_type")
  @NotNull
  @Enumerated(EnumType.STRING)
  private PortalPublicationTaskType publicationType;

  @Column(name = "publication_status")
  @NotNull
  @Enumerated(EnumType.STRING)
  private PortalPublicationTaskStatus publicationStatus;
}
