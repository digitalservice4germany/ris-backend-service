package de.bund.digitalservice.ris.caselaw.adapter.caselawldml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class RelatedDecision {
  @XmlAttribute private String date;

  @XmlElement(name = "documentNumber", namespace = CaseLawLdml.RIS_NS)
  private String documentNumber;

  @XmlElement(name = "fileNumber", namespace = CaseLawLdml.RIS_NS)
  private String fileNumber;

  @XmlElement(name = "courtType", namespace = CaseLawLdml.RIS_NS)
  private String courtType;
}
