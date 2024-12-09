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
public class Proprietary {
  @Builder.Default
  @XmlAttribute(name = "source")
  private String name = "attributsemantik-noch-undefiniert";

  @XmlElement(name = "meta", namespace = CaseLawLdml.RIS_NS)
  private RisMeta meta;
}
