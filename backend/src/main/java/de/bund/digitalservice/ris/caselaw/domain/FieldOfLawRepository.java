package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface FieldOfLawRepository {
  List<FieldOfLaw> getTopLevelNodes();

  List<FieldOfLaw> findAllByParentIdentifierOrderByIdentifierAsc(String identifier);

  FieldOfLaw findTreeByIdentifier(String identifier);

  Slice<FieldOfLaw> findAllByOrderByIdentifierAsc(Pageable pageable);

  List<FieldOfLaw> findByIdentifier(String searchStr, Pageable pageable);

  List<FieldOfLaw> findByCombinedCriteria(String identifier, String[] searchTerms, String[] norm);
}
