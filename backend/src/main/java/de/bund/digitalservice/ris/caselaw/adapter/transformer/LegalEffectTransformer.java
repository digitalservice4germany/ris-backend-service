package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalEffectDTO;
import de.bund.digitalservice.ris.caselaw.domain.LegalEffect;

public class LegalEffectTransformer {

  private LegalEffectTransformer() {
    // Private constructor to prevent instantiation of this utility class.
  }

  public static LegalEffectDTO transformToDTO(LegalEffect legalEffect) {
    LegalEffectDTO legalEffectDTO = LegalEffectDTO.FALSCHE_ANGABE;
    if (legalEffect != null) {
      switch (legalEffect) {
        case NO -> legalEffectDTO = LegalEffectDTO.NEIN;
        case YES -> legalEffectDTO = LegalEffectDTO.JA;
        case NOT_SPECIFIED -> legalEffectDTO = LegalEffectDTO.KEINE_ANGABE;
      }
    }
    return legalEffectDTO;
  }

  public static LegalEffect transformToDomain(LegalEffectDTO legalEffectDTO) {
    if (legalEffectDTO == null) {
      return null;
    }
    switch (legalEffectDTO) {
      case JA -> {
        return LegalEffect.YES;
      }
      case NEIN -> {
        return LegalEffect.NO;
      }
      case KEINE_ANGABE -> {
        return LegalEffect.NOT_SPECIFIED;
      }
      default -> {
        return null;
      }
    }
  }
}
