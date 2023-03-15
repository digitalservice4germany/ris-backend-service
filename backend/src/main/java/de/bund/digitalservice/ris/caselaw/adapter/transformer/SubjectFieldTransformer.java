package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.JPAFieldOfLawDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.JPAKeywordDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.JPANormDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.lookuptable.FieldOfLawDTO;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.subjectfield.FieldOfLaw;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.subjectfield.FieldOfLawXml;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.subjectfield.Keyword;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.subjectfield.Norm;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.subjectfield.NormXml;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SubjectFieldTransformer {
  private SubjectFieldTransformer() {}

  public static FieldOfLaw transformToDomain(FieldOfLawDTO fieldOfLawDTO) {
    List<Keyword> keywords = null;
    if (fieldOfLawDTO.getKeywords() != null) {
      keywords =
          fieldOfLawDTO.getKeywords().stream()
              .map(keywordDTO -> Keyword.builder().value(keywordDTO.getValue()).build())
              .toList();
    }

    List<Norm> norms = null;
    if (fieldOfLawDTO.getNorms() != null) {
      norms =
          fieldOfLawDTO.getNorms().stream()
              .map(
                  normDTO ->
                      Norm.builder()
                          .abbreviation(normDTO.getAbbreviation())
                          .singleNormDescription(normDTO.getSingleNormDescription())
                          .build())
              .toList();
    }

    List<String> linkedFields = null;
    if (fieldOfLawDTO.getLinkedFields() != null) {
      linkedFields =
          fieldOfLawDTO.getLinkedFields().stream()
              .map(FieldOfLawDTO::getSubjectFieldNumber)
              .toList();
    }

    return FieldOfLaw.builder()
        .id(fieldOfLawDTO.getId())
        .childrenCount(fieldOfLawDTO.getChildrenCount())
        .identifier(fieldOfLawDTO.getSubjectFieldNumber())
        .text(fieldOfLawDTO.getSubjectFieldText())
        .linkedFields(linkedFields)
        .keywords(keywords)
        .norms(norms)
        .children(new ArrayList<>())
        .build();
  }

  public static JPAFieldOfLawDTO transformToJPADTO(FieldOfLawXml fieldOfLawXml) {
    return JPAFieldOfLawDTO.builder()
        .id(fieldOfLawXml.getId())
        .changeDateMail(fieldOfLawXml.getChangeDateMail())
        .changeDateClient(fieldOfLawXml.getChangeDateClient())
        .changeIndicator(fieldOfLawXml.getChangeIndicator())
        .version(fieldOfLawXml.getVersion())
        .subjectFieldNumber(fieldOfLawXml.getSubjectFieldNumber())
        .subjectFieldText(fieldOfLawXml.getSubjectFieldText())
        .navigationTerm(fieldOfLawXml.getNavigationTerm())
        .keywords(transformKeywordsToJPADTOs(fieldOfLawXml.getKeywords()))
        .norms(transformNormsToJPADTOs(fieldOfLawXml.getNorms()))
        .build();
  }

  private static Set<JPAKeywordDTO> transformKeywordsToJPADTOs(Set<String> keywordXmls) {
    if (keywordXmls == null) {
      return null;
    }

    Set<JPAKeywordDTO> jpaKeywordDTOs = new HashSet<>();
    keywordXmls.forEach(
        keyword -> jpaKeywordDTOs.add(JPAKeywordDTO.builder().value(keyword).build()));
    return jpaKeywordDTOs;
  }

  private static Set<JPANormDTO> transformNormsToJPADTOs(Set<NormXml> normXmls) {
    if (normXmls == null) {
      return null;
    }

    Set<JPANormDTO> jpaNormDTOs = new HashSet<>();
    normXmls.forEach(
        normXml ->
            jpaNormDTOs.add(
                JPANormDTO.builder()
                    .abbreviation(normXml.getAbbreviation())
                    .singleNormDescription(normXml.getSingleNormDescription())
                    .build()));
    return jpaNormDTOs;
  }
}
