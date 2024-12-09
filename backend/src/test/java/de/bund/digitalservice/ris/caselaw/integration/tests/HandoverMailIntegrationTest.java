package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.mockUserGroups;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import com.fasterxml.jackson.core.type.TypeReference;
import de.bund.digitalservice.ris.caselaw.EntityBuilderTestUtil;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberGeneratorService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentationUnitStatusService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentNumberPatternConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentationUnitController;
import de.bund.digitalservice.ris.caselaw.adapter.DocxConverterService;
import de.bund.digitalservice.ris.caselaw.adapter.HandoverMailService;
import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.adapter.LdmlExporterService;
import de.bund.digitalservice.ris.caselaw.adapter.LegalPeriodicalEditionController;
import de.bund.digitalservice.ris.caselaw.adapter.MockXmlExporter;
import de.bund.digitalservice.ris.caselaw.adapter.OAuthService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseHandoverReportRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseLegalPeriodicalEditionRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseLegalPeriodicalRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseReferenceRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseXmlHandoverMailRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.HandoverMailAttachmentDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.HandoverMailDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.HandoverReportDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalPeriodicalDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalPeriodicalEditionDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDeltaMigrationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresLegalPeriodicalEditionRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresLegalPeriodicalRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.ReferenceDTO;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.HandoverMailTransformer;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitDocxMetadataInitializationService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.EventRecord;
import de.bund.digitalservice.ris.caselaw.domain.EventType;
import de.bund.digitalservice.ris.caselaw.domain.HandoverEntityType;
import de.bund.digitalservice.ris.caselaw.domain.HandoverMail;
import de.bund.digitalservice.ris.caselaw.domain.HandoverReport;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.HttpMailSender;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionRepository;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionService;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalRepository;
import de.bund.digitalservice.ris.caselaw.domain.MailAttachment;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@RISIntegrationTest(
    imports = {
      DocumentationUnitService.class,
      HandoverService.class,
      PostgresDeltaMigrationRepositoryImpl.class,
      KeycloakUserService.class,
      DatabaseDocumentNumberGeneratorService.class,
      DatabaseDocumentNumberRecyclingService.class,
      PostgresDocumentationUnitRepositoryImpl.class,
      PostgresLegalPeriodicalEditionRepositoryImpl.class,
      PostgresLegalPeriodicalRepositoryImpl.class,
      PostgresHandoverRepositoryImpl.class,
      PostgresHandoverReportRepositoryImpl.class,
      HandoverMailService.class,
      DatabaseDocumentationUnitStatusService.class,
      LegalPeriodicalEditionService.class,
      MockXmlExporter.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      SecurityConfig.class,
      OAuthService.class,
      TestConfig.class,
      DocumentNumberPatternConfig.class
    },
    controllers = {DocumentationUnitController.class, LegalPeriodicalEditionController.class})
@TestPropertySource(
    properties = {
      "mail.exporter.jurisUsername=test-user",
      "mail.exporter.recipientAddress=neuris@example.com"
    })
@Sql(scripts = {"classpath:legal_periodical_init.sql"})
@Sql(
    scripts = {"classpath:legal_periodical_cleanup.sql"},
    executionPhase = AFTER_TEST_METHOD)
class HandoverMailIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>("postgres:14").withInitScript("init_db.sql");

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("database.user", () -> postgreSQLContainer.getUsername());
    registry.add("database.password", () -> postgreSQLContainer.getPassword());
    registry.add("database.host", () -> postgreSQLContainer.getHost());
    registry.add("database.port", () -> postgreSQLContainer.getFirstMappedPort());
    registry.add("database.database", () -> postgreSQLContainer.getDatabaseName());
  }

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final String DELIVER_DATE =
      LocalDate.now(Clock.system(ZoneId.of("Europe/Berlin"))).format(DATE_FORMATTER);

  @Autowired private RisWebTestClient risWebTestClient;
  @Autowired private DatabaseDocumentationUnitRepository repository;
  @Autowired private DatabaseLegalPeriodicalEditionRepository editionRepository;
  @Autowired private DatabaseReferenceRepository referenceRepository;
  @Autowired private LegalPeriodicalRepository legalPeriodicalRepository;
  @Autowired private DatabaseLegalPeriodicalRepository dblegalPeriodicalRepository;
  @Autowired private DatabaseXmlHandoverMailRepository xmlHandoverRepository;
  @Autowired private DatabaseHandoverReportRepository databaseHandoverReportRepository;
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  @Autowired private LegalPeriodicalEditionRepository legalPeriodicalEditionRepository;

  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private HttpMailSender mailSender;
  @MockBean DocxConverterService docxConverterService;
  @MockBean AttachmentService attachmentService;
  @MockBean private PatchMapperService patchMapperService;
  @MockBean private ProcedureService procedureService;
  @MockBean private LdmlExporterService ldmlExporterService;
  @MockBean private UserGroupService userGroupService;

  @MockBean
  private DocumentationUnitDocxMetadataInitializationService
      documentationUnitDocxMetadataInitializationService;

  private DocumentationOfficeDTO docOffice;

  @BeforeEach
  void setUp() {
    docOffice = documentationOfficeRepository.findByAbbreviation("DS");
    mockUserGroups(userGroupService);
  }

  @AfterEach
  void cleanUp() {
    xmlHandoverRepository.deleteAll();
    repository.deleteAll();
    databaseHandoverReportRepository.deleteAll();
    editionRepository.deleteAll();
    dblegalPeriodicalRepository.deleteAll();
  }

  public static List<HandoverEntityType> getHandoverMailTestData() {
    return List.of(HandoverEntityType.DOCUMENTATION_UNIT, HandoverEntityType.EDITION);
  }

  @ParameterizedTest
  @MethodSource("getHandoverMailTestData")
  void testHandover(HandoverEntityType entityType) {
    String identifier = "docnr12345678";

    DocumentationUnitDTO savedDocumentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository,
            DocumentationUnitDTO.builder()
                .documentationOffice(docOffice)
                .documentNumber(identifier)
                .decisionDate(LocalDate.now()));
    UUID entityId = savedDocumentationUnitDTO.getId();

    assertThat(repository.findAll()).hasSize(1);

    if (entityType == HandoverEntityType.EDITION) {
      LegalPeriodicalEditionDTO legalPeriodicalEditionDTO =
          LegalPeriodicalEditionDTO.builder()
              .id(entityId)
              .legalPeriodical(
                  LegalPeriodicalDTO.builder()
                      .id(UUID.fromString("1abf62fe-9ddf-487e-962e-1c71cf661c5b"))
                      .abbreviation("ABC")
                      .build())
              .build();
      legalPeriodicalEditionDTO.setReferences(
          List.of(
              referenceRepository
                  .save(
                      ReferenceDTO.builder()
                          .id(UUID.randomUUID())
                          .citation("citation")
                          .legalPeriodicalRawValue("ABC")
                          .rank(1)
                          .documentationUnit(savedDocumentationUnitDTO)
                          .build())
                  .toBuilder()
                  .editionRank(1)
                  .build()));
      editionRepository.save(legalPeriodicalEditionDTO);
      assertThat(editionRepository.findAll()).hasSize(1);
      identifier = "edition-" + entityId;
    }

    String mailSubject =
        "id=juris name=test-user da=R df=X dt=%s mod=T ld=%s vg=%s"
            .formatted(
                entityType.equals(HandoverEntityType.DOCUMENTATION_UNIT) ? "N" : "F",
                DELIVER_DATE,
                identifier);

    var handoverId = UUID.randomUUID();
    HandoverMailDTO expectedHandoverMailDTO =
        HandoverMailDTO.builder()
            .id(handoverId)
            .entityId(entityId)
            .receiverAddress("neuris@example.com")
            .mailSubject(mailSubject)
            .attachments(
                entityType.equals(HandoverEntityType.DOCUMENTATION_UNIT)
                    ? List.of(
                        HandoverMailAttachmentDTO.builder().fileName("test.xml").xml("xml").build())
                    : List.of(
                        HandoverMailAttachmentDTO.builder()
                            .fileName("docnr12345678.xml")
                            .xml("citation: citation docunit: docnr12345678")
                            .build()))
            .statusCode("200")
            .statusMessages("message 1|message 2")
            .issuerAddress("test@test.com")
            .build();

    HandoverMail expectedHandoverMail =
        HandoverMail.builder()
            .entityId(entityId)
            .entityType(entityType)
            .receiverAddress("neuris@example.com")
            .mailSubject(mailSubject)
            .attachments(
                entityType.equals(HandoverEntityType.DOCUMENTATION_UNIT)
                    ? List.of(
                        MailAttachment.builder().fileName("test.xml").fileContent("xml").build())
                    : List.of(
                        MailAttachment.builder()
                            .fileName("docnr12345678.xml")
                            .fileContent("citation: citation docunit: docnr12345678")
                            .build()))
            .success(true)
            .statusMessages(List.of("message 1", "message 2"))
            .issuerAddress("test@test.com") // set by AuthUtils
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri(
            entityType.equals(HandoverEntityType.DOCUMENTATION_UNIT)
                ? "/api/v1/caselaw/documentunits/" + entityId + "/handover"
                : "/api/v1/caselaw/legalperiodicaledition/" + entityId + "/handover")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(HandoverMail.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody())
                    .usingRecursiveComparison()
                    .ignoringFields("handoverDate")
                    .isEqualTo(expectedHandoverMail));

    List<HandoverMailDTO> xmlPublicationList = xmlHandoverRepository.findAll();
    assertThat(xmlPublicationList).hasSize(1);
    HandoverMailDTO handoverMailDTO = xmlPublicationList.get(0);
    assertThat(handoverMailDTO)
        .usingRecursiveComparison()
        .ignoringFields("sentDate", "id", "attachments")
        .isEqualTo(expectedHandoverMailDTO);
  }

  @ParameterizedTest
  @MethodSource("getHandoverMailTestData")
  void testHandoverWithNotAllMandatoryFieldsFilled_shouldNotSucceed(HandoverEntityType entityType) {
    UUID entityId = UUID.randomUUID();

    if (entityType.equals(HandoverEntityType.DOCUMENTATION_UNIT)) {
      DocumentationUnitDTO documentationUnitDTO =
          EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
              repository, docOffice, "docnr12345678");

      assertThat(repository.findAll()).hasSize(1);
      entityId = documentationUnitDTO.getId();

    } else if (entityType == HandoverEntityType.EDITION) {
      LegalPeriodicalEditionDTO legalPeriodicalEditionDTO =
          LegalPeriodicalEditionDTO.builder()
              .id(entityId)
              .editionReferences(List.of())
              .legalPeriodical(
                  LegalPeriodicalDTO.builder()
                      .id(UUID.fromString("1abf62fe-9ddf-487e-962e-1c71cf661c5b"))
                      .abbreviation("ABC")
                      .build())
              .build();
      editionRepository.save(legalPeriodicalEditionDTO);
      assertThat(editionRepository.findAll()).hasSize(1);
    }

    HandoverMail xmlPublication =
        HandoverMail.builder()
            .entityId(entityId)
            .success(false)
            .statusMessages(
                entityType.equals(HandoverEntityType.DOCUMENTATION_UNIT)
                    ? List.of("message 1", "message 2")
                    : List.of())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri(
            entityType.equals(HandoverEntityType.DOCUMENTATION_UNIT)
                ? "/api/v1/caselaw/documentunits/" + entityId + "/handover"
                : "/api/v1/caselaw/legalperiodicaledition/" + entityId + "/handover")
        .exchange()
        .expectStatus()
        .is4xxClientError()
        .expectBody(HandoverMail.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody())
                    .usingRecursiveComparison()
                    .ignoringFields("handoverDate")
                    .isEqualTo(xmlPublication));

    List<HandoverMailDTO> xmlPublicationList = xmlHandoverRepository.findAll();
    assertThat(xmlPublicationList).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("getHandoverMailTestData")
  void testGetLastXmlHandoverMail(HandoverEntityType entityType) {
    UUID entityId = UUID.randomUUID();

    if (entityType == HandoverEntityType.DOCUMENTATION_UNIT) {
      entityId =
          repository
              .save(
                  DocumentationUnitDTO.builder()
                      .documentationOffice(docOffice)
                      .documentNumber("docnr12345678")
                      .build())
              .getId();
    } else if (entityType == HandoverEntityType.EDITION) {
      editionRepository.save(
          LegalPeriodicalEditionDTO.builder()
              .id(entityId)
              .legalPeriodical(
                  LegalPeriodicalDTO.builder()
                      .id(UUID.fromString("1abf62fe-9ddf-487e-962e-1c71cf661c5b"))
                      .build())
              .build());
    }

    HandoverMail handoverMailDTO =
        HandoverMail.builder()
            .entityId(entityId)
            .receiverAddress("exporter@neuris.de")
            .mailSubject("mailSubject")
            .attachments(
                List.of(MailAttachment.builder().fileName("test.xml").fileContent("xml").build()))
            .success(true)
            .statusMessages(List.of("message 1", "message 2"))
            .handoverDate(Instant.now())
            .build();
    xmlHandoverRepository.save(HandoverMailTransformer.transformToDTO(handoverMailDTO));

    HandoverMail expectedXmlPublication =
        HandoverMail.builder()
            .entityId(entityId)
            .entityType(entityType)
            .receiverAddress("exporter@neuris.de")
            .mailSubject("mailSubject")
            .attachments(
                List.of(MailAttachment.builder().fileName("test.xml").fileContent("xml").build()))
            .success(true)
            .statusMessages(List.of("message 1", "message 2"))
            .build();

    String url =
        entityType == HandoverEntityType.DOCUMENTATION_UNIT
            ? "/api/v1/caselaw/documentunits/" + entityId + "/handover"
            : "/api/v1/caselaw/legalperiodicaledition/" + entityId + "/handover";

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri(url)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(HandoverMail[].class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody())
                    .usingRecursiveComparison()
                    .ignoringFields("handoverDate", "attachments")
                    .isEqualTo(expectedXmlPublication));
  }

  @ParameterizedTest
  @MethodSource("getHandoverMailTestData")
  void testGetEventLog(HandoverEntityType entityType) {
    UUID entityId = UUID.randomUUID();
    if (entityType == HandoverEntityType.DOCUMENTATION_UNIT) {
      entityId =
          EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
                  repository,
                  DocumentationUnitDTO.builder()
                      .documentationOffice(docOffice)
                      .documentNumber("docnr12345678"))
              .getId();

    } else if (entityType == HandoverEntityType.EDITION) {
      editionRepository.save(
          LegalPeriodicalEditionDTO.builder()
              .id(entityId)
              .legalPeriodical(
                  LegalPeriodicalDTO.builder()
                      .id(UUID.fromString("1abf62fe-9ddf-487e-962e-1c71cf661c5b"))
                      .build())
              .build());
    }

    Instant creationDate = Instant.now();
    xmlHandoverRepository.save(
        HandoverMailTransformer.transformToDTO(
            HandoverMail.builder()
                .entityId(entityId)
                .receiverAddress("exporter@neuris.de")
                .mailSubject("mailSubject")
                .attachments(
                    List.of(
                        MailAttachment.builder().fileName("test.xml").fileContent("xml").build()))
                .success(true)
                .statusMessages(List.of("message 1", "message 2"))
                .handoverDate(creationDate)
                .build()));

    Instant receivedDate = creationDate.plus(1, ChronoUnit.HOURS);
    databaseHandoverReportRepository.save(
        HandoverReportDTO.builder()
            .entityId(entityId)
            .content("<HTML>success!</HTML>")
            .receivedDate(receivedDate)
            .build());

    List<? extends EventRecord> responseBody =
        risWebTestClient
            .withDefaultLogin()
            .get()
            .uri(
                entityType == HandoverEntityType.DOCUMENTATION_UNIT
                    ? "/api/v1/caselaw/documentunits/" + entityId + "/handover"
                    : "/api/v1/caselaw/legalperiodicaledition/" + entityId + "/handover")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new TypeReference<List<? extends EventRecord>>() {})
            .returnResult()
            .getResponseBody();
    assertThat(responseBody).hasSize(2);
    assertThat(responseBody.get(0)).isInstanceOf(HandoverReport.class);
    HandoverReport handoverReport = (HandoverReport) responseBody.get(0);
    assertThat(handoverReport.content()).isEqualTo("<HTML>success!</HTML>");
    assertThat(handoverReport.getDate()).isCloseTo(receivedDate, within(1, ChronoUnit.MILLIS));
    assertThat(handoverReport.getType()).isEqualTo(EventType.HANDOVER_REPORT);
    assertThat(responseBody.get(1)).isInstanceOf(HandoverMail.class);
    HandoverMail xmlPublication = (HandoverMail) responseBody.get(1);
    assertThat(xmlPublication.attachments().get(0).fileContent()).isEqualTo("xml");
    assertThat(xmlPublication.getDate()).isCloseTo(creationDate, within(1, ChronoUnit.MILLIS));
    assertThat(xmlPublication.getType()).isEqualTo(EventType.HANDOVER);
  }
}
