package de.bund.digitalservice.ris.caselaw.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * The domain model for a DocumentationUnit, a caselaw decision
 *
 * @param uuid the unique identifier
 * @param version the version
 * @param documentNumber the document number, pattern depending on the documentation office, e.g.
 *     KORE00012024
 * @param attachments a list of attachments (e.g. .docx files)
 * @param coreData the core data (e.g. file number, decision date)
 * @param previousDecisions a list of previous decisions
 * @param ensuingDecisions a list of ensuing decisions
 * @param shortTexts the short texts (e.g. tenor, headnote)
 * @param longTexts the long texts (e.g. reasons)
 * @param borderNumbers a list of border numbers
 * @param status the current status including publication and error status
 * @param note a note added to the documentation unit
 * @param contentRelatedIndexing the content related indexing data
 * @param references a list of legal periodical references (Fundstellen)
 * @param isEditable a flag indicating if the documentation unit is editable by the user
 */
@Builder(toBuilder = true)
public record DocumentationUnit(
    UUID uuid,
    Long version,
    @Size(min = 13, max = 14, message = "documentNumber has to be 13 or 14 characters long")
        String documentNumber,
    List<Attachment> attachments,
    @Valid CoreData coreData,
    List<PreviousDecision> previousDecisions,
    List<EnsuingDecision> ensuingDecisions,
    ShortTexts shortTexts,
    LongTexts longTexts,
    @Valid ManagementData managementData,
    Status status,
    String note,
    ContentRelatedIndexing contentRelatedIndexing,
    List<Reference> references,
    boolean isEditable,
    boolean isDeletable) {}
