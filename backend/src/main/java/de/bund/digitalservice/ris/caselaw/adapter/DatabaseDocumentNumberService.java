package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc.DatabaseDocumentNumberCounterRepository;
import de.bund.digitalservice.ris.caselaw.domain.DocumentNumberService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import java.util.Calendar;
import java.util.Map;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class DatabaseDocumentNumberService implements DocumentNumberService {
  private final DatabaseDocumentNumberCounterRepository repository;
  private static final String CASELAW_ABBREVIATION = "RE";

  private static final Map<String, String> DOCUMENT_NUMBER_ABBREVIATIONS =
      Map.ofEntries(
          Map.entry("BGH", "KO"),
          Map.entry("BVerfG", "KV"),
          Map.entry("BAG", "EF"),
          Map.entry("BFH", "ST"),
          Map.entry("BPatG", "MP"),
          Map.entry("BSG", "KS"),
          Map.entry("BVerwG", "LE"),
          Map.entry("OVG_NRW", "MW"),
          Map.entry("BZSt", "FM"),
          Map.entry("DigitalService", "XX"),
          Map.entry("CC-RIS", "XX"));

  public DatabaseDocumentNumberService(DatabaseDocumentNumberCounterRepository repository) {
    this.repository = repository;
  }

  @Override
  public Mono<String> generateNextDocumentNumber(DocumentationOffice documentationOffice) {
    int currentYear = Calendar.getInstance().get(Calendar.YEAR);

    return repository
        .getDocumentNumberCounterEntry()
        .flatMap(
            outdatedDocumentNumberCounter -> {
              // this is the switch happening when the first new DocumentUnit in a new year gets
              // created
              if (outdatedDocumentNumberCounter.getCurrentyear() != currentYear) {
                outdatedDocumentNumberCounter.setCurrentyear(currentYear);
                outdatedDocumentNumberCounter.setNextnumber(1);
              }
              outdatedDocumentNumberCounter.setNextnumber(
                  outdatedDocumentNumberCounter.getNextnumber() + 1);
              return repository.save(outdatedDocumentNumberCounter);
            })
        .map(
            documentNumberCounter ->
                String.format(
                    "%s%s%04d%05d",
                    DOCUMENT_NUMBER_ABBREVIATIONS.get(documentationOffice.abbreviation()),
                    CASELAW_ABBREVIATION,
                    currentYear,
                    documentNumberCounter.getNextnumber() - 1));
  }
}
