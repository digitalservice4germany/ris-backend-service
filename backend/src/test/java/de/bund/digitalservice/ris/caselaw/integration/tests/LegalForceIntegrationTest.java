package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDSDocOffice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberGeneratorService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentationUnitStatusService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentNumberPatternConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentationUnitController;
import de.bund.digitalservice.ris.caselaw.adapter.DocxConverterService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitSearchRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseLegalForceTypeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseNormAbbreviationRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseNormReferenceRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseRegionRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDeltaMigrationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.LegalForceTypeTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.NormAbbreviationTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.RegionTransformer;
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
import de.bund.digitalservice.ris.caselaw.domain.LegalForce;
import de.bund.digitalservice.ris.caselaw.domain.MailService;
import de.bund.digitalservice.ris.caselaw.domain.NormReference;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.SingleNorm;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalForceType;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.NormAbbreviation;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.Region;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
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
      PostgresDocumentationUnitRepositoryImpl.class,
      PostgresHandoverReportRepositoryImpl.class,
      FlywayConfig.class,
      PostgresJPAConfig.class,
      SecurityConfig.class,
      AuthService.class,
      TestConfig.class,
      DocumentNumberPatternConfig.class
    },
    controllers = {DocumentationUnitController.class},
    timeout = "PT2M")
@Sql(scripts = {"classpath:doc_office_init.sql", "classpath:legal_force_init.sql"})
@Sql(
    scripts = {"classpath:legal_force_cleanup.sql"},
    executionPhase = AFTER_TEST_METHOD)
class LegalForceIntegrationTest {
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
  @Autowired private DatabaseNormReferenceRepository normRepository;
  @Autowired private DatabaseNormAbbreviationRepository normAbbreviationRepository;
  @Autowired private DatabaseLegalForceTypeRepository legalForceTypeRepository;
  @Autowired private DatabaseRegionRepository regionRepository;
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;

  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private MailService mailService;
  @MockBean private DocxConverterService docxConverterService;
  @MockBean private UserService userService;
  @MockBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private AttachmentService attachmentService;
  @MockBean private HandoverService handoverService;
  @MockBean private ProcedureService procedureService;

  @MockBean
  private DocumentationUnitDocxMetadataInitializationService
      documentationUnitDocxMetadataInitializationService;

  @MockBean private PatchMapperService patchMapperService;

  private final DocumentationOffice docOffice = buildDSDocOffice();

  @Autowired
  private DatabaseDocumentationUnitSearchRepository databaseDocumentationUnitSearchRepository;

  @BeforeEach
  void setUp() {
    doReturn(docOffice).when(userService).getDocumentationOffice(any(OidcUser.class));
  }

  @Transactional
  @Test
  void getNormReference_withoutLegalForce() {
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/documentnr001")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody().contentRelatedIndexing().norms()).hasSize(1);
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .singleNorms()
                          .get(0)
                          .legalForce())
                  .isNull();
            });
  }

  @Transactional
  @Test
  void getNormReference_withLegalForce() {
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/documentnr002")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody().contentRelatedIndexing().norms()).hasSize(1);
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .singleNorms()
                          .get(0)
                          .legalForce())
                  .isNotNull();
            });
  }

  @Transactional
  @Test
  void updateNormReference_addNewLegalForce() {
    NormAbbreviation normAbbreviation =
        NormAbbreviationTransformer.transformToDomain(
            normAbbreviationRepository
                .findById(UUID.fromString("33333333-2222-3333-4444-555555555555"))
                .get());

    LegalForceType legalForceType =
        LegalForceTypeTransformer.transformToDomain(
            legalForceTypeRepository
                .findById(UUID.fromString("11111111-2222-3333-4444-555555555555"))
                .get());

    Region region =
        RegionTransformer.transformDTO(
            regionRepository
                .findById(UUID.fromString("55555555-2222-3333-4444-555555555555"))
                .get());

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3"))
            .documentNumber("documentnr001")
            .coreData(CoreData.builder().build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder()
                    .norms(
                        List.of(
                            NormReference.builder()
                                .normAbbreviation(normAbbreviation)
                                .singleNorms(
                                    List.of(
                                        SingleNorm.builder()
                                            .singleNorm("single norm")
                                            .legalForce(
                                                LegalForce.builder()
                                                    .region(region)
                                                    .type(legalForceType)
                                                    .build())
                                            .build()))
                                .build()))
                    .build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3")
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .singleNorms()
                          .get(0)
                          .legalForce())
                  .isNotNull();
            });
  }

  @Transactional
  @Test
  void updateNormReference_addNewLegalForce_withoutNormReference() {
    LegalForceType legalForceType =
        LegalForceTypeTransformer.transformToDomain(
            legalForceTypeRepository
                .findById(UUID.fromString("11111111-2222-3333-4444-555555555555"))
                .get());

    Region region =
        RegionTransformer.transformDTO(
            regionRepository
                .findById(UUID.fromString("55555555-2222-3333-4444-555555555555"))
                .get());

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3"))
            .documentNumber("documentnr001")
            .coreData(CoreData.builder().build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder()
                    .norms(
                        List.of(
                            NormReference.builder()
                                .singleNorms(
                                    List.of(
                                        SingleNorm.builder()
                                            .singleNorm("single norm")
                                            .legalForce(
                                                LegalForce.builder()
                                                    .region(region)
                                                    .type(legalForceType)
                                                    .build())
                                            .build()))
                                .build()))
                    .build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3")
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }

  @Transactional
  @Test
  void updateNormReference_updateExistingLegalForce() {
    NormAbbreviation normAbbreviation =
        NormAbbreviationTransformer.transformToDomain(
            normAbbreviationRepository
                .findById(UUID.fromString("33333333-2222-3333-4444-555555555555"))
                .get());

    LegalForceType legalForceType =
        LegalForceTypeTransformer.transformToDomain(
            legalForceTypeRepository
                .findById(UUID.fromString("11111111-2222-3333-4444-555555555555"))
                .get());

    Region region =
        RegionTransformer.transformDTO(
            regionRepository
                .findById(UUID.fromString("55555555-2222-3333-4444-555555555555"))
                .get());

    LegalForce legalForce = LegalForce.builder().region(region).type(legalForceType).build();

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3"))
            .documentNumber("documentnr001")
            .coreData(CoreData.builder().build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder()
                    .norms(
                        List.of(
                            NormReference.builder()
                                .normAbbreviation(normAbbreviation)
                                .singleNorms(
                                    List.of(
                                        SingleNorm.builder()
                                            .singleNorm("single norm")
                                            .legalForce(legalForce)
                                            .build()))
                                .build()))
                    .build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3")
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .singleNorms()
                          .get(0)
                          .legalForce())
                  .isNotNull();
            });

    DocumentationUnitDTO result =
        repository.findById(UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3")).get();

    assertThat(result.getNormReferences()).hasSize(1);

    assertThat(result.getNormReferences().get(0).getLegalForce().getNormReference()).isNotNull();
  }

  @Transactional
  @Test
  void updateNormReference_deleteLegalForce() {
    NormAbbreviation normAbbreviation =
        NormAbbreviationTransformer.transformToDomain(
            normAbbreviationRepository
                .findById(UUID.fromString("33333333-2222-3333-4444-555555555555"))
                .get());

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3"))
            .documentNumber("documentnr001")
            .coreData(CoreData.builder().build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder()
                    .norms(
                        List.of(
                            NormReference.builder()
                                .normAbbreviation(normAbbreviation)
                                .singleNorms(
                                    List.of(SingleNorm.builder().singleNorm("single norm").build()))
                                .build()))
                    .build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3")
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .singleNorms()
                          .get(0)
                          .legalForce())
                  .isNull();
            });

    DocumentationUnitDTO result =
        repository.findById(UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3")).get();

    assertThat(result.getNormReferences()).hasSize(1);

    assertThat(result.getNormReferences().get(0).getLegalForce()).isNull();
  }

  @Transactional
  @Test
  void updateNormReference_deleteLegalForceRegion() {

    NormAbbreviation normAbbreviation =
        NormAbbreviationTransformer.transformToDomain(
            normAbbreviationRepository
                .findById(UUID.fromString("33333333-2222-3333-4444-555555555555"))
                .get());

    LegalForceType legalForceType =
        LegalForceTypeTransformer.transformToDomain(
            legalForceTypeRepository
                .findById(UUID.fromString("11111111-2222-3333-4444-555555555555"))
                .get());

    LegalForce legalForce = LegalForce.builder().type(legalForceType).build();

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3"))
            .documentNumber("documentnr001")
            .coreData(CoreData.builder().build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder()
                    .norms(
                        List.of(
                            NormReference.builder()
                                .normAbbreviation(normAbbreviation)
                                .singleNorms(
                                    List.of(
                                        SingleNorm.builder()
                                            .singleNorm("single norm")
                                            .legalForce(legalForce)
                                            .build()))
                                .build()))
                    .build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3")
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .singleNorms()
                          .get(0)
                          .legalForce())
                  .isNotNull();
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .singleNorms()
                          .get(0)
                          .legalForce()
                          .region())
                  .isNull();
            });
  }

  @Transactional
  @Test
  void updateNormReference_deleteLegalForceType() {

    NormAbbreviation normAbbreviation =
        NormAbbreviationTransformer.transformToDomain(
            normAbbreviationRepository
                .findById(UUID.fromString("33333333-2222-3333-4444-555555555555"))
                .get());

    Region region =
        RegionTransformer.transformDTO(
            regionRepository
                .findById(UUID.fromString("55555555-2222-3333-4444-555555555555"))
                .get());

    LegalForce legalForce = LegalForce.builder().region(region).build();

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(UUID.fromString("46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3"))
            .documentNumber("documentnr001")
            .coreData(CoreData.builder().build())
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder()
                    .norms(
                        List.of(
                            NormReference.builder()
                                .normAbbreviation(normAbbreviation)
                                .singleNorms(
                                    List.of(
                                        SingleNorm.builder()
                                            .singleNorm("single norm")
                                            .legalForce(legalForce)
                                            .build()))
                                .build()))
                    .build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3")
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .singleNorms()
                          .get(0)
                          .legalForce())
                  .isNotNull();
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .singleNorms()
                          .get(0)
                          .legalForce()
                          .type())
                  .isNull();
            });
  }
}
