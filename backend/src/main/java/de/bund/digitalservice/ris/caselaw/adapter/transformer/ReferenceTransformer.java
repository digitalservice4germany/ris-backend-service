package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalPeriodicalDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.ReferenceDTO;
import de.bund.digitalservice.ris.caselaw.domain.Reference;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ReferenceTransformer {
  public static Reference transformToDomain(ReferenceDTO referenceDTO) {
    LegalPeriodical legalPeriodical = null;

    if (referenceDTO.getLegalPeriodical() != null) {
      legalPeriodical =
          LegalPeriodicalTransformer.transformToDomain(referenceDTO.getLegalPeriodical());
    }

    return Reference.builder()
        .id(referenceDTO.getId())
        .referenceSupplement(referenceDTO.getReferenceSupplement())
        .legalPeriodical(legalPeriodical)
        .legalPeriodicalRawValue(referenceDTO.getLegalPeriodicalRawValue())
        .citation(referenceDTO.getCitation())
        .footnote(referenceDTO.getFootnote())
        .documentationUnit(
            RelatedDocumentationUnitTransformer.transformFromDTO(
                referenceDTO.getDocumentationUnit()))
        .build();
  }

  public static ReferenceDTO transformToDTO(Reference reference) {
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

    return ReferenceDTO.builder()
        .id(reference.id())
        .referenceSupplement(reference.referenceSupplement())
        .legalPeriodical(legalPeriodicalDTO)
        .citation(reference.citation())
        .footnote(reference.footnote())
        .legalPeriodicalRawValue(
            legalPeriodicalRawValue != null
                ? legalPeriodicalRawValue
                : reference.legalPeriodicalRawValue())
        .documentationUnit(documentationUnitDTO)
        .build();
  }
}
