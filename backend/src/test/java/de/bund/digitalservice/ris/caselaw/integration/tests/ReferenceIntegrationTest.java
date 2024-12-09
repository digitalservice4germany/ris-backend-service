package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDSDocOffice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
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
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDependentLiteratureCitationRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentTypeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseLegalPeriodicalRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseReferenceRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DependentLiteratureCitationDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalPeriodicalDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDeltaMigrationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresLegalPeriodicalEditionRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.ReferenceDTO;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentTypeTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.LegalPeriodicalTransformer;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DependentLiteratureCitationType;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitDocxMetadataInitializationService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionRepository;
import de.bund.digitalservice.ris.caselaw.domain.MailService;
import de.bund.digitalservice.ris.caselaw.domain.Reference;
import de.bund.digitalservice.ris.caselaw.domain.ReferenceType;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
      PostgresLegalPeriodicalEditionRepositoryImpl.class,
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
class ReferenceIntegrationTest {
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
  @Autowired private DatabaseDocumentationOfficeRepository documentationOfficeRepository;
  @Autowired private DatabaseLegalPeriodicalRepository legalPeriodicalRepository;
  @Autowired private DatabaseDocumentTypeRepository documentTypeRepository;
  @Autowired private LegalPeriodicalEditionRepository editionRepository;
  @Autowired private DatabaseReferenceRepository referenceRepository;
  @Autowired private DatabaseDependentLiteratureCitationRepository literatureCitationRepository;

  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private MailService mailService;
  @MockBean private DocxConverterService docxConverterService;
  @MockBean private UserService userService;
  @MockBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private AttachmentService attachmentService;
  @MockBean private PatchMapperService patchMapperService;
  @MockBean private LdmlExporterService ldmlExporterService;
  @MockBean private HandoverService handoverService;

  @MockBean
  private DocumentationUnitDocxMetadataInitializationService
      documentationUnitDocxMetadataInitializationService;

  private final DocumentationOffice docOffice = buildDSDocOffice();
  private DocumentationOfficeDTO documentationOffice;
  private static final String DEFAULT_DOCUMENT_NUMBER = "1234567890126";
  private DocumentType eanDocumentType;
  private LegalPeriodical bverwgeLegalPeriodical;

  @BeforeEach
  void setUp() {
    documentationOffice =
        documentationOfficeRepository.findByAbbreviation(docOffice.abbreviation());

    when(userService.getDocumentationOffice(any())).thenReturn(docOffice);

    eanDocumentType =
        DocumentTypeTransformer.transformToDomain(
            documentTypeRepository.save(
                DocumentTypeDTO.builder()
                    .label("Anmerkung")
                    .abbreviation("Ean")
                    .multiple(false)
                    .build()));

    bverwgeLegalPeriodical =
        LegalPeriodicalTransformer.transformToDomain(
            legalPeriodicalRepository.save(
                LegalPeriodicalDTO.builder()
                    .abbreviation("BVerwGE")
                    .title("Bundesverwaltungsgerichtsentscheidungen")
                    .subtitle("Entscheidungen des Bundesverwaltungsgerichts")
                    .jurisId(123)
                    .primaryReference(true)
                    .build()));
  }

  @AfterEach
  void cleanUp() {
    repository.deleteAll();
    legalPeriodicalRepository.deleteAll();
  }

  @Test
  void testReferencesCanBeSaved() {
    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, documentationOffice);

    UUID referenceId = UUID.randomUUID();
    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(dto.getId())
            .documentNumber(dto.getDocumentNumber())
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .references(
                List.of(
                    Reference.builder()
                        .id(referenceId)
                        .citation("2024, S.3")
                        .referenceSupplement("Klammerzusatz")
                        .footnote("footnote")
                        .referenceType(ReferenceType.CASELAW)
                        .legalPeriodical(
                            LegalPeriodical.builder()
                                .uuid(bverwgeLegalPeriodical.uuid())
                                .abbreviation("BVerwGE")
                                .primaryReference(true)
                                .build())
                        .build()))
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId())
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().documentNumber())
                  .isEqualTo(DEFAULT_DOCUMENT_NUMBER);
              assertThat(response.getResponseBody().references()).hasSize(1);
              assertThat(response.getResponseBody().references())
                  .extracting("citation", "referenceSupplement", "footnote", "id")
                  .containsExactly(tuple("2024, S.3", "Klammerzusatz", "footnote", referenceId));
              assertThat(response.getResponseBody().references())
                  .extracting("legalPeriodical")
                  .usingRecursiveComparison()
                  .isEqualTo(List.of(bverwgeLegalPeriodical));
            });
  }

  @Test
  void testLiteratureReferencesCanBeSaved() {
    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, documentationOffice);

    UUID referenceId = UUID.randomUUID();
    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(dto.getId())
            .documentNumber(dto.getDocumentNumber())
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .references(
                List.of(
                    Reference.builder()
                        .id(referenceId)
                        .citation("2024, S.3")
                        .author("Heinz Otto")
                        .documentType(eanDocumentType)
                        .referenceType(ReferenceType.LITERATURE)
                        .legalPeriodical(
                            LegalPeriodical.builder()
                                .uuid(bverwgeLegalPeriodical.uuid())
                                .abbreviation("BVerwGE")
                                .primaryReference(true)
                                .build())
                        .build()))
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId())
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().documentNumber())
                  .isEqualTo(DEFAULT_DOCUMENT_NUMBER);
              assertThat(response.getResponseBody().references()).hasSize(1);
              assertThat(response.getResponseBody().references())
                  .extracting("citation", "author", "documentType", "id")
                  .containsExactly(tuple("2024, S.3", "Heinz Otto", eanDocumentType, referenceId));
              assertThat(response.getResponseBody().references())
                  .extracting("legalPeriodical")
                  .usingRecursiveComparison()
                  .isEqualTo(List.of(bverwgeLegalPeriodical));
            });
  }

  @Test
  void testReferencesAndLiteratureCitationsCanBeDeleted() {
    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, documentationOffice);

    UUID referenceId = UUID.randomUUID();

    UUID literatureCitationId = UUID.randomUUID();
    repository.save(
        dto.toBuilder()
            .references(
                List.of(
                    ReferenceDTO.builder()
                        .rank(1)
                        .documentationUnit(dto)
                        .id(referenceId)
                        .citation("2024, S.3")
                        .legalPeriodicalRawValue("BVerwGE")
                        .legalPeriodical(
                            LegalPeriodicalDTO.builder()
                                .id(bverwgeLegalPeriodical.uuid())
                                .abbreviation("BVerwGE")
                                .primaryReference(true)
                                .build())
                        .build()))
            .dependentLiteratureCitations(
                List.of(
                    DependentLiteratureCitationDTO.builder()
                        .rank(1)
                        .documentationUnit(dto)
                        .id(literatureCitationId)
                        .citation("2024, S.3")
                        .author("Curie, Marie")
                        .legalPeriodicalRawValue("BVerwGE")
                        .documentTypeRawValue("Ean")
                        .type(DependentLiteratureCitationType.PASSIVE)
                        .documentType(
                            DocumentTypeDTO.builder()
                                .id(eanDocumentType.uuid())
                                .abbreviation("Ean")
                                .build())
                        .legalPeriodical(
                            LegalPeriodicalDTO.builder()
                                .id(bverwgeLegalPeriodical.uuid())
                                .abbreviation("BVerwGE")
                                .primaryReference(true)
                                .build())
                        .build()))
            .build());

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(dto.getId())
            .documentNumber(dto.getDocumentNumber())
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .references(List.of())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId())
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().references()).isEmpty();
            });

    assertThat(referenceRepository.findById(referenceId)).isEmpty();
    assertThat(literatureCitationRepository.findById(literatureCitationId)).isEmpty();
  }

  @Test
  void testReferencesAndLiteratureCitationsOriginatedFromEditionCanBeDeleted() {
    DocumentationUnitDTO dto =
        EntityBuilderTestUtil.createAndSavePublishedDocumentationUnit(
            repository, documentationOffice);

    UUID referenceId = UUID.randomUUID();

    UUID literatureCitationId = UUID.randomUUID();
    var edition =
        editionRepository.save(
            LegalPeriodicalEdition.builder()
                .id(UUID.randomUUID())
                .legalPeriodical(bverwgeLegalPeriodical)
                .name("2024")
                .references(
                    List.of(
                        Reference.builder()
                            .rank(1)
                            .referenceType(ReferenceType.CASELAW)
                            .documentationUnit(
                                RelatedDocumentationUnit.builder().uuid(dto.getId()).build())
                            .id(referenceId)
                            .citation("2024, S.3")
                            .legalPeriodicalRawValue("BVerwGE")
                            .legalPeriodical(
                                LegalPeriodical.builder()
                                    .uuid(bverwgeLegalPeriodical.uuid())
                                    .abbreviation("BVerwGE")
                                    .primaryReference(true)
                                    .build())
                            .build(),
                        Reference.builder()
                            .rank(2)
                            .referenceType(ReferenceType.LITERATURE)
                            .documentationUnit(
                                RelatedDocumentationUnit.builder().uuid(dto.getId()).build())
                            .id(literatureCitationId)
                            .citation("2024, S.3")
                            .author("Curie, Marie")
                            .legalPeriodicalRawValue("BVerwGE")
                            .documentType(eanDocumentType)
                            .legalPeriodical(bverwgeLegalPeriodical)
                            .build()))
                .build());

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(dto.getId())
            .documentNumber(dto.getDocumentNumber())
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
            .references(List.of())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + dto.getId())
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().references()).isEmpty();
            });

    assertThat(referenceRepository.findById(referenceId)).isEmpty();
    assertThat(literatureCitationRepository.findById(literatureCitationId)).isEmpty();
    assertThat(editionRepository.findById(edition.id()).get().references()).isEmpty();

    editionRepository.delete(edition);
  }
}
