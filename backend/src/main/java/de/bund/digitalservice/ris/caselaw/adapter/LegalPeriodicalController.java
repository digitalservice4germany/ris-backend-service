package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.domain.LegalPeriodicalService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.LegalPeriodical;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/caselaw/legalperiodicals")
@Slf4j
public class LegalPeriodicalController {
  private final LegalPeriodicalService service;

  public LegalPeriodicalController(LegalPeriodicalService service) {
    this.service = service;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public List<LegalPeriodical> getLegalPeriodicals(
      @RequestParam(value = "q") Optional<String> searchStr) {
    return service.getLegalPeriodicals(searchStr);
  }
}
