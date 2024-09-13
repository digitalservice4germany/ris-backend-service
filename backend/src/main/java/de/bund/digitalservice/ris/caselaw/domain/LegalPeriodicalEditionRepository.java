package de.bund.digitalservice.ris.caselaw.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.NoRepositoryBean;

/** Domain repository for legal periodical editions */
@NoRepositoryBean
public interface LegalPeriodicalEditionRepository {

  Optional<LegalPeriodicalEdition> findById(UUID id);

  List<LegalPeriodicalEdition> findAllByLegalPeriodicalId(UUID legalPeriodicalId);

  LegalPeriodicalEdition save(LegalPeriodicalEdition legalPeriodicalEdition);

  void delete(LegalPeriodicalEdition legalPeriodicalEdition);
}
