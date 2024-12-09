package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** */
@Repository
public interface DatabaseDocumentTypeRepository extends JpaRepository<DocumentTypeDTO, UUID> {

  DocumentTypeDTO findFirstByAbbreviationAndCategory(
      String jurisShortcut, DocumentCategoryDTO category);

  List<DocumentTypeDTO> findAllByCategoryOrderByAbbreviationAscLabelAsc(
      DocumentCategoryDTO category);

  // see query explanation in CourtRepository, it's almost the same
  @Query(
      nativeQuery = true,
      value =
          "WITH label_added AS (SELECT *, "
              + " UPPER(CONCAT(abbreviation, ' ', label)) AS concat"
              + " from incremental_migration.document_type) "
              + "SELECT *,"
              + "       concat, "
              + "       CASE "
              + "           WHEN concat LIKE UPPER(:searchStr||'%') THEN 1 "
              + "           WHEN concat LIKE UPPER('% '||:searchStr||'%') THEN 2 "
              + "           WHEN concat LIKE UPPER('%-'||:searchStr||'%') THEN 2 "
              + "           ELSE 3 "
              + "           END AS weight "
              + "FROM label_added "
              + "WHERE concat LIKE UPPER('%'||:searchStr||'%') AND document_category_id = :category "
              + "ORDER BY weight, concat")
  List<DocumentTypeDTO> findCaselawBySearchStrAndCategory(
      @Param("searchStr") String searchStr, @Param("category") UUID category);

  @Query(
      nativeQuery = true,
      value =
          "SELECT * "
              + "FROM incremental_migration.document_type "
              + "WHERE (abbreviation = :searchStr OR label = :searchStr) "
              + "AND document_category_id = :category ")
  Optional<DocumentTypeDTO> findUniqueCaselawBySearchStrAndCategory(
      @Param("searchStr") String searchStr, @Param("category") UUID category);
}
