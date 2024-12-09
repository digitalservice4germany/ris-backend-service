package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationType.Values;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Entity
@Getter
@Setter
@DiscriminatorValue(Values.PREVIOUS_DECISION)
public class PreviousDecisionDTO extends RelatedDocumentationDTO {
  @Column(name = "date_known")
  private boolean dateKnown;

  @Column(name = "deviating_file_number")
  private String deviatingFileNumber;
}
