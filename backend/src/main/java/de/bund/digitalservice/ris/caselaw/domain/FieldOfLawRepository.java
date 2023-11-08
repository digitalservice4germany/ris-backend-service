package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface FieldOfLawRepository {
  List<FieldOfLaw> getTopLevelNodes();

  List<FieldOfLaw> findAllByParentIdentifierOrderByIdentifierAsc(String identifier);

  FieldOfLaw findTreeByIdentifier(String identifier);

  FieldOfLaw findParentByChild(FieldOfLaw child);

  Page<FieldOfLaw> findAllByOrderByIdentifierAsc(Pageable pageable);

  List<FieldOfLaw> findBySearchTerms(String[] searchTerms);

  List<FieldOfLaw> findByNormStr(String normStr);

  List<FieldOfLaw> findByNormStrAndSearchTerms(String normStr, String[] searchTerms);

  List<FieldOfLaw> getFirst30OrderByIdentifier();

  List<FieldOfLaw> findByIdentifierSearch(String searchStr);
}
