package de.bund.digitalservice.ris.caselaw.domain;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * Represents the result of a juris XML export operation.
 *
 * @param xml the documentation unit XML representation
 * @param success whether the export operation was successful
 * @param statusMessages a list of issues found during the export operation
 * @param fileName the name of the file that is going to be attached to a mail
 *     (<documentNumber>.xml)
 * @param creationDate the date when the documentation unit XML export was created
 */
@Builder
public record XmlExportResult(
    String xml,
    boolean success,
    List<String> statusMessages,
    String fileName,
    Instant creationDate) {}
