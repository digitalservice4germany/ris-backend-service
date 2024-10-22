package de.bund.digitalservice.ris.caselaw.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record DocumentationUnit(
    UUID uuid,
    Long version,
    @Size(min = 13, max = 14, message = "documentNumber has to be 13 or 14 characters long")
        String documentNumber,
    DataSource dataSource,
    List<Attachment> attachments,
    @Valid CoreData coreData,
    List<PreviousDecision> previousDecisions,
    List<EnsuingDecision> ensuingDecisions,
    ShortTexts shortTexts,
    LongTexts longTexts,
    List<String> borderNumbers,
    Status status,
    String note,
    ContentRelatedIndexing contentRelatedIndexing,
    // Fundstellen
    List<Reference> references,
    boolean isEditable,
    boolean isDeletable) {}
