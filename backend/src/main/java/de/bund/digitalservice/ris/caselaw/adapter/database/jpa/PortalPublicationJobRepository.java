package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PortalPublicationJobRepository
    extends JpaRepository<PortalPublicationJobDTO, UUID> {

  @Query(
      value =
          """
SELECT portalPublicationJob
FROM PortalPublicationJobDTO portalPublicationJob
WHERE portalPublicationJob.publicationStatus = 'PENDING'
ORDER BY portalPublicationJob.createdAt ASC
""")
  List<PortalPublicationJobDTO> findPendingJobsOrderedByCreationDate();
}
