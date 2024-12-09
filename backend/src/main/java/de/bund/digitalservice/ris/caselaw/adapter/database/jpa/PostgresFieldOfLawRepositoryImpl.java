package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.adapter.transformer.FieldOfLawTransformer;
import de.bund.digitalservice.ris.caselaw.domain.FieldOfLawRepository;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PostgresFieldOfLawRepositoryImpl implements FieldOfLawRepository {
  private final DatabaseFieldOfLawRepository repository;
  private final EntityManager entityManager;

  public PostgresFieldOfLawRepositoryImpl(
      DatabaseFieldOfLawRepository repository, EntityManager entityManager) {

    this.repository = repository;
    this.entityManager = entityManager;
  }

  @Override
  @Transactional
  public List<FieldOfLaw> getTopLevelNodes() {
    return repository.findAllByParentIsNullAndNotationOrderByIdentifier().stream()
        .map(PostgresFieldOfLawRepositoryImpl::getWithNormsWithoutChildren)
        .toList();
  }

  @Override
  @Transactional
  public List<FieldOfLaw> findAllByParentIdentifierOrderByIdentifierAsc(String identifier) {
    return repository.findByIdentifier(identifier).getChildren().stream()
        .map(PostgresFieldOfLawRepositoryImpl::getWithNormsWithoutChildren)
        .toList();
  }

  @Override
  @Transactional
  public FieldOfLaw findTreeByIdentifier(String identifier) {
    FieldOfLawDTO childDTO = repository.findByIdentifier(identifier);
    FieldOfLaw child = FieldOfLawTransformer.transformToDomain(childDTO, false, true);

    FieldOfLawDTO parentDTO;
    FieldOfLaw parent = child;
    while (childDTO.getParent() != null) {
      parentDTO = childDTO.getParent();
      parent = PostgresFieldOfLawRepositoryImpl.getWithNormsWithoutChildren(parentDTO);
      parent = parent.toBuilder().children(List.of(child)).build();

      childDTO = parentDTO;
      child = parent;
    }

    return parent;
  }

  @Override
  @Transactional
  public Slice<FieldOfLaw> findAllByOrderByIdentifierAsc(Pageable pageable) {
    return repository
        .findAllByOrderByIdentifierAsc(pageable)
        .map(PostgresFieldOfLawRepositoryImpl::getWithNormsWithoutChildren);
  }

  @Override
  @Transactional
  public List<FieldOfLaw> findByCombinedCriteria(
      String identifier, String[] descriptionSearchTerms, String[] normSearchTerms) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<FieldOfLawDTO> cq = cb.createQuery(FieldOfLawDTO.class);
    Root<FieldOfLawDTO> fieldOfLawRoot = cq.from(FieldOfLawDTO.class);
    ArrayList<Predicate> predicates = new ArrayList<>();

    Predicate notationPredicate =
        cb.equal(fieldOfLawRoot.get("notation"), cb.literal(Notation.NEW));
    predicates.add(notationPredicate);

    if (descriptionSearchTerms != null) {
      for (String searchTerm : descriptionSearchTerms) {
        predicates.add(
            cb.like(cb.lower(fieldOfLawRoot.get("text")), "%" + searchTerm.toLowerCase() + "%"));
      }
    }
    if (identifier != null) {
      Predicate identifierPredicate =
          cb.like(fieldOfLawRoot.get("identifier"), identifier.toUpperCase() + "%");
      predicates.add(identifierPredicate);
    }
    if (normSearchTerms != null) {
      fieldOfLawRoot.fetch("norms", JoinType.LEFT);
      for (String searchTerm : normSearchTerms) {
        Predicate normAbbreviationPredicate =
            cb.like(
                cb.lower(fieldOfLawRoot.get("norms").get("abbreviation")),
                "%" + searchTerm.toLowerCase() + "%");
        Predicate singleNormPredicate =
            cb.like(
                cb.lower(fieldOfLawRoot.get("norms").get("singleNormDescription")),
                "%" + searchTerm.toLowerCase() + "%");
        Predicate combined = cb.or(normAbbreviationPredicate, singleNormPredicate);
        predicates.add(combined);
      }
    }

    cq.select(fieldOfLawRoot).where(predicates.toArray(Predicate[]::new));
    cq.orderBy(cb.asc(fieldOfLawRoot.get("identifier")));

    TypedQuery<FieldOfLawDTO> query = entityManager.createQuery(cq);
    List<FieldOfLawDTO> result = query.getResultList();

    return result.stream()
        .filter(Objects::nonNull)
        .distinct()
        .map(PostgresFieldOfLawRepositoryImpl::getWithNormsWithoutChildren)
        .toList();
  }

  @Override
  @Transactional
  public List<FieldOfLaw> findByIdentifier(String searchStr, Pageable pageable) {
    return repository.findAllByIdentifierStartsWithIgnoreCaseOrderByIdentifier(searchStr).stream()
        .map(PostgresFieldOfLawRepositoryImpl::getWithNormsWithoutChildren)
        .toList();
  }

  static FieldOfLaw getWithNormsWithoutChildren(FieldOfLawDTO fieldOfLawDTO) {
    return FieldOfLawTransformer.transformToDomain(fieldOfLawDTO, false, true);
  }
}
