package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.adapter.transformer.NormAbbreviationTransformer;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.NormAbbreviation;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.NormAbbreviationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/**
 * Implementation of the {@link NormAbbreviationRepository} interface for accessing norm
 * abbreviation data stored in a PostgreSQL database.
 */
@Repository
public class PostgresNormAbbreviationRepositoryImpl implements NormAbbreviationRepository {

  private final DatabaseNormAbbreviationRepository repository;

  /**
   * Constructs a {@code PostgresNormAbbreviationRepositoryImpl} with the specified {@link
   * DatabaseNormAbbreviationRepository}.
   *
   * @param repository The repository used to access norm abbreviation data from the database.
   */
  public PostgresNormAbbreviationRepositoryImpl(DatabaseNormAbbreviationRepository repository) {
    this.repository = repository;
  }

  /**
   * Retrieves a norm abbreviation by its unique identifier.
   *
   * @param id The unique identifier of the normalized abbreviation.
   * @return The {@link NormAbbreviation} object corresponding to the given identifier, or null if
   *     not found.
   */
  @Override
  public NormAbbreviation findById(UUID id) {
    return NormAbbreviationTransformer.transformToDomain(repository.findById(id).orElse(null));
  }

  /**
   * Retrieves a list of norm abbreviations containing the specified query string, ordered by
   * accuracy.
   *
   * @param query The query string used to filter normal abbreviations.
   * @param size The maximum number of results to retrieve.
   * @param page The page number of results to retrieve.
   * @return A list of {@link NormAbbreviation} objects containing the specified query string,
   *     ordered by accuracy.
   */
  @Override
  public List<NormAbbreviation> findAllContainingOrderByAccuracy(
      String query, Integer size, Integer page) {
    List<NormAbbreviationDTO> results =
        repository.findByAbbreviationIgnoreCase(query, PageRequest.of(page, size));

    // Add results from additional queries if needed to meet size requirement
    if (results.size() < size) {
      var officialLetterAbbreviationExact =
          repository.findByOfficialLetterAbbreviationIgnoreCase(query, PageRequest.of(page, size));
      officialLetterAbbreviationExact.stream()
          .filter(e -> results.size() < size && !results.contains(e))
          .forEach(results::add);
    }

    if (results.size() < size) {
      var abbreviationStartingWith =
          repository.findByAbbreviationStartsWithIgnoreCase(query, PageRequest.of(page, size));
      abbreviationStartingWith.stream()
          .filter(e -> results.size() < size && !results.contains(e))
          .forEach(results::add);
    }

    if (results.size() < size) {
      var officialLetterAbbreviationStartingWith =
          repository.findByOfficialLetterAbbreviationStartsWithIgnoreCase(
              query, PageRequest.of(page, size));
      officialLetterAbbreviationStartingWith.stream()
          .filter(e -> results.size() < size && !results.contains(e))
          .forEach(results::add);
    }

    if (results.size() < size) {
      var officialLongTitleStartingWith =
          repository.findByOfficialLongTitleContainsIgnoreCase(query, PageRequest.of(page, size));
      officialLongTitleStartingWith.stream()
          .filter(e -> results.size() < size && !results.contains(e))
          .forEach(results::add);
    }

    if (results.size() < size) {
      var rankWeightedVector = repository.findByRankWeightedVector(query, size, page * size);
      rankWeightedVector.stream()
          .filter(e -> results.size() < size && !results.contains(e))
          .forEach(results::add);
    }

    return results.stream().map(NormAbbreviationTransformer::transformToDomain).toList();
  }
}
