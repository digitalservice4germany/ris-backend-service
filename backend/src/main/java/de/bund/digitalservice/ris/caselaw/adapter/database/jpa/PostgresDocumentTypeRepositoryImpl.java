package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentTypeTransformer;
import de.bund.digitalservice.ris.caselaw.domain.DocumentTypeRepository;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresDocumentTypeRepositoryImpl implements DocumentTypeRepository {
  private final DatabaseDocumentTypeRepository repository;
  private final DatabaseDocumentCategoryRepository categoryRepository;

  public PostgresDocumentTypeRepositoryImpl(
      DatabaseDocumentTypeRepository repository,
      DatabaseDocumentCategoryRepository categoryRepository) {
    this.repository = repository;
    this.categoryRepository = categoryRepository;
  }

  @Override
  public List<DocumentType> findCaselawBySearchStr(String searchString) {
    return repository
        .findCaselawBySearchStrAndCategory(
            searchString, categoryRepository.findFirstByLabel("R").getId())
        .stream()
        .map(DocumentTypeTransformer::transformToDomain)
        .toList();
  }

  @Override
  public List<DocumentType> findDependentLiteratureBySearchStr(String searchString) {
    return repository
        .findCaselawBySearchStrAndCategory(
            searchString, categoryRepository.findFirstByLabel("U").getId())
        .stream()
        .map(DocumentTypeTransformer::transformToDomain)
        .toList();
  }

  @Override
  public List<DocumentType> findAllDependentLiteratureOrderByAbbreviationAscLabelAsc() {
    return repository
        .findAllByCategoryOrderByAbbreviationAscLabelAsc(categoryRepository.findFirstByLabel("U"))
        .stream()
        .map(DocumentTypeTransformer::transformToDomain)
        .toList();
  }

  @Override
  public Optional<DocumentType> findUniqueCaselawBySearchStr(String searchString) {
    return repository
        .findUniqueCaselawBySearchStrAndCategory(
            searchString, categoryRepository.findFirstByLabel("R").getId())
        .map(DocumentTypeTransformer::transformToDomain);
  }

  @Override
  public List<DocumentType> findAllByDocumentTypeOrderByAbbreviationAscLabelAsc(
      @Param("shortcut") char shortcut) {
    return repository
        .findAllByCategoryOrderByAbbreviationAscLabelAsc(categoryRepository.findFirstByLabel("R"))
        .stream()
        .map(DocumentTypeTransformer::transformToDomain)
        .toList();
  }
}
