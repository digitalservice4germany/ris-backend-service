package de.bund.digitalservice.ris.caselaw.domain.docx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @Type(value = RunTextElement.class, name = "text"),
  @Type(value = InlineImageElement.class, name = "inline_image"),
  @Type(value = RunTabElement.class, name = "tab")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface RunElement extends DocumentationUnitDocx {}
