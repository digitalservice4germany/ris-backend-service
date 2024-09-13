package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

/**
 * Representing the domain object of search results, returned by a search for documentation units.
 * Has only the most important information of a documentation unit. It is consumed by clients of the
 * REST service.
 *
 * @param uuid (Universal unique identifier) the identifier of the search result
 * @param documentNumber (Dokumentnummer) of the search result
 * @param court (Gericht) of the search result
 * @param fileNumber (Aktenzeichen) of the search result
 * @param decisionDate (Entscheidungsdatum) of the search result
 * @param appraisalBody (Spruchkörper) of the search result
 * @param documentType (Dokumenttyp) of the search result
 * @param referencedDocumentationUnitId the id of a referenced documentation unit, if search result
 *     from a search for related documentation units
 * @param status (Status) of the search result
 * @param isDeletable User can delete the documentation unit
 * @param isEditable User can edit the documentation unit
 */
@Builder(toBuilder = true)
public record DocumentationUnitListItem(
    UUID uuid,
    String documentNumber,
    Court court,
    String fileNumber,
    Boolean hasAttachments,
    LocalDate decisionDate,
    String appraisalBody,
    Boolean hasHeadnoteOrPrinciple,
    DocumentType documentType,
    UUID referencedDocumentationUnitId,
    Status status,
    Boolean hasNote,
    Boolean isDeletable,
    Boolean isEditable) {}
