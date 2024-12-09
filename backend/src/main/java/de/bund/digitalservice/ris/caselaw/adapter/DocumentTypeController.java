package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.domain.DocumentTypeService;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
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
@RequestMapping("api/v1/caselaw/documenttypes")
@Slf4j
public class DocumentTypeController {
  private final DocumentTypeService service;

  public DocumentTypeController(DocumentTypeService service) {
    this.service = service;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public List<DocumentType> getDocumentTypes(
      @RequestParam(value = "q") Optional<String> searchStr) {
    return service.getDocumentTypes(searchStr);
  }

  @GetMapping(value = "/dependent-literature", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public List<DocumentType> getDependentLiteratureDocumentTypes(
      @RequestParam(value = "q") Optional<String> searchStr) {
    return service.getDependentLiteratureDocumentTypes(searchStr);
  }
}
