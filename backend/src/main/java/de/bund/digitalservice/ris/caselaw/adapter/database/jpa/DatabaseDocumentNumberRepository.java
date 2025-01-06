package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import java.time.Year;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseDocumentNumberRepository extends JpaRepository<DocumentNumberDTO, String> {

  Optional<DocumentNumberDTO> findByDocumentationOfficeAbbreviationAndYear(
      String documentationOfficeAbbreviation, Year year);
}
