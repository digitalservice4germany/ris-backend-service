package de.bund.digitalservice.ris.caselaw.adapter.caselawldml;

import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement
public class JaxbHtml {

  private static final Logger logger = LogManager.getLogger(JaxbHtml.class);

  //  private static final Templates removeNameSpacesXslt =
  //      XmlUtilService.getTemplates("caselawhandover/shared/removeNameSpaces.xslt");

  @XmlAttribute(name = "name")
  private String name;

  @XmlAnyElement @XmlMixed private List<Object> html;

  public static JaxbHtml build(List<Object> html) {
    if (html == null || html.isEmpty() || html.stream().allMatch(Objects::isNull)) {
      return null;
    }

    return new JaxbHtml(html);
  }

  public JaxbHtml(List<Object> html) {
    this.html = html;
  }
}
