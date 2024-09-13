package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.adapter.transformer.LegalPeriodicalEditionTransformer;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresLegalPeriodicalEditionRepositoryImpl
    implements LegalPeriodicalEditionRepository {
  private final DatabaseLegalPeriodicalEditionRepository repository;

  public PostgresLegalPeriodicalEditionRepositoryImpl(
      DatabaseLegalPeriodicalEditionRepository repository) {
    this.repository = repository;
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public Optional<LegalPeriodicalEdition> findById(UUID id) {
    return repository.findById(id).map(LegalPeriodicalEditionTransformer::transformToDomain);
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public List<LegalPeriodicalEdition> findAllByLegalPeriodicalId(UUID legalPeriodicalId) {
    return repository.findAllByLegalPeriodicalIdOrderByCreatedAtDesc(legalPeriodicalId).stream()
        .map(LegalPeriodicalEditionTransformer::transformToDomain)
        .toList();
  }

  public LegalPeriodicalEdition save(LegalPeriodicalEdition legalPeriodicalEdition) {
    return LegalPeriodicalEditionTransformer.transformToDomain(
        repository.save(LegalPeriodicalEditionTransformer.transformToDTO(legalPeriodicalEdition)));
  }

  @Override
  public void delete(LegalPeriodicalEdition legalPeriodicalEdition) {
    repository.deleteById(legalPeriodicalEdition.id());
  }
}
