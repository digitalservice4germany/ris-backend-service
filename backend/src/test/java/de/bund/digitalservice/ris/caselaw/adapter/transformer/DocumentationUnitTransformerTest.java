package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.CourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO.DocumentationUnitDTOBuilder;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.EnsuingDecisionDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.InputTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LeadingDecisionNormReferenceDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.LegalEffectDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.NormAbbreviationDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.NormReferenceDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PendingDecisionDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.RegionDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.StatusDTO;
import de.bund.digitalservice.ris.caselaw.domain.ContentRelatedIndexing;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.CoreData.CoreDataBuilder;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnit.DocumentUnitBuilder;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.EnsuingDecision;
import de.bund.digitalservice.ris.caselaw.domain.LegalForce;
import de.bund.digitalservice.ris.caselaw.domain.NormReference;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.SingleNorm;
import de.bund.digitalservice.ris.caselaw.domain.Texts;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalForceType;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.NormAbbreviation;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.Region;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentationUnitTransformerTest {
  @Test
  void testTransformToDTO_withoutCoreData() {
    DocumentationUnitDTO currentDto = DocumentationUnitDTO.builder().build();
    DocumentUnit updatedDomainObject = DocumentUnit.builder().build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getProcedures()).isEmpty();
    assertThat(documentationUnitDTO.getEcli()).isNull();
    assertThat(documentationUnitDTO.getJudicialBody()).isNull();
    assertThat(documentationUnitDTO.getDecisionDate()).isNull();
    assertThat(documentationUnitDTO.getCourt()).isNull();
    assertThat(documentationUnitDTO.getDocumentType()).isNull();
    assertThat(documentationUnitDTO.getDocumentationOffice()).isNull();
  }

  @Test
  void testTransformToDTO_withoutDecisionNames() {
    DocumentationUnitDTO currentDto = DocumentationUnitDTO.builder().build();
    Texts texts = Texts.builder().build();
    DocumentUnit updatedDomainObject = DocumentUnit.builder().texts(texts).build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getDecisionNames()).isEmpty();
  }

  @Test
  void testTransformToDTO_addLegalEffectWithCoreDataDeleted_shouldSetLegalEffectToNull() {
    DocumentationUnitDTO currentDto =
        DocumentationUnitDTO.builder().court(CourtDTO.builder().build()).build();
    DocumentUnit updatedDomainObject = DocumentUnit.builder().build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getLegalEffect()).isNull();
  }

  @Test
  void
      testTransformToDTO_addLegalEffectWithCourtChangedAndNotSuperiorCourtAndLegalEffectNo_shouldSetLegalEffectToNo() {
    DocumentationUnitDTO currentDto =
        DocumentationUnitDTO.builder()
            .court(
                CourtDTO.builder()
                    .id(UUID.fromString("CCCCCCCC-1111-2222-3333-444444444444"))
                    .build())
            .build();
    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .coreData(
                CoreData.builder()
                    .court(
                        Court.builder()
                            .id(UUID.fromString("CCCCCCCC-2222-3333-4444-55555555555"))
                            .type("not superior")
                            .build())
                    .legalEffect("Nein")
                    .build())
            .build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getLegalEffect()).isEqualTo(LegalEffectDTO.NEIN);
  }

  @Test
  void
      testTransformToDTO_addLegalEffectWithCourtChangedAndNotSuperiorCourtAndLegalEffectNotSpecified_shouldSetLegalEffectToNotSpecified() {
    DocumentationUnitDTO currentDto =
        DocumentationUnitDTO.builder()
            .court(
                CourtDTO.builder()
                    .id(UUID.fromString("CCCCCCCC-1111-2222-3333-444444444444"))
                    .build())
            .build();
    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .coreData(
                CoreData.builder()
                    .court(
                        Court.builder()
                            .id(UUID.fromString("CCCCCCCC-2222-3333-4444-55555555555"))
                            .type("not superior")
                            .build())
                    .legalEffect("Keine Angabe")
                    .build())
            .build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getLegalEffect()).isEqualTo(LegalEffectDTO.KEINE_ANGABE);
  }

  @Test
  void
      testTransformToDTO_addLegalEffectWithCourtChangedAndNotSuperiorCourtAndLegalEffectYes_shouldSetLegalEffectToYes() {
    DocumentationUnitDTO currentDto =
        DocumentationUnitDTO.builder()
            .court(
                CourtDTO.builder()
                    .id(UUID.fromString("CCCCCCCC-1111-2222-3333-444444444444"))
                    .build())
            .build();
    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .coreData(
                CoreData.builder()
                    .court(
                        Court.builder()
                            .id(UUID.fromString("CCCCCCCC-2222-3333-4444-55555555555"))
                            .type("not superior")
                            .build())
                    .legalEffect("Ja")
                    .build())
            .build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getLegalEffect()).isEqualTo(LegalEffectDTO.JA);
  }

  @ParameterizedTest
  @ValueSource(strings = {"BGH", "BVerwG", "BFH", "BVerfG", "BAG", "BSG"})
  void testTransformToDTO_addLegalEffectWithCourtChangedAndSuperiorCourt_shouldSetLegalEffectToYes(
      String courtType) {
    DocumentationUnitDTO currentDto =
        DocumentationUnitDTO.builder()
            .court(
                CourtDTO.builder()
                    .id(UUID.fromString("CCCCCCCC-1111-2222-3333-444444444444"))
                    .build())
            .build();
    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .coreData(
                CoreData.builder()
                    .court(
                        Court.builder()
                            .id(UUID.fromString("CCCCCCCC-2222-3333-4444-55555555555"))
                            .type(courtType)
                            .build())
                    .legalEffect("Nein")
                    .build())
            .build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getLegalEffect()).isEqualTo(LegalEffectDTO.JA);
  }

  @Test
  void testTransformToDTO_withInputTypes() {
    DocumentationUnitDTO currentDto = DocumentationUnitDTO.builder().build();
    List<String> inputTypes = List.of("input types 1", "input types 3", "input types 2");
    DocumentUnit updatedDomainObject =
        DocumentUnit.builder().coreData(CoreData.builder().inputTypes(inputTypes).build()).build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getInputTypes())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            InputTypeDTO.builder().value("input types 1").rank(1L).build(),
            InputTypeDTO.builder().value("input types 3").rank(2L).build(),
            InputTypeDTO.builder().value("input types 2").rank(3L).build());
  }

  @Test
  void testTransformToDTO_withOneNormReference() {
    DocumentationUnitDTO currentDto = DocumentationUnitDTO.builder().build();
    NormReference normReferenceInput =
        NormReference.builder()
            .normAbbreviation(
                NormAbbreviation.builder()
                    .id(UUID.fromString("33333333-2222-3333-4444-555555555555"))
                    .build())
            .singleNorms(List.of(SingleNorm.builder().singleNorm("single norm").build()))
            .build();

    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder().norms(List.of(normReferenceInput)).build())
            .build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getNormReferences().get(0).getNormAbbreviation().getId())
        .isEqualTo(normReferenceInput.normAbbreviation().id());
    assertThat(documentationUnitDTO.getNormReferences().get(0).getSingleNorm())
        .isEqualTo(normReferenceInput.singleNorms().get(0).singleNorm());
  }

  @Test
  void testTransformToDTO_withOneNormReference_withLegalForce() {
    DocumentationUnitDTO currentDto = DocumentationUnitDTO.builder().build();
    NormReference normReferenceInput =
        NormReference.builder()
            .normAbbreviation(
                NormAbbreviation.builder()
                    .id(UUID.fromString("33333333-2222-3333-4444-555555555555"))
                    .build())
            .singleNorms(
                List.of(
                    SingleNorm.builder()
                        .singleNorm("single norm")
                        .legalForce(
                            LegalForce.builder()
                                .type(LegalForceType.builder().id(UUID.randomUUID()).build())
                                .region(Region.builder().id(UUID.randomUUID()).build())
                                .build())
                        .build()))
            .build();

    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder().norms(List.of(normReferenceInput)).build())
            .build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getNormReferences().get(0).getLegalForce()).isNotNull();
    assertThat(
            documentationUnitDTO
                .getNormReferences()
                .get(0)
                .getLegalForce()
                .getLegalForceType()
                .getId())
        .isEqualTo(normReferenceInput.singleNorms().get(0).legalForce().type().id());
    assertThat(documentationUnitDTO.getNormReferences().get(0).getLegalForce().getRegion().getId())
        .isEqualTo(normReferenceInput.singleNorms().get(0).legalForce().region().id());
  }

  @Test
  void testTransformToDTO_withOneNormReference_withMultipleSingleNorms_withLegalForce() {
    DocumentationUnitDTO currentDto = DocumentationUnitDTO.builder().build();
    NormReference normReferenceInput =
        NormReference.builder()
            .normAbbreviation(
                NormAbbreviation.builder()
                    .id(UUID.fromString("33333333-2222-3333-4444-555555555555"))
                    .build())
            .singleNorms(
                List.of(
                    SingleNorm.builder()
                        .singleNorm("single norm 1")
                        .legalForce(
                            LegalForce.builder()
                                .type(LegalForceType.builder().id(UUID.randomUUID()).build())
                                .region(Region.builder().id(UUID.randomUUID()).build())
                                .build())
                        .build(),
                    SingleNorm.builder()
                        .singleNorm("single norm 2")
                        .legalForce(
                            LegalForce.builder()
                                .type(LegalForceType.builder().id(UUID.randomUUID()).build())
                                .region(Region.builder().id(UUID.randomUUID()).build())
                                .build())
                        .build()))
            .build();

    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder().norms(List.of(normReferenceInput)).build())
            .build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getNormReferences().get(0).getNormAbbreviation().getId())
        .isEqualTo(normReferenceInput.normAbbreviation().id());
    assertThat(documentationUnitDTO.getNormReferences().get(0).getSingleNorm())
        .isEqualTo(normReferenceInput.singleNorms().get(0).singleNorm());
    assertThat(documentationUnitDTO.getNormReferences().get(0).getLegalForce()).isNotNull();
    assertThat(
            documentationUnitDTO
                .getNormReferences()
                .get(0)
                .getLegalForce()
                .getLegalForceType()
                .getId())
        .isEqualTo(normReferenceInput.singleNorms().get(0).legalForce().type().id());
    assertThat(documentationUnitDTO.getNormReferences().get(0).getLegalForce().getRegion().getId())
        .isEqualTo(normReferenceInput.singleNorms().get(0).legalForce().region().id());

    assertThat(documentationUnitDTO.getNormReferences().get(1).getSingleNorm())
        .isEqualTo(normReferenceInput.singleNorms().get(1).singleNorm());
    assertThat(documentationUnitDTO.getNormReferences().get(1).getLegalForce()).isNotNull();
    assertThat(
            documentationUnitDTO
                .getNormReferences()
                .get(1)
                .getLegalForce()
                .getLegalForceType()
                .getId())
        .isEqualTo(normReferenceInput.singleNorms().get(1).legalForce().type().id());
    assertThat(documentationUnitDTO.getNormReferences().get(1).getLegalForce().getRegion().getId())
        .isEqualTo(normReferenceInput.singleNorms().get(1).legalForce().region().id());
  }

  @Test
  void testTransformToDTO_withMultipleNormReferences() {
    DocumentationUnitDTO currentDto = DocumentationUnitDTO.builder().build();
    NormReference normReferenceInput1 =
        NormReference.builder()
            .normAbbreviation(
                NormAbbreviation.builder()
                    .id(UUID.fromString("33333333-2222-3333-4444-555555555555"))
                    .build())
            .singleNorms(List.of(SingleNorm.builder().singleNorm("single norm 1").build()))
            .build();

    NormReference normReferenceInput2 =
        NormReference.builder()
            .normAbbreviation(
                NormAbbreviation.builder()
                    .id(UUID.fromString("33333333-2222-3333-4444-555555555555"))
                    .build())
            .singleNorms(
                List.of(
                    SingleNorm.builder()
                        .singleNorm("single norm 2")
                        .legalForce(
                            LegalForce.builder()
                                .type(LegalForceType.builder().id(UUID.randomUUID()).build())
                                .region(Region.builder().id(UUID.randomUUID()).build())
                                .build())
                        .build()))
            .build();

    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder()
                    .norms(List.of(normReferenceInput1, normReferenceInput2))
                    .build())
            .build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getNormReferences().get(0).getNormAbbreviation().getId())
        .isEqualTo(normReferenceInput1.normAbbreviation().id());
    assertThat(documentationUnitDTO.getNormReferences().get(0).getSingleNorm())
        .isEqualTo(normReferenceInput1.singleNorms().get(0).singleNorm());

    assertThat(documentationUnitDTO.getNormReferences().get(1).getNormAbbreviation().getId())
        .isEqualTo(normReferenceInput2.normAbbreviation().id());
    assertThat(documentationUnitDTO.getNormReferences().get(1).getSingleNorm())
        .isEqualTo(normReferenceInput2.singleNorms().get(0).singleNorm());
    assertThat(documentationUnitDTO.getNormReferences().get(1).getLegalForce()).isNotNull();
    assertThat(
            documentationUnitDTO
                .getNormReferences()
                .get(1)
                .getLegalForce()
                .getLegalForceType()
                .getId())
        .isEqualTo(normReferenceInput2.singleNorms().get(0).legalForce().type().id());
    assertThat(documentationUnitDTO.getNormReferences().get(1).getLegalForce().getRegion().getId())
        .isEqualTo(normReferenceInput2.singleNorms().get(0).legalForce().region().id());
  }

  @Test
  void testTransformToDTO_withNormReference_withoutNormAbbreviation_throwsException() {
    DocumentationUnitDTO currentDto = DocumentationUnitDTO.builder().build();
    NormReference normReferenceInput =
        NormReference.builder()
            .singleNorms(List.of(SingleNorm.builder().singleNorm("single norm 1").build()))
            .build();

    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .contentRelatedIndexing(
                ContentRelatedIndexing.builder().norms(List.of(normReferenceInput)).build())
            .build();

    Exception exception =
        assertThrows(
            DocumentUnitTransformerException.class,
            () -> {
              DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);
            });

    String expectedMessage = "Norm reference has no norm abbreviation, but is required.";
    String actualMessage = exception.getMessage();

    Assertions.assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  void testTransformToDTO_withLeadingDecisionNormReferences() {
    DocumentationUnitDTO currentDto = DocumentationUnitDTO.builder().build();
    List<String> leadingDecisionNormReferences = List.of("BGB §1", "BGB §2", "BGB §3");
    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .coreData(
                CoreData.builder()
                    .court(
                        Court.builder()
                            .id(UUID.fromString("CCCCCCCC-2222-3333-4444-55555555555"))
                            .type("BGH")
                            .build())
                    .leadingDecisionNormReferences(leadingDecisionNormReferences)
                    .build())
            .build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getLeadingDecisionNormReferences())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(
            LeadingDecisionNormReferenceDTO.builder().normReference("BGB §1").rank(1).build(),
            LeadingDecisionNormReferenceDTO.builder().normReference("BGB §2").rank(2).build(),
            LeadingDecisionNormReferenceDTO.builder().normReference("BGB §3").rank(3).build());
  }

  @Test
  void testTransformToDTO_deletedLeadingDecisionNormReferences() {
    DocumentationUnitDTO currentDto =
        DocumentationUnitDTO.builder()
            .court(
                CourtDTO.builder()
                    .id(UUID.fromString("CCCCCCCC-2222-3333-4444-55555555555"))
                    .type("BGH")
                    .build())
            .leadingDecisionNormReferences(
                List.of(
                    LeadingDecisionNormReferenceDTO.builder()
                        .id(UUID.fromString("CCCCCCCC-1111-3333-4444-55555555555"))
                        .normReference("BGB §1")
                        .rank(1)
                        .build()))
            .build();

    DocumentUnit updatedDomainObject =
        DocumentUnit.builder()
            .coreData(
                CoreData.builder()
                    .court(
                        Court.builder()
                            .id(UUID.fromString("CCCCCCCC-2222-3333-4444-55555555555"))
                            .type("BGH")
                            .build())
                    .leadingDecisionNormReferences(List.of())
                    .build())
            .build();

    DocumentationUnitDTO documentationUnitDTO =
        DocumentationUnitTransformer.transformToDTO(currentDto, updatedDomainObject);

    assertThat(documentationUnitDTO.getLeadingDecisionNormReferences()).isEmpty();
  }

  @Test
  void testTransformToDomain_withDocumentationUnitDTOIsNull_shouldReturnEmptyDocumentUnit() {

    assertThatThrownBy(() -> DocumentationUnitTransformer.transformToDomain(null))
        .isInstanceOf(DocumentUnitTransformerException.class)
        .hasMessageContaining("Document unit is null and won't transform");
  }

  @Test
  void testTransformToDomain_withRegion_shouldSetRegion() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder()
            .regions(List.of(RegionDTO.builder().code("region").build()))
            .build();
    DocumentUnit expected =
        generateSimpleDocumentUnitBuilder()
            .coreData(generateSimpleCoreDataBuilder().region("region").build())
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit).isEqualTo(expected);
  }

  @Test
  void testTransformToDomain_withLegalEffectYes_shouldSetLegalEffectToYes() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder().legalEffect(LegalEffectDTO.JA).build();
    DocumentUnit expected =
        generateSimpleDocumentUnitBuilder()
            .coreData(generateSimpleCoreDataBuilder().legalEffect("Ja").build())
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit).isEqualTo(expected);
  }

  @Test
  void testTransformToDomain_withLegalEffectNo_shouldSetLegalEffectToNo() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder().legalEffect(LegalEffectDTO.NEIN).build();
    DocumentUnit expected =
        generateSimpleDocumentUnitBuilder()
            .coreData(generateSimpleCoreDataBuilder().legalEffect("Nein").build())
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit).isEqualTo(expected);
  }

  @Test
  void testTransformToDomain_withLegalEffectNotSpecified_shouldSetLegalEffectToNotSpecified() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder().legalEffect(LegalEffectDTO.KEINE_ANGABE).build();
    DocumentUnit expected =
        generateSimpleDocumentUnitBuilder()
            .coreData(generateSimpleCoreDataBuilder().legalEffect("Keine Angabe").build())
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit).isEqualTo(expected);
  }

  @Test
  void testTransformToDomain_withLegalEffectWrongValue_shouldSetLegalEffectToNull() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder().legalEffect(LegalEffectDTO.FALSCHE_ANGABE).build();
    DocumentUnit expected =
        generateSimpleDocumentUnitBuilder()
            .coreData(generateSimpleCoreDataBuilder().build())
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit).isEqualTo(expected);
  }

  @Test
  void testTransformToDomain_withEnsuingAndPendingDecisionsWithoutARank_shouldAddItToTheEnd() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder()
            .ensuingDecisions(
                List.of(
                    EnsuingDecisionDTO.builder().note("ensuing with rank").rank(1).build(),
                    EnsuingDecisionDTO.builder().note("ensuing without rank").rank(0).build()))
            .pendingDecisions(
                List.of(
                    PendingDecisionDTO.builder().note("pending with rank").rank(2).build(),
                    PendingDecisionDTO.builder().note("pending without rank").rank(0).build()))
            .build();
    DocumentUnit expected =
        generateSimpleDocumentUnitBuilder()
            .coreData(generateSimpleCoreDataBuilder().build())
            .ensuingDecisions(
                List.of(
                    EnsuingDecision.builder().pending(false).note("ensuing with rank").build(),
                    EnsuingDecision.builder().pending(true).note("pending with rank").build(),
                    EnsuingDecision.builder().pending(false).note("ensuing without rank").build(),
                    EnsuingDecision.builder().pending(true).note("pending without rank").build()))
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit).isEqualTo(expected);
  }

  @Test
  void testTransformToDomain_textWithMultipleBorderNumberElements_shouldAddAllBorderNumbers() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder()
            .grounds(
                "lorem ipsum<border-number><number>1</number><content>foo</content></border-number> dolor sit amet <border-number><number>2</number><content>bar</content></border-number>")
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit.borderNumbers()).hasSize(2).containsExactly("1", "2");
  }

  @Test
  void testTransformToDomain_multipleTextsWithBorderNumberElements_shouldAddAllBorderNumbers() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder()
            .tenor(
                "lorem ipsum <border-number><number>1</number><content>foo</content></border-number>")
            .grounds(
                "dolor sit amet <border-number><number>2</number><content>bar</content></border-number>")
            .caseFacts(
                "consectetur <border-number><number>3</number><content>baz</content></border-number>")
            .decisionGrounds(
                "adipiscing <border-number><number>4</number><content>qux</content></border-number>")
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit.borderNumbers()).hasSize(4).containsExactly("1", "2", "3", "4");
  }

  @Test
  void testTransformToDomain_textWithoutBorderNumberElements_shouldNotAddBorderNumbers() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder().grounds("lorem ipsum dolor sit amet").build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit.borderNumbers()).isEmpty();
  }

  @Test
  void
      testTransformToDomain_textWithMalformedBorderNumberElement_shouldOnlyAddValidBorderNumbers() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder()
            .grounds(
                "lorem ipsum<border-number><content>foo</content></border-number> dolor sit amet <border-number><number>2</number><content>bar</content></border-number>")
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit.borderNumbers()).hasSize(1).containsExactly("2");
  }

  @Test
  void testTransformToDomain_shouldAddLeadingDecisionNormReferences() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder()
            .leadingDecisionNormReferences(
                List.of(
                    LeadingDecisionNormReferenceDTO.builder()
                        .normReference("BGB §1")
                        .rank(1)
                        .build(),
                    LeadingDecisionNormReferenceDTO.builder()
                        .normReference("BGB §2")
                        .rank(2)
                        .build(),
                    LeadingDecisionNormReferenceDTO.builder()
                        .normReference("BGB §3")
                        .rank(3)
                        .build()))
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit.coreData().leadingDecisionNormReferences())
        .hasSize(3)
        .containsExactly("BGB §1", "BGB §2", "BGB §3");
  }

  @Test
  void testTransformToDomain_shouldUseLatestStatus() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder()
            .status(
                List.of(
                    // was published yesterday
                    StatusDTO.builder()
                        .createdAt(Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS))
                        .publicationStatus(PublicationStatus.PUBLISHED)
                        .withError(true)
                        .build(),
                    // was unpublished now
                    StatusDTO.builder()
                        .createdAt(Instant.now())
                        .publicationStatus(PublicationStatus.UNPUBLISHED)
                        .withError(false)
                        .build()))
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit.status().publicationStatus()).isEqualTo(PublicationStatus.UNPUBLISHED);
    assertThat(documentUnit.status().withError()).isFalse();
  }

  @Test
  void testTransformToDomain_withMultipleNormReferences_withSameNormAbbreviation() {
    UUID normAbbreviationId = UUID.randomUUID();
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder()
            .normReferences(
                List.of(
                    NormReferenceDTO.builder()
                        .normAbbreviation(
                            NormAbbreviationDTO.builder().id(normAbbreviationId).build())
                        .singleNorm("single norm 1")
                        .build(),
                    NormReferenceDTO.builder()
                        .normAbbreviation(
                            NormAbbreviationDTO.builder().id(normAbbreviationId).build())
                        .singleNorm("single norm 2")
                        .build()))
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit.contentRelatedIndexing().norms()).hasSize(1);
    assertThat(documentUnit.contentRelatedIndexing().norms().get(0).normAbbreviation().id())
        .isEqualTo(normAbbreviationId);
    assertThat(
            documentUnit.contentRelatedIndexing().norms().get(0).singleNorms().get(0).singleNorm())
        .isEqualTo("single norm 1");
    assertThat(
            documentUnit.contentRelatedIndexing().norms().get(0).singleNorms().get(1).singleNorm())
        .isEqualTo("single norm 2");
  }

  @Test
  void testTransformToDomain_withMultipleNormReferences_withDifferentNormAbbreviation() {
    DocumentationUnitDTO documentationUnitDTO =
        generateSimpleDTOBuilder()
            .normReferences(
                List.of(
                    NormReferenceDTO.builder()
                        .normAbbreviation(
                            NormAbbreviationDTO.builder().id(UUID.randomUUID()).build())
                        .singleNorm("single norm 1")
                        .build(),
                    NormReferenceDTO.builder()
                        .normAbbreviation(
                            NormAbbreviationDTO.builder().id(UUID.randomUUID()).build())
                        .singleNorm("single norm 2")
                        .build()))
            .build();

    DocumentUnit documentUnit =
        DocumentationUnitTransformer.transformToDomain(documentationUnitDTO);

    assertThat(documentUnit.contentRelatedIndexing().norms()).hasSize(2);
    assertThat(documentUnit.contentRelatedIndexing().norms().get(0).normAbbreviation().id())
        .isEqualTo(documentationUnitDTO.getNormReferences().get(0).getNormAbbreviation().getId());
    assertThat(documentUnit.contentRelatedIndexing().norms().get(1).normAbbreviation().id())
        .isEqualTo(documentationUnitDTO.getNormReferences().get(1).getNormAbbreviation().getId());
    assertThat(
            documentUnit.contentRelatedIndexing().norms().get(0).singleNorms().get(0).singleNorm())
        .isEqualTo("single norm 1");
    assertThat(
            documentUnit.contentRelatedIndexing().norms().get(1).singleNorms().get(0).singleNorm())
        .isEqualTo("single norm 2");
  }

  private DocumentationUnitDTOBuilder generateSimpleDTOBuilder() {
    return DocumentationUnitDTO.builder()
        .documentationOffice(DocumentationOfficeDTO.builder().abbreviation("doc office").build());
  }

  private DocumentUnitBuilder generateSimpleDocumentUnitBuilder() {
    return DocumentUnit.builder()
        .previousDecisions(Collections.emptyList())
        .ensuingDecisions(Collections.emptyList())
        .texts(Texts.builder().build())
        .borderNumbers(Collections.emptyList())
        .attachments(Collections.emptyList())
        .contentRelatedIndexing(
            ContentRelatedIndexing.builder()
                .keywords(Collections.emptyList())
                .fieldsOfLaw(Collections.emptyList())
                .norms(Collections.emptyList())
                .activeCitations(Collections.emptyList())
                .build());
  }

  private CoreDataBuilder generateSimpleCoreDataBuilder() {
    return CoreData.builder()
        .documentationOffice(DocumentationOffice.builder().abbreviation("doc office").build())
        .fileNumbers(Collections.emptyList())
        .deviatingFileNumbers(Collections.emptyList())
        .deviatingCourts(Collections.emptyList())
        .previousProcedures(Collections.emptyList())
        .deviatingEclis(Collections.emptyList())
        .deviatingDecisionDates(Collections.emptyList())
        .inputTypes(Collections.emptyList())
        .leadingDecisionNormReferences(Collections.emptyList())
        .yearsOfDispute(Collections.emptySet());
  }
}
