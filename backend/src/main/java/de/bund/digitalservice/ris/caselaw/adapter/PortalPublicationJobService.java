package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PortalPublicationJobDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.PortalPublicationJobRepository;
import de.bund.digitalservice.ris.caselaw.domain.PortalPublicationTaskStatus;
import de.bund.digitalservice.ris.caselaw.domain.PortalPublicationTaskType;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PortalPublicationJobService {

  private final PortalPublicationJobRepository publicationJobRepository;
  // TODO: Should use external exporter service (-> different bucket)
  private final LdmlExporterService ldmlExporterService;

  public PortalPublicationJobService(
      PortalPublicationJobRepository publicationJobRepository,
      LdmlExporterService ldmlExporterService) {
    this.publicationJobRepository = publicationJobRepository;
    this.ldmlExporterService = ldmlExporterService;
  }

  @Scheduled(fixedDelayString = "PT5S")
  @SchedulerLock(name = "portal-publication-job", lockAtMostFor = "PT1H")
  public void executePendingJobs() {
    List<PortalPublicationJobDTO> pendingJobs =
        publicationJobRepository.findPendingJobsOrderedByCreationDate();
    if (pendingJobs.isEmpty()) {
      return;
    }

    log.info("Executing {} portal publication jobs", pendingJobs.size());
    for (PortalPublicationJobDTO job : pendingJobs) {
      executeJob(job);
    }
    var publicationResult = publishChangelog(pendingJobs);
    publicationJobRepository.saveAll(pendingJobs);

    log.info(
        "Portal publication jobs successfully executed: {} units published, {} units deleted.",
        publicationResult.publishedCount,
        publicationResult.deletedCount);
  }

  private void executeJob(PortalPublicationJobDTO job) {
    if (job.getPublicationType() == PortalPublicationTaskType.PUBLISH) {
      try {
        this.ldmlExporterService.publishDocumentationUnit(job.getDocumentNumber());
        job.setPublicationStatus(PortalPublicationTaskStatus.SUCCESS);
      } catch (Exception e) {
        log.error("Could not publish documentation unit {}", job.getDocumentNumber(), e);
        job.setPublicationStatus(PortalPublicationTaskStatus.ERROR);
      }
    }

    if (job.getPublicationType() == PortalPublicationTaskType.DELETE) {
      try {
        this.ldmlExporterService.deleteDocumentationUnit(job.getDocumentNumber());
        job.setPublicationStatus(PortalPublicationTaskStatus.SUCCESS);
      } catch (Exception e) {
        log.error("Could not unpublish documentation unit {}", job.getDocumentNumber(), e);
        job.setPublicationStatus(PortalPublicationTaskStatus.ERROR);
      }
    }
  }

  private PublicationResult publishChangelog(List<PortalPublicationJobDTO> pendingJobs) {
    List<String> publishDocNumbers =
        pendingJobs.stream()
            .filter(job -> job.getPublicationType() == PortalPublicationTaskType.PUBLISH)
            .filter(job -> job.getPublicationStatus() == PortalPublicationTaskStatus.SUCCESS)
            .map(PortalPublicationJobDTO::getDocumentNumber)
            .toList();
    List<String> deletedDocNumbers =
        pendingJobs.stream()
            .filter(job -> job.getPublicationType() == PortalPublicationTaskType.DELETE)
            .filter(job -> job.getPublicationStatus() == PortalPublicationTaskStatus.SUCCESS)
            .map(PortalPublicationJobDTO::getDocumentNumber)
            .toList();

    if (!publishDocNumbers.isEmpty() || !deletedDocNumbers.isEmpty()) {
      try {
        this.ldmlExporterService.uploadChangelog(publishDocNumbers, deletedDocNumbers);
      } catch (Exception e) {
        log.error("Could not upload changelog file.", e);
      }
    }

    return new PublicationResult(publishDocNumbers.size(), deletedDocNumbers.size());
  }

  private record PublicationResult(int publishedCount, int deletedCount) {}
}
