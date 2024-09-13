package de.bund.digitalservice.ris.caselaw.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class LegalPeriodicalEditionService {
  private final LegalPeriodicalEditionRepository legalPeriodicalRepository;

  public LegalPeriodicalEditionService(LegalPeriodicalEditionRepository legalPeriodicalRepository) {
    this.legalPeriodicalRepository = legalPeriodicalRepository;
  }

  public Optional<LegalPeriodicalEdition> getById(UUID id) {
    return legalPeriodicalRepository.findById(id);
  }

  public List<LegalPeriodicalEdition> getLegalPeriodicalEditions(UUID legalPeriodicalId) {
    return legalPeriodicalRepository.findAllByLegalPeriodicalId(legalPeriodicalId);
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public LegalPeriodicalEdition saveLegalPeriodicalEdition(
      LegalPeriodicalEdition legalPeriodicalEdition) {
    return legalPeriodicalRepository.save(legalPeriodicalEdition);
  }

  public boolean delete(UUID editionId) {
    var edition = legalPeriodicalRepository.findById(editionId);
    if (edition.isPresent() && edition.get().references().isEmpty()) {
      legalPeriodicalRepository.delete(edition.get());
      return true;
    }
    return false;
  }
}
