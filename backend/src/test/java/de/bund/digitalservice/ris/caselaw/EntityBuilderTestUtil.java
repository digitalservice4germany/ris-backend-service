package de.bund.digitalservice.ris.caselaw;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.CourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.FileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalPeriodicalDTO;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import java.util.List;
import java.util.UUID;

/** A static test class for generating default, commonly used entities for testing purposes. */
public class EntityBuilderTestUtil {

  public static RelatedDocumentationUnit createTestRelatedDocument() {

    return RelatedDocumentationUnit.builder()
        .uuid(UUID.fromString("e8c6f756-d6b2-4fa4-b751-e88c7c53bde4"))
        .documentNumber("YYTestDoc0013")
        .status(null)
        .fileNumber("AB 34/1")
        .court(createTestCourt())
        .referenceFound(true)
        .build();
  }

  public static DocumentationUnitDTO createTestDocumentationUnitDTO() {
    return DocumentationUnitDTO.builder()
        .id(UUID.fromString("e8c6f756-d6b2-4fa4-b751-e88c7c53bde4"))
        .documentNumber("YYTestDoc0013")
        .court(createTestCourtDTO())
        .fileNumbers(List.of(createTestFileNumberDTO()))
        .build();
  }

  public static Court createTestCourt() {
    return Court.builder()
        .id(UUID.fromString("4e254f62-ce83-43fa-86c5-ecd9caa1d610"))
        .type("BGH")
        .label("BGH Berlin")
        .location("Berlin")
        .build();
  }

  public static CourtDTO createTestCourtDTO() {
    return CourtDTO.builder()
        .id(UUID.fromString("4e254f62-ce83-43fa-86c5-ecd9caa1d610"))
        .type("BGH")
        .location("Berlin")
        .jurisId(0)
        .build();
  }

  public static FileNumberDTO createTestFileNumberDTO() {
    return FileNumberDTO.builder().value("AB 34/1").rank(0L).build();
  }

  public static LegalPeriodicalDTO createTestLegalPeriodicalDTO() {
    return LegalPeriodicalDTO.builder()
        .id(UUID.fromString("33333333-2222-3333-4444-555555555555"))
        .primaryReference(true)
        .title("Legal Periodical Title")
        .subtitle("Legal Periodical Subtitle")
        .abbreviation("LPA")
        .jurisId(0)
        .build();
  }

  public static LegalPeriodical createTestLegalPeriodical() {
    return LegalPeriodical.builder()
        .uuid(UUID.fromString("33333333-2222-3333-4444-555555555555"))
        .title("Legal Periodical Title")
        .subtitle("Legal Periodical Subtitle")
        .abbreviation("LPA")
        .primaryReference(true)
        .build();
  }
}
