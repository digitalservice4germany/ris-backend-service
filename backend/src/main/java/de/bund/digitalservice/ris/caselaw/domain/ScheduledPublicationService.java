package de.bund.digitalservice.ris.caselaw.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ScheduledPublicationService {

  private final DocumentationUnitRepository docUnitRepository;

  private final HandoverService handoverService;

  private final HttpMailSender mailSender;

  @Value("${mail.exporter.senderAddress:export.test@neuris}")
  private String senderAddress;

  /**
   * "Terminierte Abgabe": Cron job that finds doc units that have a scheduled publishing date-time
   * now, or in the past and publishes them. Even if publication fails, we will unset the scheduling
   * and save the timestamp of the last attempt.
   */
  public ScheduledPublicationService(
      DocumentationUnitRepository docUnitRepository,
      HandoverService handoverService,
      HttpMailSender mailSender) {

    this.docUnitRepository = docUnitRepository;
    this.handoverService = handoverService;
    this.mailSender = mailSender;
  }

  // Runs every minute, starts initially after 5s
  @Scheduled(fixedRateString = "PT1M", initialDelayString = "PT5S")
  @SchedulerLock(name = "scheduled-publication-job", lockAtMostFor = "PT3M")
  public void handoverScheduledDocUnits() {
    var scheduledDocUnitsDueNow = this.docUnitRepository.getScheduledDocumentationUnitsDueNow();
    if (!scheduledDocUnitsDueNow.isEmpty()) {
      log.info("Publishing {} scheduled doc units due now", scheduledDocUnitsDueNow.size());
    }
    for (var docUnit : scheduledDocUnitsDueNow) {
      // We will continue processing on any exceptions that are thrown.
      handoverDocument(docUnit);
      savePublicationDates(docUnit);
    }
  }

  private void handoverDocument(DocumentationUnit docUnit) {
    try {
      String email = docUnit.managementData().scheduledByEmail();
      var result = this.handoverService.handoverDocumentationUnitAsMail(docUnit.uuid(), email);
      if (!result.isSuccess()) {
        throw new HandoverException(String.join(", ", result.statusMessages()));
      }
    } catch (Exception e) {
      // No rethrow: even if the publication fails, we want to unset the scheduling.
      log.error(
          "Could not publish scheduled doc unit {}, scheduling will still be removed",
          docUnit.documentNumber(),
          e);
      informUserAboutErrorViaMail(docUnit, e);
    }
  }

  private void informUserAboutErrorViaMail(DocumentationUnit docUnit, Exception error) {
    try {
      var docNumber = docUnit.documentNumber();
      String email = docUnit.managementData().scheduledByEmail();
      String subject = "Terminierte Abgabe fehlgeschlagen: " + docNumber;
      String body =
          ("""
Die Terminierte Abgabe von Dokument %s konnte nicht erfolgen.
Bitte beheben Sie den Fehler und wiederholen Sie die Abgabe manuell.
Technischer Fehler: %s""")
              .formatted(docNumber, error.getMessage());
      List<MailAttachment> mailAttachments = Collections.emptyList();
      this.mailSender.sendMail(senderAddress, email, subject, body, mailAttachments, docNumber);
    } catch (Exception e) {
      log.error("Could not send error notification to user {}", docUnit.documentNumber(), e);
    }
  }

  private void savePublicationDates(DocumentationUnit docUnit) {
    try {
      var updatedDocUnit = setPublicationDates(docUnit);
      docUnitRepository.save(updatedDocUnit);
    } catch (Exception e) {
      // No rethrow: continue with other doc units, even if save fails for this one.
      log.error("Could not remove the scheduling for doc unit {}", docUnit.documentNumber(), e);
    }
  }

  private DocumentationUnit setPublicationDates(DocumentationUnit docUnit) {
    return docUnit.toBuilder()
        .managementData(
            docUnit.managementData().toBuilder()
                .scheduledPublicationDateTime(null)
                .lastPublicationDateTime(LocalDateTime.now())
                .scheduledByEmail(null)
                .build())
        .build();
  }
}
