package de.bund.digitalservice.ris.caselaw.domain.lookuptable;

import java.util.UUID;
import lombok.Builder;

/**
 * A legal periodical (Fundstelle).
 *
 * @param uuid The id of the legal periodical (Periodikum).
 * @param abbreviation The abbreviation of the legal periodical.
 * @param title The title of the legal periodical.
 * @param subtitle The subtitle of the legal periodical.
 * @param primaryReference The category of reference ('amtlich' or 'nichtamtlich').
 * @param citationStyle An example of the style of citation for referencing.
 */
@Builder
public record LegalPeriodical(
    UUID uuid,
    String abbreviation,
    String title,
    String subtitle,
    Boolean primaryReference,
    String citationStyle) {}
