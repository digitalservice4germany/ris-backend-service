package de.bund.digitalservice.ris.caselaw.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import de.bund.digitalservice.ris.caselaw.TestMemoryAppender;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitStatusService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverMail;
import de.bund.digitalservice.ris.caselaw.domain.HandoverReport;
import de.bund.digitalservice.ris.caselaw.domain.HandoverReportRepository;
import de.bund.digitalservice.ris.caselaw.domain.HandoverRepository;
import de.bund.digitalservice.ris.caselaw.domain.HttpMailSender;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionRepository;
import de.bund.digitalservice.ris.caselaw.domain.MailAttachment;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.Status;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import de.bund.digitalservice.ris.domain.export.juris.response.ImportMessageWrapper;
import de.bund.digitalservice.ris.domain.export.juris.response.MessageAttachment;
import de.bund.digitalservice.ris.domain.export.juris.response.ProcessMessageWrapper;
import de.bund.digitalservice.ris.domain.export.juris.response.StatusImporterException;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import({JurisXmlExporterResponseProcessor.class, JurisMessageWrapperFactory.class})
class JurisXmlExporterResponseProcessorTest {
  private static final String DOCUMENT_NUMBER = "KORE123456789";
  private static final UUID DOCUMENT_UUID = UUID.randomUUID();
  @MockBean private DocumentationUnitStatusService statusService;
  @MockBean private HttpMailSender mailSender;
  @MockBean private ImapStoreFactory storeFactory;
  @MockBean private HandoverReportRepository reportRepository;
  @MockBean private DocumentationUnitRepository documentationUnitRepository;
  @MockBean private HandoverRepository xmlHandoverRepository;

  @MockBean private LegalPeriodicalEditionRepository editionRepository;
  @Mock private Store store;
  @Mock private Folder inbox;
  @Mock private Folder processed;
  @Mock private Folder unprocessable;
  @Mock private Message importMessage;
  @Mock private ImportMessageWrapper importMessageWrapper;
  @Mock private Message processMessage;
  @Mock private ProcessMessageWrapper processMessageWrapper;
  @Mock private JurisMessageWrapperFactory wrapperFactory;

  private JurisXmlExporterResponseProcessor responseProcessor;

  @BeforeEach
  void setup() throws MessagingException, DocumentationUnitNotExistsException {
    when(storeFactory.createStore()).thenReturn(store);
    when(store.getFolder("INBOX")).thenReturn(inbox);
    when(store.getFolder("processed")).thenReturn(processed);
    when(store.getFolder("unprocessable")).thenReturn(unprocessable);

    when(importMessageWrapper.getMessage()).thenReturn(importMessage);
    when(importMessageWrapper.getIdentifier()).thenReturn(DOCUMENT_NUMBER);
    when(wrapperFactory.getResponsibleWrapper(importMessage))
        .thenReturn(Optional.of(importMessageWrapper));

    when(processMessageWrapper.getMessage()).thenReturn(processMessage);
    when(processMessageWrapper.getIdentifier()).thenReturn(DOCUMENT_NUMBER);
    when(wrapperFactory.getResponsibleWrapper(processMessage))
        .thenReturn(Optional.of(processMessageWrapper));

    when(reportRepository.saveAll(any())).thenReturn(Collections.emptyList());

    when(documentationUnitRepository.findByDocumentNumber(DOCUMENT_NUMBER))
        .thenReturn(DocumentationUnit.builder().uuid(DOCUMENT_UUID).build());

    when(xmlHandoverRepository.getLastXmlHandoverMail(DOCUMENT_UUID))
        .thenReturn(HandoverMail.builder().issuerAddress("test@digitalservice.bund.de").build());

    when(statusService.getLatestStatus(anyString())).thenReturn(PublicationStatus.UNPUBLISHED);

    responseProcessor =
        new JurisXmlExporterResponseProcessor(
            mailSender,
            statusService,
            storeFactory,
            reportRepository,
            wrapperFactory,
            documentationUnitRepository,
            xmlHandoverRepository,
            editionRepository);
  }

  @Test
  void testMessageGetsForwarded() throws MessagingException {
    when(inbox.getMessages()).thenReturn(new Message[] {importMessage});

    responseProcessor.readEmails();

    verify(storeFactory, times(1)).createStore();
    verify(xmlHandoverRepository, times(1)).getLastXmlHandoverMail(DOCUMENT_UUID);
    verify(mailSender, times(1))
        .sendMail(any(), any(), any(), any(), any(), eq("report-" + DOCUMENT_NUMBER));
    verify(inbox, times(1)).copyMessages(new Message[] {importMessage}, processed);
    verify(importMessage, times(1)).setFlag(Flag.DELETED, true);
  }

  @Test
  void testMessageGetsNotMovedIfNotForwarded() throws MessagingException {
    when(inbox.getMessages()).thenReturn(new Message[] {importMessage});
    when(importMessageWrapper.getSubject()).thenThrow(new MessagingException());
    TestMemoryAppender memoryAppender =
        new TestMemoryAppender(JurisXmlExporterResponseProcessor.class);

    responseProcessor.readEmails();

    verifyNoInteractions(mailSender);
    verify(inbox).copyMessages(new Message[] {importMessage}, unprocessable);
    verify(importMessage).setFlag(Flag.DELETED, true);
    assertThat(memoryAppender.count(Level.ERROR)).isEqualTo(1L);
    assertThat(memoryAppender.getMessage(Level.ERROR, 0)).isEqualTo("Message has no subject");

    memoryAppender.detachLoggingTestAppender();
  }

  @Test
  void testMessageGetsNotMovedIfDocumentNumberNotFound() throws MessagingException {
    when(inbox.getMessages()).thenReturn(new Message[] {importMessage});
    when(xmlHandoverRepository.getLastXmlHandoverMail(DOCUMENT_UUID)).thenReturn(null);
    TestMemoryAppender memoryAppender =
        new TestMemoryAppender(JurisXmlExporterResponseProcessor.class);

    responseProcessor.readEmails();

    verifyNoInteractions(mailSender);
    verify(inbox).copyMessages(new Message[] {importMessage}, unprocessable);
    verify(importMessage).setFlag(Flag.DELETED, true);
    assertThat(memoryAppender.count(Level.ERROR)).isEqualTo(1L);
    assertThat(memoryAppender.getMessage(Level.ERROR, 0))
        .isEqualTo("Message null couldn't processed");

    memoryAppender.detachLoggingTestAppender();
  }

  @Test
  void testMessageEncoding() throws MessagingException, IOException {
    when(inbox.getMessages()).thenReturn(new Message[] {importMessage});

    Multipart multipart = new MimeMultipart();
    BodyPart bodyPart = new MimeBodyPart();
    multipart.addBodyPart(bodyPart);
    MimeBodyPart attachmentPart = new MimeBodyPart();
    attachmentPart.attachFile("src/test/resources/EXAMPLE-LOGFILE.log");
    multipart.addBodyPart(attachmentPart);
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(attachmentPart.getInputStream()));

    when(importMessageWrapper.getAttachments())
        .thenReturn(
            Collections.singletonList(
                new MessageAttachment(
                    "test.txt", reader.lines().collect(Collectors.joining("\n")))));
    when(importMessage.getContent()).thenReturn(multipart);

    responseProcessor.readEmails();

    verify(mailSender, times(1))
        .sendMail(
            any(),
            any(),
            any(),
            any(),
            argThat(list -> list.get(0).equals(new MailAttachment("test.txt", "ÄÜÖäüöß"))),
            eq("report-" + DOCUMENT_NUMBER));
  }

  @Test
  void testAttachmentsGetsPersisted() throws MessagingException, IOException {
    when(inbox.getMessages()).thenReturn(new Message[] {processMessage});

    Date now = new Date();
    when(processMessageWrapper.getAttachments())
        .thenReturn(
            List.of(
                new MessageAttachment(String.format("%s.html", DOCUMENT_NUMBER), "report"),
                new MessageAttachment(
                    String.format("%s-spellcheck.html", DOCUMENT_NUMBER), "spellcheck")));
    when(processMessageWrapper.getIdentifier()).thenReturn(DOCUMENT_NUMBER);
    when(processMessageWrapper.getReceivedDate()).thenReturn(now.toInstant());

    responseProcessor.readEmails();

    verify(inbox, times(1)).copyMessages(new Message[] {processMessage}, processed);
    verify(reportRepository)
        .saveAll(
            List.of(
                HandoverReport.builder()
                    .content("report")
                    .entityId(DOCUMENT_UUID)
                    .receivedDate(now.toInstant())
                    .build(),
                HandoverReport.builder()
                    .content("spellcheck")
                    .entityId(DOCUMENT_UUID)
                    .receivedDate(now.toInstant())
                    .build()));
  }

  @Test
  void testAttachmentsGetSanitized() throws MessagingException, IOException {
    when(inbox.getMessages()).thenReturn(new Message[] {processMessage});
    Date now = new Date();
    String providedHtml =
        """
                    <html>
                    <head>
                        <META http-equiv="Content-Type" content="text/html; charset=UTF-8">
                        <title>Title</title>
                    </head>
                    <body>
                        <p><img src="https://placehold.it/120x120&text=image1" align="right"><br></p>
                        <h2>Header</h2>
                        <table border="0" width="55%">
                            <tbody>
                                <tr>
                                    <td width="50%"><strong>ABC</strong></td>
                                    <td width="50%" align="right"><strong>DEF</strong></td>
                                </tr>
                            </tbody>
                        </table>
                        <hr width="100%">
                        <p>Paragraph 1</p>
                        <p><strong>Paragraph 2<font color="#ff0000" size="+1">
                                    <i>Italic</i>
                                </font><br>
                                <table hspace="50" border="0" width="50%" cellSpacing="8">
                                    <tbody></tbody>
                                </table>Text
                            </strong><br></p>
                        <hr width="100%">
                    </body>
                    </html>""";
    String expectedHtml =
        """
                    <p><img src="https://placehold.it/120x120&amp;text&#61;image1" align="right" /><br /></p>
                    <h2>Header</h2>
                    <table border="0" width="55%">
                       <tbody>
                           <tr>
                               <td width="50%"><strong>ABC</strong></td>
                               <td width="50%" align="right"><strong>DEF</strong></td>
                           </tr>
                       </tbody>
                    </table>
                    <hr width="100%" />
                    <p>Paragraph 1</p>
                    <p><strong>Paragraph 2<font color="#ff0000">
                               <i>Italic</i>
                           </font><br />
                           <table hspace="50" border="0" width="50%" cellspacing="8">
                               <tbody></tbody>
                           </table>Text
                       </strong><br /></p>
                    <hr width="100%" />""";
    when(processMessageWrapper.getAttachments())
        .thenReturn(
            List.of(
                new MessageAttachment(
                    String.format("%s.html", DOCUMENT_NUMBER),
                    providedHtml.replaceAll("\\s+", " ").replaceAll(">\\s+<", "><")),
                new MessageAttachment(
                    String.format("%s-spellcheck.html", DOCUMENT_NUMBER),
                    "<p><script>alert('sanitize me')</script></p>")));
    when(processMessageWrapper.getIdentifier()).thenReturn(DOCUMENT_NUMBER);
    when((processMessageWrapper.getReceivedDate())).thenReturn(now.toInstant());

    responseProcessor.readEmails();

    verify(inbox, times(1)).copyMessages(new Message[] {processMessage}, processed);
    verify(reportRepository)
        .saveAll(
            List.of(
                HandoverReport.builder()
                    .content(expectedHtml.replaceAll("\\s+", " ").replaceAll(">\\s+<", "><"))
                    .entityId(DOCUMENT_UUID)
                    .receivedDate(now.toInstant())
                    .build(),
                HandoverReport.builder()
                    .content("<p></p>")
                    .entityId(DOCUMENT_UUID)
                    .receivedDate(now.toInstant())
                    .build()));
  }

  @Test
  void testProcessMessageKeepsStatus()
      throws MessagingException, DocumentationUnitNotExistsException {
    when(inbox.getMessages()).thenReturn(new Message[] {processMessage});

    when(processMessageWrapper.isPublished()).thenReturn(Optional.of(false));
    when(processMessageWrapper.hasErrors()).thenReturn(true);
    when(statusService.getLatestStatus(anyString())).thenReturn(PublicationStatus.UNPUBLISHED);

    responseProcessor.readEmails();

    verify(statusService, times(1))
        .update(
            anyString(),
            eq(
                Status.builder()
                    .publicationStatus(PublicationStatus.UNPUBLISHED)
                    .withError(true)
                    .build()));
  }

  @ParameterizedTest
  // only if is published and no errors, the resulting error state is false
  @CsvSource({"true, false, false", "true, true, true", "false, false, true", "false, true, true"})
  void testImportMessageSetsErrorOnFailedPublicationOrError(
      boolean isPublished, boolean hasError, boolean hasResultingError)
      throws MessagingException, DocumentationUnitNotExistsException {
    when(inbox.getMessages()).thenReturn(new Message[] {processMessage});

    when(processMessageWrapper.isPublished()).thenReturn(Optional.of(isPublished));
    when(processMessageWrapper.hasErrors()).thenReturn(hasError);

    responseProcessor.readEmails();

    verify(statusService, times(1))
        .update(
            anyString(),
            eq(
                Status.builder()
                    .publicationStatus(PublicationStatus.UNPUBLISHED)
                    .withError(hasResultingError)
                    .build()));
  }

  @Test
  void testImportMessagesGetProcessedFirst()
      throws MessagingException, DocumentationUnitNotExistsException {
    when(inbox.getMessages()).thenReturn(new Message[] {processMessage, importMessage});

    when(processMessageWrapper.hasErrors()).thenReturn(false);
    when(processMessageWrapper.isPublished()).thenReturn(Optional.of(true));

    when(importMessageWrapper.hasErrors()).thenReturn(true);
    when(importMessageWrapper.isPublished()).thenReturn(Optional.of(false));

    responseProcessor.readEmails();

    InOrder inOrder = Mockito.inOrder(statusService);

    inOrder.verify(statusService, times(1)).getLatestStatus(DOCUMENT_NUMBER);

    inOrder
        .verify(statusService, times(1))
        .update(
            anyString(),
            eq(
                Status.builder()
                    .publicationStatus(PublicationStatus.UNPUBLISHED)
                    .withError(true)
                    .build()));

    inOrder.verify(statusService, times(1)).getLatestStatus(DOCUMENT_NUMBER);

    inOrder
        .verify(statusService, times(1))
        .update(
            anyString(),
            eq(
                Status.builder()
                    .publicationStatus(PublicationStatus.UNPUBLISHED)
                    .withError(false)
                    .build()));

    inOrder.verifyNoMoreInteractions();
  }

  @Test
  void testLoggingForUnknownDocumentNumber() throws MessagingException {
    when(inbox.getMessages()).thenReturn(new Message[] {processMessage});
    when(xmlHandoverRepository.getLastXmlHandoverMail(DOCUMENT_UUID))
        .thenReturn(HandoverMail.builder().issuerAddress(null).build());

    assertThatCode(responseProcessor::readEmails).doesNotThrowAnyException();
  }

  @Test
  void testRethrowsIfCannotGetFolder() throws MessagingException {
    when(store.getFolder("INBOX")).thenThrow(new MessagingException());

    assertThatThrownBy(() -> responseProcessor.readEmails())
        .isInstanceOf(StatusImporterException.class)
        .hasMessage("Error processing inbox");
  }

  @Test
  void testRethrowsIfCannotSaveAttachment() throws MessagingException {
    when(inbox.getMessages()).thenReturn(new Message[] {processMessage});
    when(processMessageWrapper.getReceivedDate()).thenThrow(new MessagingException());
    TestMemoryAppender memoryAppender =
        new TestMemoryAppender(JurisXmlExporterResponseProcessor.class);

    responseProcessor.readEmails();

    assertThat(memoryAppender.count(Level.ERROR)).isEqualTo(1L);
    assertThat(memoryAppender.getMessage(Level.ERROR, 0))
        .isEqualTo("Message null couldn't processed");
    assertThat(memoryAppender.getCause(Level.ERROR, 0).getMessage())
        .isEqualTo("Error saving attachments");

    memoryAppender.detachLoggingTestAppender();
  }

  @Test
  void testRethrowsIfCannotSetStatus() throws MessagingException {
    when(inbox.getMessages()).thenReturn(new Message[] {processMessage});
    when(processMessageWrapper.hasErrors()).thenThrow(new IOException());
    TestMemoryAppender memoryAppender =
        new TestMemoryAppender(JurisXmlExporterResponseProcessor.class);

    responseProcessor.readEmails();

    assertThat(memoryAppender.count(Level.ERROR)).isEqualTo(1L);
    assertThat(memoryAppender.getMessage(Level.ERROR, 0))
        .isEqualTo("Message null couldn't processed");
    assertThat(memoryAppender.getCause(Level.ERROR, 0).getMessage())
        .isEqualTo("Could not update publicationStatus");

    memoryAppender.detachLoggingTestAppender();
  }
}
