package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DocumentUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.PreviousDecisionDTO;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnit;
import de.bund.digitalservice.ris.caselaw.domain.Texts;
import java.util.Collections;

public class DocumentUnitTransformer {
  private DocumentUnitTransformer() {}

  public static DocumentUnitDTO enrichDTO(
      DocumentUnitDTO documentUnitDTO, DocumentUnit documentUnit) {

    DocumentUnitDTO.DocumentUnitDTOBuilder builder =
        documentUnitDTO.toBuilder()
            .uuid(documentUnit.uuid())
            .documentnumber(documentUnit.documentNumber())
            .creationtimestamp(documentUnit.creationtimestamp())
            .fileuploadtimestamp(documentUnit.fileuploadtimestamp())
            .s3path(documentUnit.s3path())
            .filetype(documentUnit.filetype())
            .filename(documentUnit.filename());

    if (documentUnit.coreData() != null) {
      CoreData coreData = documentUnit.coreData();

      builder
          .category(coreData.category())
          .procedure(coreData.procedure())
          .ecli(coreData.ecli())
          .appraisalBody(coreData.appraisalBody())
          .decisionDate(coreData.decisionDate() != null ? coreData.decisionDate().toString() : null)
          .legalEffect(coreData.legalEffect())
          .inputType(coreData.inputType())
          .center(coreData.center());

      if (coreData.court() != null) {
        builder
            .courtType(documentUnit.coreData().court().type())
            .courtLocation(coreData.court().location());
      } else {
        builder.courtType(null);
        builder.courtLocation(null);
      }
    } else {
      builder
          .category(null)
          .procedure(null)
          .ecli(null)
          .appraisalBody(null)
          .decisionDate(null)
          .legalEffect(null)
          .inputType(null)
          .center(null)
          .courtType(null)
          .courtLocation(null);
    }

    if (documentUnitDTO.getId() == null
        && documentUnit.previousDecisions() != null
        && !documentUnit.previousDecisions().isEmpty()) {

      throw new DocumentUnitTransformerException(
          "Transformation of a document unit with previous decisions only allowed by update. "
              + "Document unit must have a database id!");
    }

    if (documentUnit.previousDecisions() != null) {
      builder.previousDecisions(
          documentUnit.previousDecisions().stream()
              .map(
                  previousDecision ->
                      PreviousDecisionDTO.builder()
                          .id(previousDecision.id())
                          .documentUnitId(documentUnitDTO.getId())
                          .courtLocation(previousDecision.courtPlace())
                          .courtType(previousDecision.courtType())
                          .fileNumber(previousDecision.fileNumber())
                          .decisionDate(previousDecision.date())
                          .build())
              .toList());
    } else {
      builder.previousDecisions(Collections.emptyList());
    }

    if (documentUnit.texts() != null) {
      Texts texts = documentUnit.texts();

      builder
          .decisionName(texts.decisionName())
          .headline(texts.headline())
          .guidingPrinciple(texts.guidingPrinciple())
          .headnote(texts.headnote())
          .tenor(texts.tenor())
          .reasons(texts.reasons())
          .caseFacts(texts.caseFacts())
          .decisionReasons(texts.decisionReasons());
    } else {
      builder
          .decisionName(null)
          .headline(null)
          .guidingPrinciple(null)
          .headnote(null)
          .tenor(null)
          .reasons(null)
          .caseFacts(null)
          .decisionReasons(null);
    }

    return builder.build();
  }
}
