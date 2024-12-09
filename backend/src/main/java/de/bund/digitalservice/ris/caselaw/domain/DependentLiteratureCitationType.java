package de.bund.digitalservice.ris.caselaw.domain;

public enum DependentLiteratureCitationType {
  PASSIVE("passive"),
  ACTIVE("active");

  private final String value;

  DependentLiteratureCitationType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  // Method to get enum from lowercase string
  public static DependentLiteratureCitationType of(String value) {
    for (DependentLiteratureCitationType type : values()) {
      if (type.getValue().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + value);
  }

  @Override
  public String toString() {
    return this.value;
  }
}
