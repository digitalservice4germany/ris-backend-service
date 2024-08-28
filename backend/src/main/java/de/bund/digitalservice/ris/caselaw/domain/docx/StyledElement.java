package de.bund.digitalservice.ris.caselaw.domain.docx;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public abstract class StyledElement implements DocumentationUnitDocx {
  private Map<String, List<String>> styles = new HashMap<>();
  private String styleReference;

  public String getStyleReference() {
    return styleReference;
  }

  public void setStyleReference(String styleReference) {
    this.styleReference = styleReference;
  }

  public void addStyle(String property, String value) {
    styles.computeIfAbsent(property, k -> new ArrayList<>());

    List<String> values = styles.get(property);
    if (property.equals("text-decoration") && !values.contains(value)) {
      values.add(value);
    } else {
      if (values.isEmpty()) {
        values.add(value);
      } else {
        values.set(0, value);
      }
    }
  }

  public void addStyle(Style newStyle) {
    newStyle.value().forEach(value -> addStyle(newStyle.property(), value));
  }

  public Map<String, List<String>> getStyles() {
    return styles;
  }

  public void setStyles(Map<String, List<String>> styles) {
    this.styles = styles;
  }

  public boolean containsStyle(String property) {
    return styles.containsKey(property);
  }

  public boolean hasStyle() {
    return !styles.isEmpty();
  }

  @JsonIgnore
  public String getStyleString() {
    if (styles.isEmpty()) return "";

    return " style=\""
        + styles.entrySet().stream()
            .sorted(Comparator.comparing(Entry::getKey))
            .map(entry -> entry.getKey() + ": " + String.join(" ", entry.getValue()) + "; ")
            .collect(Collectors.joining())
            .trim()
        + "\"";
  }
}
