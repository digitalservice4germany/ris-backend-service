package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DependentLiteratureCitationDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalPeriodicalDTO;
import de.bund.digitalservice.ris.caselaw.domain.DependentLiteratureCitationType;
import de.bund.digitalservice.ris.caselaw.domain.Reference;
import de.bund.digitalservice.ris.caselaw.domain.ReferenceType;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DependentLiteratureTransformer {
  public static Reference transformToDomain(DependentLiteratureCitationDTO literatureCitationDTO) {
    if (literatureCitationDTO == null) {
      return null;
    }
    LegalPeriodical legalPeriodical = null;

    if (literatureCitationDTO.getLegalPeriodical() != null) {
      legalPeriodical =
          LegalPeriodicalTransformer.transformToDomain(literatureCitationDTO.getLegalPeriodical());
    }

    return Reference.builder()
        .id(literatureCitationDTO.getId())
        .author(literatureCitationDTO.getAuthor())
        .documentType(
            DocumentTypeTransformer.transformToDomain(literatureCitationDTO.getDocumentType()))
        .legalPeriodical(legalPeriodical)
        .legalPeriodicalRawValue(literatureCitationDTO.getLegalPeriodicalRawValue())
        .citation(literatureCitationDTO.getCitation())
        .referenceType(ReferenceType.LITERATURE)
        .documentationUnit(
            RelatedDocumentationUnitTransformer.transformToDomain(
                literatureCitationDTO.getDocumentationUnit()))
        // editionRank is only set if reference is requested from edition
        .rank(
            literatureCitationDTO.getEditionRank() != null
                ? literatureCitationDTO.getEditionRank()
                : literatureCitationDTO.getRank())
        .build();
  }

  public static DependentLiteratureCitationDTO transformToDTO(Reference reference) {
    LegalPeriodicalDTO legalPeriodicalDTO = null;
    String legalPeriodicalRawValue = null;

    if (reference.legalPeriodical() != null) {
      legalPeriodicalDTO = LegalPeriodicalTransformer.transformToDTO(reference.legalPeriodical());
      legalPeriodicalRawValue = reference.legalPeriodical().abbreviation();
    }

    DocumentationUnitDTO documentationUnitDTO = null;
    if (reference.documentationUnit() != null) {
      documentationUnitDTO =
          DocumentationUnitDTO.builder().id(reference.documentationUnit().getUuid()).build();
    }

    DocumentTypeDTO documentType = null;
    if (reference.documentType() != null) {
      documentType = DocumentTypeTransformer.transformToDTO(reference.documentType());
    }

    return DependentLiteratureCitationDTO.builder()
        .id(reference.id())
        .author(reference.author())
        .citation(reference.citation())
        .documentType(documentType)
        .legalPeriodicalRawValue(
            legalPeriodicalRawValue != null
                ? legalPeriodicalRawValue
                : reference.legalPeriodicalRawValue())
        .legalPeriodical(legalPeriodicalDTO)
        .type(DependentLiteratureCitationType.PASSIVE)
        .documentTypeRawValue(documentType.getAbbreviation())
        .documentationUnit(documentationUnitDTO)
        .build();
  }
}
