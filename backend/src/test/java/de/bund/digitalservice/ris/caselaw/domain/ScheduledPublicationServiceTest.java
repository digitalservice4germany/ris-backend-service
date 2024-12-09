package de.bund.digitalservice.ris.caselaw.domain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScheduledPublicationServiceTest {

  private ScheduledPublicationService service;
  private DocumentationUnitRepository docUnitRepository;
  private HandoverService handoverService;
  private HttpMailSender httpMailSender;
  private final LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

  @BeforeEach
  void beforeEach() {
    this.docUnitRepository = mock(DocumentationUnitRepository.class);
    this.handoverService = mock(HandoverService.class);
    this.httpMailSender = mock(HttpMailSender.class);
    this.service =
        new ScheduledPublicationService(
            this.docUnitRepository, this.handoverService, this.httpMailSender);
  }

  @Test
  void shouldIdleForEmptyDueDocUnits() throws DocumentationUnitNotExistsException {
    when(this.docUnitRepository.getScheduledDocumentationUnitsDueNow())
        .thenReturn(Collections.emptyList());

    this.service.handoverScheduledDocUnits();

    verify(handoverService, never()).handoverDocumentationUnitAsMail(any(), any());
    verify(docUnitRepository, never()).save(any());
    verify(httpMailSender, never()).sendMail(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldHandoverMultipleDueDocUnits() throws DocumentationUnitNotExistsException {
    var publishedDocUnit = this.createDocUnit(pastDate, pastDate);
    var unpublishedDocUnit = this.createDocUnit(null, pastDate);
    when(this.docUnitRepository.getScheduledDocumentationUnitsDueNow())
        .thenReturn(List.of(publishedDocUnit, unpublishedDocUnit));
    mockHandoverWithSuccessStatus(true);

    this.service.handoverScheduledDocUnits();

    verifyPublicationAndDocUnitUpdate(publishedDocUnit);
    verifyPublicationAndDocUnitUpdate(unpublishedDocUnit);
    verify(httpMailSender, never()).sendMail(any(), any(), any(), any(), any(), any());
  }

  @Test
  void shouldStillSavePublicationDatesWhenHandoverIsUnsuccessful()
      throws DocumentationUnitNotExistsException {
    var publishedDocUnit = this.createDocUnit(pastDate, pastDate);
    var unpublishedDocUnit = this.createDocUnit(null, pastDate);
    when(this.docUnitRepository.getScheduledDocumentationUnitsDueNow())
        .thenReturn(List.of(publishedDocUnit, unpublishedDocUnit));
    mockHandoverWithSuccessStatus(false);
    // When sending the error notification fails, further processing should not be disrupted.
    mockFailedErrorNotificationOnce();

    this.service.handoverScheduledDocUnits();

    verifyPublicationAndDocUnitUpdate(publishedDocUnit);
    verifyPublicationAndDocUnitUpdate(unpublishedDocUnit);
    verifyEmailErrorNotification(publishedDocUnit);
    verifyEmailErrorNotification(unpublishedDocUnit);
  }

  @Test
  void shouldStillSavePublicationDatesWhenHandoverFails()
      throws DocumentationUnitNotExistsException {
    var publishedDocUnit = this.createDocUnit(pastDate, pastDate);
    var unpublishedDocUnit = this.createDocUnit(null, pastDate);
    when(this.docUnitRepository.getScheduledDocumentationUnitsDueNow())
        .thenReturn(List.of(publishedDocUnit, unpublishedDocUnit));
    when(this.handoverService.handoverDocumentationUnitAsMail(any(), any()))
        .thenThrow(DocumentationUnitNotExistsException.class);

    this.service.handoverScheduledDocUnits();

    verifyPublicationAndDocUnitUpdate(publishedDocUnit);
    verifyPublicationAndDocUnitUpdate(unpublishedDocUnit);
    verifyEmailErrorNotification(publishedDocUnit);
    verifyEmailErrorNotification(unpublishedDocUnit);
  }

  @Test
  void shouldContinueWhenDocUnitSaveFails() throws DocumentationUnitNotExistsException {
    var publishedDocUnit = this.createDocUnit(pastDate, pastDate);
    var unpublishedDocUnit = this.createDocUnit(null, pastDate);
    mockHandoverWithSuccessStatus(true);
    when(this.docUnitRepository.getScheduledDocumentationUnitsDueNow())
        .thenReturn(List.of(publishedDocUnit, unpublishedDocUnit));
    doThrow(RuntimeException.class).when(this.docUnitRepository).save(any());

    this.service.handoverScheduledDocUnits();

    verifyPublicationAndDocUnitUpdate(publishedDocUnit);
    verifyPublicationAndDocUnitUpdate(unpublishedDocUnit);
    verify(httpMailSender, never()).sendMail(any(), any(), any(), any(), any(), any());
  }

  private void mockHandoverWithSuccessStatus(boolean successStatus)
      throws DocumentationUnitNotExistsException {
    var result = new HandoverMail(null, null, "", "", null, successStatus, null, null, null);
    when(this.handoverService.handoverDocumentationUnitAsMail(any(), any())).thenReturn(result);
  }

  private DocumentationUnit createDocUnit(
      LocalDateTime lastPublicationDateTime, LocalDateTime scheduledPublicationDateTime) {
    String randomName = RandomStringGenerator.builder().selectFrom('a', 'b', 'c').get().generate(5);
    return DocumentationUnit.builder()
        .uuid(UUID.randomUUID())
        .documentNumber("KORE12345" + randomName)
        .managementData(
            ManagementData.builder()
                .borderNumbers(Collections.emptyList())
                .lastPublicationDateTime(lastPublicationDateTime)
                .scheduledPublicationDateTime(scheduledPublicationDateTime)
                .scheduledByEmail(randomName + "@example.local")
                .build())
        .build();
  }

  private void verifyPublicationAndDocUnitUpdate(DocumentationUnit docUnit)
      throws DocumentationUnitNotExistsException {
    verify(handoverService, times(1))
        .handoverDocumentationUnitAsMail(
            docUnit.uuid(), docUnit.managementData().scheduledByEmail());
    verify(docUnitRepository, times(1))
        .save(
            argThat(
                updatedDocUnit ->
                    werePublicationDatesSet(updatedDocUnit)
                        && areAllOtherFieldsEqual(docUnit, updatedDocUnit)));
  }

  private boolean werePublicationDatesSet(DocumentationUnit actualDocUnit) {
    boolean wasPublishedWithin5Seconds =
        Duration.between(
                    LocalDateTime.now(), actualDocUnit.managementData().lastPublicationDateTime())
                .abs()
                .getSeconds()
            < 5;
    boolean schedulingWasUnset =
        actualDocUnit.managementData().scheduledPublicationDateTime() == null;
    return schedulingWasUnset && wasPublishedWithin5Seconds;
  }

  private boolean areAllOtherFieldsEqual(
      DocumentationUnit expectedDocUnit, DocumentationUnit actualDocUnit) {
    return expectedDocUnit.toBuilder()
        .managementData(
            expectedDocUnit.managementData().toBuilder()
                .lastPublicationDateTime(null)
                .scheduledPublicationDateTime(null)
                .build())
        .build()
        .equals(
            actualDocUnit.toBuilder()
                .managementData(
                    expectedDocUnit.managementData().toBuilder()
                        .lastPublicationDateTime(null)
                        .scheduledPublicationDateTime(null)
                        .build())
                .build());
  }

  private void verifyEmailErrorNotification(DocumentationUnit docUnit) {
    verify(httpMailSender, times(1))
        .sendMail(
            any(),
            eq(docUnit.managementData().scheduledByEmail()),
            eq("Terminierte Abgabe fehlgeschlagen: " + docUnit.documentNumber()),
            contains("Terminierte Abgabe von"),
            eq(Collections.emptyList()),
            eq(docUnit.documentNumber()));
  }

  private void mockFailedErrorNotificationOnce() {
    // First call fails
    doThrow(new RuntimeException())
        // second call succeeds
        .doNothing()
        .when(httpMailSender)
        .sendMail(any(), any(), any(), any(), any(), any());
  }
}
