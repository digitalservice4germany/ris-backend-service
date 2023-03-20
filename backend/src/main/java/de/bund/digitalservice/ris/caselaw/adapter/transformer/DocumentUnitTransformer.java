package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DataSourceDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DocumentUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.proceedingdecision.ProceedingDecisionDTO;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DataSource;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnit;
import de.bund.digitalservice.ris.caselaw.domain.Texts;
import java.util.Collections;

public class DocumentUnitTransformer {
  private DocumentUnitTransformer() {}

  public static DocumentUnitDTO enrichDTO(
      DocumentUnitDTO documentUnitDTO, DocumentUnit documentUnit) {

    DataSourceDTO dataSourceDTO = DataSourceDTO.NEURIS;
    if (documentUnit.dataSource() == DataSource.MIGRATION) {
      dataSourceDTO = DataSourceDTO.MIGRATION;
    }

    DocumentUnitDTO.DocumentUnitDTOBuilder builder =
        documentUnitDTO.toBuilder()
            .uuid(documentUnit.uuid())
            .documentnumber(documentUnit.documentNumber())
            .creationtimestamp(documentUnit.creationtimestamp())
            .fileuploadtimestamp(documentUnit.fileuploadtimestamp())
            .dataSource(dataSourceDTO)
            .s3path(documentUnit.s3path())
            .filetype(documentUnit.filetype())
            .filename(documentUnit.filename());

    if (documentUnit.coreData() != null) {
      CoreData coreData = documentUnit.coreData();

      builder
          .procedure(coreData.procedure())
          .ecli(coreData.ecli())
          .appraisalBody(coreData.appraisalBody())
          .decisionDate(coreData.decisionDate())
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
          .procedure(null)
          .ecli(null)
          .appraisalBody(null)
          .decisionDate(null)
          .inputType(null)
          .center(null)
          .courtType(null)
          .courtLocation(null);
    }

    if (documentUnitDTO.getId() == null
        && documentUnit.proceedingDecisions() != null
        && !documentUnit.proceedingDecisions().isEmpty()) {

      throw new DocumentUnitTransformerException(
          "Transformation of a document unit with previous decisions only allowed by update. "
              + "Document unit must have a database id!");
    }

    if (documentUnit.proceedingDecisions() != null) {
      builder.proceedingDecisions(
          documentUnit.proceedingDecisions().stream()
              .map(
                  proceedingDecision ->
                      ProceedingDecisionDTO.builder()
                          //                          .id(proceedingDecision.id())
                          //
                          // .courtLocation(proceedingDecision.court().location())
                          //                          .courtType(proceedingDecision.court().type())
                          .fileNumber(proceedingDecision.fileNumber())
                          .decisionDate(proceedingDecision.date())
                          .build())
              .toList());
    } else {
      builder.proceedingDecisions(Collections.emptyList());
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
