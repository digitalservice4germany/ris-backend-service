package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentNumberRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentNumberDTO;
import de.bund.digitalservice.ris.caselaw.domain.DateUtil;
import de.bund.digitalservice.ris.caselaw.domain.DocumentNumberFormatter;
import de.bund.digitalservice.ris.caselaw.domain.DocumentNumberRecyclingService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentNumberService;
import de.bund.digitalservice.ris.caselaw.domain.StringUtils;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentNumberFormatterException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentNumberPatternException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitExistsException;
import jakarta.validation.constraints.NotEmpty;
import java.time.Year;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service to generate the next available doc unit number based on documentation office */
@Service
public class DatabaseDocumentNumberGeneratorService implements DocumentNumberService {
  private final DatabaseDocumentNumberRepository repository;
  private final DocumentNumberPatternConfig documentNumberPatternConfig;
  private final DatabaseDocumentationUnitRepository databaseDocumentationUnitRepository;

  private final DocumentNumberRecyclingService documentNumberRecyclingService;

  // because of java:S6809 - Methods with Spring proxy should not be called via "this"
  private final DatabaseDocumentNumberGeneratorService documentNumberGeneratorService = this;

  public DatabaseDocumentNumberGeneratorService(
      DatabaseDocumentNumberRepository repository,
      DocumentNumberPatternConfig documentNumberPatternConfig,
      DatabaseDocumentationUnitRepository databaseDocumentationUnitRepository,
      DocumentNumberRecyclingService documentNumberRecyclingService) {
    this.repository = repository;
    this.documentNumberPatternConfig = documentNumberPatternConfig;
    this.databaseDocumentationUnitRepository = databaseDocumentationUnitRepository;
    this.documentNumberRecyclingService = documentNumberRecyclingService;
  }

  /**
   * Executes the generation of a document number for the provided documentation office.
   *
   * @param documentationOfficeAbbreviation The abbreviation of the documentation office. Must not
   *     be empty.
   * @return The generated next available document number to use.
   * @throws DocumentNumberPatternException If the pattern for generating document numbers is
   *     invalid.
   * @throws DocumentationUnitExistsException If a documentation unit already exists.
   * @throws DocumentNumberFormatterException If there is an issue with formatting the document
   *     number.
   */
  @Override
  @Transactional(transactionManager = "jpaTransactionManager")
  public String generateDocumentNumber(@NotEmpty String documentationOfficeAbbreviation)
      throws DocumentNumberPatternException,
          DocumentationUnitExistsException,
          DocumentNumberFormatterException {
    if (StringUtils.returnTrueIfNullOrBlank(documentationOfficeAbbreviation)) {
      throw new DocumentNumberPatternException(
          "Documentation Office abbreviation can not be empty");
    }

    String pattern =
        documentNumberPatternConfig
            .getDocumentNumberPatterns()
            .getOrDefault(documentationOfficeAbbreviation, null);

    if (pattern == null) {
      throw new DocumentNumberPatternException(
          "Could not find pattern for abbreviation " + documentationOfficeAbbreviation);
    }

    String recycledId =
        documentNumberGeneratorService.recycle(documentationOfficeAbbreviation).orElse(null);

    if (recycledId != null) return recycledId;

    DocumentNumberDTO documentNumberDTO =
        repository
            .findById(documentationOfficeAbbreviation)
            .orElse(
                DocumentNumberDTO.builder()
                    .documentationOfficeAbbreviation(documentationOfficeAbbreviation)
                    .lastNumber(0)
                    .build());

    String documentNumber =
        DocumentNumberFormatter.builder()
            .sequenceNumber(documentNumberDTO.increaseLastNumber())
            .year(DateUtil.getYear())
            .pattern(pattern)
            .build()
            .generate();

    repository.save(documentNumberDTO);

    assertNotExists(documentNumber);

    return documentNumber;
  }

  /**
   * Validate document number not exists in the database
   *
   * @param documentNumber document number
   * @throws DocumentationUnitExistsException if no documentation unit for the document number exist
   */
  public void assertNotExists(String documentNumber) throws DocumentationUnitExistsException {
    if (databaseDocumentationUnitRepository.findByDocumentNumber(documentNumber).isPresent()) {
      throw new DocumentationUnitExistsException(
          "Document number already exists: " + documentNumber);
    }
  }

  @Transactional(transactionManager = "jpaTransactionManager")
  public Optional<String> recycle(String documentationOfficeAbbreviation) {
    try {
      var recycledDocumentNumber =
          documentNumberRecyclingService.recycleFromDeletedDocumentationUnit(
              documentationOfficeAbbreviation, Year.now());
      documentNumberRecyclingService.delete(recycledDocumentNumber);
      return Optional.of(recycledDocumentNumber);
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
