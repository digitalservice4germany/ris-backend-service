package de.bund.digitalservice.ris.caselaw.domain.lookuptable.citation;

import java.util.UUID;
import lombok.Builder;

@Builder
public record CitationType(
    UUID uuid,
    String documentType,
    String citationDocumentType,
    String jurisShortcut,
    String label) {}
