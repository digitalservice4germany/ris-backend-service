package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import sibModel.GetEmailEventReportEvents.EventEnum;

@Service
@Slf4j
public class SendInBlueMailTrackingService implements MailTrackingService {

  private final DocumentationUnitService documentationUnitService;

  public SendInBlueMailTrackingService(DocumentationUnitService documentationUnitService) {
    this.documentationUnitService = documentationUnitService;
  }

  @Override
  public MailStatus mapEventToStatus(String mailTrackingEvent) {
    var event = EventEnum.fromValue(mailTrackingEvent);
    if (event == null) {
      return MailStatus.UNKNOWN;
    }
    if (List.of(
            EventEnum.BOUNCES,
            EventEnum.HARDBOUNCES,
            EventEnum.SOFTBOUNCES,
            EventEnum.INVALID,
            EventEnum.SPAM,
            EventEnum.BLOCKED,
            EventEnum.ERROR)
        .contains(event)) {
      return MailStatus.ERROR;
    }
    if (event == EventEnum.DELIVERED) {
      return MailStatus.SUCCESS;
    }
    return MailStatus.UNKNOWN;
  }

  /**
   * Process updates on the email sending status reported by the email service and log errors if
   * necessary
   *
   * @param documentationUnitUuid a tag provided by the email service that helps us to categorize
   *     the mail: If the tag is a valid documentation unit UUID, the email was exporting this
   *     documentation unit. Otherwise, the email was forwarding the process result of a
   *     documentation unit export
   * @param event the event that occurred during the mail sending process, e.g. "delivered",
   * @return a response entity with status 200 if the event was processed successfully, 204 if the
   *     event could not be mapped
   */
  @Override
  public ResponseEntity<String> processMailSendingState(
      String documentationUnitUuid, String event) {

    MailStatus state = mapEventToStatus(event);
    if (state == MailStatus.UNKNOWN) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    if (state == MailStatus.ERROR) {
      try {
        documentationUnitService.getByUuid(parseDocUnitUUID(documentationUnitUuid));
        log.error(
            "Received Mail sending error for forwarded email. Event: {}, Tag {}",
            documentationUnitUuid,
            event);
      } catch (DocumentationUnitNotExistsException ex) {
        log.error(
            "Failed to send Mail for documentation unit {} because of event {}",
            documentationUnitUuid,
            event);
      }
    }

    return ResponseEntity.status(HttpStatus.OK).build();
  }

  private UUID parseDocUnitUUID(String string) {
    try {
      return UUID.fromString(string);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
