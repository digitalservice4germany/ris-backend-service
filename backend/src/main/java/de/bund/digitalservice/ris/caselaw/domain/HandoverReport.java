package de.bund.digitalservice.ris.caselaw.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

/**
 * Represents a report on a performed handover. It is created from the response mail by the jDV
 * Email interface.
 */
@Builder(toBuilder = true)
public record HandoverReport(
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) UUID entityId,
    String content,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY, value = "date") Instant receivedDate)
    implements EventRecord {
  @Override
  public EventType getType() {
    return EventType.HANDOVER_REPORT;
  }

  @Override
  public Instant getDate() {
    return receivedDate;
  }
}
