package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalPeriodicalEditionDTO;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
        .references(
            legalPeriodicalEditionDTO.getReferences().stream()
                .map(ReferenceTransformer::transformToDomain)
                .toList())
        .build();
  }

  public static LegalPeriodicalEditionDTO transformToDTO(
      LegalPeriodicalEdition legalPeriodicalEdition) {
    if (legalPeriodicalEdition == null) {
      return null;
    }

    AtomicInteger i = new AtomicInteger(1);
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
        .references(
            legalPeriodicalEdition.references() != null
                ? legalPeriodicalEdition.references().stream()
                    .map(ReferenceTransformer::transformToDTO)
                    .filter(Objects::nonNull)
                    .peek(referenceDTO -> referenceDTO.setRank(i.getAndIncrement()))
                    .toList()
                : Collections.emptyList())
        .build();
  }
}
