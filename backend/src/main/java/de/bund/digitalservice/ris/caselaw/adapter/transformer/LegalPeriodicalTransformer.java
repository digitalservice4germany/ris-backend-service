package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalPeriodicalDTO;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LegalPeriodicalTransformer {
  private LegalPeriodicalTransformer() {}

  public static LegalPeriodical transformToDomain(LegalPeriodicalDTO legalPeriodicalDTO) {
    if (legalPeriodicalDTO == null) {
      return null;
    }

    return LegalPeriodical.builder()
        .uuid(legalPeriodicalDTO.getId())
        .abbreviation(legalPeriodicalDTO.getAbbreviation())
        .title(legalPeriodicalDTO.getTitle())
        .subtitle(legalPeriodicalDTO.getSubtitle())
        .primaryReference(legalPeriodicalDTO.getPrimaryReference())
        .citationStyle(legalPeriodicalDTO.getCitationStyle())
        .build();
  }

  public static LegalPeriodicalDTO transformToDTO(LegalPeriodical legalPeriodical) {
    if (legalPeriodical == null) {
      return null;
    }

    return LegalPeriodicalDTO.builder()
        .id(legalPeriodical.uuid())
        .title(legalPeriodical.title())
        .abbreviation(legalPeriodical.abbreviation())
        .subtitle(legalPeriodical.subtitle())
        .citationStyle(legalPeriodical.citationStyle())
        .primaryReference(legalPeriodical.primaryReference())
        .build();
  }
}
