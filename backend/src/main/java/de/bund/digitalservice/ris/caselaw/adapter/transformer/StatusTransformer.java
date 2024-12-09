package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.StatusDTO;
import de.bund.digitalservice.ris.caselaw.domain.Status;

public class StatusTransformer {

  private StatusTransformer() {}

  public static Status transformToDomain(StatusDTO statusDTO) {
    if (statusDTO == null) {
      return null;
    }
    return Status.builder()
        .withError(statusDTO.isWithError())
        .publicationStatus(statusDTO.getPublicationStatus())
        .build();
  }

  public static StatusDTO transformToDTO(Status status) {
    if (status == null) {
      return null;
    }
    return StatusDTO.builder()
        .withError(status.withError())
        .publicationStatus(status.publicationStatus())
        .build();
  }
}
