package de.bund.digitalservice.ris.caselaw.adapter.caselawldml;

import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.persistence.oxm.annotations.XmlDiscriminatorValue;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@XmlDiscriminatorValue(Opinions.NAME)
public class Opinions extends AknBlock {
  public static final String NAME = "Abweichende Meinung";

  @XmlElement(name = "opinion", namespace = CaseLawLdml.AKN_NS)
  private Opinion opinion;

  public static Opinions build(List<Object> content) {
    if (content == null || content.isEmpty() || content.stream().allMatch(Objects::isNull)) {
      return null;
    }
    return new Opinions(new Opinion(content));
  }

  public String getName() {
    return Opinions.NAME;
  }
}
