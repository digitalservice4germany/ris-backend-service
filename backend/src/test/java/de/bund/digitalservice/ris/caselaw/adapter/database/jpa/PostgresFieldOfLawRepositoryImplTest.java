package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PostgresFieldOfLawRepositoryImplTest {
  private static FieldOfLawDTO generateFieldOfLawDto() {
    return FieldOfLawDTO.builder().identifier("AB-31-21").text("parent").build();
  }

  static void addNormToFieldOfLaw(FieldOfLawDTO parent) {
    var fieldOfLawNorm =
        FieldOfLawNormDTO.builder()
            .id(UUID.randomUUID())
            .abbreviation("AB-33")
            .singleNormDescription("Example single norm description")
            .fieldOfLaw(parent)
            .build();

    parent.getNorms().add(fieldOfLawNorm);
  }

  static void addChildToFieldOfLaw(FieldOfLawDTO parent) {
    var child =
        FieldOfLawDTO.builder()
            .parent(parent)
            .identifier("AB-33" + "-21")
            .text("child")
            .parent(parent)
            .build();

    parent.getChildren().add(child);
  }

  @Test
  void getWithNorms() {
    var parent = generateFieldOfLawDto();
    addNormToFieldOfLaw(parent);
    addChildToFieldOfLaw(parent);

    var result = PostgresFieldOfLawRepositoryImpl.getWithNormsWithoutChildren(parent);

    assertEquals(Collections.emptyList(), result.children());
    assertEquals(1, result.norms().size());
  }
}
