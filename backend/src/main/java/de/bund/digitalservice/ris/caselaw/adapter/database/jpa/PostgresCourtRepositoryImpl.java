package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import de.bund.digitalservice.ris.caselaw.adapter.transformer.CourtTransformer;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.court.CourtRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresCourtRepositoryImpl implements CourtRepository {

  private final DatabaseCourtRepository repository;

  public PostgresCourtRepositoryImpl(DatabaseCourtRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<Court> findBySearchStr(String searchString) {
    return repository.findBySearchStr(searchString).stream()
        .map(CourtTransformer::transformToDomain)
        .toList();
  }

  @Override
  public Optional<Court> findByTypeAndLocation(String type, String location) {
    if (type == null) {
      return Optional.empty();
    }
    if (location == null) {
      return repository.findOneByType(type).map(CourtTransformer::transformToDomain);
    }
    return repository
        .findOneByTypeAndLocation(type, location)
        .map(CourtTransformer::transformToDomain);
  }

  @Override
  public Optional<Court> findUniqueBySearchString(String searchString) {
    List<CourtDTO> foundCourts = repository.findByExactSearchString(searchString);

    if (foundCourts.size() == 1) {
      return Optional.of(CourtTransformer.transformToDomain(foundCourts.get(0)));
    }
    return Optional.empty();
  }

  @Override
  public List<Court> findAllByOrderByTypeAscLocationAsc() {
    return repository.findByOrderByTypeAscLocationAsc(Limit.of(10)).stream()
        .map(CourtTransformer::transformToDomain)
        .toList();
  }
}
