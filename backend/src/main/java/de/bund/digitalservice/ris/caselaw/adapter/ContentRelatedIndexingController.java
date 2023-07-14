package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import de.bund.digitalservice.ris.norms.framework.adapter.input.restapi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("api/v1/caselaw/documentunits/{uuid}/contentrelatedindexing")
@Tag(name = OpenApiConfiguration.CASELAW_TAG)
public class ContentRelatedIndexingController {
  private final FieldOfLawService fieldOfLawService;
  private final KeywordService keywordService;

  public ContentRelatedIndexingController(
      FieldOfLawService fieldOfLawService, KeywordService keywordService) {
    this.fieldOfLawService = fieldOfLawService;
    this.keywordService = keywordService;
  }

  @GetMapping("fieldsoflaw")
  @PreAuthorize("@userHasReadAccessByDocumentUnitUuid.apply(#documentUnitUuid)")
  public Mono<List<FieldOfLaw>> getFieldsOfLaw(@PathVariable("uuid") UUID documentUnitUuid) {
    return fieldOfLawService.getFieldsOfLawForDocumentUnit(documentUnitUuid);
  }

  @PutMapping("fieldsoflaw/{identifier}")
  @PreAuthorize("@userHasWriteAccessByDocumentUnitUuid.apply(#documentUnitUuid)")
  public Mono<List<FieldOfLaw>> addFieldOfLaw(
      @PathVariable("uuid") UUID documentUnitUuid, @PathVariable("identifier") String identifier) {

    return fieldOfLawService.addFieldOfLawToDocumentUnit(documentUnitUuid, identifier);
  }

  @DeleteMapping("fieldsoflaw/{identifier}")
  @PreAuthorize("@userHasWriteAccessByDocumentUnitUuid.apply(#documentUnitUuid)")
  public Mono<List<FieldOfLaw>> removeFieldOfLaw(
      @PathVariable("uuid") UUID documentUnitUuid, @PathVariable("identifier") String identifier) {

    return fieldOfLawService.removeFieldOfLawToDocumentUnit(documentUnitUuid, identifier);
  }

  @GetMapping("keywords")
  @PreAuthorize("@userHasReadAccessByDocumentUnitUuid.apply(#documentUnitUuid)")
  public Mono<List<String>> getKeywords(@PathVariable("uuid") UUID documentUnitUuid) {
    return keywordService.getKeywordsForDocumentUnit(documentUnitUuid);
  }

  @PutMapping("keywords/{keyword}")
  @PreAuthorize("@userHasWriteAccessByDocumentUnitUuid.apply(#documentUnitUuid)")
  public Mono<List<String>> addKeyword(
      @PathVariable("uuid") UUID documentUnitUuid, @PathVariable("keyword") String keyword) {

    return keywordService.addKeywordToDocumentUnit(documentUnitUuid, keyword);
  }

  @DeleteMapping("keywords/{keyword}")
  @PreAuthorize("@userHasWriteAccessByDocumentUnitUuid.apply(#documentUnitUuid)")
  public Mono<List<String>> deleteKeyword(
      @PathVariable("uuid") UUID documentUnitUuid, @PathVariable("keyword") String keyword) {

    return keywordService.deleteKeywordFromDocumentUnit(documentUnitUuid, keyword);
  }
}
