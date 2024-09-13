package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitListItemDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.StatusDTO;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitListItem;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit.RelatedDocumentationUnitBuilder;
import de.bund.digitalservice.ris.caselaw.domain.Status;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DocumentationUnitListItemTransformer {
  private DocumentationUnitListItemTransformer() {}

  /**
   * Transforms a documentation unit list item object from its database representation into a
   * documentation unit domain object that is suitable to be consumed by clients of the REST
   * service.
   *
   * @param documentationUnitListItemDTO the database documentation unit search result
   * @return DocumentationUnitListItem the domain documentation unit search result
   */
  public static DocumentationUnitListItem transformToDomain(
      DocumentationUnitListItemDTO documentationUnitListItemDTO) {
    if (documentationUnitListItemDTO == null) {
      return DocumentationUnitListItem.builder().build();
    }

    log.debug(
        "transfer database documentation unit '{}' to domain object",
        documentationUnitListItemDTO.getId());

    DocumentationUnitListItem.DocumentationUnitListItemBuilder builder =
        DocumentationUnitListItem.builder();

    builder
        .uuid(documentationUnitListItemDTO.getId())
        .documentNumber(documentationUnitListItemDTO.getDocumentNumber())
        .referencedDocumentationUnitId(documentationUnitListItemDTO.getId())
        .decisionDate(documentationUnitListItemDTO.getDecisionDate())
        .appraisalBody(documentationUnitListItemDTO.getJudicialBody())
        .hasHeadnoteOrPrinciple(hasHeadnoteOrPrinciple(documentationUnitListItemDTO))
        .hasAttachments(!documentationUnitListItemDTO.getAttachments().isEmpty())
        .documentType(
            DocumentTypeTransformer.transformToDomain(
                documentationUnitListItemDTO.getDocumentType()))
        .court(CourtTransformer.transformToDomain(documentationUnitListItemDTO.getCourt()))
        .fileNumber(
            documentationUnitListItemDTO.getFileNumbers() == null
                    || documentationUnitListItemDTO.getFileNumbers().isEmpty()
                ? null
                : documentationUnitListItemDTO.getFileNumbers().get(0).getValue())
        .status(
            documentationUnitListItemDTO.getStatus() == null
                    || documentationUnitListItemDTO.getStatus().isEmpty()
                ? null
                : StatusTransformer.transformToDomain(
                    documentationUnitListItemDTO.getStatus().stream()
                        .max(Comparator.comparing(StatusDTO::getCreatedAt))
                        .orElse(null)))
        .hasNote(
            documentationUnitListItemDTO.getNote() != null
                && !documentationUnitListItemDTO.getNote().isEmpty());
    return builder.build();
  }

  /**
   * Checks if a headnote or a guiding principle is given in a documentation unit, to display the
   * information in a list
   *
   * @param documentationUnitListItemDTO
   * @return a boolean value if either the headnote or the guiding principle are filled in (i.e. one
   *     or both of these fields)
   */
  private static boolean hasHeadnoteOrPrinciple(
      DocumentationUnitListItemDTO documentationUnitListItemDTO) {
    String headnote = documentationUnitListItemDTO.getHeadnote();
    String guidingPrinciple = documentationUnitListItemDTO.getGuidingPrinciple();

    return !(headnote == null || headnote.isEmpty())
        || !(guidingPrinciple == null || guidingPrinciple.isEmpty());
  }

  /**
   * Transforms a documentation unit list item object from its database representation into a domain
   * object of related (linked) documentation unit, that is suitable to be consumed by clients of
   * the REST service.
   *
   * @param documentationUnitListItemDTO A database representation of a documentation unit search
   *     result, containing only relevant information
   * @return RelatedDocumentationUnit A representation of a linked documentation unit, containing
   *     only relevant data
   */
  public static RelatedDocumentationUnit transformToRelatedDocumentation(
      DocumentationUnitListItemDTO documentationUnitListItemDTO) {

    RelatedDocumentationUnitBuilder<?, ?> builder =
        RelatedDocumentationUnit.builder()
            .uuid(documentationUnitListItemDTO.getId())
            .documentNumber(documentationUnitListItemDTO.getDocumentNumber())
            .court(CourtTransformer.transformToDomain(documentationUnitListItemDTO.getCourt()))
            .status(getStatus(documentationUnitListItemDTO))
            .decisionDate(documentationUnitListItemDTO.getDecisionDate())
            .documentType(
                DocumentTypeTransformer.transformToDomain(
                    documentationUnitListItemDTO.getDocumentType()))
            .referenceFound(true);

    if (documentationUnitListItemDTO.getFileNumbers() != null
        && !documentationUnitListItemDTO.getFileNumbers().isEmpty()) {

      builder.fileNumber(documentationUnitListItemDTO.getFileNumbers().get(0).getValue());
    }

    return builder.build();
  }

  private static Status getStatus(DocumentationUnitListItemDTO documentationUnitListItemDTO) {
    return StatusTransformer.transformToDomain(
        documentationUnitListItemDTO.getStatus().stream()
            .max(Comparator.comparing(StatusDTO::getCreatedAt))
            .orElse(null));
  }
}
