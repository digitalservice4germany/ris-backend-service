package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.domain.NormElement;
import de.bund.digitalservice.ris.caselaw.domain.NormElementRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresNormElementRepositoryImpl implements NormElementRepository {

  private final DatabaseNormElementRepository normElementRepository;

  public PostgresNormElementRepositoryImpl(DatabaseNormElementRepository normElementRepository) {
    this.normElementRepository = normElementRepository;
  }

  @Override
  public List<NormElement> findAllByDocumentCategoryLabelR() {
    return normElementRepository.findAllByDocumentCategoryLabelR().stream()
        .map(
            normElementDTO ->
                NormElement.builder()
                    .label(normElementDTO.getLabel())
                    .hasNumberDesignation(normElementDTO.isHasNumberDesignation())
                    .normCode(normElementDTO.getNormCode())
                    .build())
        .toList();
  }
}
