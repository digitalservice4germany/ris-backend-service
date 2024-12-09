package de.bund.digitalservice.ris.caselaw.adapter.caselawldml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Opinion {

  @Builder.Default
  @XmlAttribute(name = "type")
  private String name = "dissenting";

  @XmlElement(name = "embeddedStructure", namespace = CaseLawLdml.AKN_NS)
  private JaxbHtml content;

  public Opinion(List<Object> content) {
    this.content = JaxbHtml.build(content);
  }
}
