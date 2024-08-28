package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocumentConverterException;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.ConvertedDocumentElementDTO;
import de.bund.digitalservice.ris.caselaw.domain.ConvertedDocumentElement;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocumentationUnitDocx;
import de.bund.digitalservice.ris.caselaw.domain.docx.HasElementId;

/**
 * Transformer for transformation between database object {@link ConvertedDocumentElementDTO} and
 * domain object {@link ConvertedDocumentElement}
 */
public class ConvertedDocumentElementTransformer {
  private ConvertedDocumentElementTransformer() {}

  /**
   * Transform a database object of the converted document element {@link
   * ConvertedDocumentElementDTO} to the domain object {@link ConvertedDocumentElement}
   *
   * @param objectMapper object mapper to convert the xml string representation of the element to
   *     the object {@link DocumentationUnitDocx}
   * @param dto database object {@link ConvertedDocumentElementDTO}
   * @return domain object {@link ConvertedDocumentElement}
   * @throws DocumentConverterException exception was thrown if the xml representation couldn't
   *     convert into the object {@link DocumentationUnitDocx}
   */
  public static ConvertedDocumentElement transformDTO(
      ObjectMapper objectMapper, ConvertedDocumentElementDTO dto)
      throws DocumentConverterException {

    String content;
    DocumentationUnitDocx obj = getContentObject(objectMapper, dto);
    if (obj instanceof HasElementId hasElementId) {
      content = hasElementId.toHtmlString(dto.getId());
    } else {
      content = obj.toHtmlString();
    }

    return ConvertedDocumentElement.builder().id(dto.getId()).content(content).build();
  }

  public static DocumentationUnitDocx getContentObject(
      ObjectMapper objectMapper, ConvertedDocumentElementDTO dto)
      throws DocumentConverterException {

    try {
      return objectMapper.readValue(dto.getContent(), DocumentationUnitDocx.class);
    } catch (JsonProcessingException e) {
      throw new DocumentConverterException(
          "Couldn't convert database xml representation of a DocumentationUnitDocx to an object",
          e);
    }
  }
}
