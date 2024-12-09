package de.bund.digitalservice.ris.caselaw.domain;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DocumentationOfficeService {
  private final DocumentationOfficeRepository documentationOfficeRepository;

  public DocumentationOfficeService(DocumentationOfficeRepository documentationOfficeRepository) {
    this.documentationOfficeRepository = documentationOfficeRepository;
  }

  /**
   * Returns documentation office objects in a list with optional search string.
   *
   * @param searchStr An optional search string, which filters the list.
   * @return all documentation offices containing the search string in their abbreviation
   */
  public List<DocumentationOffice> getDocumentationOffices(String searchStr) {
    if (searchStr != null && !searchStr.trim().isBlank()) {
      return documentationOfficeRepository.findBySearchStr(searchStr);
    }

    return documentationOfficeRepository.findAllOrderByAbbreviationAsc();
  }
}
