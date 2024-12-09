package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.NormAbbreviationDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.NormReferenceDTO;
import de.bund.digitalservice.ris.caselaw.domain.ContentRelatedIndexing;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.NormReference;
import de.bund.digitalservice.ris.caselaw.domain.SingleNorm;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.NormAbbreviation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NormReferenceTransformerTest {
  private static final UUID NORM_ABBREVIATION_UUID_1 = UUID.randomUUID();
  private static final UUID NORM_ABBREVIATION_UUID_2 = UUID.randomUUID();

  @Test
  void testTransformToDTO_addingANewSingleNormToTheExistingNormAbbreviation() {
    DocumentationUnit documentationUnit = generateDocumentationUnit();
    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitDTO.builder().normReferences(new ArrayList<>()).build();
    NormReferenceDTO normReferenceDTO1 =
        NormReferenceDTO.builder()
            .normAbbreviation(NormAbbreviationDTO.builder().id(NORM_ABBREVIATION_UUID_1).build())
            .singleNorm("single norm 1")
            .rank(1)
            .build();
    NormReferenceDTO normReferenceDTO2 =
        NormReferenceDTO.builder()
            .normAbbreviation(NormAbbreviationDTO.builder().id(NORM_ABBREVIATION_UUID_1).build())
            .singleNorm("single norm 2")
            .rank(2)
            .build();
    addNormReferenceToDomain(
        documentationUnit, NORM_ABBREVIATION_UUID_1, "single norm 1", "single norm 2");
    addNormReferenceToDTO(documentationUnitDTO, NORM_ABBREVIATION_UUID_1, 1, "single norm 1");

    DocumentationUnitDTO result =
        DocumentationUnitTransformer.transformToDTO(documentationUnitDTO, documentationUnit);

    assertThat(result.getNormReferences())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(normReferenceDTO1, normReferenceDTO2);
  }

  @Test
  void testTransformToDTO_addingANewSingleNormWithANewNormAbbreviation() {
    DocumentationUnit documentationUnit = generateDocumentationUnit();
    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitDTO.builder().normReferences(new ArrayList<>()).build();
    NormReferenceDTO normReferenceDTO1 =
        NormReferenceDTO.builder()
            .normAbbreviation(NormAbbreviationDTO.builder().id(NORM_ABBREVIATION_UUID_1).build())
            .singleNorm("single norm 1")
            .rank(1)
            .build();
    NormReferenceDTO normReferenceDTO2 =
        NormReferenceDTO.builder()
            .normAbbreviation(NormAbbreviationDTO.builder().id(NORM_ABBREVIATION_UUID_2).build())
            .singleNorm("single norm 2")
            .rank(2)
            .build();
    addNormReferenceToDomain(documentationUnit, NORM_ABBREVIATION_UUID_1, "single norm 1");
    addNormReferenceToDomain(documentationUnit, NORM_ABBREVIATION_UUID_2, "single norm 2");
    addNormReferenceToDTO(documentationUnitDTO, NORM_ABBREVIATION_UUID_1, 1, "single norm 1");

    DocumentationUnitDTO result =
        DocumentationUnitTransformer.transformToDTO(documentationUnitDTO, documentationUnit);

    assertThat(result.getNormReferences())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(normReferenceDTO1, normReferenceDTO2);
  }

  @Test
  void
      testTransformToDTO_withTwoNormReferencesWithDifferentNormAbbreviationAddingANewSingleNormToTheFirstNormAbbreviation() {
    DocumentationUnit documentationUnit = generateDocumentationUnit();
    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitDTO.builder().normReferences(new ArrayList<>()).build();
    NormReferenceDTO normReferenceDTO1 =
        NormReferenceDTO.builder()
            .normAbbreviation(NormAbbreviationDTO.builder().id(NORM_ABBREVIATION_UUID_1).build())
            .singleNorm("single norm 1")
            .rank(1)
            .build();
    // rank change because third one added to first reference
    NormReferenceDTO normReferenceDTO2 =
        NormReferenceDTO.builder()
            .normAbbreviation(NormAbbreviationDTO.builder().id(NORM_ABBREVIATION_UUID_2).build())
            .singleNorm("single norm 2")
            .rank(3)
            .build();
    NormReferenceDTO normReferenceDTO3 =
        NormReferenceDTO.builder()
            .normAbbreviation(NormAbbreviationDTO.builder().id(NORM_ABBREVIATION_UUID_1).build())
            .singleNorm("single norm 3")
            .rank(2)
            .build();
    addNormReferenceToDomain(
        documentationUnit, NORM_ABBREVIATION_UUID_1, "single norm 1", "single norm 3");
    addNormReferenceToDomain(documentationUnit, NORM_ABBREVIATION_UUID_2, "single norm 2");
    addNormReferenceToDTO(documentationUnitDTO, NORM_ABBREVIATION_UUID_1, 1, "single norm 1");
    addNormReferenceToDTO(documentationUnitDTO, NORM_ABBREVIATION_UUID_2, 2, "single norm 2");

    DocumentationUnitDTO result =
        DocumentationUnitTransformer.transformToDTO(documentationUnitDTO, documentationUnit);

    // sequence change. second norm go to the end
    assertThat(result.getNormReferences())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(normReferenceDTO1, normReferenceDTO3, normReferenceDTO2);
  }

  @Test
  void testTransformToDTO_removeAExistingSingleNorm() {
    DocumentationUnit documentationUnit = generateDocumentationUnit();
    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitDTO.builder().normReferences(new ArrayList<>()).build();
    NormReferenceDTO normReferenceDTO1 =
        NormReferenceDTO.builder()
            .normAbbreviation(NormAbbreviationDTO.builder().id(NORM_ABBREVIATION_UUID_1).build())
            .singleNorm("single norm 1")
            .rank(1)
            .build();
    addNormReferenceToDomain(documentationUnit, NORM_ABBREVIATION_UUID_1, "single norm 1");
    addNormReferenceToDTO(
        documentationUnitDTO, NORM_ABBREVIATION_UUID_1, 1, "single norm 1", "single norm 2");

    DocumentationUnitDTO result =
        DocumentationUnitTransformer.transformToDTO(documentationUnitDTO, documentationUnit);

    assertThat(result.getNormReferences())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(normReferenceDTO1);
  }

  @Test
  void testTransformToDTO_removeAllExistingSingleNorm() {
    DocumentationUnit documentationUnit = generateDocumentationUnit();
    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitDTO.builder().normReferences(new ArrayList<>()).build();
    NormReferenceDTO normReferenceDTO1 =
        NormReferenceDTO.builder()
            .normAbbreviation(NormAbbreviationDTO.builder().id(NORM_ABBREVIATION_UUID_1).build())
            .singleNorm(null)
            .rank(1)
            .build();
    addNormReferenceToDomain(documentationUnit, NORM_ABBREVIATION_UUID_1);
    addNormReferenceToDTO(
        documentationUnitDTO, NORM_ABBREVIATION_UUID_1, 1, "single norm 1", "single norm 2");

    DocumentationUnitDTO result =
        DocumentationUnitTransformer.transformToDTO(documentationUnitDTO, documentationUnit);

    assertThat(result.getNormReferences())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(normReferenceDTO1);
  }

  @Test
  void testTransformToDTO_removeExistingNormAbbreviation() {
    DocumentationUnit documentationUnit = generateDocumentationUnit();
    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitDTO.builder().normReferences(new ArrayList<>()).build();
    addNormReferenceToDTO(
        documentationUnitDTO, NORM_ABBREVIATION_UUID_1, 1, "single norm 1", "single norm 2");

    DocumentationUnitDTO result =
        DocumentationUnitTransformer.transformToDTO(documentationUnitDTO, documentationUnit);

    assertThat(result.getNormReferences()).isEmpty();
  }

  @Test
  void testTransformToDomain_twoSingleNormsForTwoDifferentAbbreviations() {
    DocumentationUnitDTO documentationUnitDTO = generateDocumentationUnitDTO();
    addNormReferenceToDTO(documentationUnitDTO, NORM_ABBREVIATION_UUID_1, 1, "single norm 1");
    addNormReferenceToDTO(documentationUnitDTO, NORM_ABBREVIATION_UUID_2, 2, "single norm 2");
    NormReference normReference1 =
        NormReference.builder()
            .normAbbreviation(
                NormAbbreviation.builder()
                    .id(NORM_ABBREVIATION_UUID_1)
                    .documentTypes(Collections.emptyList())
                    .build())
            .singleNorms(List.of(SingleNorm.builder().singleNorm("single norm 1").build()))
            .build();
    NormReference normReference2 =
        NormReference.builder()
            .normAbbreviation(
                NormAbbreviation.builder()
                    .id(NORM_ABBREVIATION_UUID_2)
                    .documentTypes(Collections.emptyList())
                    .build())
            .singleNorms(List.of(SingleNorm.builder().singleNorm("single norm 2").build()))
            .build();

    DocumentationUnit result = DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(result.contentRelatedIndexing().norms())
        .containsExactly(normReference1, normReference2);
  }

  private DocumentationUnit generateDocumentationUnit() {
    ContentRelatedIndexing contentRelatedIndexing =
        ContentRelatedIndexing.builder().norms(new ArrayList<>()).build();
    return DocumentationUnit.builder().contentRelatedIndexing(contentRelatedIndexing).build();
  }

  private DocumentationUnitDTO generateDocumentationUnitDTO() {
    return DocumentationUnitDTO.builder()
        .documentationOffice(DocumentationOfficeDTO.builder().build())
        .normReferences(new ArrayList<>())
        .build();
  }

  private void addNormReferenceToDomain(
      DocumentationUnit documentationUnit, UUID normAbbreviationId, String... singleNormTexts) {

    NormAbbreviation normAbbreviation = NormAbbreviation.builder().id(normAbbreviationId).build();
    List<SingleNorm> singleNorms = new ArrayList<>();
    for (String singleNormText : singleNormTexts) {
      singleNorms.add(SingleNorm.builder().singleNorm(singleNormText).build());
    }
    NormReference normReference =
        NormReference.builder().normAbbreviation(normAbbreviation).singleNorms(singleNorms).build();
    documentationUnit.contentRelatedIndexing().norms().add(normReference);
  }

  private void addNormReferenceToDTO(
      DocumentationUnitDTO documentationUnitDTO,
      UUID normAbbreviationId,
      int rank,
      String... singleNormTexts) {

    NormAbbreviationDTO normAbbreviationDTO =
        NormAbbreviationDTO.builder().id(normAbbreviationId).build();
    for (String singleNormText : singleNormTexts) {
      documentationUnitDTO
          .getNormReferences()
          .add(
              NormReferenceDTO.builder()
                  .normAbbreviation(normAbbreviationDTO)
                  .singleNorm(singleNormText)
                  .rank(rank)
                  .build());
    }
  }
}
