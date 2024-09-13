package de.bund.digitalservice.ris.caselaw.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseLegalPeriodicalEditionRepository;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.HandoverEntityType;
import de.bund.digitalservice.ris.caselaw.domain.HandoverException;
import de.bund.digitalservice.ris.caselaw.domain.HandoverMail;
import de.bund.digitalservice.ris.caselaw.domain.HandoverRepository;
import de.bund.digitalservice.ris.caselaw.domain.HttpMailSender;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import de.bund.digitalservice.ris.caselaw.domain.MailAttachment;
import de.bund.digitalservice.ris.caselaw.domain.Reference;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.XmlExporter;
import de.bund.digitalservice.ris.caselaw.domain.XmlTransformationResult;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
      "mail.exporter.jurisUsername=test-user",
      "mail.exporter.senderAddress=export@neuris"
    })
@ActiveProfiles(profiles = {"uat"})
class HandoverMailServiceTest {
  private static final String RECEIVER_ADDRESS = "test-to@mail.com";
  private static final String ISSUER_ADDRESS = "neuris-user@example.com";
  private static final String SENDER_ADDRESS = "export@neuris";
  private static final String JURIS_USERNAME = "test-user";
  private static final Instant CREATED_DATE = Instant.parse("2020-05-05T10:21:35.00Z");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String DELIVER_DATE =
      LocalDate.now(Clock.system(ZoneId.of("Europe/Berlin"))).format(DATE_FORMATTER);
  private static final String DOC_UNIT_MAIL_SUBJECT =
      "id=juris name=%s da=R df=X dt=N mod=T ld=%s vg=test-document-number"
          .formatted(JURIS_USERNAME, DELIVER_DATE);

  private static final UUID TEST_UUID = UUID.fromString("88888888-4444-4444-4444-121212121212");

  private static final String EDITION_MAIL_SUBJECT =
      "id=juris name=%s da=R df=X dt=F mod=T ld=%s vg=edition-%s"
          .formatted(JURIS_USERNAME, DELIVER_DATE, TEST_UUID);

  private static final HandoverMail DOC_UNIT_SAVED_MAIL =
      HandoverMail.builder()
          .entityId(TEST_UUID)
          .entityType(HandoverEntityType.DOCUMENTATION_UNIT)
          .receiverAddress(RECEIVER_ADDRESS)
          .mailSubject(DOC_UNIT_MAIL_SUBJECT)
          .attachments(
              List.of(MailAttachment.builder().fileName("test.xml").fileContent("xml").build()))
          .success(true)
          .statusMessages(List.of("succeed"))
          .handoverDate(CREATED_DATE)
          .issuerAddress(ISSUER_ADDRESS)
          .build();

  private static final HandoverMail EDITION_SAVED_MAIL =
      HandoverMail.builder()
          .entityId(TEST_UUID)
          .entityType(HandoverEntityType.EDITION)
          .receiverAddress(RECEIVER_ADDRESS)
          .mailSubject(EDITION_MAIL_SUBJECT)
          .attachments(
              List.of(
                  MailAttachment.builder().fileName("test1.xml").fileContent("xml 1").build(),
                  MailAttachment.builder().fileName("test2.xml").fileContent("xml 2").build()))
          .success(true)
          .statusMessages(List.of("succeed", "succeed"))
          .handoverDate(CREATED_DATE)
          .issuerAddress(ISSUER_ADDRESS)
          .build();
  private static final XmlTransformationResult DOC_UNIT_XML =
      new XmlTransformationResult("xml", true, List.of("succeed"), "test.xml", CREATED_DATE);

  private static final List<XmlTransformationResult> EDITION_XML =
      List.of(
          new XmlTransformationResult("xml 1", true, List.of("succeed"), "test1.xml", CREATED_DATE),
          new XmlTransformationResult(
              "xml 2", true, List.of("succeed"), "test2.xml", CREATED_DATE));

  private DocumentationUnit documentationUnit;

  private LegalPeriodicalEdition edition;

  @Autowired private HandoverMailService service;

  @MockBean private XmlExporter xmlExporter;

  @MockBean private HandoverRepository repository;

  @MockBean private DatabaseDocumentationUnitRepository documentationUnitRepository;

  @MockBean private DatabaseLegalPeriodicalEditionRepository editionRepository;

  @MockBean private HttpMailSender mailSender;

  @BeforeEach
  void setUp() throws ParserConfigurationException, TransformerException {
    documentationUnit =
        DocumentationUnit.builder().uuid(TEST_UUID).documentNumber("test-document-number").build();
    LegalPeriodical legalPeriodical = LegalPeriodical.builder().abbreviation("ABC").build();
    edition =
        LegalPeriodicalEdition.builder()
            .legalPeriodical(legalPeriodical)
            .id(TEST_UUID)
            .references(
                List.of(
                    Reference.builder()
                        .citation("2004, 1")
                        .legalPeriodical(legalPeriodical)
                        .documentationUnit(
                            RelatedDocumentationUnit.builder()
                                .documentNumber("document-number-1")
                                .build())
                        .build(),
                    Reference.builder()
                        .citation("2004, 2")
                        .legalPeriodical(legalPeriodical)
                        .documentationUnit(
                            RelatedDocumentationUnit.builder()
                                .documentNumber("document-number-2")
                                .build())
                        .build()))
            .build();
    when(xmlExporter.transformToXml(any(DocumentationUnit.class))).thenReturn(DOC_UNIT_XML);
    when(xmlExporter.transformToXml(any(LegalPeriodicalEdition.class))).thenReturn(EDITION_XML);
    when(repository.save(DOC_UNIT_SAVED_MAIL)).thenReturn(DOC_UNIT_SAVED_MAIL);
    when(repository.save(EDITION_SAVED_MAIL)).thenReturn(EDITION_SAVED_MAIL);
  }

  @Test
  void testSendDocumentationUnit() throws ParserConfigurationException, TransformerException {
    var response = service.handOver(documentationUnit, RECEIVER_ADDRESS, ISSUER_ADDRESS);

    assertThat(response).usingRecursiveComparison().isEqualTo(DOC_UNIT_SAVED_MAIL);

    verify(xmlExporter)
        .transformToXml(
            documentationUnit.toBuilder()
                .coreData(
                    CoreData.builder()
                        .court(
                            Court.builder()
                                .label("VGH Mannheim")
                                .location("Mannheim")
                                .type("VGH")
                                .build())
                        .fileNumbers(List.of("TEST"))
                        .build())
                .build());
    verify(repository).save(DOC_UNIT_SAVED_MAIL);
    verify(mailSender)
        .sendMail(
            SENDER_ADDRESS,
            RECEIVER_ADDRESS,
            DOC_UNIT_SAVED_MAIL.mailSubject(),
            "neuris",
            Collections.singletonList(
                MailAttachment.builder()
                    .fileName(DOC_UNIT_SAVED_MAIL.attachments().get(0).fileName())
                    .fileContent(DOC_UNIT_SAVED_MAIL.attachments().get(0).fileContent())
                    .build()),
            DOC_UNIT_SAVED_MAIL.entityId().toString());
  }

  @Test
  void testSendEdition() throws ParserConfigurationException, TransformerException {
    var response = service.handOver(edition, RECEIVER_ADDRESS, ISSUER_ADDRESS);

    assertThat(response).usingRecursiveComparison().isEqualTo(EDITION_SAVED_MAIL);

    verify(xmlExporter).transformToXml(edition);
    verify(repository).save(EDITION_SAVED_MAIL);
    verify(mailSender)
        .sendMail(
            SENDER_ADDRESS,
            RECEIVER_ADDRESS,
            EDITION_SAVED_MAIL.mailSubject(),
            "neuris",
            EDITION_SAVED_MAIL.attachments(),
            EDITION_SAVED_MAIL.entityId().toString());
  }

  @Test
  void testSend_withValidationError() throws ParserConfigurationException, TransformerException {
    var xmlWithValidationError =
        new XmlTransformationResult(
            "xml", false, List.of("status-message"), "test.xml", CREATED_DATE);
    var expected =
        HandoverMail.builder()
            .entityId(TEST_UUID)
            .statusMessages(List.of("status-message"))
            .success(false)
            .build();

    when(xmlExporter.transformToXml(any(DocumentationUnit.class)))
        .thenReturn(xmlWithValidationError);

    var response = service.handOver(documentationUnit, RECEIVER_ADDRESS, ISSUER_ADDRESS);
    assertThat(response).usingRecursiveComparison().isEqualTo(expected);

    verify(repository, times(0)).save(any(HandoverMail.class));
    verify(mailSender, times(0))
        .sendMail(anyString(), anyString(), anyString(), anyString(), anyList(), anyString());
  }

  @Test
  void testSend_withExceptionFromXmlExporter()
      throws ParserConfigurationException, TransformerException {
    when(xmlExporter.transformToXml(any(DocumentationUnit.class)))
        .thenThrow(ParserConfigurationException.class);

    HandoverException ex =
        Assertions.assertThrows(
            HandoverException.class,
            () -> service.handOver(documentationUnit, RECEIVER_ADDRESS, ISSUER_ADDRESS));
    Assertions.assertEquals("Couldn't generate xml for documentationUnit.", ex.getMessage());

    verify(repository, times(0)).save(any(HandoverMail.class));
    verify(mailSender, times(0))
        .sendMail(anyString(), anyString(), anyString(), anyString(), anyList(), anyString());
  }

  @Test
  void testSend_withoutDocumentNumber() {
    documentationUnit = documentationUnit.toBuilder().documentNumber(null).build();

    // Call the method and check for the exception
    Throwable throwable =
        Assert.assertThrows(
            HandoverException.class,
            () -> service.handOver(documentationUnit, RECEIVER_ADDRESS, ISSUER_ADDRESS));

    assertThat(throwable.getMessage())
        .isEqualTo("No document number has set in the document unit.");

    // Verify that repository.save and mailSender.sendMail were not called
    verify(repository, times(0)).save(any(HandoverMail.class));
    verify(mailSender, times(0))
        .sendMail(anyString(), anyString(), anyString(), anyString(), any(List.class), anyString());
  }

  @Test
  void testSend_withExceptionBySaving() {
    when(repository.save(DOC_UNIT_SAVED_MAIL)).thenThrow(IllegalArgumentException.class);

    Assert.assertThrows(
        IllegalArgumentException.class,
        () -> service.handOver(documentationUnit, RECEIVER_ADDRESS, ISSUER_ADDRESS));

    verify(repository).save(any(HandoverMail.class));
    verify(mailSender)
        .sendMail(anyString(), anyString(), anyString(), anyString(), anyList(), anyString());
  }

  @Test
  void testSend_withoutToReceiverAddressSet() {
    Throwable throwable =
        Assert.assertThrows(
            HandoverException.class, () -> service.handOver(documentationUnit, null, null));

    assertThat(throwable.getMessage()).isEqualTo("No receiver mail address is set");

    verify(repository, times(0)).save(any(HandoverMail.class));
    verify(mailSender, times(0))
        .sendMail(anyString(), anyString(), anyString(), anyString(), anyList(), anyString());
  }

  @Test
  void testSend_withExceptionBySendingEmail() {
    doThrow(HandoverException.class)
        .when(mailSender)
        .sendMail(
            SENDER_ADDRESS,
            RECEIVER_ADDRESS,
            DOC_UNIT_MAIL_SUBJECT,
            "neuris",
            Collections.singletonList(
                MailAttachment.builder().fileName("test.xml").fileContent("xml").build()),
            TEST_UUID.toString());

    Assert.assertThrows(
        HandoverException.class,
        () -> service.handOver(documentationUnit, RECEIVER_ADDRESS, ISSUER_ADDRESS));

    verify(repository, times(0)).save(any(HandoverMail.class));
    verify(mailSender)
        .sendMail(
            SENDER_ADDRESS,
            RECEIVER_ADDRESS,
            DOC_UNIT_MAIL_SUBJECT,
            "neuris",
            Collections.singletonList(
                MailAttachment.builder().fileName("test.xml").fileContent("xml").build()),
            TEST_UUID.toString());
  }

  // Method providing the parameters for the test
  static Stream<Arguments> provideEntityTypes() {
    return Stream.of(
        Arguments.of(HandoverEntityType.DOCUMENTATION_UNIT),
        Arguments.of(HandoverEntityType.EDITION));
  }

  @ParameterizedTest
  @MethodSource("provideEntityTypes")
  void testGetLastHandoverXmlMail(HandoverEntityType entityType) {
    List<HandoverMail> list = List.of(DOC_UNIT_SAVED_MAIL);

    when(repository.getHandoversByEntity(TEST_UUID, entityType)).thenReturn(list);

    var response = service.getHandoverResult(TEST_UUID, entityType);

    assertThat(response.get(0)).usingRecursiveComparison().isEqualTo(DOC_UNIT_SAVED_MAIL);
    verify(repository).getHandoversByEntity(TEST_UUID, entityType);
  }
}
