package de.bund.digitalservice.ris.caselaw.domain;

import java.util.List;

public enum LegalEffect {
  YES("Ja"),
  NO("Nein"),
  NOT_SPECIFIED("Keine Angabe");

  private final String label;

  // as defined in RISDEV-628
  private static final List<String> autoYesCourtTypes =
      List.of("BGH", "BVerwG", "BFH", "BVerfG", "BAG", "BSG");

  LegalEffect(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static String deriveFrom(DocumentUnit documentUnit, boolean courtHasChanged) {
    if (documentUnit == null || documentUnit.coreData() == null) {
      return null;
    }
    if (courtHasChanged
        && documentUnit.coreData().court() != null
        && documentUnit.coreData().court().type() != null
        && autoYesCourtTypes.contains(documentUnit.coreData().court().type())) {
      return YES.getLabel();
    }
    return documentUnit.coreData().legalEffect();
  }

  public static LegalEffect deriveLegalEffectFrom(
      DocumentUnit documentUnit, boolean courtHasChanged) {
    return LegalEffect.valueOf(deriveFrom(documentUnit, courtHasChanged));
  }
}
