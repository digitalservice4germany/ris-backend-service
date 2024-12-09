package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.mockUserGroups;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.EntityBuilderTestUtil;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberGeneratorService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentationUnitStatusService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseProcedureService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentNumberPatternConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentationUnitController;
import de.bund.digitalservice.ris.caselaw.adapter.DocxConverterService;
import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.adapter.LdmlExporterService;
import de.bund.digitalservice.ris.caselaw.adapter.OAuthService;
import de.bund.digitalservice.ris.caselaw.adapter.S3AttachmentService;
import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocxConverter;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.AttachmentDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.AttachmentRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.CourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseCourtRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentCategoryRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentTypeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseFileNumberRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentCategoryDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalEffectDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresCourtRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDeltaMigrationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentTypeRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentTypeRepository;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitDocxMetadataInitializationService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.MailService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import de.bund.digitalservice.ris.caselaw.domain.court.CourtRepository;
import de.bund.digitalservice.ris.caselaw.domain.docx.Docx2Html;
import de.bund.digitalservice.ris.caselaw.domain.docx.DocxMetadataProperty;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@RISIntegrationTest(
    imports = {
      DocumentationUnitService.class,
      DocumentationUnitDocxMetadataInitializationService.class,
      PostgresDeltaMigrationRepositoryImpl.class,
      DatabaseDocumentNumberGeneratorService.class,
      DatabaseDocumentNumberRecyclingService.class,
      DatabaseDocumentationUnitStatusService.class,
      DatabaseProcedureService.class,
      PostgresHandoverReportRepositoryImpl.class,
      PostgresDocumentationUnitRepositoryImpl.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      SecurityConfig.class,
      OAuthService.class,
      TestConfig.class,
      DocumentNumberPatternConfig.class,
      S3AttachmentService.class,
      DocxConverterService.class,
      DocxConverter.class,
      PostgresCourtRepositoryImpl.class,
      PostgresDocumentTypeRepositoryImpl.class,
      KeycloakUserService.class,
    },
    controllers = {DocumentationUnitController.class})
@Sql(scripts = {"classpath:doc_office_init.sql"})
class DocumentationUnitControllerDocxFilesIntegrationTest {
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
  @Autowired private DatabaseDocumentationUnitRepository repository;
  @Autowired private DatabaseFileNumberRepository fileNumberRepository;
  @Autowired private DatabaseDocumentTypeRepository databaseDocumentTypeRepository;
  @Autowired private DatabaseDocumentCategoryRepository databaseDocumentCategoryRepository;
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  @Autowired private DatabaseCourtRepository databaseCourtRepository;
  @Autowired private AttachmentService attachmentService;
  @Autowired private AttachmentRepository attachmentRepository;
  @Autowired private DocxConverterService docxConverterService;
  @Autowired private CourtRepository courtRepository;
  @Autowired private DocumentTypeRepository documentTypeRepository;
  @SpyBean private DocumentationUnitDocxMetadataInitializationService service;
  @Autowired private DocumentationUnitService documentationUnitService;

  @MockBean
  @Qualifier("docxS3Client")
  private S3Client s3Client;

  @MockBean private MailService mailService;

  @MockBean private HandoverService handoverService;

  @MockBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private DocumentBuilderFactory documentBuilderFactory;
  @MockBean private PatchMapperService patchMapperService;
  @MockBean private ProcedureService procedureService;
  @MockBean private UserGroupService userGroupService;
  @MockBean private LdmlExporterService ldmlExporterService;

  private DocumentationOfficeDTO dsDocOffice = null;

  @BeforeEach
  void setUp() {
    dsDocOffice = documentationOfficeRepository.findByAbbreviation("DS");
    databaseDocumentCategoryRepository.save(DocumentCategoryDTO.builder().label("R").build());
    mockUserGroups(userGroupService);
  }

  @AfterEach
  void cleanUp() {
    repository.deleteAll();
    attachmentRepository.deleteAll();
    databaseCourtRepository.deleteAll();
    databaseDocumentCategoryRepository.deleteAll();
  }

  @Test
  void testAttachDocxToDocumentationUnit() throws IOException {
    var attachment = Files.readAllBytes(Paths.get("src/test/resources/fixtures/attachment.docx"));
    mockS3ClientToReturnFile(attachment);

    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, dsDocOffice, "1234567890123");

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId() + "/file")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .bodyAsByteArray(attachment)
        .exchange()
        .expectStatus()
        .isOk();

    var savedAttachment = attachmentRepository.findAllByDocumentationUnitId(dto.getId()).get(0);
    assertThat(savedAttachment.getUploadTimestamp()).isInstanceOf(Instant.class);
    assertThat(savedAttachment.getId()).isInstanceOf(UUID.class);
  }

  @Test
  void testAttachMultipleDocxToDocumentationUnitSequentially() throws IOException {
    var attachment = Files.readAllBytes(Paths.get("src/test/resources/fixtures/attachment.docx"));
    mockS3ClientToReturnFile(attachment);

    DocumentationUnitDTO documentationUnitDto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, dsDocOffice, "1234567890123");

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDto.getId() + "/file")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .bodyAsByteArray(attachment)
        .exchange()
        .expectStatus()
        .isOk();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDto.getId() + "/file")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .bodyAsByteArray(attachment)
        .exchange()
        .expectStatus()
        .isOk();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDto.getId() + "/file")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .bodyAsByteArray(attachment)
        .exchange()
        .expectStatus()
        .isOk();

    assertThat(attachmentRepository.findAllByDocumentationUnitId(documentationUnitDto.getId()))
        .hasSize(3);
  }

  @Test
  void testAttachFileToDocumentationUnit_withInvalidUuid() {
    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/abc/file")
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  void testAttachFileToDocumentationUnit_withECLIInFooter_shouldExtractECLIAndSetItInUnitIfNotSet()
      throws IOException {
    var attachmentWithEcli =
        Files.readAllBytes(Paths.get("src/test/resources/fixtures/attachment_ecli.docx"));
    mockS3ClientToReturnFile(attachmentWithEcli);

    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, dsDocOffice, "1234567890123");

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId() + "/file")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .bodyAsByteArray(attachmentWithEcli)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(Docx2Html.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().ecliList())
                  .containsExactly("ECLI:DE:BGH:2023:210423UVZR86.22.0");
            });

    DocumentationUnitDTO savedDTO = repository.findById(dto.getId()).get();
    assertThat(savedDTO.getEcli()).isEqualTo("ECLI:DE:BGH:2023:210423UVZR86.22.0");
  }

  @Test
  void
      testAttachFileToDocumentationUnit_withECLIInFooter_shouldExtractECLIAndNotChangeTheECLIInUnitIfECLIIsSet()
          throws IOException {
    var attachmentWithEcli =
        Files.readAllBytes(Paths.get("src/test/resources/fixtures/attachment_ecli.docx"));
    mockS3ClientToReturnFile(attachmentWithEcli);

    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository,
            DocumentationUnitDTO.builder()
                .documentNumber("1234567890123")
                .ecli("oldEcli")
                .documentationOffice(dsDocOffice));

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId() + "/file")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .bodyAsByteArray(attachmentWithEcli)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(Docx2Html.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().ecliList())
                  .containsExactly("ECLI:DE:BGH:2023:210423UVZR86.22.0");
            });

    DocumentationUnitDTO savedDTO = repository.findById(dto.getId()).get();
    assertThat(savedDTO.getEcli()).isEqualTo("oldEcli");
  }

  @Test
  void
      testAttachFileToDocumentationUnit_withMetadataProperties_shouldExtractCoreDataAndShouldNotOverrideFields()
          throws IOException {
    var attachmentWithMetadata =
        Files.readAllBytes(Paths.get("src/test/resources/fixtures/with_metadata.docx"));
    mockS3ClientToReturnFile(attachmentWithMetadata);

    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository,
            DocumentationUnitDTO.builder()
                .documentNumber("1234567890123")
                .legalEffect(LegalEffectDTO.JA) // file has "Nein"
                .judicialBody("1. Senat") // file has "2. Senat"
                .court(null) // file has BFH
                .documentationOffice(documentationOfficeRepository.findByAbbreviation("DS")));

    databaseCourtRepository.save(
        CourtDTO.builder().type("BFH").isForeignCourt(true).isSuperiorCourt(false).build());

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId() + "/file")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .bodyAsByteArray(attachmentWithMetadata)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(Docx2Html.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().properties())
                  .containsAllEntriesOf(
                      Map.of(
                          DocxMetadataProperty.APPRAISAL_BODY,
                          "2. Senat",
                          DocxMetadataProperty.FILE_NUMBER,
                          "II B 29/24",
                          DocxMetadataProperty.LEGAL_EFFECT,
                          "Nein",
                          DocxMetadataProperty.COURT_TYPE,
                          "BFH"));
            });

    DocumentationUnitDTO savedDTO = repository.findById(dto.getId()).get();
    assertThat(savedDTO.getLegalEffect()).isEqualTo(LegalEffectDTO.JA); // kept old value
    assertThat(savedDTO.getJudicialBody()).isEqualTo("1. Senat"); // kept old value
    assertThat(savedDTO.getCourt().getType())
        .isEqualTo("BFH"); // added court based on docx properties
  }

  @Test
  void testRemoveFileFromDocumentationUnit() {
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());

    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, dsDocOffice, "1234567890123");

    attachmentRepository.save(
        AttachmentDTO.builder()
            .s3ObjectPath("fooPath")
            .documentationUnit(dto)
            .uploadTimestamp(Instant.now())
            .filename("fooFile")
            .format("docx")
            .build());

    assertThat(attachmentRepository.findAll()).hasSize(1);

    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId() + "/file/fooPath")
        .exchange()
        .expectStatus()
        .isNoContent();

    assertThat(attachmentRepository.findAll()).isEmpty();
  }

  @Test
  void testAttachFileToDocumentationUnit_withExternalUser_shouldBeForbidden() throws IOException {
    var attachment = Files.readAllBytes(Paths.get("src/test/resources/fixtures/attachment.docx"));
    mockS3ClientToReturnFile(attachment);

    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, dsDocOffice, "1234567890123");

    risWebTestClient
        .withExternalLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId() + "/file")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .bodyAsByteArray(attachment)
        .exchange()
        .expectStatus()
        .isForbidden();

    assertThat(attachmentRepository.findAllByDocumentationUnitId(dto.getId())).isEmpty();
  }

  @Test
  void testRemoveFileFromDocumentationUnit_withInvalidUuid_shouldFail() {
    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/abc/file/fooPath")
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  void testRemoveFileFromDocumentationUnit_withExternalUser_shouldBeForbidden() {
    when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());

    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, dsDocOffice, "1234567890123");

    attachmentRepository.save(
        AttachmentDTO.builder()
            .s3ObjectPath("fooPath")
            .documentationUnit(dto)
            .uploadTimestamp(Instant.now())
            .filename("fooFile")
            .format("docx")
            .build());

    assertThat(attachmentRepository.findAll()).hasSize(1);
    risWebTestClient
        .withExternalLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId() + "/file/fooPath")
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  private void mockS3ClientToReturnFile(byte[] file) {
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    when(s3Client.getObject(
            any(GetObjectRequest.class),
            Mockito
                .<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>any()))
        .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), file));
  }
}
