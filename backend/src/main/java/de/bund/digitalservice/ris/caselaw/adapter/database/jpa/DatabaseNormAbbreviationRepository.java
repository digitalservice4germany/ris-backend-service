package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public interface DatabaseNormAbbreviationRepository
    extends JpaRepository<NormAbbreviationDTO, UUID> {

  List<NormAbbreviationDTO> findByAbbreviationIgnoreCase(String abbreviation, PageRequest of);

  List<NormAbbreviationDTO> findByOfficialLetterAbbreviationIgnoreCase(
      String officialLetterAbbreviation, PageRequest of);

  List<NormAbbreviationDTO> findByAbbreviationStartsWithIgnoreCase(
      String abbreviation, PageRequest of);

  List<NormAbbreviationDTO> findByOfficialLongTitleContainsIgnoreCase(
      String officialLongTitle, PageRequest of);

  List<NormAbbreviationDTO> findByOfficialLetterAbbreviationStartsWithIgnoreCase(
      String officialLetterAbbreviation, PageRequest size);

  @Query(
      value =
          "select"
              + " id,"
              + " abbreviation,"
              + " decision_date,"
              + " document_id,"
              + " document_number,"
              + " official_letter_abbreviation,"
              + " official_long_title,"
              + " official_short_title,"
              + " source,"
              + " region_id,"
              + " ts_rank_cd(weighted_vector, plainto_tsquery('german', '' || :tsQuery || '')) rank"
              + " from incremental_migration.norm_abbreviation_search_migration"
              + " where weighted_vector @@ plainto_tsquery('german', '' || :tsQuery || '')"
              + " order by rank desc"
              + " limit :size"
              + " offset :offset",
      nativeQuery = true)
  List<NormAbbreviationDTO> findByRankWeightedVector(String tsQuery, Integer size, Integer offset);

  @Transactional
  @Modifying
  @Query(
      value = "REFRESH MATERIALIZED VIEW incremental_migration.norm_abbreviation_search_migration",
      nativeQuery = true)
  void refreshMaterializedViews();
}
