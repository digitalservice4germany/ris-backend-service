package de.bund.digitalservice.ris.caselaw.integration.tests;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDSDocOffice;
import static de.bund.digitalservice.ris.caselaw.AuthUtils.mockUserGroups;
import static de.bund.digitalservice.ris.caselaw.domain.PublicationStatus.PUBLISHED;
import static de.bund.digitalservice.ris.caselaw.domain.PublicationStatus.PUBLISHING;
import static de.bund.digitalservice.ris.caselaw.domain.PublicationStatus.UNPUBLISHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import de.bund.digitalservice.ris.caselaw.SliceTestImpl;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberGeneratorService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseDocumentationUnitStatusService;
import de.bund.digitalservice.ris.caselaw.adapter.DatabaseProcedureService;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentNumberPatternConfig;
import de.bund.digitalservice.ris.caselaw.adapter.DocumentationUnitController;
import de.bund.digitalservice.ris.caselaw.adapter.DocxConverterService;
import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.CourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseCourtRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDeletedDocumentationIdsRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentCategoryRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentNumberRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentTypeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseFileNumberRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseRegionRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseStatusRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DeletedDocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DeviatingFileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentCategoryDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.FileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LeadingDecisionNormReferenceDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDeltaMigrationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresDocumentationUnitRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresHandoverReportRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PreviousDecisionDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.RegionDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.StatusDTO;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.ContentRelatedIndexing;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitDocxMetadataInitializationService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitListItem;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitSearchInput;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.MailService;
import de.bund.digitalservice.ris.caselaw.domain.NormReference;
import de.bund.digitalservice.ris.caselaw.domain.PreviousDecision;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.SingleNorm;
import de.bund.digitalservice.ris.caselaw.domain.Status;
import de.bund.digitalservice.ris.caselaw.domain.Texts;
import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
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
      AuthService.class,
      TestConfig.class,
      DocumentNumberPatternConfig.class,
      KeycloakUserService.class
    },
    controllers = {DocumentationUnitController.class})
class DocumentationUnitIntegrationTest {
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
  @Autowired private DatabaseRegionRepository regionRepository;
  @Autowired private DatabaseDeletedDocumentationIdsRepository deletedDocumentationIdsRepository;

  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private MailService mailService;
  @MockBean private DocxConverterService docxConverterService;
  @MockBean private UserGroupService userGroupService;
  @MockBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private AttachmentService attachmentService;
  @MockBean private PatchMapperService patchMapperService;
  @MockBean private HandoverService handoverService;
  @MockBean private DatabaseDocumentNumberRepository databaseDocumentNumberRepository;

  @MockBean
  private DocumentationUnitDocxMetadataInitializationService
      documentationUnitDocxMetadataInitializationService;

  @MockBean DocumentNumberPatternConfig documentNumberPatternConfig;
  @MockBean DatabaseStatusRepository statusRepository;

  private final DocumentationOffice docOffice = buildDSDocOffice();
  private DocumentationOfficeDTO documentationOffice;

  @BeforeEach
  void setUp() {
    documentationOffice =
        documentationOfficeRepository.findByAbbreviation(docOffice.abbreviation());

    mockUserGroups(userGroupService);
  }

  @AfterEach
  void cleanUp() {
    fileNumberRepository.deleteAll();
    repository.deleteAll();
    databaseDocumentTypeRepository.deleteAll();
  }

  @Test
  void testForCorrectDbEntryAfterNewDocumentationUnitCreation() {
    when(documentNumberPatternConfig.getDocumentNumberPatterns())
        .thenReturn(Map.of("DS", "XXRE0******YY"));

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/new")
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().documentNumber()).startsWith("XXRE0");
              assertThat(response.getResponseBody().status())
                  .isEqualTo(
                      Status.builder().publicationStatus(UNPUBLISHED).withError(false).build());
            });

    List<DocumentationUnitDTO> list = repository.findAll();
    assertThat(list).hasSize(1);
  }

  @Test
  void testDocumentationUnitDeletionAndRecyclingOfDocumentNumber() {
    when(documentNumberPatternConfig.getDocumentNumberPatterns())
        .thenReturn(Map.of("DS", "XXRE0******YY"));

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/new")
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().documentNumber()).startsWith("XXRE0");
            });

    assertThat(repository.findAll()).hasSize(1);
    var deletedDocumentationUnit = repository.findAll().get(0);
    var reusableDocumentNumber = deletedDocumentationUnit.getDocumentNumber();

    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/" + deletedDocumentationUnit.getId())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
            });

    assertThat(repository.findAll()).isEmpty();

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/new")
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().documentNumber())
                  .isEqualTo(reusableDocumentNumber);
            });
  }

  @Test
  void testForFileNumbersDbEntryAfterUpdateByUuid() {

    DocumentationUnitDTO dto =
        repository.save(
            DocumentationUnitDTO.builder()
                .documentNumber("1234567890123")
                .documentationOffice(documentationOffice)
                .build());

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(dto.getId())
            .documentNumber(dto.getDocumentNumber())
            .coreData(
                CoreData.builder()
                    .fileNumbers(List.of("AkteX"))
                    .documentationOffice(docOffice)
                    .build())
            .texts(Texts.builder().decisionName("decisionName").build())
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
              assertThat(response.getResponseBody().documentNumber()).isEqualTo("1234567890123");
              assertThat(response.getResponseBody().coreData().fileNumbers().get(0))
                  .isEqualTo("AkteX");
            });

    List<DocumentationUnitDTO> list = repository.findAll();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getDocumentNumber()).isEqualTo("1234567890123");

    List<FileNumberDTO> fileNumberEntries =
        fileNumberRepository.findAllByDocumentationUnit(list.get(0));
    assertThat(fileNumberEntries).hasSize(1);
    assertThat(fileNumberEntries.get(0).getValue()).isEqualTo("AkteX");
  }

  @Test
  void testDeleteLeadingDecisionNormReferencesForNonBGHDecisions() {
    CourtDTO bghCourt =
        databaseCourtRepository.save(
            CourtDTO.builder()
                .type("BGH")
                .isSuperiorCourt(true)
                .isForeignCourt(false)
                .jurisId(new Random().nextInt())
                .build());
    CourtDTO lgCourt =
        databaseCourtRepository.save(
            CourtDTO.builder()
                .type("LG")
                .isSuperiorCourt(false)
                .isForeignCourt(false)
                .jurisId(new Random().nextInt())
                .build());

    DocumentationUnitDTO dto =
        repository.save(
            DocumentationUnitDTO.builder()
                .documentNumber("1234567890123")
                .leadingDecisionNormReferences(
                    List.of(
                        LeadingDecisionNormReferenceDTO.builder()
                            .normReference("BGB §1")
                            .rank(1)
                            .build()))
                .court(bghCourt)
                .documentationOffice(documentationOffice)
                .build());

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(dto.getId())
            .documentNumber(dto.getDocumentNumber())
            .coreData(
                CoreData.builder()
                    .leadingDecisionNormReferences(List.of("BGB §1"))
                    .documentationOffice(docOffice)
                    .court(Court.builder().id(lgCourt.getId()).build())
                    .build())
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
              assertThat(response.getResponseBody().coreData().leadingDecisionNormReferences())
                  .isEmpty();
            });
  }

  /**
   * The normAbbreviationRawValue is not insertable or updatable. This test validates that the
   * client can send a normReference without normAbbreviation and with normAbbreviationRawValue
   * without breaking anything.
   */
  @Test
  void testUpdateNormReferenceWithoutNormAbbreviationAndWithNormAbbreviationRawValue() {
    DocumentationUnitDTO dto =
        repository.save(
            DocumentationUnitDTO.builder()
                .documentNumber("1234567890123")
                .documentationOffice(documentationOffice)
                .build());

    List<SingleNorm> singleNorms = List.of(SingleNorm.builder().singleNorm("Art 7 S 1").build());

    List<NormReference> norms =
        List.of(
            NormReference.builder()
                .normAbbreviation(null)
                .normAbbreviationRawValue("EWGAssRBes 1/80")
                .singleNorms(singleNorms)
                .build());

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(dto.getId())
            .documentNumber(dto.getDocumentNumber())
            .contentRelatedIndexing(ContentRelatedIndexing.builder().norms(norms).build())
            .coreData(CoreData.builder().documentationOffice(docOffice).build())
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
              assertThat(response.getResponseBody().documentNumber()).isEqualTo("1234567890123");
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .singleNorms()
                          .get(0)
                          .singleNorm())
                  .isEqualTo("Art 7 S 1");
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .normAbbreviation())
                  .isNull();
              assertThat(
                      response
                          .getResponseBody()
                          .contentRelatedIndexing()
                          .norms()
                          .get(0)
                          .normAbbreviationRawValue())
                  .isNull();
            });

    List<DocumentationUnitDTO> list = repository.findAll();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getDocumentNumber()).isEqualTo("1234567890123");
  }

  @Test
  void testSetRegionForCourt() {
    RegionDTO region =
        regionRepository.save(RegionDTO.builder().id(UUID.randomUUID()).code("DEU").build());

    CourtDTO bghCourt =
        databaseCourtRepository.save(
            CourtDTO.builder()
                .type("BGH")
                .location("Karlsruhe")
                .isSuperiorCourt(true)
                .isForeignCourt(false)
                .jurisId(new Random().nextInt())
                .region(region)
                .build());

    DocumentationUnitDTO dto =
        repository.save(
            DocumentationUnitDTO.builder()
                .documentNumber("1234567890123")
                .documentationOffice(documentationOffice)
                .build());

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(dto.getId())
            .documentNumber(dto.getDocumentNumber())
            .coreData(
                CoreData.builder()
                    .documentationOffice(docOffice)
                    .court(Court.builder().id(bghCourt.getId()).build())
                    .build())
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
              assertThat(response.getResponseBody().coreData().region()).isEqualTo("DEU");
            });
  }

  @Test
  void testDocumentTypeToSetIdFromLookuptable() {
    var categoryA =
        databaseDocumentCategoryRepository.saveAndFlush(
            DocumentCategoryDTO.builder().label("A").build());
    var categoryR =
        databaseDocumentCategoryRepository.saveAndFlush(
            DocumentCategoryDTO.builder().label("R").build());
    var categoryC =
        databaseDocumentCategoryRepository.saveAndFlush(
            DocumentCategoryDTO.builder().label("C").build());

    databaseDocumentTypeRepository.save(
        DocumentTypeDTO.builder()
            .abbreviation("ABC")
            .category(categoryA)
            .label("ABC123")
            .multiple(true)
            .build());

    databaseDocumentTypeRepository.save(
        DocumentTypeDTO.builder()
            .abbreviation("ABC")
            .category(categoryC)
            .label("ABC123")
            .multiple(true)
            .build());

    var documentTypeDTOR =
        databaseDocumentTypeRepository.save(
            DocumentTypeDTO.builder()
                .abbreviation("ABC")
                .category(categoryR)
                .label("ABC123")
                .multiple(true)
                .build());

    // TODO find out why this is necessary when the whole test class is executed
    repository.deleteAll();
    DocumentationUnitDTO documentationUnitDto =
        repository.save(
            DocumentationUnitDTO.builder()
                .documentNumber("1234567890123")
                .documentationOffice(documentationOffice)
                .build());

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(documentationUnitDto.getId())
            .documentNumber(documentationUnitDto.getDocumentNumber())
            .coreData(
                CoreData.builder()
                    .documentType(
                        DocumentType.builder()
                            .uuid(documentTypeDTOR.getId())
                            .jurisShortcut(documentTypeDTOR.getAbbreviation())
                            .label(documentTypeDTOR.getLabel())
                            .build())
                    .documentationOffice(docOffice)
                    .build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitFromFrontend.uuid())
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().coreData().documentType().label())
                  .isEqualTo(documentTypeDTOR.getLabel());
              assertThat(response.getResponseBody().coreData().documentType().jurisShortcut())
                  .isEqualTo(documentTypeDTOR.getAbbreviation());
            });

    List<DocumentationUnitDTO> list = repository.findAll();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getDocumentType().getId()).isEqualTo(documentTypeDTOR.getId());
    assertThat(list.get(0).getDocumentType()).isNotNull();
  }

  @Test
  void testUndoSettingDocumentType() {
    var docType =
        databaseDocumentTypeRepository.saveAndFlush(
            DocumentTypeDTO.builder().abbreviation("test").multiple(true).build());

    DocumentationUnitDTO dto =
        repository.save(
            DocumentationUnitDTO.builder()
                .documentNumber("1234567890123")
                .documentType(docType)
                .documentationOffice(documentationOffice)
                .build());

    assertThat(repository.findAll()).hasSize(1);
    assertThat(repository.findById(dto.getId())).isPresent();

    DocumentationUnit documentationUnitFromFrontend =
        DocumentationUnit.builder()
            .uuid(dto.getId())
            .documentNumber(dto.getDocumentNumber())
            .coreData(CoreData.builder().documentationOffice(docOffice).documentType(null).build())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + documentationUnitFromFrontend.uuid())
        .bodyValue(documentationUnitFromFrontend)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isNotNull();
              assertThat(response.getResponseBody().coreData().documentType()).isNull();
            });

    List<DocumentationUnitDTO> list = repository.findAll();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getDocumentType()).isNull();
  }

  @Test
  void testSearchResultsAreDeterministic() {
    var office = documentationOffice;

    var documentNumberToExclude = "KORE000000000";

    for (int i = 0; i < 21; i++) {
      var randomDocNumber =
          i == 0 ? documentNumberToExclude : RandomStringUtils.random(10, true, true);
      CourtDTO court =
          databaseCourtRepository.save(
              CourtDTO.builder()
                  .type("LG")
                  .location("Kassel")
                  .isSuperiorCourt(true)
                  .isForeignCourt(false)
                  .jurisId(i)
                  .build());

      DocumentationUnitDTO dto =
          repository.save(
              DocumentationUnitDTO.builder()
                  .documentNumber(randomDocNumber)
                  .court(court)
                  .documentationOffice(office)
                  .build());

      dto = repository.findById(dto.getId()).get();

      repository.save(
          dto.toBuilder()
              .status(
                  List.of(
                      StatusDTO.builder()
                          .documentationUnitDTO(dto)
                          .publicationStatus(PUBLISHED)
                          .createdAt(Instant.now())
                          .build()))
              .build());
    }

    assertThat(repository.findAll()).hasSize(21);

    List<UUID> responseUUIDs = new ArrayList<>();

    PreviousDecision proceedingDecision = PreviousDecision.builder().build();
    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri(
            "/api/v1/caselaw/documentunits/"
                + "search-linkable-documentation-units?pg=0&sz=20&documentNumber="
                + documentNumberToExclude)
        .bodyValue(proceedingDecision)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<SliceTestImpl<RelatedDocumentationUnit>>() {})
        .consumeWith(
            response -> {
              List<RelatedDocumentationUnit> content = response.getResponseBody().getContent();
              assertThat(content).isNotNull();
              assertThat(content).hasSize(20);
              assertThat(content)
                  .extracting("documentNumber")
                  .doesNotContain(documentNumberToExclude);

              responseUUIDs.addAll(
                  content.stream().map(RelatedDocumentationUnit::getUuid).toList());
            });

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri(
            "/api/v1/caselaw/documentunits/"
                + "search-linkable-documentation-units?pg=0&sz=20&documentNumber="
                + documentNumberToExclude)
        .bodyValue(proceedingDecision)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(new TypeReference<SliceTestImpl<RelatedDocumentationUnit>>() {})
        .consumeWith(
            response -> {
              List<RelatedDocumentationUnit> content = response.getResponseBody().getContent();
              assertThat(content).isNotNull();

              List<UUID> responseUUIDs2 =
                  content.stream().map(RelatedDocumentationUnit::getUuid).toList();

              assertThat(responseUUIDs2).hasSize(20);
              assertThat(responseUUIDs2).isEqualTo(responseUUIDs);
            });
  }

  @Test
  void testSearchByDocumentationUnitSearchInput() {
    UUID otherDocOfficeUuid = documentationOfficeRepository.findByAbbreviation("BGH").getId();

    List<UUID> docOfficeIds =
        List.of(
            documentationOffice.getId(),
            documentationOffice.getId(),
            documentationOffice.getId(),
            documentationOffice.getId(),
            otherDocOfficeUuid,
            otherDocOfficeUuid);
    List<String> documentNumbers =
        List.of(
            "ABCD202300007",
            "EFGH202200123",
            "IJKL202101234",
            "MNOP202300099",
            "QRST202200102",
            "UVWX202311090");
    List<String> fileNumbers = List.of("jkl", "ghi", "def", "ABC", "mno", "pqr");
    List<String> courtTypes = List.of("MNO", "PQR", "STU", "VWX", "YZA", "BCD");
    List<String> courtLocations =
        List.of("Hamburg", "München", "Berlin", "Frankfurt", "Köln", "Leipzig");
    List<LocalDate> decisionDates =
        List.of(
            LocalDate.parse("2021-01-02"),
            LocalDate.parse("2022-02-03"),
            LocalDate.parse("2023-03-04"),
            LocalDate.parse("2023-08-01"),
            LocalDate.parse("2023-08-10"),
            LocalDate.parse("2023-09-10"));
    List<PublicationStatus> statuses =
        List.of(PUBLISHED, UNPUBLISHED, PUBLISHING, PUBLISHED, UNPUBLISHED, PUBLISHED);
    List<Boolean> errorStatuses = List.of(false, true, true, false, true, true);

    for (int i = 0; i < 6; i++) {

      CourtDTO court =
          databaseCourtRepository.save(
              CourtDTO.builder()
                  .type(courtTypes.get(i))
                  .location(courtLocations.get(i))
                  .isSuperiorCourt(true)
                  .isForeignCourt(false)
                  .jurisId(new Random().nextInt())
                  .build());
      DocumentationUnitDTO dto =
          repository.save(
              DocumentationUnitDTO.builder()
                  .id(UUID.randomUUID())
                  .documentNumber(documentNumbers.get(i))
                  .court(court)
                  .decisionDate(decisionDates.get(i))
                  .documentationOffice(
                      DocumentationOfficeDTO.builder().id(docOfficeIds.get(i)).build())
                  .build());

      dto = repository.findById(dto.getId()).get();

      repository.save(
          dto.toBuilder()
              .fileNumbers(
                  List.of(
                      FileNumberDTO.builder()
                          .documentationUnit(dto)
                          .value(fileNumbers.get(i))
                          .rank((long) i)
                          .build()))
              .build());

      dto = repository.findById(dto.getId()).get();

      repository.save(
          dto.toBuilder()
              .status(
                  List.of(
                      StatusDTO.builder()
                          .documentationUnitDTO(dto)
                          .publicationStatus(statuses.get(i))
                          .createdAt(Instant.now())
                          .withError(errorStatuses.get(i))
                          .build()))
              .build());
    }

    // no search criteria
    DocumentationUnitSearchInput searchInput = DocumentationUnitSearchInput.builder().build();
    // the unpublished one from the other docoffice is not in it, the others are ordered
    // by documentNumber
    assertThat(extractDocumentNumbersFromSearchCall(searchInput))
        .contains(
            "ABCD202300007", "EFGH202200123", "IJKL202101234", "MNOP202300099", "UVWX202311090");

    // by documentNumber
    searchInput = DocumentationUnitSearchInput.builder().documentNumber("abc").build();
    assertThat(extractDocumentNumbersFromSearchCall(searchInput)).containsExactly("ABCD202300007");

    // by fileNumber
    searchInput = DocumentationUnitSearchInput.builder().fileNumber("abc").build();
    assertThat(extractDocumentNumbersFromSearchCall(searchInput)).containsExactly("MNOP202300099");

    // by documentNumber & fileNumber
    searchInput =
        DocumentationUnitSearchInput.builder().fileNumber("abc").documentNumber("abc").build();
    assertThat(extractDocumentNumbersFromSearchCall(searchInput)).isEmpty();

    // by court
    searchInput =
        DocumentationUnitSearchInput.builder().courtType("pqr").courtLocation("münchen").build();

    assertThat(extractDocumentNumbersFromSearchCall(searchInput)).containsExactly("EFGH202200123");

    // by decisionDate
    searchInput = DocumentationUnitSearchInput.builder().decisionDate(decisionDates.get(2)).build();

    assertThat(extractDocumentNumbersFromSearchCall(searchInput)).containsExactly("IJKL202101234");

    // by status
    searchInput =
        DocumentationUnitSearchInput.builder()
            .status(Status.builder().publicationStatus(UNPUBLISHED).build())
            .build();

    assertThat(extractDocumentNumbersFromSearchCall(searchInput)).containsExactly("EFGH202200123");

    searchInput =
        DocumentationUnitSearchInput.builder()
            .status(Status.builder().publicationStatus(PUBLISHED).build())
            .build();

    assertThat(extractDocumentNumbersFromSearchCall(searchInput))
        .contains("ABCD202300007", "MNOP202300099", "UVWX202311090");

    // by error status
    searchInput =
        DocumentationUnitSearchInput.builder()
            .status(Status.builder().withError(true).build())
            .build();
    // the docunit with error from the other docoffice should not appear
    assertThat(extractDocumentNumbersFromSearchCall(searchInput))
        .contains("EFGH202200123", "IJKL202101234");

    // by documentation office
    searchInput = DocumentationUnitSearchInput.builder().myDocOfficeOnly(true).build();
    assertThat(extractDocumentNumbersFromSearchCall(searchInput))
        .contains("ABCD202300007", "EFGH202200123", "IJKL202101234", "MNOP202300099");

    // between two decision dates
    LocalDate start = LocalDate.parse("2022-02-01");
    LocalDate end = LocalDate.parse("2023-08-05");
    searchInput =
        DocumentationUnitSearchInput.builder().decisionDate(start).decisionDateEnd(end).build();
    assertThat(extractDocumentNumbersFromSearchCall(searchInput))
        .contains("EFGH202200123", "IJKL202101234", "MNOP202300099");

    // all combined
    searchInput =
        DocumentationUnitSearchInput.builder()
            .documentNumber("abc")
            .courtType("MNO")
            .courtLocation("Hamburg")
            .decisionDate(decisionDates.get(0))
            .status(Status.builder().publicationStatus(PUBLISHED).build())
            .build();

    assertThat(extractDocumentNumbersFromSearchCall(searchInput)).contains("ABCD202300007");
  }

  @Test
  void testSearchByFileNumber_withFileNumberAndDeviatingFileNumber_shouldOnlyReturnOneResult() {
    DocumentationUnitDTO dto =
        repository.save(
            DocumentationUnitDTO.builder()
                .id(UUID.randomUUID())
                .documentNumber("documentNumber")
                .decisionDate(LocalDate.parse("2021-01-02"))
                .documentationOffice(documentationOffice)
                .build());

    repository.save(
        dto.toBuilder()
            .fileNumbers(
                List.of(
                    FileNumberDTO.builder()
                        .documentationUnit(dto)
                        .value("Vf. 19-VIII-22 (e.A.)")
                        .rank(1L)
                        .build()))
            .deviatingFileNumbers(
                List.of(
                    DeviatingFileNumberDTO.builder()
                        .documentationUnit(dto)
                        .value("Vf.19-VIII-22 ea")
                        .rank(1L)
                        .build()))
            .build());

    DocumentationUnitSearchInput searchInput =
        DocumentationUnitSearchInput.builder().fileNumber("Vf.").build();
    assertThat(extractDocumentNumbersFromSearchCall(searchInput)).hasSize(1);
    assertThat(extractDocumentNumbersFromSearchCall(searchInput).get(0)).contains("documentNumber");
  }

  private List<String> extractDocumentNumbersFromSearchCall(
      DocumentationUnitSearchInput searchInput) {

    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("pg", "0");
    queryParams.add("sz", "30");

    if (searchInput.documentNumber() != null) {
      queryParams.add("documentNumber", searchInput.documentNumber());
    }

    if (searchInput.fileNumber() != null) {
      queryParams.add("fileNumber", searchInput.fileNumber());
    }

    if (searchInput.courtType() != null) {
      queryParams.add("courtType", searchInput.courtType());
    }

    if (searchInput.courtLocation() != null) {
      queryParams.add("courtLocation", searchInput.courtLocation());
    }

    if (searchInput.decisionDate() != null) {
      queryParams.add("decisionDate", searchInput.decisionDate().toString());
    }

    if (searchInput.decisionDateEnd() != null) {
      queryParams.add("decisionDateEnd", searchInput.decisionDateEnd().toString());
    }

    if (searchInput.status() != null) {
      if (searchInput.status().publicationStatus() != null) {
        queryParams.add("publicationStatus", searchInput.status().publicationStatus().toString());
      }
      queryParams.add("withError", String.valueOf(searchInput.status().withError()));
    }

    queryParams.add("myDocOfficeOnly", String.valueOf(searchInput.myDocOfficeOnly()));
    URI uri =
        new DefaultUriBuilderFactory()
            .builder()
            .path("/api/v1/caselaw/documentunits/search")
            .queryParams(queryParams)
            .build();

    List<DocumentationUnitListItem> content =
        risWebTestClient
            .withDefaultLogin()
            .get()
            .uri(uri)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new TypeReference<SliceTestImpl<DocumentationUnitListItem>>() {})
            .returnResult()
            .getResponseBody()
            .getContent();

    return content.stream().map(DocumentationUnitListItem::documentNumber).toList();
  }

  @Test
  void testDeleteByUuid_withExistingReference_shouldNotRecycleDocumentNumberAfterFailedDeletion() {
    DocumentationUnitDTO referencedDTO =
        DocumentationUnitDTO.builder()
            .documentNumber("ZZRE202400001")
            .documentationOffice(documentationOffice)
            .build();
    repository.save(referencedDTO);
    DocumentationUnitDTO dto =
        DocumentationUnitDTO.builder()
            .documentNumber("ZZRE202400002")
            .documentationOffice(documentationOffice)
            .previousDecisions(
                List.of(
                    PreviousDecisionDTO.builder()
                        .documentNumber(referencedDTO.getDocumentNumber())
                        .rank(1)
                        .build()))
            .build();
    repository.save(dto);
    when(documentNumberPatternConfig.hasValidPattern(anyString(), anyString())).thenReturn(true);
    when(statusRepository.findAllByDocumentationUnitDTO_Id(any(UUID.class)))
        .thenReturn(
            List.of(
                StatusDTO.builder()
                    .publicationStatus(UNPUBLISHED)
                    .createdAt(Instant.now())
                    .build()));

    risWebTestClient
        .withDefaultLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/" + referencedDTO.getId())
        .exchange()
        .expectStatus()
        .is5xxServerError();

    List<DeletedDocumentationUnitDTO> allDeletedIds = deletedDocumentationIdsRepository.findAll();
    assertThat(allDeletedIds).isEmpty();

    List<DocumentationUnitDTO> allDTOsAfterDelete = repository.findAll();
    assertThat(allDTOsAfterDelete)
        .extracting("documentNumber")
        .containsExactlyInAnyOrder("ZZRE202400001", "ZZRE202400002");
  }

  @Test
  void
      testGenerateNewDocumentationUnit_withDeletedDocumentNumberWhichExistAsDocumentationUnit_shouldRemoveDeletedDocumentsEntryAndGenerateANewDocumentNumber() {
    DocumentationUnitDTO referencedDTO =
        DocumentationUnitDTO.builder()
            .documentNumber("ZZRE202400001")
            .documentationOffice(documentationOffice)
            .build();
    repository.save(referencedDTO);
    DeletedDocumentationUnitDTO deletedDocumentationUnitDTO =
        DeletedDocumentationUnitDTO.builder()
            .abbreviation("DS")
            .documentNumber("ZZRE202400001")
            .year(Year.of(LocalDate.now().get(ChronoField.YEAR)))
            .build();
    deletedDocumentationIdsRepository.save(deletedDocumentationUnitDTO);
    when(documentNumberPatternConfig.getDocumentNumberPatterns())
        .thenReturn(Map.of("DS", "ZZREYYYY*****"));
    when(databaseDocumentNumberRepository.findById("DS"))
        .thenReturn(
            Optional.of(
                DocumentNumberDTO.builder()
                    .documentationOfficeAbbreviation("DS")
                    .lastNumber(1)
                    .build()));

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/new")
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(DocumentationUnit.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody())
                    .extracting("documentNumber")
                    .isEqualTo("ZZRE" + LocalDate.now().getYear() + "00002"));

    List<DeletedDocumentationUnitDTO> allDeletedIds = deletedDocumentationIdsRepository.findAll();
    assertThat(allDeletedIds).isEmpty();
  }

  @Test
  void testGenerateNewDocumentationUnit_withInternalUser_shouldSucceed() {
    when(documentNumberPatternConfig.getDocumentNumberPatterns())
        .thenReturn(Map.of("DS", "ZZREYYYY*****"));
    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/new")
        .exchange()
        .expectStatus()
        .isCreated();
  }

  @Test
  void testGenerateNewDocumentationUnit_withExternalUser_shouldBeForbidden() {
    risWebTestClient
        .withExternalLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/new")
        .exchange()
        .expectStatus()
        .isForbidden();
  }
}
