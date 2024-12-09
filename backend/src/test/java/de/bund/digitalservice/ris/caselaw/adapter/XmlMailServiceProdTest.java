package de.bund.digitalservice.ris.caselaw.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.domain.Attachment;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.HandoverEntityType;
import de.bund.digitalservice.ris.caselaw.domain.HandoverMail;
import de.bund.digitalservice.ris.caselaw.domain.HandoverRepository;
import de.bund.digitalservice.ris.caselaw.domain.HttpMailSender;
import de.bund.digitalservice.ris.caselaw.domain.MailAttachment;
import de.bund.digitalservice.ris.caselaw.domain.XmlExporter;
import de.bund.digitalservice.ris.caselaw.domain.XmlTransformationResult;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import({HandoverMailService.class})
@TestPropertySource(
    properties = {
      "mail.exporter.senderAddress=export@neuris",
      "mail.exporter.jurisUsername=NeuRIS"
    })
@ActiveProfiles(profiles = {"production"})
class XmlMailServiceProdTest {
  private static final String RECEIVER_ADDRESS = "test-to@mail.com";

  private static final String ISSUER_ADDRESS = "neuris-user@example.com";
  private static final String SENDER_ADDRESS = "export@neuris";
  private static final Instant CREATED_DATE = Instant.parse("2020-05-05T10:21:35.00Z");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String DELIVER_DATE =
      LocalDate.now(Clock.system(ZoneId.of("Europe/Berlin"))).format(DATE_FORMATTER);
  private static final String PROD_MAIL_SUBJECT =
      "id=juris name=NeuRIS da=R df=X dt=N mod=T ld=" + DELIVER_DATE + " vg=test-document-number";
  private static final UUID TEST_UUID = UUID.fromString("88888888-4444-4444-4444-121212121212");
  private static final HandoverMail EXPECTED_BEFORE_SAVE_PROD =
      HandoverMail.builder()
          .entityId(TEST_UUID)
          .entityType(HandoverEntityType.DOCUMENTATION_UNIT)
          .receiverAddress(RECEIVER_ADDRESS)
          .mailSubject(PROD_MAIL_SUBJECT)
          .attachments(
              Collections.singletonList(
                  MailAttachment.builder().fileContent("xml").fileName("test.xml").build()))
          .success(true)
          .statusMessages(List.of("succeed"))
          .handoverDate(CREATED_DATE)
          .issuerAddress(ISSUER_ADDRESS)
          .build();

  private static final HandoverMail SAVED_XML_MAIL_PROD =
      HandoverMail.builder()
          .entityId(TEST_UUID)
          .receiverAddress(RECEIVER_ADDRESS)
          .mailSubject(PROD_MAIL_SUBJECT)
          .attachments(
              Collections.singletonList(
                  MailAttachment.builder().fileContent("xml").fileName("test.xml").build()))
          .success(true)
          .statusMessages(List.of("succeed"))
          .handoverDate(CREATED_DATE)
          .issuerAddress(ISSUER_ADDRESS)
          .build();
  private static final XmlTransformationResult FORMATTED_XML =
      new XmlTransformationResult("xml", true, List.of("succeed"), "test.xml", CREATED_DATE);

  private DocumentationUnit documentationUnit;

  @Autowired private HandoverMailService service;

  @MockBean private XmlExporter xmlExporter;

  @MockBean private HandoverRepository repository;

  @MockBean private DatabaseDocumentationUnitRepository documentationUnitRepository;

  @MockBean private HttpMailSender mailSender;

  @BeforeEach
  void setUp() throws ParserConfigurationException, TransformerException {
    documentationUnit =
        DocumentationUnit.builder()
            .uuid(TEST_UUID)
            .documentNumber("test-document-number")
            .coreData(
                CoreData.builder()
                    .court(Court.builder().location("testLocation").label("testLabel").build())
                    .build())
            .attachments(Collections.singletonList(Attachment.builder().name("file_name").build()))
            .build();
    when(xmlExporter.transformToXml(any(DocumentationUnit.class))).thenReturn(FORMATTED_XML);

    when(repository.save(EXPECTED_BEFORE_SAVE_PROD)).thenReturn(SAVED_XML_MAIL_PROD);
  }

  @Test
  void testSendWithProdSubjectAndOriginalCourtAndFileNumber()
      throws ParserConfigurationException, TransformerException {

    HandoverMail response = service.handOver(documentationUnit, RECEIVER_ADDRESS, ISSUER_ADDRESS);

    assertThat(response.mailSubject()).isEqualTo(PROD_MAIL_SUBJECT);

    verify(xmlExporter).transformToXml(documentationUnit);
    verify(repository).save(EXPECTED_BEFORE_SAVE_PROD);
    verify(mailSender)
        .sendMail(
            SENDER_ADDRESS,
            RECEIVER_ADDRESS,
            SAVED_XML_MAIL_PROD.mailSubject(),
            "neuris",
            Collections.singletonList(
                MailAttachment.builder()
                    .fileName(SAVED_XML_MAIL_PROD.attachments().get(0).fileName())
                    .fileContent(SAVED_XML_MAIL_PROD.attachments().get(0).fileContent())
                    .build()),
            SAVED_XML_MAIL_PROD.entityId().toString());
  }
}
