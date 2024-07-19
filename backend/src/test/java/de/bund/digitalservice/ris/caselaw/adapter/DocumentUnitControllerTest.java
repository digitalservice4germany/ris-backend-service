package de.bund.digitalservice.ris.caselaw.adapter;

import static de.bund.digitalservice.ris.caselaw.AuthUtils.buildDefaultDocOffice;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.gravity9.jsonpatch.JsonPatch;
import com.gravity9.jsonpatch.JsonPatchOperation;
import com.gravity9.jsonpatch.ReplaceOperation;
import de.bund.digitalservice.ris.caselaw.DocumentUnitControllerTestConfig;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseApiKeyRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.DocumentationUnitTransformer;
import de.bund.digitalservice.ris.caselaw.domain.Attachment;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentService;
import de.bund.digitalservice.ris.caselaw.domain.CoreData;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentUnitService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.EventRecord;
import de.bund.digitalservice.ris.caselaw.domain.HandoverMail;
import de.bund.digitalservice.ris.caselaw.domain.HandoverReport;
import de.bund.digitalservice.ris.caselaw.domain.HandoverService;
import de.bund.digitalservice.ris.caselaw.domain.RelatedDocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.RisJsonPatch;
import de.bund.digitalservice.ris.caselaw.domain.XmlExportResult;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import de.bund.digitalservice.ris.caselaw.domain.mapper.PatchMapperService;
import de.bund.digitalservice.ris.caselaw.webtestclient.RisWebTestClient;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = DocumentUnitController.class)
@Import({DocumentUnitControllerTestConfig.class})
class DocumentUnitControllerTest {
  @Autowired private RisWebTestClient risWebClient;
  @MockBean private DocumentUnitService service;

  @MockBean private HandoverService handoverService;
  @MockBean private KeycloakUserService userService;
  @MockBean private DocxConverterService docxConverterService;
  @MockBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockBean private AttachmentService attachmentService;
  @MockBean DatabaseApiKeyRepository apiKeyRepository;
  @MockBean DatabaseDocumentationOfficeRepository officeRepository;
  @MockBean private PatchMapperService patchMapperService;

  private static final UUID TEST_UUID = UUID.fromString("88888888-4444-4444-4444-121212121212");
  private static final String ISSUER_ADDRESS = "test-issuer@exporter.neuris";
  private final DocumentationOffice docOffice = buildDefaultDocOffice();
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    doReturn(docOffice)
        .when(userService)
        .getDocumentationOffice(
            argThat(
                (OidcUser user) -> {
                  List<String> groups = user.getAttribute("groups");
                  return Objects.requireNonNull(groups).get(0).equals("/DS");
                }));

    when(service.getByUuid(TEST_UUID))
        .thenReturn(
            DocumentUnit.builder()
                .coreData(CoreData.builder().documentationOffice(docOffice).build())
                .build());
  }

  @Test
  void testGenerateNewDocumentUnit() {
    // userService.getDocumentationOffice is mocked in @BeforeEach
    when(service.generateNewDocumentUnit(docOffice)).thenReturn(DocumentUnit.builder().build());

    risWebClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/new")
        .exchange()
        .expectStatus()
        .isCreated();

    verify(service, times(1)).generateNewDocumentUnit(docOffice);
    verify(userService, times(1)).getDocumentationOffice(any(OidcUser.class));
  }

  @Test
  void testGetByDocumentnumber() {
    when(service.getByDocumentNumber("ABCD202200001"))
        .thenReturn(
            DocumentUnit.builder()
                .coreData(CoreData.builder().documentationOffice(docOffice).build())
                .build());

    risWebClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/ABCD202200001")
        .exchange()
        .expectStatus()
        .isOk();

    // once by the AuthService and once by the controller asking the service
    verify(service, times(2)).getByDocumentNumber("ABCD202200001");
  }

  @Test
  void testGetByDocumentNumber_withInvalidDocumentNumber() {
    when(service.getByDocumentNumber("abc")).thenReturn(null);

    risWebClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/abc")
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  void testDeleteByUuid() throws DocumentationUnitNotExistsException {
    when(service.deleteByUuid(TEST_UUID)).thenReturn(null);

    risWebClient
        .withDefaultLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/" + TEST_UUID)
        .exchange()
        .expectStatus()
        .isOk();

    verify(service).deleteByUuid(TEST_UUID);
  }

  @Test
  void testDeleteByUuid_withInvalidUuid() {
    when(service.getByDocumentNumber("abc")).thenReturn(null);

    risWebClient
        .withDefaultLogin()
        .delete()
        .uri("/api/v1/caselaw/documentunits/abc")
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  void testUpdateByUuid() throws DocumentationUnitNotExistsException {
    DocumentationUnitDTO documentUnitDTO =
        DocumentationUnitDTO.builder()
            .id(TEST_UUID)
            .documentNumber("ABCD202200001")
            .documentationOffice(DocumentationOfficeDTO.builder().abbreviation("DS").build())
            .build();
    DocumentUnit documentUnit = DocumentationUnitTransformer.transformToDomain(documentUnitDTO);

    when(service.updateDocumentUnit(documentUnit)).thenReturn(null);

    risWebClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + TEST_UUID)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(documentUnit)
        .exchange()
        .expectStatus()
        .isOk();

    verify(service).updateDocumentUnit(documentUnit);
  }

  @Test
  @Disabled
  void testPatchUpdateByUuid() throws DocumentationUnitNotExistsException, JsonProcessingException {
    DocumentationUnitDTO documentUnitDTO =
        DocumentationUnitDTO.builder()
            .id(TEST_UUID)
            .documentNumber("ABCD202200001")
            .documentationOffice(DocumentationOfficeDTO.builder().abbreviation("DS").build())
            .build();
    DocumentUnit documentUnit = DocumentationUnitTransformer.transformToDomain(documentUnitDTO);

    when(service.updateDocumentUnit(documentUnit)).thenReturn(documentUnit);
    when(service.getByUuid(TEST_UUID)).thenReturn(documentUnit);

    JsonNode valueToReplace = new TextNode("newValue");
    JsonPatchOperation replaceOp = new ReplaceOperation("/coreData/appraisalBody", valueToReplace);
    JsonPatch patch = new JsonPatch(List.of(replaceOp));
    RisJsonPatch risJsonPatch = new RisJsonPatch(0L, patch, Collections.emptyList());

    risWebClient
        .withDefaultLogin()
        .patch()
        .uri("/api/v1/caselaw/documentunits/" + TEST_UUID)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(risJsonPatch)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void testUpdateByUuid_withInvalidUuid() {
    DocumentUnit documentUnitDTO = DocumentUnit.builder().uuid(TEST_UUID).build();

    risWebClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/abc")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(documentUnitDTO)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  void testHandoverAsEmail() throws DocumentationUnitNotExistsException {
    when(userService.getEmail(any(OidcUser.class))).thenReturn(ISSUER_ADDRESS);
    when(handoverService.handoverAsEmail(TEST_UUID, ISSUER_ADDRESS))
        .thenReturn(
            HandoverMail.builder()
                .documentUnitUuid(TEST_UUID)
                .receiverAddress("receiver address")
                .mailSubject("mailSubject")
                .xml("xml")
                .success(true)
                .statusMessages(List.of("status-messages"))
                .fileName("test.xml")
                .handoverDate(Instant.parse("2020-01-01T01:01:01.00Z"))
                .build());

    HandoverMail responseBody =
        risWebClient
            .withDefaultLogin()
            .put()
            .uri("/api/v1/caselaw/documentunits/" + TEST_UUID + "/handover")
            .exchange()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectStatus()
            .isOk()
            .expectBody(HandoverMail.class)
            .returnResult()
            .getResponseBody();

    assertThat(responseBody)
        .isEqualTo(
            HandoverMail.builder()
                .documentUnitUuid(TEST_UUID)
                .receiverAddress("receiver address")
                .mailSubject("mailSubject")
                .xml("xml")
                .success(true)
                .fileName("test.xml")
                .statusMessages(List.of("status-messages"))
                .handoverDate(Instant.parse("2020-01-01T01:01:01Z"))
                .build());

    verify(handoverService).handoverAsEmail(TEST_UUID, ISSUER_ADDRESS);
  }

  @Test
  void testHandoverAsEmail_withServiceThrowsException() throws DocumentationUnitNotExistsException {
    when(userService.getEmail(any(OidcUser.class))).thenReturn(ISSUER_ADDRESS);
    when(handoverService.handoverAsEmail(TEST_UUID, ISSUER_ADDRESS))
        .thenThrow(DocumentationUnitNotExistsException.class);

    risWebClient
        .withDefaultLogin()
        .put()
        .uri("/api/v1/caselaw/documentunits/" + TEST_UUID + "/handover")
        .exchange()
        .expectStatus()
        .is5xxServerError();

    verify(handoverService).handoverAsEmail(TEST_UUID, ISSUER_ADDRESS);
  }

  @Test
  void testGetLastHandoverXmlMail() {

    when(handoverService.getEventLog(TEST_UUID))
        .thenReturn(
            List.of(
                HandoverReport.builder()
                    .content("<html>2021 Report</html>")
                    .receivedDate(Instant.parse("2021-01-01T01:01:01.00Z"))
                    .build(),
                HandoverMail.builder()
                    .documentUnitUuid(TEST_UUID)
                    .receiverAddress("receiver address")
                    .mailSubject("mailSubject")
                    .xml("xml")
                    .success(true)
                    .statusMessages(List.of("status-messages"))
                    .fileName("test.xml")
                    .handoverDate(Instant.parse("2020-01-01T01:01:01.00Z"))
                    .build(),
                HandoverReport.builder()
                    .content("<html>2019 Report</html>")
                    .receivedDate(Instant.parse("2019-01-01T01:01:01.00Z"))
                    .build()));

    List<EventRecord> responseBody =
        risWebClient
            .withDefaultLogin()
            .get()
            .uri("/api/v1/caselaw/documentunits/" + TEST_UUID + "/handover")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new TypeReference<List<EventRecord>>() {})
            .returnResult()
            .getResponseBody();

    assertThat(responseBody)
        .containsExactly(
            HandoverReport.builder()
                .content("<html>2021 Report</html>")
                .receivedDate(Instant.parse("2021-01-01T01:01:01Z"))
                .build(),
            HandoverMail.builder()
                .documentUnitUuid(TEST_UUID)
                .receiverAddress("receiver address")
                .mailSubject("mailSubject")
                .xml("xml")
                .success(true)
                .statusMessages(List.of("status-messages"))
                .handoverDate(Instant.parse("2020-01-01T01:01:01Z"))
                .fileName("test.xml")
                .build(),
            HandoverReport.builder()
                .content("<html>2019 Report</html>")
                .receivedDate(Instant.parse("2019-01-01T01:01:01Z"))
                .build());

    verify(handoverService).getEventLog(TEST_UUID);
  }

  @Test
  void testGetXmlPreview() throws DocumentationUnitNotExistsException {
    when(userService.getEmail(any(OidcUser.class))).thenReturn(ISSUER_ADDRESS);
    when(handoverService.createPreviewXml(TEST_UUID))
        .thenReturn(
            new XmlExportResult(
                "xml",
                true,
                List.of("status-messages"),
                "test.xml",
                Instant.parse("2020-01-01T01:01:01.00Z")));

    XmlExportResult responseBody =
        risWebClient
            .withDefaultLogin()
            .get()
            .uri("/api/v1/caselaw/documentunits/" + TEST_UUID + "/preview-xml")
            .exchange()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectStatus()
            .isOk()
            .expectBody(XmlExportResult.class)
            .returnResult()
            .getResponseBody();
    assertThat(responseBody)
        .isEqualTo(
            XmlExportResult.builder()
                .xml("xml")
                .success(true)
                .statusMessages(List.of("status-messages"))
                .creationDate(Instant.parse("2020-01-01T01:01:01Z"))
                .fileName("test.xml")
                .build());

    verify(handoverService).createPreviewXml(TEST_UUID);
  }

  @Test
  void testSearchByLinkedDocumentationUnit() {
    RelatedDocumentationUnit linkedDocumentationUnit = RelatedDocumentationUnit.builder().build();
    PageRequest pageRequest = PageRequest.of(0, 10);

    when(service.searchLinkableDocumentationUnits(
            linkedDocumentationUnit, docOffice, "KORE0000000000", pageRequest))
        .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

    risWebClient
        .withDefaultLogin()
        .put()
        .uri(
            "/api/v1/caselaw/documentunits/KORE0000000000/search-linkable-documentation-units?pg=0&sz=10")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(linkedDocumentationUnit)
        .exchange()
        .expectStatus()
        .isOk();

    verify(service)
        .searchLinkableDocumentationUnits(
            linkedDocumentationUnit, docOffice, "KORE0000000000", pageRequest);
  }

  @Test
  void testSearchByDocumentUnitListEntry() {
    PageRequest pageRequest = PageRequest.of(0, 10);

    when(service.searchByDocumentationUnitSearchInput(
            pageRequest,
            docOffice,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()))
        .thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

    risWebClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/search?pg=0&sz=10")
        .exchange()
        .expectStatus()
        .isOk();

    verify(service)
        .searchByDocumentationUnitSearchInput(
            pageRequest,
            docOffice,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
  }

  @Test
  void testGetHtml() {
    when(service.getByUuid(TEST_UUID))
        .thenReturn(
            DocumentUnit.builder()
                .attachments(Collections.singletonList(Attachment.builder().s3path("123").build()))
                .coreData(CoreData.builder().documentationOffice(docOffice).build())
                .build());
    when(docxConverterService.getConvertedObject("123")).thenReturn(null);

    risWebClient
        .withDefaultLogin()
        .get()
        .uri("/api/v1/caselaw/documentunits/" + TEST_UUID + "/docx/123")
        .exchange()
        .expectStatus()
        .isOk();

    // once by the AuthService and once by the controller asking the service
    verify(service, times(2)).getByUuid(TEST_UUID);
    verify(docxConverterService).getConvertedObject("123");
  }
}
