package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.EnsuingDecisionDTO;
import de.bund.digitalservice.ris.caselaw.domain.EnsuingDecision;
import de.bund.digitalservice.ris.caselaw.domain.StringUtils;
import java.util.Optional;

public class EnsuingDecisionTransformer extends RelatedDocumentationUnitTransformer {
  public static EnsuingDecision transformToDomain(EnsuingDecisionDTO ensuingDecisionDTO) {
    Optional<DocumentationUnitDTO> referencedDocumentationUnit =
        Optional.ofNullable(ensuingDecisionDTO.getReferencedDocumentationUnit());
    return EnsuingDecision.builder()
        .uuid(ensuingDecisionDTO.getId())
        .documentNumber(ensuingDecisionDTO.getDocumentNumber())
        .court(getCourtFromDTO(ensuingDecisionDTO.getCourt()))
        .fileNumber(ensuingDecisionDTO.getFileNumber())
        .documentType(getDocumentTypeFromDTO(ensuingDecisionDTO.getDocumentType()))
        .decisionDate(ensuingDecisionDTO.getDate())
        .note(ensuingDecisionDTO.getNote())
        .pending(false)
        .referenceFound(referencedDocumentationUnit.isPresent())
        .build();
  }

  public static EnsuingDecisionDTO transformToDTO(EnsuingDecision ensuingDecision) {
    if (ensuingDecision.hasNoValues()) {
      return null;
    }
    return EnsuingDecisionDTO.builder()
        .id(ensuingDecision.getUuid())
        .court(getCourtFromDomain(ensuingDecision.getCourt()))
        .date(ensuingDecision.getDecisionDate())
        .documentNumber(ensuingDecision.getDocumentNumber())
        .documentType(getDocumentTypeFromDomain(ensuingDecision.getDocumentType()))
        .fileNumber(StringUtils.normalizeSpace(ensuingDecision.getFileNumber()))
        .note(StringUtils.normalizeSpace(ensuingDecision.getNote()))
        .build();
  }
}
