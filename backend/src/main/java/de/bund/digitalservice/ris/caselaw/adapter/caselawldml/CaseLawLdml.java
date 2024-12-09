package de.bund.digitalservice.ris.caselaw.adapter.caselawldml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@XmlRootElement(name = "akomaNtoso", namespace = CaseLawLdml.AKN_NS)
@XmlAccessorType(XmlAccessType.FIELD)
public class CaseLawLdml {
  public static final String AKN_NS = "http://docs.oasis-open.org/legaldocml/ns/akn/3.0/WD17";
  public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
  public static final String RIS_NS = "http://example.com/0.1/";

  @Builder.Default
  @XmlAttribute(name = "xsi:schemaLocation")
  private String schemaLocation =
      "http://docs.oasis-open.org/legaldocml/ns/akn/3.0/WD17 "
          + "https://docs.oasis-open.org/legaldocml/akn-core/v1.0/csprd02/part2-specs/schemas/akomantoso30.xsd";

  @XmlElement(name = "judgment", namespace = CaseLawLdml.AKN_NS)
  private Judgment judgment;

  public String getUniqueId() {
    return judgment.getMeta().getIdentification().getFrbrWork().getFrbrThis().getValue();
  }
}
