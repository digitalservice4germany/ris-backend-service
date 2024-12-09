package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.adapter.transformer.LegalPeriodicalTransformer;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalRepository;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresLegalPeriodicalRepositoryImpl implements LegalPeriodicalRepository {
  private final DatabaseLegalPeriodicalRepository repository;

  public PostgresLegalPeriodicalRepositoryImpl(DatabaseLegalPeriodicalRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<LegalPeriodical> findAllBySearchStr(Optional<String> searchStr) {
    return repository.findBySearchStr(searchStr).stream()
        .map(LegalPeriodicalTransformer::transformToDomain)
        .toList();
  }
}
