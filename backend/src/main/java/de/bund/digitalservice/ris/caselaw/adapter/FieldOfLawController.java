package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/caselaw/fieldsoflaw")
@Tag(name = OpenApiConfiguration.CASELAW_TAG)
public class FieldOfLawController {
  private final FieldOfLawService service;

  public FieldOfLawController(FieldOfLawService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public Mono<Page<FieldOfLaw>> getFieldsOfLawBySearchQuery(
      @RequestParam("q") Optional<String> searchStr,
      @RequestParam("pg") int page,
      @RequestParam("sz") int size) {
    return service.getFieldsOfLawBySearchQuery(searchStr, PageRequest.of(page, size));
  }

  @GetMapping(value = "/search-by-identifier")
  @PreAuthorize("isAuthenticated()")
  public Flux<FieldOfLaw> getFieldsOfLawByIdentifierSearch(
      @RequestParam("q") Optional<String> searchStr) {
    return service.getFieldsOfLawByIdentifierSearch(searchStr);
  }

  @GetMapping(value = "{identifier}/children")
  @PreAuthorize("isAuthenticated()")
  public Flux<FieldOfLaw> getChildrenOfFieldOfLaw(@PathVariable String identifier) {
    return service.getChildrenOfFieldOfLaw(identifier);
  }

  @GetMapping(value = "{identifier}/tree")
  @PreAuthorize("isAuthenticated()")
  public Mono<FieldOfLaw> getTreeForFieldOfLaw(@PathVariable String identifier) {
    return service.getTreeForFieldOfLaw(identifier);
  }
}
