package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LegalPeriodicalService {
  private final LegalPeriodicalRepository legalPeriodicalRepository;

  public LegalPeriodicalService(LegalPeriodicalRepository legalPeriodicalRepository) {

    this.legalPeriodicalRepository = legalPeriodicalRepository;
  }

  /**
   * Get legal periodical objects in a list, filtered by an optional search string
   *
   * @param searchStr optional search string
   * @return list of legal periodical which contain the search string or the whole list of legal
   *     periodical if no search string is given
   */
  public List<LegalPeriodical> getLegalPeriodicals(Optional<String> searchStr) {
    return legalPeriodicalRepository.findAllBySearchStr(searchStr.map(String::trim));
  }
}
