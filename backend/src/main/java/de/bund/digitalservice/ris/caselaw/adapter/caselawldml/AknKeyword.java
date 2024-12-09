package de.bund.digitalservice.ris.caselaw.adapter.caselawldml;

import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class AknKeyword {
  @XmlAttribute(name = "dictionary")
  private String dictionary = "attributsemantik-noch-undefiniert";

  @XmlAttribute(name = "showAs")
  private String showAs = "attributsemantik-noch-undefiniert";

  @XmlAttribute(name = "value")
  private String value;

  public AknKeyword(String value) {
    this.value = value;
  }
}
