package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Table(schema = "incremental_migration", name = "participating_judge")
public class ParticipatingJudgeDTO {

  @Id @GeneratedValue private UUID id;

  @Column
  @Size(max = 255)
  @NotBlank
  private String name;

  @Column(name = "referenced_opinions")
  private @Size(max = 255) String referencedOpinions;

  @Column @NotNull private Long rank;
}
