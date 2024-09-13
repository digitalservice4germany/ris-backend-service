package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseLegalPeriodicalRepository extends JpaRepository<LegalPeriodicalDTO, UUID> {
  LegalPeriodicalDTO findByAbbreviation(String abbreviation);

  @Query(
      """
  SELECT ct FROM LegalPeriodicalDTO ct
  WHERE LOWER(ct.abbreviation) LIKE COALESCE(concat(LOWER(:searchStr), '%'), '%')
  ORDER BY ct.abbreviation
  LIMIT 15
""")
  List<LegalPeriodicalDTO> findBySearchStr(@Param("searchStr") Optional<String> searchStr);
}
