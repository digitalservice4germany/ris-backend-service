package de.bund.digitalservice.ris.caselaw.domain;

import org.springframework.http.ResponseEntity;

public interface MailTrackingService {

  MailStatus mapEventToStatus(String mailTrackingEvent);

  ResponseEntity<String> processMailSendingState(String documentationUnitUuid, String event);
}
