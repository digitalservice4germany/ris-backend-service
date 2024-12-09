package de.bund.digitalservice.ris.caselaw.domain.court;

import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Court(
    UUID id,
    String type,
    String location,
    String label,
    String revoked,
    DocumentationOffice responsibleDocOffice) {}
