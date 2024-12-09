package de.bund.digitalservice.ris.caselaw.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;

public enum ReferenceType {
  CASELAW("caselaw"),
  LITERATURE("literature");

  private final String label;

  // A map to quickly resolve string labels to enum values
  private static final Map<String, ReferenceType> LABEL_MAP = new HashMap<>();

  static {
    for (ReferenceType type : values()) {
      LABEL_MAP.put(type.label, type);
    }
  }

  ReferenceType(String label) {
    this.label = label;
  }

  @JsonValue
  public String getLabel() {
    return label;
  }

  @JsonCreator
  public static ReferenceType fromString(String label) {
    ReferenceType type = LABEL_MAP.get(label.toLowerCase());
    if (type == null) {
      throw new IllegalArgumentException("Invalid reference type: " + label);
    }
    return type;
  }
}
