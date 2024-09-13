package de.bund.digitalservice.ris.caselaw.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.bund.digitalservice.ris.caselaw.config.ConverterConfig;
import de.bund.digitalservice.ris.caselaw.domain.ActiveCitation;
import de.bund.digitalservice.ris.caselaw.domain.ContentRelatedIndexing;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.EnsuingDecision;
import de.bund.digitalservice.ris.caselaw.domain.NormReference;
import de.bund.digitalservice.ris.caselaw.domain.PreviousDecision;
import de.bund.digitalservice.ris.caselaw.domain.Procedure;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.SingleNorm;
import de.bund.digitalservice.ris.caselaw.domain.Status;
import de.bund.digitalservice.ris.caselaw.domain.Texts;
import de.bund.digitalservice.ris.caselaw.domain.XmlExporterException;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.NormAbbreviation;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.citation.CitationType;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.Norm;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import({ConverterConfig.class})
@Slf4j
class XmlExporterTest {
  @Autowired private ObjectMapper objectMapper;

  @Test
  void testExporter() throws XmlExporterException {
    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .coreData(generateCoreData())
            .documentNumber("document number")
            .previousDecisions(generatePreviousDecisions())
            .ensuingDecisions(generateEnsuingDecisions())
            .texts(generateTexts())
            .status(generateStatus())
            .contentRelatedIndexing(generateContentRelatedIndexing())
            .build();

    String encryptedXml =
        new JurisXmlExporterWrapper(objectMapper).generateEncryptedXMLString(documentationUnit);

    assertThat(encryptedXml)
        .isEqualTo(
            "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");
  }

  private ContentRelatedIndexing generateContentRelatedIndexing() {
    return ContentRelatedIndexing.builder()
        .activeCitations(generateActiveCitations())
        .norms(generateNorms())
        .keywords(generateKeywords())
        .fieldsOfLaw(generateFieldOfLaws())
        .jobProfiles(generateJobProfiles())
        .build();
  }

  private List<FieldOfLaw> generateFieldOfLaws() {
    List<Norm> norms =
        List.of(
            Norm.builder()
                .abbreviation("field of law norm abbreviation 1")
                .singleNormDescription("field of law norm description 1")
                .build(),
            Norm.builder()
                .abbreviation("field of law norm abbreviation 2")
                .singleNormDescription("field of law norm description 2")
                .build());
    return List.of(
        FieldOfLaw.builder()
            .identifier("norm identifier 1")
            .text("norm text 1")
            .norms(norms)
            .build(),
        FieldOfLaw.builder().identifier("norm identifier 2").text("norm text 2").build());
  }

  private List<String> generateKeywords() {
    return List.of("keyword 1", "keyword 2");
  }

  private List<NormReference> generateNorms() {
    List<SingleNorm> singleNorms1 =
        List.of(
            SingleNorm.builder()
                .singleNorm("single norm 1")
                .dateOfVersion(LocalDate.parse("1999-01-17"))
                .dateOfRelevance("1999")
                .build(),
            SingleNorm.builder().singleNorm("single norm 2").build(),
            SingleNorm.builder().dateOfVersion(LocalDate.parse("1985-07-26")).build(),
            SingleNorm.builder().dateOfRelevance("1973").build());
    return List.of(
        NormReference.builder()
            .singleNorms(singleNorms1)
            .normAbbreviation(
                NormAbbreviation.builder().abbreviation("norm abbreviation 1").build())
            .build(),
        NormReference.builder()
            .singleNorms(Collections.emptyList())
            .normAbbreviationRawValue("norm abbreviation raw value")
            .build());
  }

  private List<ActiveCitation> generateActiveCitations() {
    return List.of(
        ActiveCitation.builder()
            .citationType(
                CitationType.builder().jurisShortcut("ct1").label("citation type 1").build())
            .fileNumber("active citation file number 1")
            .documentType(
                DocumentType.builder()
                    .jurisShortcut("acdt1")
                    .label("active citation document type 1")
                    .build())
            .court(
                Court.builder()
                    .type("active citation court type 1")
                    .location("active citation court location 1")
                    .build())
            .decisionDate(LocalDate.parse("2001-07-22"))
            .documentNumber("active citation document number")
            .build(),
        ActiveCitation.builder()
            .citationType(
                CitationType.builder().jurisShortcut("ct2").label("citation type2").build())
            .fileNumber("active citation file number 2")
            .documentType(
                DocumentType.builder()
                    .jurisShortcut("acdt2")
                    .label("active citation document type 2")
                    .build())
            .court(
                Court.builder()
                    .type("active citation court type 2")
                    .location("active citation court location 2")
                    .build())
            .decisionDate(LocalDate.parse("2005-11-29"))
            .build());
  }

  private List<String> generateJobProfiles() {
    return List.of("job profile 1", "job profile 2");
  }

  private Status generateStatus() {
    LocalDateTime localDateTime = LocalDateTime.of(2020, Month.MAY, 6, 17, 35);
    ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("UTC"));

    return Status.builder()
        .publicationStatus(PublicationStatus.PUBLISHED)
        .withError(false)
        .createdAt(zonedDateTime.toInstant())
        .build();
  }

  private Texts generateTexts() {
    return Texts.builder()
        .decisionReasons("decision reasons")
        .caseFacts("case facts")
        .reasons("reasons")
        .tenor("tenor")
        .headnote("headnote")
        .otherHeadnote("other headnote")
        .guidingPrinciple("guiding principle")
        .headline("headline")
        .decisionName("decision name")
        .dissentingOpinion("dissenting opinion")
        .otherLongText("other long text")
        // outline is missing here because otherHeadnote and outline must not be filled both at the
        // same time
        .build();
  }

  private List<EnsuingDecision> generateEnsuingDecisions() {
    return List.of(
        EnsuingDecision.builder()
            .fileNumber("pending decision file number")
            .court(
                Court.builder()
                    .type("pending decision court type")
                    .location("pending decision court location")
                    .build())
            .documentType(
                DocumentType.builder()
                    .jurisShortcut("peddt")
                    .label("pending decision document type")
                    .build())
            .note("pending decision note")
            .pending(true)
            .build(),
        EnsuingDecision.builder()
            .fileNumber("ensuing decision file number")
            .court(
                Court.builder()
                    .type("ensuing decision court type")
                    .location("ensuing decision court location")
                    .build())
            .decisionDate(LocalDate.parse("2005-06-17"))
            .documentType(
                DocumentType.builder()
                    .jurisShortcut("eddt")
                    .label("ensuing decision document type")
                    .build())
            .note("ensuing decision note")
            .pending(false)
            .documentNumber("ensuing decision document number")
            .build());
  }

  private List<PreviousDecision> generatePreviousDecisions() {
    return List.of(
        PreviousDecision.builder()
            .fileNumber("previous decision file number 1")
            .deviatingFileNumber("previous decision deviating file number 1")
            .court(
                Court.builder()
                    .type("previous decision court type 1")
                    .location("previous decision court location 1")
                    .build())
            .decisionDate(LocalDate.parse("2005-06-17"))
            .documentType(
                DocumentType.builder()
                    .jurisShortcut("pddt1")
                    .label("previous decision document type 1")
                    .build())
            .documentNumber("previous decision document number 1")
            .build(),
        PreviousDecision.builder()
            .fileNumber("previous decision file number 2")
            .court(
                Court.builder()
                    .type("previous decision court type 2")
                    .location("previous decision court location 2")
                    .build())
            .decisionDate(LocalDate.parse("2013-08-12"))
            .documentType(
                DocumentType.builder()
                    .jurisShortcut("pddt2")
                    .label("previous decision document type 2")
                    .build())
            .build());
  }

  private CoreData generateCoreData() {
    return CoreData.builder()
        .court(Court.builder().type("court type").location("court location").build())
        .leadingDecisionNormReferences(
            List.of("leading decision norm reference 1", "leading decision norm reference 2"))
        .documentationOffice(
            DocumentationOffice.builder().abbreviation("documentation office").build())
        .inputTypes(List.of("input type 1", "input type 2"))
        .deviatingDecisionDates(
            List.of(LocalDate.parse("2011-01-09"), LocalDate.parse("2011-01-07")))
        .deviatingEclis(List.of("deviating ecli 1", "deviating ecli 2"))
        .previousProcedures(List.of("procedure 1", "procedure 2"))
        .deviatingCourts(List.of("deviating court 1", "deviating court 2"))
        .deviatingFileNumbers(List.of("deviating file number 1", "deviating file number 2"))
        .fileNumbers(List.of("file number 1", "file number 2"))
        .region("region")
        .legalEffect("ja")
        .documentType(DocumentType.builder().jurisShortcut("dt").label("document type").build())
        .appraisalBody("appraisal body")
        .ecli("ecli")
        .decisionDate(LocalDate.parse("2011-01-08"))
        .procedure(Procedure.builder().label("procedure 3").build())
        .build();
  }
}
