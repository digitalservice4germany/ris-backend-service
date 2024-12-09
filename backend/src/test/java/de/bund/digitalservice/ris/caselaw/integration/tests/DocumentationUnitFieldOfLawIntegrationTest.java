package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDSDocOffice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import de.bund.digitalservice.ris.caselaw.adapter.LdmlExporterService;
import de.bund.digitalservice.ris.caselaw.adapter.OAuthService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitFieldOfLawDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.FieldOfLawDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDeltaMigrationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.ContentRelatedIndexing;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitDocxMetadataInitializationService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.MailService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@RISIntegrationTest(
    imports = {
      DocumentationUnitService.class,
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
      DocumentNumberPatternConfig.class
    },
    controllers = {DocumentationUnitController.class})
@Sql(
    scripts = {"classpath:fields_of_law_init.sql"},
    executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(
    scripts = {"classpath:fields_of_law_cleanup.sql"},
    executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class DocumentationUnitFieldOfLawIntegrationTest {
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
  @Autowired private DatabaseDocumentationUnitRepository documentationUnitRepository;
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;

  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private MailService mailService;
  @MockBean private DocxConverterService docxConverterService;
  @MockBean private UserService userService;
  @MockBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private AttachmentService attachmentService;
  @MockBean private PatchMapperService patchMapperService;
  @MockBean private HandoverService handoverService;
  @MockBean private ProcedureService procedureService;
  @MockBean private LdmlExporterService ldmlExporterService;

  @MockBean
  private DocumentationUnitDocxMetadataInitializationService
      documentationUnitDocxMetadataInitializationService;

  private final DocumentationOffice docOffice = buildDSDocOffice();
  private DocumentationOfficeDTO documentationOfficeDTO;

  @BeforeEach
  void setUp() {
    documentationOfficeDTO =
        documentationOfficeRepository.findByAbbreviation(docOffice.abbreviation());

    when(userService.getDocumentationOffice(any())).thenReturn(docOffice);
  }

  @Test
  void testGetAllFieldsOfLawForDocumentationUnit_withoutFieldOfLawLinked_shouldReturnEmptyList() {
    EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
        documentationUnitRepository, documentationOfficeDTO, "docnr12345678");

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/docnr12345678")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().fieldsOfLaw())
                    .isEmpty());
  }

  @Test
  void
      testGetAllFieldsOfLawForDocumentationUnit_withFirstFieldOfLawLinked_shouldReturnListWithLinkedFieldOfLaw() {

    var documentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            documentationUnitRepository, documentationOfficeDTO, "docnr12345678");

    DocumentationUnitFieldOfLawDTO documentationUnitFieldOfLawDTO =
        new DocumentationUnitFieldOfLawDTO();
    documentationUnitFieldOfLawDTO.setFieldOfLaw(
        FieldOfLawDTO.builder()
            .id(UUID.fromString("71defe05-cd4d-43e5-a07e-06c611b81a26"))
            .build());
    documentationUnitFieldOfLawDTO.setDocumentationUnit(documentationUnitDTO);
    documentationUnitFieldOfLawDTO.setRank(1);
    documentationUnitDTO.setDocumentationUnitFieldsOfLaw(List.of(documentationUnitFieldOfLawDTO));

    documentationUnitRepository.save(documentationUnitDTO);

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/docnr12345678")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().fieldsOfLaw())
                    .extracting("identifier")
                    .containsExactly("FL-01"));
  }

  @Test
  void testGetAllFieldsOfLawForDocumentationUnit_shouldReturnSortedList() {
    var documentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            documentationUnitRepository, documentationOfficeDTO, "docnr12345678");

    DocumentationUnitFieldOfLawDTO documentationUnitFieldOfLawDTO1 =
        new DocumentationUnitFieldOfLawDTO();
    documentationUnitFieldOfLawDTO1.setFieldOfLaw(
        FieldOfLawDTO.builder()
            .id(UUID.fromString("71defe05-cd4d-43e5-a07e-06c611b81a26"))
            .build());
    documentationUnitFieldOfLawDTO1.setDocumentationUnit(documentationUnitDTO);
    documentationUnitFieldOfLawDTO1.setRank(1);
    DocumentationUnitFieldOfLawDTO documentationUnitFieldOfLawDTO2 =
        new DocumentationUnitFieldOfLawDTO();
    documentationUnitFieldOfLawDTO2.setFieldOfLaw(
        FieldOfLawDTO.builder()
            .id(UUID.fromString("6959af10-7355-4e22-858d-29a485189957"))
            .build());
    documentationUnitFieldOfLawDTO2.setDocumentationUnit(documentationUnitDTO);
    documentationUnitFieldOfLawDTO2.setRank(2);
    DocumentationUnitFieldOfLawDTO documentationUnitFieldOfLawDTO3 =
        new DocumentationUnitFieldOfLawDTO();
    documentationUnitFieldOfLawDTO3.setFieldOfLaw(
        FieldOfLawDTO.builder()
            .id(UUID.fromString("93393410-0ab0-48ab-a61d-5056e440174a"))
            .build());
    documentationUnitFieldOfLawDTO3.setDocumentationUnit(documentationUnitDTO);
    documentationUnitFieldOfLawDTO3.setRank(3);
    DocumentationUnitFieldOfLawDTO documentationUnitFieldOfLawDTO4 =
        new DocumentationUnitFieldOfLawDTO();
    documentationUnitFieldOfLawDTO4.setFieldOfLaw(
        FieldOfLawDTO.builder()
            .id(UUID.fromString("b4f9ee05-38ed-49c3-89d6-50141f031017"))
            .build());
    documentationUnitFieldOfLawDTO4.setDocumentationUnit(documentationUnitDTO);
    documentationUnitFieldOfLawDTO4.setRank(4);
    documentationUnitDTO.setDocumentationUnitFieldsOfLaw(
        List.of(
            documentationUnitFieldOfLawDTO1,
            documentationUnitFieldOfLawDTO2,
            documentationUnitFieldOfLawDTO3,
            documentationUnitFieldOfLawDTO4));

    documentationUnitRepository.save(documentationUnitDTO);

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/docnr12345678")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().fieldsOfLaw())
                    .extracting("identifier")
                    .containsExactly("FL-01", "AB-01", "FL-02", "CD-01"));
  }

  @Test
  void testAddFieldsOfLawForDocumentationUnit_shouldReturnListWithAllLinkedFieldOfLaw() {
    var documentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            documentationUnitRepository, documentationOfficeDTO, "docnr12345678");

    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .uuid(documentationUnitDTO.getId())
            .documentNumber("docnr12345678")
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder()
                    .fieldsOfLaw(
                        List.of(
                            FieldOfLaw.builder()
                                .id(UUID.fromString("71defe05-cd4d-43e5-a07e-06c611b81a26"))
                                .build(),
                            FieldOfLaw.builder()
                                .id(UUID.fromString("93393410-0ab0-48ab-a61d-5056e440174a"))
                                .build()))
                    .build())
            .build();

    assertThat(documentationUnitDTO).isNotNull();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDTO.getId())
        .bodyValue(documentationUnit)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().fieldsOfLaw())
                    .extracting("identifier")
                    .containsExactly("FL-01", "FL-02"));
  }

  @Test
  void
      testAddFieldsOfLawForDocumentationUnit_withNotExistingFieldOfLaw_shouldReturnListWithAllLinkedFieldOfLaw() {
    var documentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            documentationUnitRepository, documentationOfficeDTO, "docnr12345678");

    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .uuid(documentationUnitDTO.getId())
            .documentNumber("docnr12345678")
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder()
                    .fieldsOfLaw(
                        List.of(
                            FieldOfLaw.builder()
                                .id(UUID.fromString("11defe05-cd4d-43e5-a07e-06c611b81a26"))
                                .build()))
                    .build())
            .build();

    assertThat(documentationUnitDTO).isNotNull();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDTO.getId())
        .bodyValue(documentationUnit)
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }

  @Test
  void testRemoveFieldsOfLawForDocumentationUnit_shouldReturnListWithAllLinkedFieldOfLaw() {
    var documentationUnitDTO =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            documentationUnitRepository, documentationOfficeDTO, "docnr12345678");

    DocumentationUnitFieldOfLawDTO documentationUnitFieldOfLawDTO =
        new DocumentationUnitFieldOfLawDTO();
    documentationUnitFieldOfLawDTO.setFieldOfLaw(
        FieldOfLawDTO.builder()
            .id(UUID.fromString("71defe05-cd4d-43e5-a07e-06c611b81a26"))
            .build());
    documentationUnitFieldOfLawDTO.setDocumentationUnit(documentationUnitDTO);
    documentationUnitFieldOfLawDTO.setRank(1);
    documentationUnitDTO.setDocumentationUnitFieldsOfLaw(List.of(documentationUnitFieldOfLawDTO));

    documentationUnitRepository.save(documentationUnitDTO);

    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .uuid(documentationUnitDTO.getId())
            .documentNumber("docnr12345678")
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder().fieldsOfLaw(Collections.emptyList()).build())
            .build();

    assertThat(documentationUnitDTO).isNotNull();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitDTO.getId())
        .bodyValue(documentationUnit)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody().contentRelatedIndexing().fieldsOfLaw())
                    .isEmpty());
  }
}
