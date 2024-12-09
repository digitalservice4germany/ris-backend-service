package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseDocumentationUnitProcedureRepository
    extends JpaRepository<DocumentationUnitProcedureDTO, UUID> {

  @Query("SELECT pl FROM DocumentationUnitProcedureDTO pl ORDER BY pl.primaryKey.rank ASC LIMIT 1")
  DocumentationUnitProcedureDTO findFirstByDocumentationUnitOrderByRankDesc(
      DocumentationUnitDTO documentationUnitDTO);
}
