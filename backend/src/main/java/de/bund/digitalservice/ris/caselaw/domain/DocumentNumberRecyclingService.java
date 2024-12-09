package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentNumberPatternException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentNumberRecyclingException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import java.time.Year;
import java.util.UUID;

public interface DocumentNumberRecyclingService {

  void addForRecycling(
      UUID documentationOfficeId, String documentNumber, String documentationOfficeAbbreviation)
      throws DocumentNumberPatternException,
          DocumentationUnitNotExistsException,
          DocumentNumberRecyclingException;

  String recycleFromDeletedDocumentationUnit(String documentationOfficeAbbreviation, Year year)
      throws DocumentNumberPatternException, DocumentNumberRecyclingException;

  void assertDocumentationUnitHasNeverBeenHandedOverOrMigrated(UUID documentationUnitId)
      throws DocumentNumberRecyclingException;

  void assertPatternIsValid(String documentationOfficeAbbreviation, String documentationUnitNumber)
      throws DocumentNumberPatternException;

  void delete(String documentNumber);
}
