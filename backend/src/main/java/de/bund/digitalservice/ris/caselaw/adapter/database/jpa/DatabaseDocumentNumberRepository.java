package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotBlank;
import java.time.Year;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseDocumentNumberRepository extends JpaRepository<DocumentNumberDTO, String> {

  @NotNull
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<DocumentNumberDTO> findByDocumentationOfficeAbbreviationAndYear(
      @NotBlank String documentationOfficeAbbreviation, @NotNull Year year);
}
