package de.bund.digitalservice.ris.caselaw.domain;

import java.util.UUID;
import org.springframework.data.repository.NoRepositoryBean;

/** Domain repository for delta migration runs */
@NoRepositoryBean
public interface DeltaMigrationRepository {

  /**
   * Get the latest migration for a given document unit
   *
   * @param documentationUnitUuid the document unit UUID
   * @return the latest migration
   */
  DeltaMigration getLatestMigration(UUID documentationUnitUuid);
}
