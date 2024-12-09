package de.bund.digitalservice.ris.caselaw.domain;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;

@Builder
public record DocumentationUnitSearchInput(
    UUID uuid,
    String documentNumber,
    String fileNumber,
    String courtType,
    String courtLocation,
    LocalDate decisionDate,
    LocalDate decisionDateEnd,
    LocalDate publicationDate,
    boolean scheduledOnly,
    DocumentationOffice documentationOffice,
    Status status,
    boolean myDocOfficeOnly) {}
