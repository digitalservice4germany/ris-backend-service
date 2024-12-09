package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDSDocOffice;
import static de.bund.digitalservice.ris.caselaw.AuthUtils.mockUserGroups;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import de.bund.digitalservice.ris.caselaw.adapter.MockXmlExporter;
import de.bund.digitalservice.ris.caselaw.adapter.OAuthService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDeltaMigrationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresLegalPeriodicalEditionRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresLegalPeriodicalRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitDocxMetadataInitializationService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.HttpMailSender;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.ScheduledPublicationService;
import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@RISIntegrationTest(
    imports = {
      DocumentationUnitService.class,
      HandoverService.class,
      PostgresDeltaMigrationRepositoryImpl.class,
      KeycloakUserService.class,
      ScheduledPublicationService.class,
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
    controllers = {DocumentationUnitController.class})
@TestPropertySource(properties = {"mail.exporter.recipientAddress=neuris@example.com"})
class ScheduledPublicationIntegrationTest {
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

  @Autowired private RisWebTestClient risWebTestClient;
  @Autowired private DatabaseDocumentationUnitRepository docUnitRepository;
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;

  @MockBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private HttpMailSender mailSender;
  @MockBean private DocxConverterService docxConverterService;
  @MockBean private AttachmentService attachmentService;
  @MockBean private PatchMapperService patchMapperService;
  @MockBean private ProcedureService procedureService;
  @MockBean private LdmlExporterService ldmlExporterService;
  @MockBean private UserGroupService userGroupService;

  @MockBean
  private DocumentationUnitDocxMetadataInitializationService
      documentationUnitDocxMetadataInitializationService;

  private final DocumentationOffice docOffice = buildDSDocOffice();
  private DocumentationOfficeDTO documentationOffice;

  @BeforeEach
  void setUp() {
    documentationOffice =
        documentationOfficeRepository.findByAbbreviation(docOffice.abbreviation());

    mockUserGroups(userGroupService);
  }

  @Test
  void shouldPublishOnlyDueDocUnitsAndSendErrorNotificationOnSchedule() {
    // Valid doc unit -> publication will succeed
    DocumentationUnitDTO docUnitDueNow =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            docUnitRepository,
            DocumentationUnitDTO.builder()
                .documentationOffice(documentationOffice)
                .documentNumber("docnr123456_1")
                .scheduledByEmail("test@example.local")
                .scheduledPublicationDateTime(LocalDateTime.now())
                .decisionDate(LocalDate.now()));

    // Doc unit is not yet due -> will not be touched
    DocumentationUnitDTO docUnitScheduledForFuture =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            docUnitRepository,
            DocumentationUnitDTO.builder()
                .documentationOffice(documentationOffice)
                .documentNumber("docnr123456_2")
                .scheduledByEmail("test@example.local")
                .scheduledPublicationDateTime(LocalDateTime.now().plusMinutes(3))
                .decisionDate(LocalDate.now()));

    // Invalid doc unit will be unscheduled + send error notification
    DocumentationUnitDTO docUnitWithFailingXmlExport =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            docUnitRepository,
            DocumentationUnitDTO.builder()
                .documentationOffice(documentationOffice)
                .documentNumber("docnr123456_3")
                .scheduledByEmail("invalid-docunit@example.local")
                .scheduledPublicationDateTime(LocalDateTime.now().minusMinutes(3))
                // Missing decision date let's MockXmlExporter fail
                .decisionDate(null));
    // The assertion might take longer -> record now beforehand.
    LocalDateTime now = LocalDateTime.now();

    await()
        .atMost(Duration.ofSeconds(62))
        .untilAsserted(
            () -> {
              var publishedDocUnit = docUnitRepository.findById(docUnitDueNow.getId()).get();
              assertThat(publishedDocUnit.getScheduledByEmail()).isNull();
              assertThat(publishedDocUnit.getScheduledPublicationDateTime()).isNull();
              assertThat(publishedDocUnit.getLastPublicationDateTime())
                  .isCloseTo(now, byLessThan(60, ChronoUnit.SECONDS));

              var failedDocUnit =
                  docUnitRepository.findById(docUnitWithFailingXmlExport.getId()).get();
              assertThat(failedDocUnit.getScheduledByEmail()).isNull();
              assertThat(failedDocUnit.getScheduledPublicationDateTime()).isNull();
              assertThat(failedDocUnit.getLastPublicationDateTime())
                  .isCloseTo(now, byLessThan(60, ChronoUnit.SECONDS));

              var scheduledDocUnit =
                  docUnitRepository.findById(docUnitScheduledForFuture.getId()).get();
              assertThat(scheduledDocUnit.getScheduledByEmail())
                  .isEqualTo(docUnitScheduledForFuture.getScheduledByEmail());
              assertThat(scheduledDocUnit.getScheduledPublicationDateTime())
                  .isCloseTo(
                      docUnitScheduledForFuture.getScheduledPublicationDateTime(),
                      byLessThan(1, ChronoUnit.SECONDS));
              assertThat(scheduledDocUnit.getLastPublicationDateTime()).isNull();
            });

    assertThat(docUnitRepository.findAll()).hasSize(3);

    var error = "Terminierte Abgabe fehlgeschlagen: ";
    var uuid = docUnitDueNow.getId();
    // One handover mail to jDV is sent out.
    verify(mailSender, times(1))
        .sendMail(any(), any(), argThat(s -> !s.startsWith(error)), any(), any(), eq(uuid + ""));

    var subject = error + docUnitWithFailingXmlExport.getDocumentNumber();
    // One error notification mail to the user is sent out.
    verify(mailSender, times(1))
        .sendMail(any(), eq("invalid-docunit@example.local"), eq(subject), any(), any(), any());
  }
}
