package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitProcedureDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.ProcedureDTO;
import de.bund.digitalservice.ris.caselaw.domain.Procedure;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProcedureTransformer {
  public static Procedure transformFirstToDomain(
      List<DocumentationUnitProcedureDTO> procedureDTOs) {
    if (procedureDTOs == null || procedureDTOs.isEmpty()) return null;

    return transformToDomain(procedureDTOs.get(procedureDTOs.size() - 1).getProcedure());
  }

  public static List<String> transformPreviousProceduresToLabel(
      List<DocumentationUnitProcedureDTO> procedureDTOs) {
    if (procedureDTOs == null || procedureDTOs.size() < 2) {
      return Collections.emptyList();
    }

    return procedureDTOs.subList(0, procedureDTOs.size() - 1).stream()
        .map(DocumentationUnitProcedureDTO::getProcedure)
        .map(ProcedureDTO::getLabel)
        .toList();
  }

  private static Long getDocumentationUnitCount(ProcedureDTO procedureDTO) {
    if (procedureDTO.getDocumentationUnits() == null
        || procedureDTO.getDocumentationUnits().isEmpty()) {
      return 0L;
    }

    return procedureDTO.getDocumentationUnits().stream()
        .filter(
            documentationUnitDTO ->
                documentationUnitDTO
                    .getProcedures()
                    .get(documentationUnitDTO.getProcedures().size() - 1)
                    .getProcedure()
                    .equals(procedureDTO))
        .distinct()
        .count();
  }

  public static Procedure transformToDomain(ProcedureDTO procedureDTO) {
    if (procedureDTO == null) {
      return null;
    }

    return Procedure.builder()
        .id(procedureDTO.getId())
        .label(procedureDTO.getLabel())
        .createdAt(procedureDTO.getCreatedAt())
        .documentationUnitCount(getDocumentationUnitCount(procedureDTO))
        .userGroupId(extractUserGroupId(procedureDTO))
        .build();
  }

  private static UUID extractUserGroupId(ProcedureDTO procedureDTO) {
    if (procedureDTO.getUserGroupDTO() != null) {
      return procedureDTO.getUserGroupDTO().getId();
    }
    return null;
  }
}
