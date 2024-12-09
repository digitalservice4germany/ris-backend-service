package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

/** A legal periodical edition (Ausgabe eines Periodikums für die Periodikaauswertung). */
@Builder(toBuilder = true)
public record LegalPeriodicalEdition(
    UUID id,
    LegalPeriodical legalPeriodical,
    @CreationTimestamp LocalDate createdAt,
    String prefix,
    String suffix,
    String name,
    List<Reference> references) {}
