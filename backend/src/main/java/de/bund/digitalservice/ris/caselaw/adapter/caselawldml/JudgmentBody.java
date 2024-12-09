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
  @XmlElement(name = "motivation", namespace = CaseLawLdml.AKN_NS)
  private JaxbHtml motivation;

  @XmlElement(name = "introduction", namespace = CaseLawLdml.AKN_NS)
  private AknMultipleBlock introduction;

  @XmlElement(name = "background", namespace = CaseLawLdml.AKN_NS)
  private JaxbHtml background;

  @XmlElement(name = "decision", namespace = CaseLawLdml.AKN_NS)
  private AknMultipleBlock decision;
}
