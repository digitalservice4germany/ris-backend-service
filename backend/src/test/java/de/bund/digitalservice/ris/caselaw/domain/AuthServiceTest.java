package de.bund.digitalservice.ris.caselaw.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.gravity9.jsonpatch.AddOperation;
import com.gravity9.jsonpatch.JsonPatch;
import com.gravity9.jsonpatch.JsonPatchOperation;
import com.gravity9.jsonpatch.RemoveOperation;
import com.gravity9.jsonpatch.ReplaceOperation;
import com.gravity9.jsonpatch.TestOperation;
import de.bund.digitalservice.ris.caselaw.adapter.AuthService;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseApiKeyRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import({AuthService.class})
class AuthServiceTest {

  @SpyBean private AuthService service;

  @MockBean private DocumentationUnitService documentationUnitService;
  @MockBean private UserService userService;
  @MockBean private ProcedureService procedureService;
  @MockBean private DatabaseApiKeyRepository keyRepository;
  @MockBean private DatabaseDocumentationOfficeRepository officeRepository;
  @MockBean private SecurityContext securityContext;
  @MockBean private Authentication authentication;
  @MockBean private OidcUser oidcUser;

  @Test
  void testUserHasReadAccessByDocumentNumber_withStatusNull_shouldReturnTrue()
      throws DocumentationUnitNotExistsException {
    // Arrange
    String documentNumber = "DOC12345";
    DocumentationUnit documentationUnit =
        DocumentationUnit.builder().documentNumber(documentNumber).build();
    when(documentationUnitService.getByDocumentNumber(documentNumber))
        .thenReturn(documentationUnit);

    // Act
    Function<String, Boolean> result = service.userHasReadAccessByDocumentNumber();

    // Assert
    assertThat(result.apply("DOC12345")).isTrue();
  }

  @Test
  void testUserHasReadAccessByDocumentNumber_withStatusPublished_shouldReturnTrue()
      throws DocumentationUnitNotExistsException {

    // Arrange
    String documentNumber = "DOC12345";
    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .documentNumber(documentNumber)
            .status(Status.builder().publicationStatus(PublicationStatus.PUBLISHED).build())
            .build();
    when(documentationUnitService.getByDocumentNumber(documentNumber))
        .thenReturn(documentationUnit);

    // Act
    Function<String, Boolean> result = service.userHasReadAccessByDocumentNumber();

    // Assert
    assertThat(result.apply("DOC12345")).isTrue();
  }

  @Test
  void testUserHasReadAccessByDocumentNumber_withSameDocOffice_shouldReturnTrue()
      throws DocumentationUnitNotExistsException {

    // Arrange
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    String documentNumber = "DOC12345";
    DocumentationOffice office = DocumentationOffice.builder().abbreviation("DS").build();
    DocumentationUnit documentationUnit =
        DocumentationUnit.builder()
            .documentNumber(documentNumber)
            .status(Status.builder().publicationStatus(PublicationStatus.DUPLICATED).build())
            .coreData(CoreData.builder().documentationOffice(office).build())
            .build();
    when(documentationUnitService.getByDocumentNumber(documentNumber))
        .thenReturn(documentationUnit);
    when(userService.getDocumentationOffice(any())).thenReturn(office);

    // Act
    Function<String, Boolean> result = service.userHasReadAccessByDocumentNumber();

    // Assert
    assertThat(result.apply(documentNumber)).isTrue();
  }

  @Test
  void testUserIsInternal_withInternalUser_shouldReturnTrue() {
    // Arrange
    when(userService.isInternal(any(OidcUser.class))).thenReturn(true);

    // Act
    Function<OidcUser, Boolean> result = service.userIsInternal();

    // Assert
    assertThat(result.apply(oidcUser)).isTrue();
  }

  @Test
  void TestUserIsInternal_withExternalUser_shouldReturnFalse() {
    // Arrange
    when(oidcUser.getClaimAsStringList("roles")).thenReturn(List.of("External"));

    // Act
    Function<OidcUser, Boolean> result = service.userIsInternal();

    // Assert
    assertThat(result.apply(oidcUser)).isFalse();
  }

  @Test
  void testUserHasWriteAccessByProcedureId_withSameDocOffice_shouldReturnTrue() {
    // Arrange
    UUID uuid = UUID.randomUUID();
    when(procedureService.getDocumentationOfficeByUUID(uuid))
        .thenReturn(DocumentationOffice.builder().abbreviation("DS").build());
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    DocumentationOffice office = DocumentationOffice.builder().abbreviation("DS").build();
    when(userService.getDocumentationOffice(any())).thenReturn(office);

    // Act
    Function<UUID, Boolean> result = service.userHasWriteAccessByProcedureId();

    // Assert
    assertThat(result.apply(uuid)).isTrue();
  }

  @Test
  void testUserHasWriteAccessByProcedureId_withOtherDocOffice_shouldReturnFalse() {
    // Arrange
    UUID uuid = UUID.randomUUID();
    when(procedureService.getDocumentationOfficeByUUID(uuid))
        .thenReturn(DocumentationOffice.builder().abbreviation("BGH").build());
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    DocumentationOffice office = DocumentationOffice.builder().abbreviation("DS").build();
    when(userService.getDocumentationOffice(any())).thenReturn(office);

    // Act
    Function<UUID, Boolean> result = service.userHasWriteAccessByProcedureId();

    // Assert
    assertThat(result.apply(uuid)).isFalse();
  }

  @Test
  void test_userHasSameDocumentationOffice_withoutDocumentationUnit_shouldReturnFalse()
      throws DocumentationUnitNotExistsException {
    // Arrange
    UUID uuid = UUID.randomUUID();
    when(documentationUnitService.getByUuid(uuid)).thenReturn(null);

    // Act
    Function<UUID, Boolean> result = service.userHasSameDocumentationOffice();

    // Assert
    assertThat(result.apply(uuid)).isFalse();
  }

  @Test
  void test_userHasSameDocumentationOffice_withSameDocumentationOffice_shouldReturnTrue()
      throws DocumentationUnitNotExistsException {

    // Arrange
    UUID uuid = UUID.randomUUID();
    DocumentationOffice documentationOffice = DocumentationOffice.builder().build();
    SecurityContextHolder.setContext(securityContext);
    when(documentationUnitService.getByUuid(uuid))
        .thenReturn(
            DocumentationUnit.builder()
                .coreData(CoreData.builder().documentationOffice(documentationOffice).build())
                .build());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(userService.getDocumentationOffice(oidcUser)).thenReturn(documentationOffice);

    // Act
    Function<UUID, Boolean> result = service.userHasSameDocumentationOffice();

    // Assert
    assertThat(result.apply(uuid)).isTrue();
  }

  @Test
  void test_isAssignedViaProcedure_withoutDocumentationUnit_shouldReturnFalse()
      throws DocumentationUnitNotExistsException {

    // Arrange
    UUID uuid = UUID.randomUUID();
    when(documentationUnitService.getByUuid(uuid)).thenReturn(null);

    // Act
    Function<UUID, Boolean> result = service.isAssignedViaProcedure();

    // Assert
    assertThat(result.apply(uuid)).isFalse();
  }

  @Test
  void test_isAssignedViaProcedure_withAssignedProcedure_shouldReturnTrue()
      throws DocumentationUnitNotExistsException {

    // Arrange
    UUID documentationUnitId = UUID.randomUUID();
    UUID userGroupId = UUID.randomUUID();
    SecurityContextHolder.setContext(securityContext);
    when(documentationUnitService.getByUuid(documentationUnitId))
        .thenReturn(
            DocumentationUnit.builder()
                .coreData(
                    CoreData.builder()
                        .procedure(Procedure.builder().userGroupId(userGroupId).build())
                        .build())
                .build());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(userService.getUserGroup(oidcUser))
        .thenReturn(Optional.ofNullable(UserGroup.builder().id(userGroupId).build()));

    // Act
    Function<UUID, Boolean> result = service.isAssignedViaProcedure();

    // Assert
    assertThat(result.apply(documentationUnitId)).isTrue();
  }

  @Test
  void test_isAssignedViaProcedure_withUnAssignedProcedure_shouldReturnFalse()
      throws DocumentationUnitNotExistsException {

    // Arrange
    UUID documentationUnitId = UUID.randomUUID();
    UUID userGroupId = UUID.randomUUID();
    SecurityContextHolder.setContext(securityContext);
    when(documentationUnitService.getByUuid(documentationUnitId))
        .thenReturn(
            DocumentationUnit.builder()
                .coreData(
                    CoreData.builder()
                        .procedure(Procedure.builder().userGroupId(null).build())
                        .build())
                .build());
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(oidcUser);
    when(userService.getUserGroup(oidcUser))
        .thenReturn(Optional.ofNullable(UserGroup.builder().id(userGroupId).build()));

    // Act
    Function<UUID, Boolean> result = service.isAssignedViaProcedure();

    // Assert
    assertThat(result.apply(documentationUnitId)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/previousDecisions",
        "/ensuingDecisions",
        "/contentRelatedIndexing/keywords",
        "/contentRelatedIndexing/fieldsOfLaw",
        "/contentRelatedIndexing/norms",
        "/contentRelatedIndexing/activeCitations",
        "/texts/decisionName",
        "/texts/headline",
        "/texts/guidingPrinciple",
        "/texts/headnote",
        "/texts/otherHeadnote",
        "/note",
        "/version"
      })
  void test_isPatchAllowedForExternalUsers_withAllowedPath_shouldReturnTrue(String path) {
    // Arrange
    List<JsonPatchOperation> operations = List.of(new AddOperation(path, new TextNode("anyValue")));
    RisJsonPatch patch = RisJsonPatch.builder().patch(new JsonPatch(operations)).build();

    // Act
    Function<RisJsonPatch, Boolean> result = service.isPatchAllowedForExternalUsers();

    // Assert
    assertThat(result.apply(patch)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/coreData/court",
        "/coreData/deviatingCourts",
        "/coreData/fileNumbers",
        "/coreData/deviatingFileNumbers",
        "/coreData/decisionDate",
        "/coreData/deviatingDecisionDates",
        "/coreData/appraisalBody",
        "/coreData/documentType",
        "/coreData/ecli",
        "/coreData/deviatingEclis",
        "/coreData/procedure",
        "/coreData/procedure/createdAt",
        "/coreData/procedure/documentationUnitCount",
        "/coreData/procedure/label",
        "/coreData/procedure/id",
        "/coreData/legalEffect",
        "/coreData/leadingDecisionNormReferences",
        "/coreData/yearsOfDispute",
        "/texts/tenor",
        "/texts/reasons",
        "/texts/caseFacts",
        "/texts/decisionReasons",
        "/texts/dissentingOpinion",
        "/texts/otherLongText",
        "/texts/outline",
        "/references"
      })
  void test_isPatchAllowedForExternalUsers_withProhibitedPath_shouldReturnFalse(String path) {
    // Arrange
    List<JsonPatchOperation> operations = List.of(new AddOperation(path, new TextNode("anyValue")));
    RisJsonPatch patch = RisJsonPatch.builder().patch(new JsonPatch(operations)).build();

    // Act
    Function<RisJsonPatch, Boolean> result = service.isPatchAllowedForExternalUsers();

    // Assert
    assertThat(result.apply(patch)).isFalse();
  }

  @Test
  void test_isPatchAllowedForExternalUsers_withProhibitedPathInTestOperation_shouldReturnTrue() {
    // Arrange
    JsonNode anyValue = new TextNode("newValue");
    String firstAllowedPath = "/previousDecisions";
    String secondAllowedPath = "/ensuingDecisions";
    String thirdAllowedPath = "/contentRelatedIndexing/keywords";
    String prohibitedPath = "/coreData/court";
    List<JsonPatchOperation> operations =
        List.of(
            new AddOperation(firstAllowedPath, new TextNode("anyValue")),
            new ReplaceOperation(secondAllowedPath, anyValue),
            new RemoveOperation(thirdAllowedPath),
            new TestOperation(prohibitedPath, anyValue));
    RisJsonPatch patch = RisJsonPatch.builder().patch(new JsonPatch(operations)).build();

    // Act
    Function<RisJsonPatch, Boolean> result = service.isPatchAllowedForExternalUsers();

    // Assert
    assertThat(result.apply(patch)).isTrue();
  }

  @Test
  void test_isPatchAllowedForExternalUsers_withAtLeastOneProhibitedPath_shouldReturnFalse() {
    // Arrange
    JsonNode anyValue = new TextNode("newValue");
    String firstAllowedPath = "/previousDecisions";
    String secondAllowedPath = "/ensuingDecisions";
    String prohibitedPath = "/coreData/court";
    List<JsonPatchOperation> operations =
        List.of(
            new AddOperation(firstAllowedPath, new TextNode("anyValue")),
            new ReplaceOperation(secondAllowedPath, anyValue),
            new RemoveOperation(prohibitedPath));
    RisJsonPatch patch = RisJsonPatch.builder().patch(new JsonPatch(operations)).build();

    // Act
    Function<RisJsonPatch, Boolean> result = service.isPatchAllowedForExternalUsers();

    // Assert
    assertThat(result.apply(patch)).isFalse();
  }
}
