package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.NormAbbreviationDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.NormReferenceDTO;
import de.bund.digitalservice.ris.caselaw.domain.NormReference;
import de.bund.digitalservice.ris.caselaw.domain.SingleNorm;
import de.bund.digitalservice.ris.caselaw.domain.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for transforming NormReference objects between DTOs (Data Transfer Objects) and
 * domain objects.
 */
public class NormReferenceTransformer {

  private NormReferenceTransformer() {
    // Private constructor to prevent instantiation of this utility class.
  }

  /**
   * Transforms a NormReferenceDTO (Data Transfer Object) into a {@link NormReference} domain
   * object, before all NormReferences with the same normAbbreviation are being grouped into one
   * NormReference object in the {@link DocumentationUnitTransformer}
   *
   * @param normDTO The NormReferenceDTO to be transformed.
   * @return The NormReference domain object representing the transformed NormReferenceDTO.
   */
  public static NormReference transformToDomain(NormReferenceDTO normDTO) {
    List<SingleNorm> list = new ArrayList<>();
    SingleNorm singleNorm = SingleNormTransformer.transformToDomain(normDTO);

    if (singleNorm != null) {
      list.add(singleNorm);
    }

    return NormReference.builder()
        .normAbbreviation(
            NormAbbreviationTransformer.transformToDomain(normDTO.getNormAbbreviation()))
        .normAbbreviationRawValue(normDTO.getNormAbbreviationRawValue())
        .singleNorms(list)
        .build();
  }

  /**
   * Transforms a NormReference domain object into a list of NormReferenceDTOs (Data Transfer
   * Objects). When converting into a DTO object, each single norm in a normReference is converted
   * into its own {@link NormReferenceDTO}.
   *
   * @param normReference The NormReference object to be transformed.
   * @return A list of NormReferenceDTOs representing the transformed NormReference object.
   */
  public static List<NormReferenceDTO> transformToDTO(NormReference normReference) {
    if (normReference == null) {
      return Collections.emptyList();
    }

    if (normReference.normAbbreviation() == null
        && normReference.normAbbreviationRawValue() == null) {
      throw new DocumentationUnitTransformerException(
          "Norm reference has no norm abbreviation, but is required.");
    }

    NormAbbreviationDTO normAbbreviationDTO =
        normReference.normAbbreviation() != null
            ? NormAbbreviationDTO.builder().id(normReference.normAbbreviation().id()).build()
            : null;

    String normAbbreviationRawValue =
        normReference.normAbbreviationRawValue() != null
            ? normReference.normAbbreviationRawValue()
            : null;

    if (normReference.singleNorms() == null || normReference.singleNorms().isEmpty()) {
      return List.of(
          NormReferenceDTO.builder()
              .normAbbreviation(normAbbreviationDTO)
              .normAbbreviationRawValue(normAbbreviationRawValue)
              .build());
    }

    return normReference.singleNorms().stream()
        .distinct()
        .map(
            singleNorm -> {
              var builder =
                  NormReferenceDTO.builder()
                      .id(singleNorm.id())
                      .normAbbreviation(normAbbreviationDTO)
                      .normAbbreviationRawValue(
                          StringUtils.normalizeSpace(normAbbreviationRawValue))
                      .singleNorm(StringUtils.normalizeSpace(singleNorm.singleNorm()))
                      .dateOfVersion(singleNorm.dateOfVersion())
                      .dateOfRelevance(singleNorm.dateOfRelevance());

              if (singleNorm.legalForce() != null) {
                builder.legalForce(LegalForceTransformer.transformToDTO(singleNorm.legalForce()));
              }

              return builder.build();
            })
        .toList();
  }
}
