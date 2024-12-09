package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOfficeService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/caselaw/documentationoffices")
@Slf4j
public class DocumentationOfficeController {
  private final DocumentationOfficeService service;

  public DocumentationOfficeController(DocumentationOfficeService service) {
    this.service = service;
  }

  /**
   * Returns documentation office objects in a list with optional search string.
   *
   * @return all documentation offices containing the search string in their abbreviation
   */
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public List<DocumentationOffice> getDocumentationOffices(
      @RequestParam(value = "q", required = false) String searchStr) {
    return service.getDocumentationOffices(searchStr);
  }
}
