package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/caselaw/lookuptableimporter")
@Slf4j
@Tag(name = OpenApiConfiguration.CASELAW_TAG)
public class LookupTableImporterController {

  private final LookupTableImporterService service;

  private final JdbcTemplate jdbcTemplate;

  public LookupTableImporterController(
      LookupTableImporterService service, JdbcTemplate jdbcTemplate) {
    this.service = service;
    this.jdbcTemplate = jdbcTemplate;
  }

  // In Postman go to "Body", select "raw" and "XML" and paste the XML-contents.
  // Can we use @RequestBody @Valid DocumentTypesXML directly instead of ByteBuffer?

  @PutMapping(value = "doktyp")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<String>> importDocumentTypeLookupTable(
      @RequestBody ByteBuffer byteBuffer) {
    return service
        .importDocumentTypeLookupTable(byteBuffer)
        .map(resultString -> ResponseEntity.status(HttpStatus.OK).body(resultString))
        .onErrorReturn(
            ResponseEntity.internalServerError()
                .body("Could not import the document type lookup table"));
  }

  @PutMapping(value = "gerichtdata")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<String>> importCourtLookupTable(@RequestBody ByteBuffer byteBuffer) {
    return service
        .importCourtLookupTable(byteBuffer)
        .map(resultString -> ResponseEntity.status(HttpStatus.OK).body(resultString))
        .onErrorReturn(
            ResponseEntity.internalServerError().body("Could not import the court lookup table"));
  }

  @PutMapping(value = "buland")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<String>> importStateLookupTable(@RequestBody ByteBuffer byteBuffer) {
    return service
        .importStateLookupTable(byteBuffer)
        .map(resultString -> ResponseEntity.status(HttpStatus.OK).body(resultString))
        .onErrorReturn(
            ResponseEntity.internalServerError().body("Could not import the state lookup table"));
  }

  @PutMapping(value = "fieldOfLaw")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<String>> importFieldOfLawLookupTable(
      @RequestBody ByteBuffer byteBuffer) {
    return service
        .importFieldOfLawLookupTable(byteBuffer)
        .map(resultString -> ResponseEntity.status(HttpStatus.OK).body(resultString))
        .onErrorReturn(
            ResponseEntity.internalServerError()
                .body("Could not import the fieldOfLaw lookup table"));
  }

  @PutMapping(value = "zitart")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<String>> importCitationStyleLookupTable(
      @RequestBody ByteBuffer byteBuffer) {
    return service
        .importCitationStyleLookupTable(byteBuffer)
        .map(resultString -> ResponseEntity.status(HttpStatus.OK).body(resultString))
        .onErrorReturn(
            ResponseEntity.internalServerError()
                .body("Could not import the citation lookup table"));
  }

  @PutMapping("/refreshMaterializedViews")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<String>> refreshMaterializedViews() {
    String msg;
    try {
      jdbcTemplate.execute("REFRESH MATERIALIZED VIEW norm_abbreviation_search");
    } catch (Exception e) {
      msg = "Could not refresh the materialized view 'norm_abbreviation_search'";
      log.error(msg, e);
      return Mono.just(ResponseEntity.internalServerError().body(msg));
    }
    msg = "Successfully refreshed the materialized view 'norm_abbreviation_search'";
    log.info(msg);
    return Mono.just(ResponseEntity.ok(msg));
  }
}
