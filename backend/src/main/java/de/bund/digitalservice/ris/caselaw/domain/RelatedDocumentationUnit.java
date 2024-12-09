package de.bund.digitalservice.ris.caselaw.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import java.beans.Transient;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelatedDocumentationUnit {
  protected UUID uuid;
  protected String documentNumber;
  protected Status status;
  protected Court court;
  protected LocalDate decisionDate;
  protected String fileNumber;
  protected DocumentType documentType;
  protected UUID createdByReference;
  protected boolean referenceFound;

  @Transient
  public boolean isEmpty() {
    return uuid == null
        && documentNumber == null
        && status == null
        && court == null
        && decisionDate == null
        && fileNumber == null
        && documentType == null
        && createdByReference == null;
  }
}
