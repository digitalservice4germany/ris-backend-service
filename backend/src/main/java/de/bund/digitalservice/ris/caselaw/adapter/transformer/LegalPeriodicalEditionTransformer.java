package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalPeriodicalEditionDTO;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import java.time.LocalDate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LegalPeriodicalEditionTransformer {
  private LegalPeriodicalEditionTransformer() {}

  public static LegalPeriodicalEdition transformToDomain(
      LegalPeriodicalEditionDTO legalPeriodicalEditionDTO) {
    if (legalPeriodicalEditionDTO == null) {
      return null;
    }

    return LegalPeriodicalEdition.builder()
        .id(legalPeriodicalEditionDTO.getId())
        .createdAt(legalPeriodicalEditionDTO.getCreatedAt())
        .legalPeriodical(
            LegalPeriodicalTransformer.transformToDomain(
                legalPeriodicalEditionDTO.getLegalPeriodical()))
        .name(legalPeriodicalEditionDTO.getName())
        .prefix(legalPeriodicalEditionDTO.getPrefix())
        .suffix(legalPeriodicalEditionDTO.getSuffix())
        .build();
  }

  public static LegalPeriodicalEditionDTO transformToDTO(
      LegalPeriodicalEdition legalPeriodicalEdition) {
    if (legalPeriodicalEdition == null) {
      return null;
    }

    return LegalPeriodicalEditionDTO.builder()
        .id(legalPeriodicalEdition.id() != null ? legalPeriodicalEdition.id() : UUID.randomUUID())
        .createdAt(
            legalPeriodicalEdition.createdAt() != null
                ? legalPeriodicalEdition.createdAt()
                : LocalDate.now())
        .legalPeriodical(
            LegalPeriodicalTransformer.transformToDTO(legalPeriodicalEdition.legalPeriodical()))
        .name(legalPeriodicalEdition.name())
        .prefix(legalPeriodicalEdition.prefix())
        .suffix(legalPeriodicalEdition.suffix())
        .build();
  }
}
