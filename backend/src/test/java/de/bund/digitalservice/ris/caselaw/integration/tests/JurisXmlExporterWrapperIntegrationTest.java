package de.bund.digitalservice.ris.caselaw.integration.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.LongTexts;
import de.bund.digitalservice.ris.caselaw.domain.PreviousDecision;
import de.bund.digitalservice.ris.caselaw.domain.ShortTexts;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import de.bund.digitalservice.ris.domain.export.juris.JurisXmlExporter;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.docx4j.org.apache.xalan.processor.TransformerFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class JurisXmlExporterWrapperIntegrationTest {

  private JurisXmlExporter jurisXmlExporter;
  private String encryptedXml;
  private ObjectMapper objectMapper;
  private static final UUID TEST_UUID = UUID.fromString("88888888-4444-4444-4444-121212121212");
  private final String documentNr = "ABCDE20220001";

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Test
  void test_validDocumentationUnit_shouldReturnEncryptedXMLString() {
    this.jurisXmlExporter = new JurisXmlExporter(objectMapper, new TransformerFactoryImpl());

    CoreData coreData =
        CoreData.builder()
            .fileNumbers(List.of("fileNumber1", "fileNumber2"))
            .deviatingFileNumbers(List.of("deviatingFileNumber1", "deviatingFileNumber2"))
            .court(new Court(UUID.randomUUID(), "courtType", null, null, null, null))
            .ecli("ecli")
            .deviatingEclis(List.of("dev-ecli-1", "dev-ecli-2"))
            .documentType(
                DocumentType.builder().jurisShortcut("category").label("category123").build())
            .ecli("ecli")
            .decisionDate(LocalDate.parse("2021-01-01"))
            .deviatingDecisionDates(
                List.of(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-01-02")))
            .leadingDecisionNormReferences(List.of("BGB §1"))
            .build();

    ShortTexts texts = ShortTexts.builder().decisionName("decisionName").build();
    LongTexts longTexts = LongTexts.builder().tenor("tenor").build();

    List<PreviousDecision> previousDecisions =
        List.of(
            PreviousDecision.builder()
                .uuid(UUID.randomUUID())
                .court(
                    new Court(
                        UUID.randomUUID(), "courtType", "courtPlace", "courtLabel", null, null))
                .decisionDate(LocalDate.parse("2020-05-06"))
                .dateKnown(true)
                .fileNumber("fileNumber")
                .documentType(
                    DocumentType.builder().jurisShortcut("category").label("category123").build())
                .build());

    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .uuid(TEST_UUID)
            .documentNumber(documentNr)
            .coreData(coreData)
            .previousDecisions(previousDecisions)
            .shortTexts(texts)
            .longTexts(longTexts)
            .build();

    encryptedXml = jurisXmlExporter.generateEncryptedXMLString(documentationUnit);

    assertEquals(
        "hoj9Xi74aXi9dPWMdaJm3noJt/m8BEO8DAYMRnMGQvpxxtnuRDwB+x8bVG6O0BTpTokyk+hWClr6pwQ5Bm5Xj7n5CUPU7L1LxgQOake4IDv3U9KN0Ucqu3G1lFcZMlZcssuqhMqwwj9zm8eZC7BC3fqyzuto8rDbB/p0zsZ8cZ6F5yWm3K5dCB9IGszBdUm/uTB9jl1YnX4UnEFCyXiAMN2heQKKX3Al5l45MpcUjTM5BQLabX1Dn6ujyMTdEoyLktsudw6/VQlob8XhLkjVbFG+zClZcOSPqNc+DrWiR22CAJD+3VrRKjc4Cu7/FUzxYi1tgfXq7m43jX8TfjNALUDvPR5UQj03VA7K6vfSTtuVgW101iEWxVCsmA4pRaE2DO3iBuomr7DGHfyRB3f8MO4mcwzooVQjSdOy6tbcUxYsa3L2j+ExH4JY6AMbRmgPqjhJb2OnyIM29KkF4c8FHLrohsaXP+0N2ZsEefQjJbdjD+/y0X1tg4IMWdGDJsujZkHmuDK+ARx4F8F0KScOvial9bUAz6c/joEhiD25Tjuj/flk/Ad+Mv6Njt9He6FprSBoNAAUISOZXtVEH05D/cCpvzfaR2JE2ziJDnacaj3b0/DAwzbsP5cUgvkE+MS4taeSfZagytt/3HUYIoUrn2oiMRSwdhhuXN6KPQFEzCbwoxVD4WoE0tqgMGkuhTtL8ES8PUpiAOA1LuZn2NAy1psABdQiC2ikwmWbS81eQY4Rv8+ae6YdEzpI/UMhExFp+PyC0G6k+L6A5OHmTYYE0+oIBUiCKktVO3dr3CKdlxgLkPmrPgLjjPGbAZrKZ6VeqXqpBZpjC5jroCtzfS+bIxTotBIxqKfQj2WLkOVEsYGEyTZ4P5L2jZg+TujSGuPef70eVBOIgssOx5fqJZKfacW25Tc3MSTA82Z5XhZNB04Z6W0BTpiNoWALy+T/WIC1qcxJk48eZ2k0jfISqTfzoHyCLYWO/Y4duQX+iNl8jLJpna4g5jQeiLez4k+BSfctHZKw34wo3fkHIO43S0CkKN2SbzbjRL3uZpZB6G3tuS4gnWrDddMZGBfg1NGKrurqIFtUPr0VbaaxR5pw2iylxSOtasMcJbDTbPmOi26iVJgBNSBiL1CFMA4dh4xxsccsHegA94wRB/BhT/PXCdCoXgPC+7rnCmOCHWQwqthSDgFBGZMAHjEIARpqdHKR2sj2eZZgOtKxhLmnHwikkBxTjN3KM8TvvcJIttlN49Z4GJ6zFf7/nhtLt9yTAWFue7IBQQOji3B7OvFy9tX4wEc45WBebVaRh8qVliFkvVv+z9r9+qN/7n8Sk2EjsLEwGdLqp8c4OVaoXOj7jcCrvECft3mWYDrSsYS5px8IpJAcU4xslSaxf3DJ6CTQdWiSkHD1+hQ4P3S702NRnsMkl55PatJNzLtVmMDFW2h512X5K2R24Ne0a93FQuYIwEhB8Ws2lFMEJ94OEzlYVCRauXEL6JQcskLNBfyZQcjk8oiIYthJAnz/3B3zn4tlLL2of6CJ88sCwLUzVh9D5RqPxSWC5nxiTn3cleBEEzuBWNUqYYGZ49C+Chtfd7vG/gXepm0EDIuK8IFIFotar5REt4y03PPMuZDaz6jWKSIWjV2x3W8SvjeFyvRCCj2KTohjnZOf8Cywikz70tPSQnfgimqDuF3/9wCl3u1Hy44yZop62k263MbhS/0ky7mw/MxWwJlcFEee8vL4ahiAaCoo4PU98cfKinFZts+2SLvoFU2QMkzWYAp2tIzZVHluupPEXGu/90x2IVAmZMgUpsd/2FYORybzozcUoekDC3+TY16XD1yAEvE6fzWHJjTRPy4cArHHgV5Ss7kGcXjXBE5RmJcRhCvmPG4IFZqxC9fh4ztjjRYrhPkin8TAaFjZPNd9k7YHMt52HqnUBnYB1aywH1cHw2fiUj8jJQ71WhRgPUHRGwTJxObsd5hMyB2s/MypG6ljIodOECyGpU10gvDG/b2+tcjdupLR5DaS18Xz7qJPmfI0rrudqt4LI4xrEHJtTnIQhNeQhjyDS72mARVywzOXUqq6tFXQWccglQSHa/22Fkl6QhLWFiKa3csSpDLFhepxMNmx3IHBzDAqamx7xgGWniLyjz9pScqM59QgTErYhIckeKGYYarrRHpbz7nIp+n/SMTJHZ5acvLxJK5Oguc1kVDEmUEGXVKa+bP3Gov7sSArQgwwYemIAP2YGggWrCT9XD9PCbgWWsia0ND7foqKgDCFFYCgJ0oEvfZhdJePY4aiSDu4p7Q1A2yha0OxmblVy5Udj1DrQu8JcJJIBavpYa3UAHgqEmkC+nYcum2pSX5BA6OLcHs68XL21fjARzjlH0sJNIRZD0eibFEjzP97blaeACGZkoPdB1P/nNWHB1LBOr4/Yj+w9qkHOEpY2NNFCQDoowXImknbRVC7Ts1jTIweKdYsk2azY2FusGcehtOM2g/PiIpCTIIaO5fi/ZGpcE2X6aNO0jbCmF3Y+FyOY4NOkZiS+AjW+UIvgqSo0q8S7Q8Bkk73Uk6WRbfDoPrWGaqp0AyHKXjRSsjVaf2/b8hN5fZrSmF5IQdjRChwCsvDTkpmX2hH/d/IcbxFqzXvo8YY7WV9H1uHQU+329zeakxPtzb62aksdI0HxoCICjYw8y6uevPhKnDi9Eg0HHnE",
        encryptedXml);
  }

  @Test
  void test_invalidDocumentationUnitMissingMandatoryFields_shouldReturnEmptyString() {
    this.jurisXmlExporter = new JurisXmlExporter(objectMapper, new TransformerFactoryImpl());

    DocumentationUnit documentationUnit =
        DocumentationUnit.builder().uuid(TEST_UUID).documentNumber(documentNr).build();

    encryptedXml = jurisXmlExporter.generateEncryptedXMLString(documentationUnit);

    assertEquals("", encryptedXml);
  }
}
