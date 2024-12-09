package de.bund.digitalservice.ris.caselaw.adapter.caselawldml;

import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Meta {
  @XmlElement(name = "identification", namespace = CaseLawLdml.AKN_NS)
  private Identification identification;

  @XmlElement(name = "classification", namespace = CaseLawLdml.AKN_NS)
  private Classification classification;

  @XmlElement(name = "proprietary", namespace = CaseLawLdml.AKN_NS)
  private Proprietary proprietary;
}
