package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import lombok.Builder;

@Builder(toBuilder = true)
public record DocumentationUnitCreationParameters(
    DocumentationOffice documentationOffice,
    Court court,
    DocumentType documentType,
    @PastOrPresent LocalDate decisionDate,
    String fileNumber,
    Reference reference) {}
