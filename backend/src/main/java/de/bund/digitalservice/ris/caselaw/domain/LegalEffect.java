package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public enum LegalEffect {
  YES("Ja"),
  NO("Nein"),
  NOT_SPECIFIED("Keine Angabe");

  private final String label;

  // as defined in RISDEV-628
  private static final List<String> autoYesCourtTypes =
      List.of("BGH", "BVerwG", "BFH", "BVerfG", "BAG", "BSG");

  private static final Map<String, LegalEffect> map = new HashMap<>(values().length, 1);

  static {
    for (LegalEffect c : values()) map.put(c.label, c);
  }

  LegalEffect(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static LegalEffect of(String name) {
    LegalEffect result = map.get(name);

    if (result == null) {
      throw new IllegalArgumentException("Invalid category name: " + name);
    }

    return result;
  }

  @Override
  public String toString() {
    return label;
  }

  public static LegalEffect deriveFrom(
      DocumentationUnit documentationUnit, boolean courtHasChanged) {
    if (documentationUnit == null
        || documentationUnit.coreData() == null
        || documentationUnit.coreData().legalEffect() == null) {
      return null;
    }

    return deriveFrom(documentationUnit.coreData().court(), courtHasChanged)
        .orElse(of(documentationUnit.coreData().legalEffect()));
  }

  public static Optional<LegalEffect> deriveFrom(Court court, boolean courtHasChanged) {
    if (courtHasChanged
        && court != null
        && court.type() != null
        && autoYesCourtTypes.contains(court.type())) {
      return Optional.of(YES);
    }

    return Optional.empty();
  }
}
