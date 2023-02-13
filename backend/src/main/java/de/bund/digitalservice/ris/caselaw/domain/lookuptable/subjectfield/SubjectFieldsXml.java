package de.bund.digitalservice.ris.caselaw.domain.lookuptable.subjectfield;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "juris-table")
public class SubjectFieldsXml {
  @JsonProperty(value = "juris-sachg")
  @JacksonXmlElementWrapper(useWrapping = false)
  List<SubjectFieldXml> list;
}
