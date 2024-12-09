package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DocumentTypeService {
  private final DocumentTypeRepository documentTypeRepository;

  public DocumentTypeService(DocumentTypeRepository documentTypeRepository) {
    this.documentTypeRepository = documentTypeRepository;
  }

  public List<DocumentType> getDocumentTypes(Optional<String> searchStr) {
    if (searchStr.isPresent() && !searchStr.get().isBlank()) {
      return documentTypeRepository.findCaselawBySearchStr(searchStr.get().trim());
    }

    return documentTypeRepository.findAllByDocumentTypeOrderByAbbreviationAscLabelAsc('R');
  }

  public List<DocumentType> getDependentLiteratureDocumentTypes(Optional<String> searchStr) {
    if (searchStr.isPresent() && !searchStr.get().isBlank()) {
      return documentTypeRepository.findDependentLiteratureBySearchStr(searchStr.get().trim());
    }

    return documentTypeRepository.findAllDependentLiteratureOrderByAbbreviationAscLabelAsc();
  }
}
