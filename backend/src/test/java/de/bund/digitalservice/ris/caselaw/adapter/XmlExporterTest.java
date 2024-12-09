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
import de.bund.digitalservice.ris.caselaw.domain.LongTexts;
import de.bund.digitalservice.ris.caselaw.domain.NormReference;
import de.bund.digitalservice.ris.caselaw.domain.PreviousDecision;
import de.bund.digitalservice.ris.caselaw.domain.Procedure;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.ShortTexts;
import de.bund.digitalservice.ris.caselaw.domain.SingleNorm;
import de.bund.digitalservice.ris.caselaw.domain.Status;
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
import javax.xml.transform.TransformerFactory;
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
  @Autowired private TransformerFactory transformerFactory;

  @Test
  void testExporter() throws XmlExporterException {
    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .coreData(generateCoreData())
            .documentNumber("document number")
            .previousDecisions(generatePreviousDecisions())
            .ensuingDecisions(generateEnsuingDecisions())
            .shortTexts(generateShortTexts())
            .longTexts(generateLongTexts())
            .status(generateStatus())
            .contentRelatedIndexing(generateContentRelatedIndexing())
            .build();

    String encryptedXml =
        new JurisXmlExporterWrapper(objectMapper, transformerFactory)
            .generateEncryptedXMLString(documentationUnit);

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
        .dismissalTypes(List.of("type 1", "type 2"))
        .dismissalGrounds(List.of("ground 1", "ground 2"))
        .collectiveAgreements(List.of("agreement 1", "agreement 2"))
        .hasLegislativeMandate(true)
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

  private ShortTexts generateShortTexts() {
    return ShortTexts.builder()
        .decisionName("decision name")
        .headline("headline")
        .guidingPrinciple("guiding principle")
        .headnote("headnote")
        .otherHeadnote("other headnote")
        .build();
  }

  private LongTexts generateLongTexts() {
    return LongTexts.builder()
        .tenor("tenor")
        .reasons("reasons")
        .caseFacts("case facts")
        .decisionReasons("decision reasons")
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
