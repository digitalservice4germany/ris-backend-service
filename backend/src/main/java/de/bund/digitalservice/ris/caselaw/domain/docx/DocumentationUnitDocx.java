package de.bund.digitalservice.ris.caselaw.domain.docx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonInclude(Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @Type(value = ECLIElement.class, name = "ecli"),
  @Type(value = ParagraphElement.class, name = "paragraph"),
  @Type(value = BorderNumber.class, name = "border_number"),
  @Type(value = MetadataProperty.class, name = "metadata_property")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface DocumentationUnitDocx {
  String toHtmlString();
}
