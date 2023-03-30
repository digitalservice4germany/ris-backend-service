package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.proceedingdecision.ProceedingDecisionLinkDTO;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface DocumentUnitRepository {

  Mono<DocumentUnit> findByDocumentNumber(String documentNumber);

  Mono<DocumentUnit> findByUuid(UUID uuid);

  Mono<DocumentUnit> createNewDocumentUnit(String documentNumber);

  Mono<DocumentUnit> save(DocumentUnit documentUnit);

  Mono<DocumentUnit> attachFile(
      UUID documentUnitUuid, String fileUuid, String type, String fileName);

  Mono<DocumentUnit> removeFile(UUID documentUnitId);

  Mono<Void> delete(DocumentUnit documentUnit);

  Flux<ProceedingDecision> searchForDocumentUnityByProceedingDecisionInput(
      ProceedingDecision proceedingDecision);

  Flux<DocumentUnitListEntry> findAll(Sort sort);

  Flux<ProceedingDecision> findAllLinkedDocumentUnits(UUID parentDocumentUnitUuid);

  Mono<ProceedingDecisionLinkDTO> linkDocumentUnits(
      UUID parentDocumentUnitUuid, UUID childDocumentUnitUuid);

  Mono<Void> unlinkDocumentUnits(UUID parentDocumentUnitUuid, UUID childDocumentUnitUuid);
}
