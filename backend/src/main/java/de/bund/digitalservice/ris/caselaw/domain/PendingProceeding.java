package de.bund.digitalservice.ris.caselaw.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/**
 * The domain model for a pending proceeding (Anhängiges Verfahren)
 *
 * @param uuid the unique identifier
 * @param documentNumber the document number, pattern depending on the documentation office, e.g.
 *     KORE00012024
 * @param coreData the core data (e.g. file number, decision date)
 * @param previousDecisions a list of previous decisions (Vorinstanz)
 * @param ensuingDecisions a list of ensuing decisions
 * @param shortTexts the short texts (Kurztexte)
 * @param status the current status including publication and error status
 * @param contentRelatedIndexing the content related indexing data (Inhaltliche Erschließung)
 * @param caselawReferences a list of legal periodical caselaw references (Fundstellen)
 * @param literatureReferences a list of legal periodical literature references
 *     (Literaturfundstellen)
 */
@Builder(toBuilder = true)
public record PendingProceeding(
    UUID uuid,
    @Size(min = 13, max = 14, message = "documentNumber has to be 13 or 14 characters long")
        String documentNumber,
    @Valid CoreData coreData,
    List<PreviousDecision> previousDecisions,
    List<EnsuingDecision> ensuingDecisions,
    ShortTexts shortTexts,
    Status status,
    ContentRelatedIndexing contentRelatedIndexing,
    List<Reference> caselawReferences,
    List<Reference> literatureReferences) {}
