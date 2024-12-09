package de.bund.digitalservice.ris.caselaw.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents a mail to handover an entity to jDV
 *
 * @param entityId the UUID of the entity (documentation unit or edition)
 * @param entityType tells whether it's a documentation unit or an edition handover
 * @param receiverAddress the address of the receiver (jDV mail interface)
 * @param mailSubject the subject of the mail containing handover options (e.g. desired operation)
 * @param success true if the XML export operation was successful (200)
 * @param statusMessages a list of issues found during the export operation
 * @param handoverDate the date the mail was sent
 * @param issuerAddress the address of the user issuing the handover, used to redirect replies by
 *     the mail interface
 */
@Builder(toBuilder = true)
public record HandoverMail(
    UUID entityId,
    HandoverEntityType entityType,
    String receiverAddress,
    String mailSubject,
    List<MailAttachment> attachments,
    @Getter boolean success,
    List<String> statusMessages,
    @Getter @JsonProperty(access = JsonProperty.Access.WRITE_ONLY, value = "date")
        Instant handoverDate,
    @Getter String issuerAddress)
    implements EventRecord {
  @Override
  public EventType getType() {
    return EventType.HANDOVER;
  }

  @Override
  public Instant getDate() {
    return getHandoverDate();
  }
}
