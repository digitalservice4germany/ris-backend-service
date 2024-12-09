package de.bund.digitalservice.ris.caselaw.adapter;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bund.digitalservice.ris.caselaw.adapter.caselawldml.CaseLawLdml;
import de.bund.digitalservice.ris.caselaw.adapter.exception.BucketException;
import de.bund.digitalservice.ris.caselaw.adapter.exception.LdmlTransformationException;
import de.bund.digitalservice.ris.caselaw.adapter.exception.PublishException;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationUnitToLdmlTransformer;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.domain.LongTexts;
import de.bund.digitalservice.ris.caselaw.domain.PreviousDecision;
import de.bund.digitalservice.ris.caselaw.domain.ShortTexts;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.saxon.TransformerFactoryImpl;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseLawLdmlExportTest {

  static DocumentationUnitRepository documentationUnitRepository;
  static LdmlBucket caseLawBucket;
  static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
  static XmlUtilService xmlUtilService = new XmlUtilService(new TransformerFactoryImpl());
  static LdmlExporterService exporter;
  static DocumentationUnit testDocumentUnit;
  static UUID testUUID;
  static ObjectMapper objectMapper;

  @BeforeAll
  static void setUpBeforeClass() {
    documentationUnitRepository = mock(DocumentationUnitRepository.class);
    caseLawBucket = mock(LdmlBucket.class);
    objectMapper = mock(ObjectMapper.class);
    exporter =
        new LdmlExporterService(
            documentationUnitRepository,
            xmlUtilService,
            documentBuilderFactory,
            caseLawBucket,
            objectMapper);

    PreviousDecision related1 =
        PreviousDecision.builder()
            .decisionDate(LocalDate.of(2020, 1, 1))
            .court(Court.builder().type("Test court type").build())
            .documentType(DocumentType.builder().label("Test decision type").build())
            .fileNumber("Test file number")
            .documentNumber("Test document number 1")
            .build();
    PreviousDecision related2 =
        related1.toBuilder().documentNumber("Test document number 2").build();

    testUUID = UUID.randomUUID();
    testDocumentUnit =
        DocumentationUnit.builder()
            .uuid(testUUID)
            .coreData(
                CoreData.builder()
                    .ecli("testecli")
                    .court(
                        Court.builder().type("testCourtType").location("testCourtLocation").build())
                    .documentType(
                        DocumentType.builder().label("testDocumentTypeAbbreviation").build())
                    .legalEffect("ja")
                    .fileNumbers(List.of("testFileNumber"))
                    .decisionDate(LocalDate.of(2020, 1, 1))
                    .build())
            .documentNumber("testDocumentNumber")
            .longTexts(LongTexts.builder().caseFacts("<p>Example content 1</p>").build())
            .shortTexts(ShortTexts.builder().build())
            .previousDecisions(List.of(related1, related2))
            .build();
  }

  @BeforeEach
  void mockReset() throws JsonProcessingException {
    Mockito.reset(caseLawBucket);
    Mockito.reset(objectMapper);
    when(objectMapper.writeValueAsString(any())).thenReturn("");
  }

  @Test
  @DisplayName("Should call caselaw bucket save once")
  void exportOneCaseLaw() throws DocumentationUnitNotExistsException {
    when(documentationUnitRepository.getRandomDocumentationUnitIds())
        .thenReturn(List.of(UUID.randomUUID()));
    when(documentationUnitRepository.findByUuid(any())).thenReturn(testDocumentUnit);

    exporter.exportMultipleRandomDocumentationUnits();
    verify(caseLawBucket, times(2)).save(anyString(), anyString());
  }

  @Test
  @DisplayName("Invalid Case Law Ldml should fail validation 1")
  void xsdValidationFailure1() throws DocumentationUnitNotExistsException {
    DocumentationUnit invalidTestDocumentUnit =
        testDocumentUnit.toBuilder()
            .longTexts(
                testDocumentUnit.longTexts().toBuilder()
                    .caseFacts("<p>Example <p>nested</p> content 1</p>")
                    .build())
            .build();
    when(documentationUnitRepository.getRandomDocumentationUnitIds())
        .thenReturn(List.of(UUID.randomUUID()));
    when(documentationUnitRepository.findByUuid(any())).thenReturn(invalidTestDocumentUnit);

    exporter.exportMultipleRandomDocumentationUnits();
    verify(caseLawBucket, times(0)).save(anyString(), anyString());
  }

  @Test
  @DisplayName("Invalid Case Law Ldml should fail validation 2")
  void xsdValidationFailure2() throws DocumentationUnitNotExistsException {
    DocumentationUnit invalidTestDocumentUnit =
        testDocumentUnit.toBuilder().longTexts(null).build();
    when(documentationUnitRepository.getRandomDocumentationUnitIds())
        .thenReturn(List.of(UUID.randomUUID()));
    when(documentationUnitRepository.findByUuid(any())).thenReturn(invalidTestDocumentUnit);

    exporter.exportMultipleRandomDocumentationUnits();
    verify(caseLawBucket, times(0)).save(anyString(), anyString());
  }

  @Test
  @DisplayName("Should publish single documentation unit succesfully")
  void publishSuccessfully() throws DocumentationUnitNotExistsException {
    UUID documentationUnitId = UUID.randomUUID();
    when(documentationUnitRepository.findByUuid(documentationUnitId)).thenReturn(testDocumentUnit);

    exporter.publishDocumentationUnit(documentationUnitId);

    verify(caseLawBucket, times(2)).save(anyString(), anyString());
  }

  @Test
  @DisplayName("Publish fails with transformation exception")
  void failLdmlTransformation() throws DocumentationUnitNotExistsException {
    DocumentationUnit invalidTestDocumentUnit =
        testDocumentUnit.toBuilder().coreData(CoreData.builder().build()).build();
    UUID documentationUnitId = UUID.randomUUID();
    when(documentationUnitRepository.findByUuid(documentationUnitId))
        .thenReturn(invalidTestDocumentUnit);

    assertThatExceptionOfType(LdmlTransformationException.class)
        .isThrownBy(() -> exporter.publishDocumentationUnit(documentationUnitId))
        .withMessageContaining("Could not transform documentation unit to LDML.");
    verify(caseLawBucket, times(0)).save(anyString(), anyString());
  }

  @Test
  @DisplayName("Publish fails with missing judgement body")
  void failMissingJudgementBody() throws DocumentationUnitNotExistsException {
    DocumentationUnit invalidTestDocumentUnit =
        testDocumentUnit.toBuilder().longTexts(null).build();
    UUID documentationUnitId = UUID.randomUUID();
    when(documentationUnitRepository.findByUuid(documentationUnitId))
        .thenReturn(invalidTestDocumentUnit);

    assertThatExceptionOfType(LdmlTransformationException.class)
        .isThrownBy(() -> exporter.publishDocumentationUnit(documentationUnitId))
        .withMessageContaining("Could not transform documentation unit to valid LDML.");
    verify(caseLawBucket, times(0)).save(anyString(), anyString());
  }

  @Test
  @DisplayName("Should fail when changelog file cannot be created")
  void failChangelogFileFailure()
      throws DocumentationUnitNotExistsException, JsonProcessingException {
    UUID documentationUnitId = UUID.randomUUID();
    when(documentationUnitRepository.findByUuid(documentationUnitId)).thenReturn(testDocumentUnit);
    when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

    assertThatExceptionOfType(PublishException.class)
        .isThrownBy(() -> exporter.publishDocumentationUnit(documentationUnitId))
        .withMessageContaining(
            "Could not publish documentation unit to portal, because changelog file could not be created.");
    verify(caseLawBucket, times(0)).save(anyString(), anyString());
  }

  @Test
  @DisplayName("Should fail when changelog file cannot be saved to bucket")
  void failWithBucketException() throws DocumentationUnitNotExistsException {
    UUID documentationUnitId = UUID.randomUUID();
    when(documentationUnitRepository.findByUuid(documentationUnitId)).thenReturn(testDocumentUnit);

    doThrow(BucketException.class).when(caseLawBucket).save(contains("changelogs/"), anyString());

    assertThatExceptionOfType(PublishException.class)
        .isThrownBy(() -> exporter.publishDocumentationUnit(documentationUnitId))
        .withMessageContaining("Could not save changelog to bucket");
  }

  @Test
  @DisplayName("Should fail when ldml file cannot be saved to bucket")
  void failWithBucketException2() throws DocumentationUnitNotExistsException {
    UUID documentationUnitId = UUID.randomUUID();
    when(documentationUnitRepository.findByUuid(documentationUnitId)).thenReturn(testDocumentUnit);
    doThrow(BucketException.class).when(caseLawBucket).save(contains(".xml"), anyString());

    assertThatExceptionOfType(PublishException.class)
        .isThrownBy(() -> exporter.publishDocumentationUnit(documentationUnitId))
        .withMessageContaining("Could not save LDML to bucket");
  }

  @Test
  @DisplayName("Fallback title test")
  void documentNumberIsFallbackTitleTest() {
    String expected =
        """
      <akn:header>
         <akn:p>testDocumentNumber</akn:p>
      </akn:header>
     """;
    Optional<CaseLawLdml> ldml =
        DocumentationUnitToLdmlTransformer.transformToLdml(
            testDocumentUnit, documentBuilderFactory);
    Assertions.assertTrue(ldml.isPresent());
    Optional<String> fileContent = exporter.ldmlToString(ldml.get());
    Assertions.assertTrue(fileContent.isPresent());
    Assertions.assertTrue(
        StringUtils.deleteWhitespace(fileContent.get())
            .contains(StringUtils.deleteWhitespace(expected)));
  }

  @Test
  @DisplayName("Dissenting Opinion test")
  void dissentingOpinionTest() {
    String expected =
        """
           <akn:block name="Abweichende Meinung">
              <akn:opinion>
                 <akn:embeddedStructure>
                    <akn:p>dissenting test</akn:p>
                 </akn:embeddedStructure>
              </akn:opinion>
           </akn:block>
           """;
    DocumentationUnit dissentingCaseLaw =
        testDocumentUnit.toBuilder()
            .longTexts(
                testDocumentUnit.longTexts().toBuilder()
                    .dissentingOpinion("<p>dissenting test</p>")
                    .build())
            .build();
    Optional<CaseLawLdml> ldml =
        DocumentationUnitToLdmlTransformer.transformToLdml(
            dissentingCaseLaw, documentBuilderFactory);
    Assertions.assertTrue(ldml.isPresent());
    Optional<String> fileContent = exporter.ldmlToString(ldml.get());
    Assertions.assertTrue(fileContent.isPresent());
    Assertions.assertTrue(
        StringUtils.deleteWhitespace(fileContent.get())
            .contains(StringUtils.deleteWhitespace(expected)));
  }

  @Test
  @DisplayName("Headnote test")
  void headnoteTest() {
    String expected =
        """
            <akn:block name="Orientierungssatz">
               <akn:embeddedStructure>
                  <akn:p>headnote test</akn:p>
               </akn:embeddedStructure>
            </akn:block>
           """;
    DocumentationUnit headnoteCaseLaw =
        testDocumentUnit.toBuilder()
            .shortTexts(
                testDocumentUnit.shortTexts().toBuilder().headnote("<p>headnote test</p>").build())
            .build();
    Optional<CaseLawLdml> ldml =
        DocumentationUnitToLdmlTransformer.transformToLdml(headnoteCaseLaw, documentBuilderFactory);
    Assertions.assertTrue(ldml.isPresent());
    Optional<String> fileContent = exporter.ldmlToString(ldml.get());
    Assertions.assertTrue(fileContent.isPresent());
    Assertions.assertTrue(
        StringUtils.deleteWhitespace(fileContent.get())
            .contains(StringUtils.deleteWhitespace(expected)));
  }

  @Test
  @DisplayName("OtherHeadnote test")
  void otherHeadnoteTest() {
    String expected =
        """
            <akn:block name="Sonstiger Orientierungssatz">
               <akn:embeddedStructure>
                  <akn:p>other headnote test</akn:p>
               </akn:embeddedStructure>
            </akn:block>
           """;
    DocumentationUnit otherHeadnoteCaseLaw =
        testDocumentUnit.toBuilder()
            .shortTexts(
                testDocumentUnit.shortTexts().toBuilder()
                    .otherHeadnote("<p>other headnote test</p>")
                    .build())
            .build();
    Optional<CaseLawLdml> ldml =
        DocumentationUnitToLdmlTransformer.transformToLdml(
            otherHeadnoteCaseLaw, documentBuilderFactory);
    Assertions.assertTrue(ldml.isPresent());
    Optional<String> fileContent = exporter.ldmlToString(ldml.get());
    Assertions.assertTrue(fileContent.isPresent());
    Assertions.assertTrue(
        StringUtils.deleteWhitespace(fileContent.get())
            .contains(StringUtils.deleteWhitespace(expected)));
  }

  @Test
  @DisplayName("Grounds test")
  void groundTest() {
    String expected =
        """
            <akn:block name="Gründe">
               <akn:embeddedStructure>
                  <akn:p>grounds test</akn:p>
               </akn:embeddedStructure>
            </akn:block>
           """;
    DocumentationUnit groundsCaseLaw =
        testDocumentUnit.toBuilder()
            .longTexts(
                testDocumentUnit.longTexts().toBuilder().reasons("<p>grounds test</p>").build())
            .build();
    Optional<CaseLawLdml> ldml =
        DocumentationUnitToLdmlTransformer.transformToLdml(groundsCaseLaw, documentBuilderFactory);
    Assertions.assertTrue(ldml.isPresent());
    Optional<String> fileContent = exporter.ldmlToString(ldml.get());
    Assertions.assertTrue(fileContent.isPresent());
    Assertions.assertTrue(
        StringUtils.deleteWhitespace(fileContent.get())
            .contains(StringUtils.deleteWhitespace(expected)));
  }

  @Test
  @DisplayName("OtherLongText without main decision test")
  void otherLongTextWithoutMainDecisionTest() {
    String expected =
        """
         <akn:decision>
            <akn:block name="Sonstiger Langtext">
               <akn:embeddedStructure>
                  <akn:p>Other long text test</akn:p>
               </akn:embeddedStructure>
            </akn:block>
         </akn:decision>
         """;
    DocumentationUnit otherLongTextCaseLaw =
        testDocumentUnit.toBuilder()
            .longTexts(
                testDocumentUnit.longTexts().toBuilder()
                    .otherLongText("<p>Other long text test</p>")
                    .build())
            .build();
    Optional<CaseLawLdml> ldml =
        DocumentationUnitToLdmlTransformer.transformToLdml(
            otherLongTextCaseLaw, documentBuilderFactory);
    Assertions.assertTrue(ldml.isPresent());
    Optional<String> fileContent = exporter.ldmlToString(ldml.get());
    Assertions.assertTrue(fileContent.isPresent());
    Assertions.assertTrue(
        StringUtils.deleteWhitespace(fileContent.get())
            .contains(StringUtils.deleteWhitespace(expected)));
  }
}
