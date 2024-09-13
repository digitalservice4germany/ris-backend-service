package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.CourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.FileNumberDTO;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;

public class RelatedDocumentationUnitTransformer {
  RelatedDocumentationUnitTransformer() {}

  static Court getCourtFromDTO(CourtDTO courtDTO) {
    if (courtDTO == null || courtDTO.getType() == null) {
      return null;
    }

    return CourtTransformer.transformToDomain(courtDTO);
  }

  static RelatedDocumentationUnit transformFromDTO(DocumentationUnitDTO documentationUnitDTO) {
    return RelatedDocumentationUnit.builder()
        .uuid(documentationUnitDTO.getId())
        .documentNumber(documentationUnitDTO.getDocumentNumber())
        .court(CourtTransformer.transformToDomain(documentationUnitDTO.getCourt()))
        .decisionDate(documentationUnitDTO.getDecisionDate())
        .fileNumber(
            documentationUnitDTO.getFileNumbers().stream()
                .findFirst()
                .map(FileNumberDTO::getValue)
                .orElse(null))
        .documentType(
            DocumentTypeTransformer.transformToDomain(documentationUnitDTO.getDocumentType()))
        .referenceFound(true)
        .build();
  }

  static CourtDTO getCourtFromDomain(Court court) {
    if (court == null) {
      return null;
    }

    return CourtTransformer.transformToDTO(court);
  }

  static DocumentType getDocumentTypeFromDTO(DocumentTypeDTO documentTypeDTO) {
    if (documentTypeDTO == null) {
      return null;
    }

    return DocumentTypeTransformer.transformToDomain(documentTypeDTO);
  }

  static DocumentTypeDTO getDocumentTypeFromDomain(DocumentType documentType) {
    if (documentType == null) {
      return null;
    }

    return DocumentTypeTransformer.transformToDTO(documentType);
  }
}
