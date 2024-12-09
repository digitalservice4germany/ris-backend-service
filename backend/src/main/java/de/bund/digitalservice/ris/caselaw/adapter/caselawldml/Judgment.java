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
public class Judgment {
  @Builder.Default
  @XmlAttribute(name = "name")
  private String name = "attributsemantik-noch-undefiniert";

  @XmlElement(name = "meta", namespace = CaseLawLdml.AKN_NS)
  private Meta meta;

  @XmlElement(name = "header", namespace = CaseLawLdml.AKN_NS)
  private JaxbHtml header;

  @XmlElement(name = "judgmentBody", namespace = CaseLawLdml.AKN_NS)
  private JudgmentBody judgmentBody;
}
