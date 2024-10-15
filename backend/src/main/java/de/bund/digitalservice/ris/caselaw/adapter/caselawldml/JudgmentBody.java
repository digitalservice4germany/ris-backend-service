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
public class JudgmentBody {
  @XmlElement(name = "introduction", namespace = CaseLawLdml.AKN_NS)
  private JaxbHtml introduction;

  @XmlElement(name = "background", namespace = CaseLawLdml.AKN_NS)
  private JaxbHtml background;

  @XmlElement(name = "decision", namespace = CaseLawLdml.AKN_NS)
  private Decision decision;

  @XmlElement(name = "arguments", namespace = CaseLawLdml.AKN_NS)
  private JaxbHtml arguments;

  @XmlElement(name = "motivation", namespace = CaseLawLdml.AKN_NS)
  private Motivation motivation;
}
