package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.adapter.transformer.DependentLiteratureTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.LegalPeriodicalEditionTransformer;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.ReferenceTransformer;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEdition;
import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalEditionRepository;
import de.bund.digitalservice.ris.caselaw.domain.Reference;
import de.bund.digitalservice.ris.caselaw.domain.ReferenceType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresLegalPeriodicalEditionRepositoryImpl
    implements LegalPeriodicalEditionRepository {
  private final DatabaseLegalPeriodicalEditionRepository repository;
  private final DatabaseDocumentationUnitRepository documentationUnitRepository;
  private final DatabaseReferenceRepository referenceRepository;
  private final DatabaseDependentLiteratureCitationRepository dependentLiteratureCitationRepository;

  public PostgresLegalPeriodicalEditionRepositoryImpl(
      DatabaseLegalPeriodicalEditionRepository repository,
      DatabaseDocumentationUnitRepository documentationUnitRepository,
      DatabaseReferenceRepository referenceRepository,
      DatabaseDependentLiteratureCitationRepository dependentLiteratureCitationRepository) {
    this.repository = repository;
    this.documentationUnitRepository = documentationUnitRepository;
    this.referenceRepository = referenceRepository;
    this.dependentLiteratureCitationRepository = dependentLiteratureCitationRepository;
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public Optional<LegalPeriodicalEdition> findById(UUID id) {
    return repository
        .findById(id)
        .map(
            edition ->
                LegalPeriodicalEditionTransformer.transformToDomain(edition).toBuilder()
                    .references(createReferenceListFromDTO(edition))
                    .build());
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public List<LegalPeriodicalEdition> findAllByLegalPeriodicalId(UUID legalPeriodicalId) {
    return repository.findAllByLegalPeriodicalIdOrderByCreatedAtDesc(legalPeriodicalId).stream()
        .map(
            edition ->
                LegalPeriodicalEditionTransformer.transformToDomain(edition).toBuilder()
                    .references(createReferenceListFromDTO(edition))
                    .build())
        .toList();
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public LegalPeriodicalEdition save(LegalPeriodicalEdition legalPeriodicalEdition) {

    // create new lists of references and literature citation DTOs to save using common rank
    List<ReferenceDTO> referenceDTOS = new ArrayList<>();
    List<DependentLiteratureCitationDTO> dependentLiteratureCitationDTOS = new ArrayList<>();

    AtomicInteger editionRank = new AtomicInteger(1);
    for (Reference reference :
        Optional.ofNullable(legalPeriodicalEdition.references()).orElse(List.of())) {

      var docUnit =
          documentationUnitRepository.findByDocumentNumber(
              reference.documentationUnit().getDocumentNumber());

      if (docUnit.isEmpty()) {
        continue;
      }
      if (reference.referenceType().equals(ReferenceType.CASELAW)) {
        referenceDTOS.add(
            saveReference(reference, docUnit.get()).toBuilder()
                // add transient edition rank to be used for saving in edition later on
                .editionRank(editionRank.getAndIncrement())
                .build());
      }
      if (reference.referenceType().equals(ReferenceType.LITERATURE)) {
        dependentLiteratureCitationDTOS.add(
            saveLiteratureCitation(reference, docUnit.get()).toBuilder()
                // add transient edition rank to be used for saving in edition later on
                .editionRank(editionRank.getAndIncrement())
                .build());
      }
    }

    // create edition DTO and assign references and literature citation lists
    var editionDTO = LegalPeriodicalEditionTransformer.transformToDTO(legalPeriodicalEdition);
    editionDTO.setReferences(referenceDTOS);
    editionDTO.setLiteratureCitations(dependentLiteratureCitationDTOS);

    // remove references deleted in edition from DocumentationUnit
    removeDeletedReferences(legalPeriodicalEdition);

    return LegalPeriodicalEditionTransformer.transformToDomain(repository.save(editionDTO))
        .toBuilder()
        .references(createReferenceListFromDTO(editionDTO))
        .build();
  }

  /**
   * Manually fetch the references and literature citations of the editionDTO from the DB using
   * reference and literatureCitation repositories and transform them into reference domain objects
   * while maintaining the transient editionRank. This is not possible for the edition transformer
   * via JPA fetching, because the edition does not link to the references or literature citations
   * via JPA (for they are completely unrelated entities), but only through their id.
   *
   * @param editionDTO the DTO to create the reference list from
   * @return the list of references (containing references and literature citations) ordered by rank
   */
  private ArrayList<Reference> createReferenceListFromDTO(LegalPeriodicalEditionDTO editionDTO) {
    ArrayList<Reference> references = new ArrayList<>();

    if (editionDTO.getReferences() != null) {
      references.addAll(
          editionDTO.getReferences().entrySet().stream()
              .map(
                  entry ->
                      referenceRepository
                          .findById(entry.getKey())
                          .map(dto -> dto.toBuilder().editionRank(entry.getValue()).build())
                          .orElse(null))
              .map(ReferenceTransformer::transformToDomain)
              .toList());
    }

    if (editionDTO.getLiteratureCitations() != null) {
      references.addAll(
          editionDTO.getLiteratureCitations().entrySet().stream()
              .map(
                  entry ->
                      dependentLiteratureCitationRepository
                          .findById(entry.getKey())
                          .map(dto -> dto.toBuilder().editionRank(entry.getValue()).build())
                          .orElse(null))
              .map(DependentLiteratureTransformer::transformToDomain)
              .toList());
    }
    return references.stream()
        .filter(Objects::nonNull)
        .sorted(Comparator.comparingInt(Reference::rank))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * Delete references removed in legal periodical evaluation from their DocumentationUnit to
   * prevent reference orphans and ensure a source relationship is removed if exists. The references
   * to delete are identified by comparing the last saved edition to the updated one.
   *
   * @param updatedEdition the new version of the legal periodical edition
   */
  private void removeDeletedReferences(LegalPeriodicalEdition updatedEdition) {
    var oldEdition = repository.findById(updatedEdition.id());
    if (oldEdition.isEmpty()) {
      return;
    }

    // Ensure references deleted in edition are removed from DocumentationUnit's references
    for (Map.Entry<UUID, Integer> reference : oldEdition.get().getReferences().entrySet()) {
      // identify deleted references (not null and not in updated edition)
      var referenceDTO = referenceRepository.findById(reference.getKey());
      if (referenceDTO.isEmpty()
          || updatedEdition.references().stream()
              .anyMatch(newReference -> newReference.id().equals(reference.getKey()))) {
        continue;
      }

      // delete all deleted references and possible source reference
      documentationUnitRepository
          .findById(referenceDTO.get().getDocumentationUnit().getId())
          .ifPresent(
              docUnit -> {
                docUnit.getReferences().remove(referenceDTO.get());
                if (docUnit.getSource().stream()
                    .findFirst()
                    .map(SourceDTO::getReference)
                    .filter(ref -> ref.getId().equals(reference.getKey()))
                    .isPresent()) {
                  docUnit.getSource().removeFirst();
                }
                documentationUnitRepository.save(docUnit);
              });
    }

    for (Map.Entry<UUID, Integer> citation : oldEdition.get().getLiteratureCitations().entrySet()) {
      // identify deleted citations (not null and not in updated edition)
      var citationDTO = dependentLiteratureCitationRepository.findById(citation.getKey());
      if (citationDTO.isEmpty()
          || updatedEdition.references().stream()
              .anyMatch(newCitation -> newCitation.id().equals(citation.getKey()))) {
        continue;
      }

      // delete all deleted references and possible source reference
      documentationUnitRepository
          .findById(citationDTO.get().getDocumentationUnit().getId())
          .ifPresent(
              docUnit -> {
                docUnit.getDependentLiteratureCitations().remove(citationDTO.get());
                if (docUnit.getSource().stream()
                    .findFirst()
                    .map(SourceDTO::getReference)
                    .filter(ref -> ref.getId().equals(citation.getKey()))
                    .isPresent()) {
                  docUnit.getSource().removeFirst();
                }
                documentationUnitRepository.save(docUnit);
              });
    }
  }

  private ReferenceDTO saveReference(Reference reference, DocumentationUnitDTO docUnit) {
    ReferenceDTO newReference = ReferenceTransformer.transformToDTO(reference);
    newReference.setDocumentationUnit(docUnit);

    // keep doc unit's rank for existing references and set to max rank +1 for new references
    newReference.setRank(
        docUnit.getReferences().stream()
            .filter(referenceDTO -> referenceDTO.getId().equals(reference.id()))
            .findFirst()
            .map(ReferenceDTO::getRank)
            .orElseGet(
                () ->
                    docUnit.getReferences().stream()
                            .map(ReferenceDTO::getRank)
                            .max(Comparator.naturalOrder())
                            .orElse(0)
                        + 1));

    return referenceRepository.save(newReference);
  }

  private DependentLiteratureCitationDTO saveLiteratureCitation(
      Reference reference, DocumentationUnitDTO docUnit) {
    DependentLiteratureCitationDTO newReference =
        DependentLiteratureTransformer.transformToDTO(reference);
    newReference.setDocumentationUnit(docUnit);

    // keep docUnit's rank for existing references and set to max rank +1 for new references
    newReference.setRank(
        docUnit.getDependentLiteratureCitations().stream()
            .filter(
                dependentLiteratureCitationDTO ->
                    dependentLiteratureCitationDTO.getId().equals(reference.id()))
            .findFirst()
            .map(DependentLiteratureCitationDTO::getRank)
            .orElseGet(
                () ->
                    docUnit.getDependentLiteratureCitations().stream()
                            .map(DependentLiteratureCitationDTO::getRank)
                            .max(Comparator.naturalOrder())
                            .orElse(0)
                        + 1));

    return dependentLiteratureCitationRepository.save(newReference);
  }

  @Override
  public void delete(LegalPeriodicalEdition legalPeriodicalEdition) {
    repository.deleteById(legalPeriodicalEdition.id());
  }
}
