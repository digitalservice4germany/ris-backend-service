package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.adapter.transformer.LegalPeriodicalEditionTransformer;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresLegalPeriodicalEditionRepositoryImpl
    implements LegalPeriodicalEditionRepository {
  private final DatabaseLegalPeriodicalEditionRepository repository;

  public PostgresLegalPeriodicalEditionRepositoryImpl(
      DatabaseLegalPeriodicalEditionRepository repository) {
    this.repository = repository;
  }

  public Optional<LegalPeriodicalEdition> findById(UUID id) {
    return repository.findById(id).map(LegalPeriodicalEditionTransformer::transformToDomain);
  }

  @Override
  public List<LegalPeriodicalEdition> findAllByLegalPeriodicalId(UUID legalPeriodicalId) {
    return repository.findAllByLegalPeriodicalId(legalPeriodicalId).stream()
        .map(LegalPeriodicalEditionTransformer::transformToDomain)
        .toList();
  }

  @Override
  public LegalPeriodicalEdition save(LegalPeriodicalEdition legalPeriodicalEdition) {
    return LegalPeriodicalEditionTransformer.transformToDomain(
        repository.save(LegalPeriodicalEditionTransformer.transformToDTO(legalPeriodicalEdition)));
  }
}
