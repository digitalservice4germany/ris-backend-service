package de.bund.digitalservice.ris.caselaw.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import java.util.UUID;
import lombok.Builder;

/**
 * A reference to a legal periodical (Fundstelle).
 *
 * @param id The id of the reference.
 * @param legalPeriodical The periodical, where reference was found.
 * @param citation The citation (Zitierung).
 * @param referenceSupplement The reference supplement (Klammerzusatz).
 * @param footnote The footnote (used by BFH).
 */
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Reference(
    UUID id,
    String citation,
    String referenceSupplement,
    String footnote,
    LegalPeriodical legalPeriodical,
    String legalPeriodicalRawValue,
    DocumentType documentType,
    String author,
    ReferenceType referenceType,
    RelatedDocumentationUnit documentationUnit,
    Boolean primaryReference,
    int rank) {}
