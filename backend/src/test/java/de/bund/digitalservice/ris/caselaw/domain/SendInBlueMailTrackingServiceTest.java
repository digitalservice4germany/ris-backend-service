package de.bund.digitalservice.ris.caselaw.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import(SendInBlueMailTrackingService.class)
class SendInBlueMailTrackingServiceTest {

  @SpyBean private SendInBlueMailTrackingService service;

  @MockBean private DocumentationUnitService documentationUnitService;

  private static final UUID TEST_UUID = UUID.fromString("88888888-4444-4444-4444-121212121212");

  @Test
  void testMapMailEventToState_withSuccessfulState() {
    MailStatus expectedMailStatus = MailStatus.SUCCESS;
    MailStatus mappedMailStatus = service.mapEventToStatus("delivered");

    assertThat(mappedMailStatus).isEqualTo(expectedMailStatus);
  }

  @Test
  void testMapMailEventToState_withUnsuccessfulState() {
    MailStatus expectedMailStatus = MailStatus.ERROR;
    MailStatus mappedMailStatus = service.mapEventToStatus("bounces");

    assertThat(mappedMailStatus).isEqualTo(expectedMailStatus);
  }

  @Test
  void testMailEventToState_withNeutralState() {
    MailStatus expectedMailStatus = MailStatus.UNKNOWN;
    MailStatus mappedMailStatus = service.mapEventToStatus("opened");

    assertThat(mappedMailStatus).isEqualTo(expectedMailStatus);
  }

  @Test
  void testProcessDeliveredEvent_shouldNotSearchUUIDInDb()
      throws DocumentationUnitNotExistsException {
    when(documentationUnitService.getByUuid(TEST_UUID))
        .thenReturn(DocumentationUnit.builder().uuid(TEST_UUID).build());

    service.processMailSendingState("88888888-4444-4444-4444-121212121212", "delivered");
    verifyNoInteractions(documentationUnitService);
  }

  @Test
  void testProcessDeliveredEventOfKnownUUID_shouldSearchInDb()
      throws DocumentationUnitNotExistsException {
    when(documentationUnitService.getByUuid(TEST_UUID))
        .thenReturn(DocumentationUnit.builder().uuid(TEST_UUID).build());

    service.processMailSendingState(TEST_UUID.toString(), "error");
    verify(documentationUnitService).getByUuid(TEST_UUID);
  }

  @Test
  void testProcessFailedDeliveryEventOfUnknownId_shouldSearchInDb()
      throws DocumentationUnitNotExistsException {

    when(documentationUnitService.getByUuid(TEST_UUID))
        .thenReturn(DocumentationUnit.builder().uuid(TEST_UUID).build());

    service.processMailSendingState("88888888-4444-4444-4444-121212121212", "error");
    verify(documentationUnitService)
        .getByUuid(UUID.fromString("88888888-4444-4444-4444-121212121212"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        // no-op event
        "clicks",
        // unknown event
        "randomEvent"
      })
  void testUpdateState_noReactionOnOtherState(String event) {
    ResponseEntity<String> responseEntity =
        service.processMailSendingState(TEST_UUID.toString(), event);
    assertThat(responseEntity.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(204))).isTrue();
    verifyNoInteractions(documentationUnitService);
  }
}
