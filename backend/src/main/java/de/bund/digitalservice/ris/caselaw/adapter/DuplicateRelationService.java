package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DecisionDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DuplicateRelationDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DuplicateRelationRepository;
import de.bund.digitalservice.ris.caselaw.domain.DuplicateRelationStatus;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DuplicateRelationService {

  private final DuplicateRelationRepository relationRepository;

  public DuplicateRelationService(DuplicateRelationRepository relationRepository) {
    this.relationRepository = relationRepository;
  }

  Optional<DuplicateRelationDTO> findByDocUnitIds(UUID docUnitIdA, UUID docUnitIdB) {
    DuplicateRelationDTO.DuplicateRelationId duplicateRelationId =
        new DuplicateRelationDTO.DuplicateRelationId(docUnitIdA, docUnitIdB);

    return relationRepository.findById(duplicateRelationId);
  }

  List<DuplicateRelationDTO> findAllByDocUnitId(UUID docUnitId) {
    return relationRepository.findAllByDocUnitId(docUnitId);
  }

  void create(DecisionDTO docUnitA, DecisionDTO docUnitB, DuplicateRelationStatus status) {
    DuplicateRelationDTO.DuplicateRelationId duplicateRelationId =
        new DuplicateRelationDTO.DuplicateRelationId(docUnitA.getId(), docUnitB.getId());

    DecisionDTO docUnit1;
    DecisionDTO docUnit2;
    // duplicateRelationId determines the order of the two docUnits
    if (docUnitA.getId().equals(duplicateRelationId.getDocumentationUnitId1())) {
      docUnit1 = docUnitA;
      docUnit2 = docUnitB;
    } else {
      docUnit1 = docUnitB;
      docUnit2 = docUnitA;
    }

    var newRelation =
        DuplicateRelationDTO.builder()
            .relationStatus(status)
            .documentationUnit1(docUnit1)
            .documentationUnit2(docUnit2)
            .id(duplicateRelationId)
            .build();
    try {
      relationRepository.save(newRelation);
    } catch (ConstraintViolationException e) {
      // Duplicate relations might be created multiple times in parallel or while a deletion of a
      // doc unit is in progress (e2e tests). Instead of locking we choose to ignore this.
    }
  }

  void setStatus(DuplicateRelationDTO duplicateRelation, DuplicateRelationStatus status) {
    duplicateRelation.setRelationStatus(status);
    relationRepository.save(duplicateRelation);
  }

  void delete(DuplicateRelationDTO duplicateRelation) {
    relationRepository.delete(duplicateRelation);
  }

  void updateAllDuplicates() {
    log.info("Updating all duplicate relations");
    var removedRelations = this.relationRepository.removeObsoleteDuplicateRelations();
    var insertedRelations = this.relationRepository.addMissingDuplicateRelations();
    var ignoredRelations =
        this.relationRepository.ignoreDuplicateRelationsWhenJdvDupCheckDisabled();
    log.info(
        "Updating duplicate relations finished: {} duplicates added, {} duplicates removed, {} duplicates set to ignored.",
        insertedRelations,
        removedRelations,
        ignoredRelations);
  }
}
