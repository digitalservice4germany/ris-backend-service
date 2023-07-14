package de.bund.digitalservice.ris.caselaw.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import de.bund.digitalservice.ris.caselaw.RisWebTestClient;
import de.bund.digitalservice.ris.caselaw.TestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.LookupTableImporterController;
import de.bund.digitalservice.ris.caselaw.adapter.LookupTableImporterService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.JPADocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.JPADocumentTypeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.CitationStyleDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.CourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.DatabaseCitationStyleRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.DatabaseCourtRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.DatabaseFieldOfLawRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.FieldOfLawDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.FieldOfLawKeywordDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.FieldOfLawKeywordRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.FieldOfLawLinkDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.FieldOfLawLinkRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.NormDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.NormRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.StateDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.StateRepository;
import de.bund.digitalservice.ris.caselaw.config.FlywayConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresConfig;
import de.bund.digitalservice.ris.caselaw.config.PostgresJPAConfig;
import de.bund.digitalservice.ris.caselaw.config.SecurityConfig;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitService;
import de.bund.digitalservice.ris.caselaw.domain.EmailPublishService;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@RISIntegrationTest(
    imports = {
      LookupTableImporterService.class,
      FlywayConfig.class,
      PostgresConfig.class,
      PostgresJPAConfig.class,
      SecurityConfig.class,
      AuthService.class,
      TestConfig.class
    },
    controllers = {LookupTableImporterController.class})
class LookupTableImporterIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:12");

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("database.user", () -> postgreSQLContainer.getUsername());
    registry.add("database.password", () -> postgreSQLContainer.getPassword());
    registry.add("database.host", () -> postgreSQLContainer.getHost());
    registry.add("database.port", () -> postgreSQLContainer.getFirstMappedPort());
    registry.add("database.database", () -> postgreSQLContainer.getDatabaseName());
  }

  @Autowired private RisWebTestClient risWebTestClient;
  @Autowired private JPADocumentTypeRepository jpaDocumentTypeRepository;
  @Autowired private DatabaseCourtRepository databaseCourtRepository;
  @Autowired private DatabaseCitationStyleRepository databaseCitationStyleRepository;
  @Autowired private StateRepository stateRepository;
  @Autowired private DatabaseFieldOfLawRepository fieldOfLawRepository;
  @Autowired private FieldOfLawKeywordRepository fieldOfLawKeywordRepository;
  @Autowired private NormRepository normRepository;
  @Autowired private FieldOfLawLinkRepository fieldOfLawLinkRepository;

  @MockBean private S3AsyncClient s3AsyncClient;
  @MockBean private EmailPublishService publishService;
  @MockBean UserService userService;
  @MockBean private DocumentUnitService documentUnitService;
  @MockBean ReactiveClientRegistrationRepository clientRegistrationRepository;
  @MockBean private JdbcTemplate jdbcTemplate;

  @AfterEach
  void cleanUp() {
    jpaDocumentTypeRepository.deleteAll();
    databaseCourtRepository.deleteAll().block();
    databaseCitationStyleRepository.deleteAll().block();
    stateRepository.deleteAll().block();
    fieldOfLawRepository.deleteAll().block(); // will cascade delete the other 3 repo-contents
  }

  @Test
  void shouldImportDocumentTypeLookupTableCorrectly() {
    String doktypXml =
        """
        <?xml version="1.0" encoding="utf-8"?>
        <juris-table>
          <juris-doktyp id="7" aendkz="N" version="1.0">
            <jurisabk>ÄN</jurisabk>
            <dokumentart>N</dokumentart>
            <mehrfach>Ja</mehrfach>
            <bezeichnung>Änderungsnorm</bezeichnung>
          </juris-doktyp>
        </juris-table>""";

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/lookuptableimporter/doktyp")
        .bodyValue(doktypXml)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody())
                    .isEqualTo("Successfully imported the document type lookup table"));

    List<JPADocumentTypeDTO> list = jpaDocumentTypeRepository.findAll();
    assertThat(list).hasSize(1);
    JPADocumentTypeDTO documentTypeDTO = list.get(0);
    assertThat(documentTypeDTO.getId()).isEqualTo(7L);
    assertThat(documentTypeDTO.getJurisShortcut()).isEqualTo("ÄN");
    assertThat(documentTypeDTO.getDocumentType()).isEqualTo('N');
    assertThat(documentTypeDTO.getMultiple()).isEqualTo("Ja");
    assertThat(documentTypeDTO.getLabel()).isEqualTo("Änderungsnorm");
  }

  @Test
  void shouldImportCourtLookupTableCorrectly() {
    String gerichtdataXml =
        """
        <?xml version="1.0"?>
        <juris-table>
          <juris-gericht id="5" aenddatum_client="2022-01-01" aendkz="J" version="1.0">
            <gertyp>Gertyp123</gertyp>
            <gerort>Ort</gerort>
            <buland>BW</buland>
          </juris-gericht>
        </juris-table>""";

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/lookuptableimporter/gerichtdata")
        .bodyValue(gerichtdataXml)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody())
                    .isEqualTo("Successfully imported the court lookup table"));

    List<CourtDTO> courtDTOs = databaseCourtRepository.findAll().collectList().block();
    assertThat(courtDTOs).hasSize(1);
    CourtDTO courtDTO = courtDTOs.get(0);
    assertThat(courtDTO.getId()).isEqualTo(5L);
    assertThat(courtDTO.getChangedateclient()).isEqualTo("2022-01-01");
    assertThat(courtDTO.getChangeindicator()).isEqualTo('J');
    assertThat(courtDTO.getCourttype()).isEqualTo("Gertyp123");
    assertThat(courtDTO.getCourtlocation()).isEqualTo("Ort");
    assertThat(courtDTO.getFederalstate()).isEqualTo("BW");
  }

  @Test
  void shouldImportStateLookupTableCorrectly() {
    String bulandXml =
        """
        <?xml version="1.0"?>
        <juris-table>
          <juris-buland id="4" aendkz="N" version="1.0">
            <jurisabk>AB</jurisabk>
            <bezeichnung>Bezeichnung123</bezeichnung>
          </juris-buland>
        </juris-table>""";

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/lookuptableimporter/buland")
        .bodyValue(bulandXml)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody())
                    .isEqualTo("Successfully imported the state lookup table"));

    List<StateDTO> stateDTOS = stateRepository.findAll().collectList().block();
    assertThat(stateDTOS).hasSize(1);
    StateDTO stateDTO = stateDTOS.get(0);
    assertThat(stateDTO.getId()).isEqualTo(4L);
    assertThat(stateDTO.getChangeindicator()).isEqualTo('N');
    assertThat(stateDTO.getJurisshortcut()).isEqualTo("AB");
    assertThat(stateDTO.getLabel()).isEqualTo("Bezeichnung123");
  }

  @Test
  void shouldImportCitationStyleLookupTableCorrectly() {
    String citationStyleXml =
        """
                <?xml version="1.0"?>
                <juris-table>
                  <juris-zitart id="1" aendkz="N" version="1.0">
                    <dok_dokumentart>R</dok_dokumentart>
                    <zit_dokumentart>R</zit_dokumentart>
                    <abk>Änderung</abk>
                    <bezeichnung>Änderung</bezeichnung>
                  </juris-zitart>
                </juris-table>
                """;

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/lookuptableimporter/zitart")
        .bodyValue(citationStyleXml)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody())
                    .isEqualTo("Successfully imported the citation lookup table"));

    List<CitationStyleDTO> citationStyleDTOS =
        databaseCitationStyleRepository.findAll().collectList().block();
    assertThat(citationStyleDTOS).hasSize(1);
    CitationStyleDTO citationStyleDTO = citationStyleDTOS.get(0);
    assertThat(citationStyleDTO.getJurisShortcut()).isEqualTo("Änderung");
    assertThat(citationStyleDTO.getLabel()).isEqualTo("Änderung");
  }

  @Test
  void shouldImportFieldOfLawLookupTableCorrectly() {
    NormDTO expectedNorm1 =
        NormDTO.builder()
            .fieldOfLawId(2L)
            .abbreviation("normabk 2.1")
            .singleNormDescription("§ 2.1")
            .build();
    NormDTO expectedNorm2 = NormDTO.builder().fieldOfLawId(2L).abbreviation("normabk 2.2").build();

    FieldOfLawKeywordDTO expectedKeyword1 =
        FieldOfLawKeywordDTO.builder().fieldOfLawId(2L).value("schlagwort 2.1").build();
    FieldOfLawKeywordDTO expectedKeyword2 =
        FieldOfLawKeywordDTO.builder().fieldOfLawId(2L).value("schlagwort 2.2").build();

    FieldOfLawDTO expectedLinkedField1 =
        FieldOfLawDTO.builder()
            .id(3L)
            .childrenCount(0)
            .changeIndicator('N')
            .identifier("ÄB-01-02")
            .build();
    FieldOfLawDTO expectedLinkedField2 =
        FieldOfLawDTO.builder()
            .id(4L)
            .childrenCount(0)
            .changeIndicator('N')
            .identifier("CD-01")
            .build();

    FieldOfLawDTO expectedParent =
        FieldOfLawDTO.builder()
            .id(1L)
            .childrenCount(1)
            .identifier("TS-01")
            .text("stext 1")
            .changeIndicator('N')
            .build();

    FieldOfLawDTO expectedChild =
        new FieldOfLawDTO(
            2L,
            0,
            1L,
            "2022-12-22",
            "2022-12-24",
            'J',
            "1.0",
            "TS-01-01",
            "Linked fields, valid: ÄB-01-02, CD-01, invalid: EF01, Gh-01, IJ-01-023, KL-01a",
            "navbez 2",
            List.of(expectedLinkedField1, expectedLinkedField2),
            Arrays.asList(expectedKeyword1, expectedKeyword2),
            Arrays.asList(expectedNorm1, expectedNorm2),
            false);

    String fieldOfLawXml =
        """
            <?xml version="1.0"?>
            <juris-table>

                <juris-sachg id="2" aenddatum_mail="2022-12-22" aenddatum_client="2022-12-24" aendkz="J" version="1.0">
                    <sachgebiet>TS-01-01</sachgebiet>
                    <stext>Linked fields, valid: ÄB-01-02, CD-01, invalid: EF01, Gh-01, IJ-01-023, KL-01a</stext>
                    <navbez>navbez 2</navbez>
                    <norm>
                        <normabk>normabk 2.1</normabk>
                        <enbez>§ 2.1</enbez>
                    </norm>
                    <norm>
                        <normabk>normabk 2.2</normabk>
                    </norm>
                    <schlagwort>schlagwort 2.1</schlagwort>
                    <schlagwort>schlagwort 2.2</schlagwort>
                </juris-sachg>

                <juris-sachg id="1" aendkz="N">
                    <sachgebiet>TS-01-</sachgebiet>
                    <stext>stext 1</stext>
                </juris-sachg>

                <juris-sachg id="3" aendkz="N">
                    <sachgebiet>ÄB-01-02</sachgebiet>
                </juris-sachg>

                <juris-sachg id="4" aendkz="N">
                    <sachgebiet>CD-01</sachgebiet>
                </juris-sachg>

                <juris-sachg id="5" aendkz="N">
                    <sachgebiet>IJ-01-02</sachgebiet>
                </juris-sachg>

                <juris-sachg id="6" aendkz="N">
                    <sachgebiet>KL-01</sachgebiet>
                </juris-sachg>

            </juris-table>
            """;

    risWebTestClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/lookuptableimporter/fieldOfLaw")
        .bodyValue(fieldOfLawXml)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .consumeWith(
            response ->
                assertThat(response.getResponseBody())
                    .isEqualTo("Successfully imported the fieldOfLaw lookup table"));

    List<FieldOfLawDTO> fieldOfLawDTOS =
        fieldOfLawRepository
            .findAllByOrderByIdentifierAsc(Pageable.unpaged())
            .collectList()
            .block();
    List<FieldOfLawKeywordDTO> keywordDTOs =
        fieldOfLawKeywordRepository.findAllByOrderByFieldOfLawIdAscValueAsc().collectList().block();
    List<NormDTO> normDTOs =
        normRepository.findAllByOrderByFieldOfLawIdAscAbbreviationAsc().collectList().block();

    assertThat(fieldOfLawDTOS).hasSize(6);
    assertThat(keywordDTOs).hasSize(2);
    assertThat(normDTOs).hasSize(2);

    FieldOfLawDTO parent = fieldOfLawDTOS.get(4); // index due to alphabetical sorting
    FieldOfLawDTO child = fieldOfLawDTOS.get(5);

    List<FieldOfLawLinkDTO> linksRaw =
        fieldOfLawLinkRepository.findAllByFieldOfLawId(child.getId()).collectList().block();
    List<FieldOfLawDTO> linkedFields =
        linksRaw.stream()
            .map(
                fieldOfLawLinkDTO ->
                    fieldOfLawRepository
                        .findById(fieldOfLawLinkDTO.getLinkedFieldOfLawId())
                        .block())
            .toList();

    child.setKeywords(keywordDTOs);
    child.setNorms(normDTOs);
    child.setLinkedFieldsOfLaw(linkedFields);

    assertThat(parent).usingRecursiveComparison().isEqualTo(expectedParent);
    assertThat(child)
        .usingRecursiveComparison()
        .ignoringFields("norms.id", "keywords.id")
        .isEqualTo(expectedChild);
  }
}
