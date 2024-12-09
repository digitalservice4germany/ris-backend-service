package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.domain.DeltaMigration;
import de.bund.digitalservice.ris.caselaw.domain.DeltaMigrationRepository;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDeltaMigrationRepositoryImpl implements DeltaMigrationRepository {

  private final OriginalXmlRepository dbRepository;

  public PostgresDeltaMigrationRepositoryImpl(OriginalXmlRepository dbRepository) {
    this.dbRepository = dbRepository;
  }

  @Override
  public DeltaMigration getLatestMigration(UUID documentationUnitUuid) {

    return dbRepository
        .findByDocumentationUnitId(documentationUnitUuid)
        .map(
            originalXmlDTO ->
                DeltaMigration.builder()
                    .xml(originalXmlDTO.getContent())
                    .migratedDate(originalXmlDTO.getUpdatedAt())
                    .build())
        .orElse(null);
  }
}
