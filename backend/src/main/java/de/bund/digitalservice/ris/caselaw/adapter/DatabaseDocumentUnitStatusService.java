package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseStatusRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.StatusDTO;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitRepository;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitStatus;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitStatusService;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DatabaseDocumentUnitStatusService implements DocumentUnitStatusService {

  private final DatabaseStatusRepository repository;
  private final DocumentUnitRepository documentUnitRepository;
  private final DatabaseDocumentationUnitRepository databaseDocumentationUnitRepository;

  public DatabaseDocumentUnitStatusService(
      DatabaseStatusRepository repository,
      DatabaseDocumentationUnitRepository databaseDocumentationUnitRepository,
      DocumentUnitRepository documentUnitRepository) {
    this.repository = repository;
    this.documentUnitRepository = documentUnitRepository;
    this.databaseDocumentationUnitRepository = databaseDocumentationUnitRepository;
  }

  @Override
  public Mono<DocumentUnit> setInitialStatus(DocumentUnit documentUnit) {
    return Mono.just(
            repository.save(
                StatusDTO.builder()
                    .createdAt(Instant.now())
                    .documentationUnitDTO(
                        databaseDocumentationUnitRepository.getReferenceById(documentUnit.uuid()))
                    .publicationStatus(PublicationStatus.UNPUBLISHED)
                    .withError(false)
                    .build()))
        .then(documentUnitRepository.findByUuid(documentUnit.uuid()));
  }

  @Override
  public Mono<DocumentUnit> setToPublishing(
      DocumentUnit documentUnit, Instant publishDate, String issuerAddress) {
    return Mono.just(
            repository.save(
                StatusDTO.builder()
                    .createdAt(publishDate)
                    .documentationUnitDTO(
                        databaseDocumentationUnitRepository.getReferenceById(documentUnit.uuid()))
                    .publicationStatus(PublicationStatus.PUBLISHING)
                    .withError(false)
                    .issuerAddress(issuerAddress)
                    .build()))
        .then(documentUnitRepository.findByUuid(documentUnit.uuid()));
  }

  @Override
  public Mono<Void> update(String documentNumber, DocumentUnitStatus status) {
    return getLatestPublishing(documentNumber)
        .flatMap(previousStatusDTO -> saveStatus(status, previousStatusDTO))
        .then();
  }

  @Override
  public Mono<Void> update(UUID documentUuid, DocumentUnitStatus status) {
    return getLatestPublishing(documentUuid)
        .flatMap(previousStatusDTO -> saveStatus(status, previousStatusDTO))
        .then();
  }

  @NotNull
  private Mono<StatusDTO> saveStatus(DocumentUnitStatus status, StatusDTO previousStatusDTO) {

    return Mono.just(
        repository.save(
            StatusDTO.builder()
                .createdAt(Instant.now())
                .documentationUnitDTO(previousStatusDTO.getDocumentationUnitDTO())
                .issuerAddress(previousStatusDTO.getIssuerAddress())
                .publicationStatus(status.publicationStatus())
                .withError(status.withError())
                .build()));
  }

  public Mono<String> getLatestIssuerAddress(String documentNumber) {
    return getLatestPublishing(documentNumber).map(StatusDTO::getIssuerAddress);
  }

  @Override
  public Mono<PublicationStatus> getLatestStatus(UUID documentUuid) {
    return Mono.just(
            repository.findFirstByDocumentationUnitDTOOrderByCreatedAtDesc(
                databaseDocumentationUnitRepository.getReferenceById(documentUuid)))
        .map(StatusDTO::getPublicationStatus);
  }

  private Mono<StatusDTO> getLatestPublishing(String documentNumber) {
    return documentUnitRepository
        .findByDocumentNumber(documentNumber)
        .flatMap(
            documentUnit -> {
              if (documentUnit == null || documentUnit.uuid() == null) {
                return Mono.empty();
              }
              return Mono.just(
                  Objects.requireNonNull(
                      databaseDocumentationUnitRepository
                          .findByDocumentNumber(documentNumber)
                          .map(
                              documentationUnitDTO ->
                                  repository
                                      .findFirstByDocumentationUnitDTOAndPublicationStatusOrderByCreatedAtDesc(
                                          documentationUnitDTO, PublicationStatus.PUBLISHING))
                          .orElse(null)));
            });
  }

  private Mono<StatusDTO> getLatestPublishing(UUID documentUuid) {
    return Mono.just(
        repository.findFirstByDocumentationUnitDTOAndPublicationStatusOrderByCreatedAtDesc(
            databaseDocumentationUnitRepository.getReferenceById(documentUuid),
            PublicationStatus.PUBLISHING));
  }
}
