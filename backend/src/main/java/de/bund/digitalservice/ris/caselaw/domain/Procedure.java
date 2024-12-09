package de.bund.digitalservice.ris.caselaw.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record Procedure(
    UUID id, String label, Long documentationUnitCount, Instant createdAt, UUID userGroupId) {}
