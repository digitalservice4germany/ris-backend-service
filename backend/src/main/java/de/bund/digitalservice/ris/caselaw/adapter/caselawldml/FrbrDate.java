package de.bund.digitalservice.ris.caselaw.adapter.caselawldml;

import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class FrbrDate {
  @XmlAttribute private String date;
  @XmlAttribute private String name = "entscheidungsdatum";

  public FrbrDate(String date) {
    this.date = date;
  }
}
