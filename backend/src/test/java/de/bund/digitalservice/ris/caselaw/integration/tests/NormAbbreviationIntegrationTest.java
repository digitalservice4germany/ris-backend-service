package de.bund.digitalservice.ris.caselaw.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.NormAbbreviationController;
import de.bund.digitalservice.ris.caselaw.adapter.NormAbbreviationService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentCategoryRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentTypeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseNormAbbreviationRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseRegionRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentCategoryDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.NormAbbreviationDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PostgresNormAbbreviationRepositoryImpl;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.RegionDTO;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.MailService;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.NormAbbreviation;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.NormAbbreviation.NormAbbreviationBuilder;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.Region;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
      NormAbbreviationService.class,
      PostgresJPAConfig.class,
      FlywayConfig.class,
      PostgresNormAbbreviationRepositoryImpl.class,
      SecurityConfig.class,
      AuthService.class,
      TestConfig.class
    },
    controllers = {NormAbbreviationController.class})
class NormAbbreviationIntegrationTest {
  private NormAbbreviationDTO abbreviation1 =
      NormAbbreviationDTO.builder()
          .abbreviation("norm abbreviation 1")
          .decisionDate(LocalDate.of(2023, Month.MAY, 19))
          .documentId(1234L)
          .documentNumber("document number 1")
          .officialLetterAbbreviation("official letter abbreviation 1")
          .officialLongTitle("official long title 1")
          .officialShortTitle("official short title 1")
          .source("S")
          .build();

  private NormAbbreviationDTO abbreviation2 =
      NormAbbreviationDTO.builder()
          .abbreviation("search query exact")
          .decisionDate(LocalDate.of(2023, Month.MAY, 20))
          .documentId(2345L)
          .documentNumber("document number 2")
          .officialLetterAbbreviation("official letter abbreviation 2")
          .officialLongTitle("official long title 2")
          .officialShortTitle("official short title 2")
          .source("T")
          .build();
  private NormAbbreviationDTO abbreviation3 =
      NormAbbreviationDTO.builder()
          .abbreviation("in the middle the query search is located")
          .decisionDate(LocalDate.of(2023, Month.MAY, 21))
          .documentId(3456L)
          .documentNumber("document number 3")
          .officialLetterAbbreviation("official letter abbreviation 3")
          .officialLongTitle("official long title 3")
          .officialShortTitle("official short title 3")
          .source("U")
          .build();
  private NormAbbreviationDTO abbreviation4 =
      NormAbbreviationDTO.builder()
          .abbreviation("SeaRch QueRY not in the right case")
          .decisionDate(LocalDate.of(2023, Month.MAY, 22))
          .documentId(4567L)
          .documentNumber("document number 4")
          .officialLetterAbbreviation("official letter abbreviation 4")
          .officialLongTitle("official long title 4")
          .officialShortTitle("official short title 4")
          .source("V")
          .build();
  private NormAbbreviationDTO abbreviation5 =
      NormAbbreviationDTO.builder()
          .abbreviation("Search B")
          .decisionDate(LocalDate.of(2023, Month.MAY, 23))
          .documentId(5678L)
          .documentNumber("document number 5")
          .officialLetterAbbreviation("official letter abbreviation 5")
          .officialLongTitle("official long title 5")
          .officialShortTitle("official short title 5")
          .source("W")
          .build();
  private NormAbbreviationDTO abbreviation6 =
      NormAbbreviationDTO.builder()
          .abbreviation("Search A")
          .decisionDate(LocalDate.of(2023, Month.MAY, 24))
          .documentId(6789L)
          .documentNumber("document number 6")
          .officialLetterAbbreviation("official letter abbreviation 6")
          .officialLongTitle("official long title 6")
          .officialShortTitle("official short title 6")
          .source("X")
          .build();
  private NormAbbreviationDTO abbreviation7 =
      NormAbbreviationDTO.builder()
          .abbreviation("Search C")
          .decisionDate(LocalDate.of(2023, Month.MAY, 25))
          .documentId(7890L)
          .documentNumber("document number 7")
          .officialLetterAbbreviation("official letter abbreviation 7")
          .officialLongTitle("official long title 7")
          .officialShortTitle("official short title 7")
          .source("Y")
          .build();

  private NormAbbreviationDTO abbreviationWithSpecialCharacters =
      NormAbbreviationDTO.builder()
          .abbreviation("With special / characters ;+-*:()")
          .decisionDate(LocalDate.of(2023, Month.MAY, 25))
          .documentId(7891L)
          .documentNumber("document number 8")
          .officialLetterAbbreviation("official letter abbreviation 8")
          .officialLongTitle("official long title 8")
          .officialShortTitle("official short title 8")
          .source("Z")
          .build();

  private DocumentCategoryDTO documentCategoryDTO1 =
      DocumentCategoryDTO.builder().label("L").build();
  private DocumentCategoryDTO documentCategoryDTO2 =
      DocumentCategoryDTO.builder().label("M").build();
  private DocumentTypeDTO documentType1 =
      DocumentTypeDTO.builder()
          .id(UUID.randomUUID())
          .abbreviation("document type abbreviation 1")
          .label("document type label 1")
          .multiple(false)
          .superLabel1("super label 11")
          .superLabel2("super label 21")
          .build();
  private DocumentTypeDTO documentType2 =
      DocumentTypeDTO.builder()
          .id(UUID.randomUUID())
          .abbreviation("document type abbreviation 2")
          .label("document type label 2")
          .multiple(false)
          .superLabel1("super label 12")
          .superLabel2("super label 22")
          .build();
  private RegionDTO region1 = RegionDTO.builder().code("region code 1").build();
  private RegionDTO region2 = RegionDTO.builder().code("region code 2").build();

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
  @Autowired private DatabaseNormAbbreviationRepository repository;
  @Autowired private DatabaseDocumentTypeRepository documentTypeRepository;
  @Autowired private DatabaseDocumentCategoryRepository documentCategoryRepository;
  @Autowired private DatabaseRegionRepository regionRepository;

  @MockBean UserService userService;
  @MockBean private DocumentationUnitService documentationUnitService;
  @MockBean ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private MailService mailService;
  @MockBean private ProcedureService procedureService;

  @AfterEach
  void cleanUp() {
    repository.deleteAll();
    documentTypeRepository.deleteAll();
    documentCategoryRepository.deleteAll();
    regionRepository.deleteAll();
  }

  @Test
  void testGetNormAbbreviationById_allValuesFilled() {
    generateAbbreviations();
    generateOtherLookupValues();

    repository.save(
        abbreviation1.toBuilder().documentTypeList(List.of(documentType1)).region(region2).build());

    NormAbbreviation expectedNormAbbreviation =
        new NormAbbreviationTestBuilder()
            .getExpectedNormAbbreviation()
            .addDocumentType(documentType1.getId())
            .setRegion(region2.getId())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/" + abbreviation1.getId())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isEqualTo(expectedNormAbbreviation);
            });
  }

  @Test
  void testGetNormAbbreviationById_withoutLinkedDocumentType() {
    generateAbbreviations();
    generateOtherLookupValues();

    repository.save(abbreviation1.toBuilder().region(region1).build());

    NormAbbreviation expectedNormAbbreviation =
        new NormAbbreviationTestBuilder()
            .getExpectedNormAbbreviation()
            .setRegion(region1.getId())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/" + abbreviation1.getId())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isEqualTo(expectedNormAbbreviation);
            });
  }

  @Test
  void testGetNormAbbreviationById_withoutLinkedRegion() {
    generateAbbreviations();
    generateOtherLookupValues();

    repository.save(abbreviation1.toBuilder().documentTypeList(List.of(documentType1)).build());

    NormAbbreviation expectedNormAbbreviation =
        new NormAbbreviationTestBuilder()
            .getExpectedNormAbbreviation()
            .addDocumentType(documentType1.getId())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/" + abbreviation1.getId())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isEqualTo(expectedNormAbbreviation);
            });
  }

  @Test
  void testGetNormAbbreviationById_withoutLinkedDocumentTypeAndRegion() {
    generateAbbreviations();
    generateOtherLookupValues();

    NormAbbreviation expectedNormAbbreviation =
        new NormAbbreviationTestBuilder().getExpectedNormAbbreviation().build();

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/" + abbreviation1.getId())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation.class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody()).isEqualTo(expectedNormAbbreviation);
            });
  }

  @Test
  void testGetNormAbbreviationById_withTwoLinkedDocumentTypesAndTwoRegions() {
    generateAbbreviations();
    generateOtherLookupValues();

    repository.saveAndFlush(
        abbreviation1.toBuilder()
            .documentTypeList(List.of(documentType1, documentType2))
            .region(region1)
            .build());

    NormAbbreviation expectedNormAbbreviation =
        new NormAbbreviationTestBuilder()
            .getExpectedNormAbbreviation()
            .addDocumentType(documentType1.getId())
            .addDocumentType(documentType2.getId())
            .build();

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/" + abbreviation1.getId())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation.class)
        .consumeWith(
            response -> {
              assertThat(Objects.requireNonNull(response.getResponseBody()).documentTypes())
                  .containsAll(expectedNormAbbreviation.documentTypes());
            });
  }

  @Test
  void testGetNormAbbreviationByExactSearchQuery() {
    generateOtherLookupValues();
    generateAbbreviations();

    repository.refreshMaterializedViews();
    String query = "search query exact";

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/search?pg=0&sz=30&q=" + query)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody())
                  .extracting("id")
                  .containsExactly(abbreviation2.getId());
            });
  }

  @Test
  void testGetNormAbbreviationByPartialSearchQuery() {
    generateOtherLookupValues();
    generateAbbreviations();
    repository.refreshMaterializedViews();
    String query = "search query";

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/search?pg=0&sz=30&q=" + query)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody())
                  .extracting("id")
                  .containsExactly(
                      abbreviation2.getId(), abbreviation4.getId(), abbreviation3.getId());
            });
  }

  @Test
  void testGetNormAbbreviationBySearchQuery_returnRightOrder() {
    generateOtherLookupValues();
    // Exact abbreviation
    NormAbbreviationDTO abbreviation8 =
        NormAbbreviationDTO.builder().abbreviation("Abbreviation").documentId(1234L).build();
    // Exact official letter abbreviation
    NormAbbreviationDTO abbreviation9 =
        NormAbbreviationDTO.builder()
            .abbreviation("No match")
            .documentId(2345L)
            .officialLetterAbbreviation("abbreviation")
            .build();
    // Abbreviation starts with
    NormAbbreviationDTO abbreviation10 =
        NormAbbreviationDTO.builder()
            .abbreviation("Abbreviation starts with")
            .documentId(3456L)
            .build();
    // Official letter abbreviation starts with
    NormAbbreviationDTO abbreviation11 =
        NormAbbreviationDTO.builder()
            .abbreviation("No match")
            .documentId(4567L)
            .officialLetterAbbreviation("Abbreviation official letter")
            .build();
    // Some abbreviation found by weighted vector (no detailed test possible)
    NormAbbreviationDTO abbreviation12 =
        NormAbbreviationDTO.builder()
            .abbreviation("No match")
            .documentId(5567L)
            .officialLetterAbbreviation("Some ranked result")
            .officialShortTitle("Abbreviation")
            .build();
    // Some abbreviation which should be excluded from the search result
    NormAbbreviationDTO abbreviation13 =
        NormAbbreviationDTO.builder()
            .abbreviation("No match")
            .documentId(6567L)
            .officialLetterAbbreviation("No match")
            .officialShortTitle("No match")
            .build();

    repository.save(abbreviation8);
    repository.save(abbreviation9);
    repository.save(abbreviation10);
    repository.save(abbreviation11);
    repository.save(abbreviation12);
    repository.save(abbreviation13);

    repository.refreshMaterializedViews();

    String query = "Abbreviation";

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/search?pg=0&sz=30&q=" + query)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody())
                  .extracting("id")
                  .containsExactly(
                      abbreviation8.getId(),
                      abbreviation9.getId(),
                      abbreviation10.getId(),
                      abbreviation11.getId(),
                      abbreviation12.getId());
            });
  }

  @Test
  void testGetNormAbbreviationBySearchQuery_allowSpecialCharacters() {
    generateOtherLookupValues();
    generateAbbreviations();
    repository.refreshMaterializedViews();

    String query = "With special / characters ;";

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/search?pg=0&sz=30&q=" + query)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody())
                  .extracting("id")
                  .containsExactly(abbreviationWithSpecialCharacters.getId());
            });
  }

  @Test
  void testGetNormAbbreviationBySearchQuery_returnInTheRightOrder() {
    generateOtherLookupValues();
    generateAbbreviations();
    repository.refreshMaterializedViews();

    String query = "search query";

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/search?q=" + query + "&pg=0&sz=30")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody())
                  .extracting("id")
                  .containsExactly(
                      abbreviation2.getId(), abbreviation4.getId(), abbreviation3.getId());
            });

    query = "letter abbreviation query";

    risWebTestClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/normabbreviation/search?q=" + query + "&pg=0&sz=30")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(NormAbbreviation[].class)
        .consumeWith(
            response -> {
              assertThat(response.getResponseBody())
                  .extracting("id")
                  .containsExactlyInAnyOrder(
                      abbreviation3.getId(), abbreviation2.getId(), abbreviation4.getId());
            });
  }

  private void generateOtherLookupValues() {

    documentCategoryDTO1 = documentCategoryRepository.save(documentCategoryDTO1);
    documentCategoryDTO2 = documentCategoryRepository.save(documentCategoryDTO2);

    documentType1.setCategory(documentCategoryDTO1);
    documentType1 = documentTypeRepository.save(documentType1);
    documentType2.setCategory(documentCategoryDTO2);
    documentType2 = documentTypeRepository.save(documentType2);

    region1 = regionRepository.save(region1);
    region2 = regionRepository.save(region2);
  }

  private void generateAbbreviations() {
    abbreviation1 = repository.save(abbreviation1);
    abbreviation2 = repository.save(abbreviation2);
    abbreviation3 = repository.save(abbreviation3);
    abbreviation4 = repository.save(abbreviation4);
    abbreviation5 = repository.save(abbreviation5);
    abbreviation6 = repository.save(abbreviation6);
    abbreviation7 = repository.save(abbreviation7);
    abbreviationWithSpecialCharacters = repository.save(abbreviationWithSpecialCharacters);
  }

  private class NormAbbreviationTestBuilder {

    private final NormAbbreviationBuilder builder;
    private Region region;
    private final List<DocumentType> documentTypes;

    private NormAbbreviationTestBuilder() {
      this.builder = NormAbbreviation.builder();
      documentTypes = new ArrayList<>();
    }

    private NormAbbreviationTestBuilder getExpectedNormAbbreviation() {
      builder
          .id(abbreviation1.getId())
          .abbreviation("norm abbreviation 1")
          .documentId(1234L)
          .documentNumber("document number 1")
          .decisionDate(
              LocalDate.of(2023, Month.MAY, 19)
                  .atStartOfDay()
                  .atZone(ZoneId.of("Europe/Berlin"))
                  .toInstant())
          .officialLetterAbbreviation("official letter abbreviation 1")
          .officialShortTitle("official short title 1")
          .officialLongTitle("official long title 1")
          .source("S");
      return this;
    }

    private NormAbbreviationTestBuilder setRegion(UUID regionUuid) {
      RegionDTO regionDTO = regionRepository.findById(regionUuid).orElse(null);
      if (regionDTO == null) {
        return this;
      }

      region = Region.builder().code(regionDTO.getCode()).id(regionDTO.getId()).build();

      return this;
    }

    private NormAbbreviationTestBuilder addDocumentType(UUID documentTypeUuid) {
      DocumentTypeDTO documentTypeNewDTO =
          documentTypeRepository.findById(documentTypeUuid).orElse(null);
      if (documentTypeNewDTO == null) {
        return this;
      }

      documentTypes.add(
          DocumentType.builder()
              .uuid(documentTypeNewDTO.getId())
              .jurisShortcut(documentTypeNewDTO.getAbbreviation())
              .label(documentTypeNewDTO.getLabel())
              .build());
      return this;
    }

    private NormAbbreviation build() {
      return builder.region(region).documentTypes(documentTypes).build();
    }
  }
}
