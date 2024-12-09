package de.bund.digitalservice.ris.caselaw.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.CourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.JurisdictionTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.CourtTransformer;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CourtTransformerTest {

  @ParameterizedTest
  @CsvSource({
    "Bonn, NonSuperiorNonForeignCourt, false, false",
    "London, NonSuperiorForeignCourt, false, true"
  })
  void shouldTransformLocation(
      String location, String type, boolean isSuperiorCourt, boolean isForeignCourt) {
    CourtDTO courtDTO =
        CourtDTO.builder()
            .jurisId(1)
            .type(type)
            .location(location)
            .isSuperiorCourt(isSuperiorCourt)
            .isForeignCourt(isForeignCourt)
            .jurisdictionType(
                JurisdictionTypeDTO.builder()
                    .documentationOffice(
                        DocumentationOfficeDTO.builder().abbreviation("BGH").build())
                    .build())
            .build();

    Court court = CourtTransformer.transformToDomain(courtDTO);

    // Assertion
    assertThat(court)
        .extracting("type", "location", "label", "responsibleDocOffice")
        .containsExactly(
            type, location, type + " " + location, new DocumentationOffice("BGH", null));
  }

  @ParameterizedTest
  @CsvSource({
    "BGH, SuperiorNonForeignCourt, true, false",
    "DenHaag, SuperiorForeignCourt, true, true"
  })
  void shouldNotTransformLocation(
      String location, String type, boolean isSuperiorCourt, boolean isForeignCourt) {
    CourtDTO courtDTO =
        CourtDTO.builder()
            .jurisId(1)
            .type(type)
            .location(location)
            .isSuperiorCourt(isSuperiorCourt)
            .isForeignCourt(isForeignCourt)
            .build();

    Court court = CourtTransformer.transformToDomain(courtDTO);
    assertThat(court).extracting("type", "location", "label").containsExactly(type, null, type);
  }
}
