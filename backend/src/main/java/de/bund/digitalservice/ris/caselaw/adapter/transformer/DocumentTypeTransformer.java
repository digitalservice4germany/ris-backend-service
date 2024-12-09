package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;

public class DocumentTypeTransformer {
  private DocumentTypeTransformer() {}

  public static DocumentType transformToDomain(DocumentTypeDTO documentTypeDTO) {
    if (documentTypeDTO == null) {
      return null;
    }

    return DocumentType.builder()
        .uuid(documentTypeDTO.getId())
        .jurisShortcut(documentTypeDTO.getAbbreviation())
        .label(documentTypeDTO.getLabel())
        .build();
  }

  public static DocumentTypeDTO transformToDTO(DocumentType documentType) {
    return documentType == null
        ? null
        : DocumentTypeDTO.builder()
            .id(documentType.uuid())
            .label(documentType.label())
            .abbreviation(documentType.jurisShortcut())
            .build();
  }
}
