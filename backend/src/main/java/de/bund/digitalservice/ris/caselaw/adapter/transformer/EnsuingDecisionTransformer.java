package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.EnsuingDecisionDTO;
import de.bund.digitalservice.ris.caselaw.domain.EnsuingDecision;

public class EnsuingDecisionTransformer extends RelatedDocumentationUnitTransformer {
  public static EnsuingDecision transformToDomain(EnsuingDecisionDTO ensuingDecisionDTO) {
    return EnsuingDecision.builder()
        .uuid(ensuingDecisionDTO.getId())
        .documentNumber(ensuingDecisionDTO.getDocumentNumber())
        .court(getCourtFromDTO(ensuingDecisionDTO.getCourt()))
        .fileNumber(getFileNumber(ensuingDecisionDTO.getFileNumber()))
        .documentType(getDocumentTypeFromDTO(ensuingDecisionDTO.getDocumentType()))
        .decisionDate(ensuingDecisionDTO.getDate())
        .note(ensuingDecisionDTO.getNote())
        .isPending(false)
        .build();
  }

  public static EnsuingDecisionDTO transformToDTO(EnsuingDecision ensuingDecision, Integer rank) {
    if (ensuingDecision.hasNoValues()) {
      return null;
    }
    return EnsuingDecisionDTO.builder()
        .court(getCourtFromDomain(ensuingDecision.getCourt()))
        .date(ensuingDecision.getDecisionDate())
        .documentNumber(ensuingDecision.getDocumentNumber())
        .documentType(getDocumentTypeFromDomain(ensuingDecision.getDocumentType()))
        .fileNumber(getFileNumber(ensuingDecision.getFileNumber()))
        .note(ensuingDecision.getNote())
        .rank(rank)
        .build();
  }
}
