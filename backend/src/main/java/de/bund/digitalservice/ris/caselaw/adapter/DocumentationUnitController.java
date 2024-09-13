package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.converter.docx.DocumentConverterException;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationUnitTransformerException;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.ConvertedDocumentElement;
import de.bund.digitalservice.ris.caselaw.domain.ConverterService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitDocxMetadataInitializationService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitListItem;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.EventRecord;
import de.bund.digitalservice.ris.caselaw.domain.HandoverEntityType;
import de.bund.digitalservice.ris.caselaw.domain.HandoverException;
import de.bund.digitalservice.ris.caselaw.domain.HandoverMail;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.RisJsonPatch;
import de.bund.digitalservice.ris.caselaw.domain.SingleNormValidationInfo;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.XmlTransformationResult;
import de.bund.digitalservice.ris.caselaw.domain.docx.Docx2Html;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitDeletionException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitPatchException;
import jakarta.validation.Valid;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/caselaw/documentunits")
@Slf4j
public class DocumentationUnitController {
  private final DocumentationUnitService service;
  private final UserService userService;
  private final AttachmentService attachmentService;
  private final ConverterService converterService;
  private final HandoverService handoverService;
  private final DocumentationUnitDocxMetadataInitializationService
      documentationUnitDocxMetadataInitializationService;

  public DocumentationUnitController(
      DocumentationUnitService service,
      UserService userService,
      AttachmentService attachmentService,
      ConverterService converterService,
      HandoverService handoverService,
      DocumentationUnitDocxMetadataInitializationService
          documentationUnitDocxMetadataInitializationService) {
    this.service = service;
    this.userService = userService;
    this.attachmentService = attachmentService;
    this.converterService = converterService;
    this.handoverService = handoverService;
    this.documentationUnitDocxMetadataInitializationService =
        documentationUnitDocxMetadataInitializationService;
  }

  @GetMapping(value = "new", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated() and @userIsInternal.apply(#oidcUser)")
  public ResponseEntity<DocumentationUnit> generateNewDocumentationUnit(
      @AuthenticationPrincipal OidcUser oidcUser) {
    var docOffice = userService.getDocumentationOffice(oidcUser);
    try {
      var documentationUnit = service.generateNewDocumentationUnit(docOffice);
      return ResponseEntity.status(HttpStatus.CREATED).body(documentationUnit);
    } catch (DocumentationUnitException e) {
      log.error("error in generate new documentation unit", e);
      return ResponseEntity.internalServerError().body(DocumentationUnit.builder().build());
    }
  }

  /**
   * Attach a content file (docx) to the documentation unit. This file is used to fill the
   * categories of the documentation unit.
   *
   * <p>Do a conversion into html and parse the footer for ECLI information.
   *
   * @param uuid UUID of the documentation unit
   * @param bytes bytes of the content file
   * @param httpHeaders http headers with the X-Filename information
   * @return the into html converted content of the file with some additional metadata (ECLI)
   */
  @PutMapping(
      value = "/{uuid}/file",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
  @PreAuthorize("@userIsInternal.apply(#oidcUser) and @userHasSameDocumentationOffice.apply(#uuid)")
  public ResponseEntity<Docx2Html> attachFileToDocumentationUnit(
      @AuthenticationPrincipal OidcUser oidcUser,
      @PathVariable UUID uuid,
      @RequestBody byte[] bytes,
      @RequestHeader HttpHeaders httpHeaders) {
    var docx2html =
        converterService.getConvertedObject(
            attachmentService
                .attachFileToDocumentationUnit(uuid, ByteBuffer.wrap(bytes), httpHeaders)
                .s3path());
    documentationUnitDocxMetadataInitializationService.initializeCoreData(uuid, docx2html);
    if (docx2html == null) {
      return ResponseEntity.unprocessableEntity().build();
    }
    return ResponseEntity.status(HttpStatus.OK).body(docx2html);
  }

  @DeleteMapping(value = "/{uuid}/file/{s3Path}")
  @PreAuthorize("@userIsInternal.apply(#oidcUser) and @userHasSameDocumentationOffice.apply(#uuid)")
  public ResponseEntity<Object> removeAttachmentFromDocumentationUnit(
      @AuthenticationPrincipal OidcUser oidcUser,
      @PathVariable UUID uuid,
      @PathVariable String s3Path) {

    try {
      attachmentService.deleteByS3Path(s3Path);
      return ResponseEntity.noContent().build();
    } catch (Exception e) {
      log.error("Error by deleting attachment '{}' for documentation unit {}", s3Path, uuid, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  // Access rights are being enforced through SQL filtering
  public Slice<DocumentationUnitListItem> searchByDocumentationUnitListEntry(
      @RequestParam("pg") int page,
      @RequestParam("sz") int size,
      @RequestParam(value = "documentNumber") Optional<String> documentNumber,
      @RequestParam(value = "fileNumber") Optional<String> fileNumber,
      @RequestParam(value = "courtType") Optional<String> courtType,
      @RequestParam(value = "courtLocation") Optional<String> courtLocation,
      @RequestParam(value = "decisionDate") Optional<LocalDate> decisionDate,
      @RequestParam(value = "decisionDateEnd") Optional<LocalDate> decisionDateEnd,
      @RequestParam(value = "publicationStatus") Optional<String> publicationStatus,
      @RequestParam(value = "withError") Optional<Boolean> withError,
      @RequestParam(value = "myDocOfficeOnly") Optional<Boolean> myDocOfficeOnly,
      @AuthenticationPrincipal OidcUser oidcUser) {

    return service.searchByDocumentationUnitSearchInput(
        PageRequest.of(page, size),
        oidcUser,
        documentNumber,
        fileNumber,
        courtType,
        courtLocation,
        decisionDate,
        decisionDateEnd,
        publicationStatus,
        withError,
        myDocOfficeOnly);
  }

  @GetMapping(value = "/{documentNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasReadAccessByDocumentNumber.apply(#documentNumber)")
  public ResponseEntity<DocumentationUnit> getByDocumentNumber(
      @NonNull @PathVariable String documentNumber) {

    if (documentNumber.length() != 13 && documentNumber.length() != 14) {
      throw new DocumentationUnitException("Die Dokumentennummer unterstützt nur 13-14 Zeichen");
    }

    try {
      return ResponseEntity.ok(service.getByDocumentNumber(documentNumber));
    } catch (DocumentationUnitNotExistsException e) {
      log.error("Documentation unit '{}' doesn't exist", documentNumber);
      return ResponseEntity.ok(DocumentationUnit.builder().build());
    }
  }

  @DeleteMapping(value = "/{uuid}")
  @PreAuthorize("@userIsInternal.apply(#oidcUser) and @userHasSameDocumentationOffice.apply(#uuid)")
  public ResponseEntity<String> deleteByUuid(
      @AuthenticationPrincipal OidcUser oidcUser, @PathVariable UUID uuid) {

    try {
      var str = service.deleteByUuid(uuid);
      return ResponseEntity.status(HttpStatus.OK).body(str);
    } catch (DocumentationUnitNotExistsException | DocumentationUnitDeletionException ex) {
      return ResponseEntity.internalServerError().body(ex.getMessage());
    }
  }

  @PutMapping(
      value = "/{uuid}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasSameDocumentationOffice.apply(#uuid)")
  public ResponseEntity<DocumentationUnit> updateByUuid(
      @PathVariable UUID uuid,
      @Valid @RequestBody DocumentationUnit documentationUnit,
      @AuthenticationPrincipal OidcUser oidcUser) {

    if (!uuid.equals(documentationUnit.uuid())) {
      return ResponseEntity.unprocessableEntity().body(DocumentationUnit.builder().build());
    }
    try {
      var du = service.updateDocumentationUnit(documentationUnit);
      return ResponseEntity.status(HttpStatus.OK).body(du);
    } catch (DocumentationUnitNotExistsException
        | DocumentationUnitException
        | DocumentationUnitTransformerException e) {
      log.error("Error by updating documentation unit '{}'", documentationUnit.documentNumber(), e);
      return ResponseEntity.internalServerError().body(DocumentationUnit.builder().build());
    }
  }

  /**
   * Update a documentation unit with a {@link com.gravity9.jsonpatch.JsonPatch} object.
   *
   * @param uuid id of the documentation unit
   * @param patch patch with the change operations
   * @return updated and saved documentation unit
   */
  @PatchMapping(
      value = "/{uuid}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "@userHasSameDocumentationOffice.apply(#uuid) and (@userIsInternal.apply(#oidcUser) or (@isAssignedViaProcedure.apply(#uuid) and @isPatchAllowedForExternalUsers.apply(#patch)))")
  public ResponseEntity<RisJsonPatch> partialUpdateByUuid(
      @AuthenticationPrincipal OidcUser oidcUser,
      @PathVariable UUID uuid,
      @RequestBody RisJsonPatch patch) {

    String documentNumber = "unknown";

    try {
      if (patch == null) {
        return ResponseEntity.internalServerError().build();
      }

      var documentationUnit = service.getByUuid(uuid);
      if (documentationUnit != null) {
        documentNumber = documentationUnit.documentNumber();
      }
      var newPatch = service.updateDocumentationUnit(uuid, patch);

      return ResponseEntity.ok().body(newPatch);
    } catch (DocumentationUnitNotExistsException e) {
      return ResponseEntity.internalServerError().build();
    } catch (DocumentationUnitPatchException e) {
      log.error("Error by updating documentation unit '{}/{}'", uuid, documentNumber, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Hands over the documentation unit to jDV as XML via email.
   *
   * @param uuid UUID of the documentation unit
   * @param oidcUser the logged-in user, used to forward the response email
   * @return the email sent containing the XML or an empty response with status code 400 * if the
   *     user is not authorized
   */
  @PutMapping(value = "/{uuid}/handover", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasSameDocumentationOffice.apply(#uuid)")
  public ResponseEntity<HandoverMail> handoverDocumentationUnitAsMail(
      @PathVariable UUID uuid, @AuthenticationPrincipal OidcUser oidcUser) {

    try {
      HandoverMail handoverMail =
          handoverService.handoverDocumentationUnitAsMail(uuid, userService.getEmail(oidcUser));
      return ResponseEntity.ok(handoverMail);
    } catch (DocumentationUnitNotExistsException | HandoverException e) {
      log.error("Error handing over documentation unit '{}' as email", uuid, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Get all events of a documentation unit (can be handover events, received handover reports,
   * import/migration events)
   *
   * @param uuid UUID of the documentation unit
   * @return ordered list of event records (newest first) or an empty response with status code 400
   *     if the user is not authorized
   */
  @GetMapping(value = "/{uuid}/handover", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasSameDocumentationOffice.apply(#uuid)")
  public List<EventRecord> getEventLog(@PathVariable UUID uuid) {
    return handoverService.getEventLog(uuid, HandoverEntityType.DOCUMENTATION_UNIT);
  }

  /**
   * Get the XML preview of a documentation unit.
   *
   * @param uuid UUID of the documentation unit
   * @return the XML preview or an empty response with status code 400 if the user is not authorized
   *     or an empty response if the documentation unit does not exist
   */
  @GetMapping(value = "/{uuid}/preview-xml", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasReadAccessByDocumentationUnitId.apply(#uuid)")
  public XmlTransformationResult getXmlPreview(@PathVariable UUID uuid) {
    try {
      return handoverService.createPreviewXml(uuid);
    } catch (DocumentationUnitNotExistsException e) {
      return null;
    }
  }

  @PutMapping(
      value = "/search-linkable-documentation-units",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public Slice<RelatedDocumentationUnit> searchLinkableDocumentationUnits(
      @RequestParam("pg") int page,
      @RequestParam("sz") int size,
      @RequestParam(value = "documentNumber") Optional<String> documentNumberToExclude,
      @RequestBody RelatedDocumentationUnit relatedDocumentationUnit,
      @AuthenticationPrincipal OidcUser oidcUser) {

    var documentationOffice = userService.getDocumentationOffice(oidcUser);
    return service.searchLinkableDocumentationUnits(
        relatedDocumentationUnit,
        documentationOffice,
        documentNumberToExclude,
        PageRequest.of(page, size));
  }

  @GetMapping(value = "/{uuid}/docx/{s3Path}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasReadAccessByDocumentationUnitId.apply(#uuid)")
  public ResponseEntity<Docx2Html> getHtml(@PathVariable UUID uuid, @PathVariable String s3Path) {

    try {
      service.getByUuid(uuid);
    } catch (DocumentationUnitNotExistsException ex) {
      return ResponseEntity.notFound().build();
    }

    try {
      var docx2Html = converterService.getConvertedObject(s3Path);
      return ResponseEntity.ok()
          .cacheControl(CacheControl.maxAge(Duration.ofDays(1))) // Set cache duration
          .body(docx2Html);
    } catch (Exception ex) {
      log.error("Error by getting docx for documentation unit {}", uuid, ex);
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Get a list of
   *
   * @param id
   * @param s3Path
   * @return
   */
  @GetMapping(value = "/{id}/docx/{s3Path}/converted", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasReadAccessByDocumentationUnitId.apply(#id)")
  public ResponseEntity<List<ConvertedDocumentElement>> getConvertedDocx(
      @PathVariable UUID id, @PathVariable String s3Path) {

    try {
      service.getByUuid(id);
      return ResponseEntity.ok(converterService.getConvertedObjectList(id, s3Path));
    } catch (DocumentConverterException ex) {
      log.error(
          "Error by getting list of converted documentation elements for original document "
              + "'{}' for document unit '{}'",
          s3Path,
          id,
          ex);
      return ResponseEntity.internalServerError().build();
    } catch (DocumentationUnitNotExistsException ex) {
      log.error("document unit '{}' does not exist", id);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(value = "/{id}/docx/{s3Path}/reconvert", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasReadAccessByDocumentationUnitId.apply(#id)")
  public ResponseEntity<List<ConvertedDocumentElement>> getReconvertDocx(
      @PathVariable UUID id, @PathVariable String s3Path) {

    try {
      service.getByUuid(id);
      return ResponseEntity.ok(converterService.getReconvertObjectList(id, s3Path));
    } catch (DocumentConverterException ex) {
      log.error(
          "Error by getting list of converted documentation elements for original document "
              + "'{}' for document unit '{}'",
          s3Path,
          id,
          ex);
      return ResponseEntity.internalServerError().build();
    } catch (DocumentationUnitNotExistsException ex) {
      log.error("document unit '{}' does not exist", id);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(
      value = "/{id}/docx/{s3Path}/removebordernumbers",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasReadAccessByDocumentationUnitId.apply(#id)")
  public ResponseEntity<List<ConvertedDocumentElement>> removeBorderNumbers(
      @PathVariable UUID id, @PathVariable String s3Path) {

    try {
      service.getByUuid(id);
      return ResponseEntity.ok(converterService.removeBorderNumbers(id, s3Path));
    } catch (DocumentConverterException ex) {
      log.error(
          "Error by getting list of converted documentation elements for original document "
              + "'{}' for document unit '{}'",
          s3Path,
          id,
          ex);
      return ResponseEntity.internalServerError().build();
    } catch (DocumentationUnitNotExistsException ex) {
      log.error("document unit '{}' does not exist", id);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(
      value = "/{id}/docx/{s3Path}/addbordernumbers",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasReadAccessByDocumentationUnitId.apply(#id)")
  public ResponseEntity<List<ConvertedDocumentElement>> addBorderNumbers(
      @PathVariable UUID id,
      @PathVariable String s3Path,
      @RequestParam(value = "startAt", required = false) UUID startId) {

    try {
      service.getByUuid(id);
      return ResponseEntity.ok(converterService.addBorderNumbers(id, s3Path, startId));
    } catch (DocumentConverterException ex) {
      log.error(
          "Error by getting list of converted documentation elements for original document "
              + "'{}' for document unit '{}'",
          s3Path,
          id,
          ex);
      return ResponseEntity.internalServerError().build();
    } catch (DocumentationUnitNotExistsException ex) {
      log.error("document unit '{}' does not exist", id);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(
      value = "/{id}/docx/{s3Path}/removebordernumber/{elementId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasReadAccessByDocumentationUnitId.apply(#id)")
  public ResponseEntity<List<ConvertedDocumentElement>> removeBorderNumber(
      @PathVariable UUID id, @PathVariable String s3Path, @PathVariable UUID elementId) {

    try {
      service.getByUuid(id);
      return ResponseEntity.ok(converterService.removeBorderNumber(id, s3Path, elementId));
    } catch (DocumentConverterException ex) {
      log.error(
          "Error by getting list of converted documentation elements for original document "
              + "'{}' for document unit '{}'",
          s3Path,
          id,
          ex);
      return ResponseEntity.internalServerError().build();
    } catch (DocumentationUnitNotExistsException ex) {
      log.error("document unit '{}' does not exist", id);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping(
      value = "/{id}/docx/{s3Path}/joinbordernumbers/{elementId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("@userHasReadAccessByDocumentationUnitId.apply(#id)")
  public ResponseEntity<List<ConvertedDocumentElement>> joinBorderNumbers(
      @PathVariable UUID id, @PathVariable String s3Path, @PathVariable UUID elementId) {

    try {
      service.getByUuid(id);
      return ResponseEntity.ok(converterService.joinBorderNumbers(id, s3Path, elementId));
    } catch (DocumentConverterException ex) {
      log.error(
          "Error by getting list of converted documentation elements for original document "
              + "'{}' for document unit '{}'",
          s3Path,
          id,
          ex);
      return ResponseEntity.internalServerError().build();
    } catch (DocumentationUnitNotExistsException ex) {
      log.error("document unit '{}' does not exist", id);
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping(value = "/validateSingleNorm")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<String> validateSingleNorm(
      @RequestBody SingleNormValidationInfo singleNormValidationInfo) {
    try {
      return ResponseEntity.ok(service.validateSingleNorm(singleNormValidationInfo));
    } catch (Exception ex) {
      log.error(
          "Error by validation of single norm '{} - {}'",
          singleNormValidationInfo.normAbbreviation(),
          singleNormValidationInfo.singleNorm(),
          ex);
      return ResponseEntity.internalServerError().build();
    }
  }
}
