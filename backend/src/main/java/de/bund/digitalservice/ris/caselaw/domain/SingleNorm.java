package de.bund.digitalservice.ris.caselaw.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record SingleNorm(
    UUID id,
    String singleNorm,
    LocalDate dateOfVersion,
    String dateOfRelevance,
    LegalForce legalForce) {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SingleNorm that = (SingleNorm) o;
    return Objects.equals(singleNorm, that.singleNorm)
        && Objects.equals(dateOfVersion, that.dateOfVersion)
        && Objects.equals(dateOfRelevance, that.dateOfRelevance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(singleNorm, dateOfVersion, dateOfRelevance);
  }
}
