package de.bund.digitalservice.ris.caselaw.adapter;

import com.gravity9.jsonpatch.JsonPatchOperation;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.ApiKeyDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseApiKeyRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationOfficeRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.transformer.ApiKeyTransformer;
import de.bund.digitalservice.ris.caselaw.domain.ApiKey;
import de.bund.digitalservice.ris.caselaw.domain.AuthService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnit;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitService;
import de.bund.digitalservice.ris.caselaw.domain.Procedure;
import de.bund.digitalservice.ris.caselaw.domain.ProcedureService;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.RisJsonPatch;
import de.bund.digitalservice.ris.caselaw.domain.Status;
import de.bund.digitalservice.ris.caselaw.domain.UserGroup;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.exception.DocumentationUnitNotExistsException;
import de.bund.digitalservice.ris.caselaw.domain.exception.ImportApiKeyException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Service responsible for handling authorization checks for users.
 *
 * <p>This service provides functionality to verify user access rights to various resources,
 * including {@link DocumentationUnit}s and {@link Procedure}s, based on user roles and assigned
 * permissions.
 *
 * <p>Key methods include:
 *
 * <ul>
 *   <li>{@link #userHasReadAccessByDocumentNumber()}: Checks if a user has read access to a {@link
 *       DocumentationUnit} by its {@link DocumentationUnit#documentNumber() documentNumber}.
 *   <li>{@link #userHasReadAccessByDocumentationUnitId()}: Checks if a user has read access to a
 *       {@link DocumentationUnit} by its {@link UUID}.
 *   <li>{@link #userIsInternal()}: Determines if a user is an internal user based on their {@link
 *       OidcUser} roles.
 *   <li>{@link #userHasWriteAccessByProcedureId()}: Checks if a user has write access to a {@link
 *       Procedure} by its {@link UUID}.
 *   <li>{@link #userHasWriteAccess()}: Checks if a user has the same {@link DocumentationOffice} as
 *       the {@link DocumentationUnit} by its {@link UUID}
 *   <li>{@link #isAssignedViaProcedure()}: Checks if a {@link Procedure} associated with a {@link
 *       DocumentationUnit} is assigned to the current {@link OidcUser}
 *   <li>{@link #isPatchAllowedForExternalUsers()}: Checks if a {@link RisJsonPatch} operation is
 *       permitted for external users.
 * </ul>
 */
@Service
@Slf4j
public class OAuthService implements AuthService {

  private final UserService userService;
  private final DocumentationUnitService documentationUnitService;
  private final ProcedureService procedureService;
  private final DatabaseApiKeyRepository keyRepository;
  private final DatabaseDocumentationOfficeRepository officeRepository;
  private static final List<String> allowedPaths =
      List.of(
          "/previousDecisions",
          "/ensuingDecisions",
          "/contentRelatedIndexing/keywords",
          "/contentRelatedIndexing/fieldsOfLaw",
          "/contentRelatedIndexing/norms",
          "/contentRelatedIndexing/activeCitations",
          "/contentRelatedIndexing/jobProfiles",
          "/contentRelatedIndexing/dismissalGrounds",
          "/contentRelatedIndexing/dismissalTypes",
          "/contentRelatedIndexing/collectiveAgreements",
          "/contentRelatedIndexing/hasLegislativeMandate",
          "/shortTexts/decisionName",
          "/shortTexts/headline",
          "/shortTexts/guidingPrinciple",
          "/shortTexts/headnote",
          "/shortTexts/otherHeadnote",
          "/note",
          "/version");

  public OAuthService(
      UserService userService,
      DocumentationUnitService documentationUnitService,
      ProcedureService procedureService,
      DatabaseApiKeyRepository keyRepository,
      DatabaseDocumentationOfficeRepository officeRepository) {

    this.userService = userService;
    this.documentationUnitService = documentationUnitService;
    this.procedureService = procedureService;
    this.keyRepository = keyRepository;
    this.officeRepository = officeRepository;
  }

  /**
   * Creates a Spring bean that checks if a user has read access to a {@link DocumentationUnit} by
   * its {@link DocumentationUnit#documentNumber() documentNumber}.
   *
   * <p>The function retrieves the {@link DocumentationUnit} using the {@link
   * DocumentationUnit#documentNumber() documentNumber} and checks if the user has read access.
   * Returns {@link Boolean#FALSE false} if the {@link DocumentationUnit} is not found.
   *
   * @return a {@link Function} that accepts a {@link DocumentationUnit#documentNumber()
   *     documentNumber} as {@link String} and returns {@link Boolean#TRUE true} if the user has
   *     read access, otherwise {@link Boolean#FALSE false}.
   */
  @Bean
  public Function<String, Boolean> userHasReadAccessByDocumentNumber() {
    return documentNumber -> {
      try {
        return Optional.ofNullable(documentationUnitService.getByDocumentNumber(documentNumber))
            .map(this::userHasReadAccess)
            .orElse(false);
      } catch (DocumentationUnitNotExistsException ex) {
        return false;
      }
    };
  }

  /**
   * Creates a Spring bean that checks if a user has read access to a {@link DocumentationUnit} by
   * its {@link UUID}.
   *
   * <p>The function retrieves the {@link DocumentationUnit} and checks if the user has read access.
   * Returns {@link Boolean#FALSE false} if the {@link DocumentationUnit} is not found.
   *
   * @return a {@link Function} that accepts a {@link UUID} and returns {@link Boolean#TRUE true} if
   *     the user has read access, otherwise {@link Boolean#FALSE false}.
   */
  @Bean
  public Function<UUID, Boolean> userHasReadAccessByDocumentationUnitId() {
    return uuid -> {
      try {
        return Optional.ofNullable(documentationUnitService.getByUuid(uuid))
            .map(this::userHasReadAccess)
            .orElse(false);
      } catch (DocumentationUnitNotExistsException ex) {
        return false;
      }
    };
  }

  /**
   * Defines a Spring bean that returns a function to check if a user has the internal role.
   *
   * <p>This bean provides a function that takes an {@link OidcUser} as input and returns a {@link
   * Boolean} indicating whether the user has the internal role or not. It delegates this check to
   * the {@link UserService#isInternal(OidcUser)} method.
   *
   * @return a {@link Function} that accepts an {@link OidcUser} and returns a {@link Boolean}
   *     value. {@link Boolean#TRUE true} if the user has the internal role, {@link Boolean#FALSE
   *     false} otherwise.
   */
  @Bean
  public Function<OidcUser, Boolean> userIsInternal() {
    return userService::isInternal;
  }

  /**
   * Creates a Spring bean that checks if a user has write access to a procedure by its UUID.
   *
   * <p>The function retrieves the {@link Procedure}'s {@link DocumentationOffice} and verifies if
   * the user has the same {@link DocumentationOffice}. Returns {@link Boolean#FALSE false} if the
   * {@link Procedure} is not found.
   *
   * @return a {@link Function} that accepts a {@link UUID} and returns {@link Boolean#TRUE true} if
   *     the user has write access, otherwise {@link Boolean#FALSE false}.
   */
  @Bean
  public Function<UUID, Boolean> userHasWriteAccessByProcedureId() {
    return uuid ->
        Optional.ofNullable(procedureService.getDocumentationOfficeByUUID(uuid))
            .map(this::userHasSameDocOfficeAsProcedure)
            .orElse(false);
  }

  /**
   * Creates a Spring bean that checks if a user has the same {@link DocumentationOffice} as the
   * {@link DocumentationUnit} by its {@link UUID}.
   *
   * <p>The function retrieves the {@link DocumentationUnit} by its {@link UUID} and verifies if the
   * user has the same {@link DocumentationOffice}. Returns {@link Boolean#FALSE false} if the
   * {@link DocumentationUnit} is not found.
   *
   * @return a {@link Function} that accepts a {@link UUID} and returns {@link Boolean#TRUE true} if
   *     the user has the same {@link DocumentationOffice}, otherwise {@link Boolean#FALSE false}.
   */
  @Bean
  public Function<UUID, Boolean> userHasWriteAccess() {
    return uuid -> {
      try {
        return Optional.ofNullable(documentationUnitService.getByUuid(uuid))
            .map(this::userHasWriteAccess)
            .orElse(false);
      } catch (DocumentationUnitNotExistsException e) {
        return false;
      }
    };
  }

  @Bean
  public Function<String, Boolean> userHasSameDocOfficeAsDocument() {
    return documentNumber ->
        getUserDocumentationOffice()
            .flatMap(
                userOffice -> {
                  try {
                    return Optional.ofNullable(
                            documentationUnitService.getByDocumentNumber(documentNumber))
                        .map(
                            documentationUnit ->
                                userHasSameDocOfficeAsDocument(
                                    userOffice,
                                    documentationUnit.coreData().documentationOffice()));
                  } catch (DocumentationUnitNotExistsException e) {
                    return Optional.of(false);
                  }
                })
            .orElse(false);
  }

  /**
   * Creates a Spring bean that checks if a {@link Procedure} associated with a {@link
   * DocumentationUnit} is assigned to the current {@link OidcUser}.
   *
   * <p>The function retrieves the {@link Procedure} of the {@link DocumentationUnit} by the given
   * {@link UUID} and verifies if it is assigned to the current {@link OidcUser}. Returns {@link
   * Boolean#FALSE false} if no {@link DocumentationUnit} or {@link Procedure} is found.
   *
   * @return a {@link Function} that accepts a {@link UUID} and returns {@link Boolean#TRUE true} if
   *     the {@link Procedure} is assigned to the current user, otherwise {@link Boolean#FALSE
   *     false}.
   */
  @Bean
  public Function<UUID, Boolean> isAssignedViaProcedure() {
    return uuid -> {
      try {
        var documentationUnit = Optional.ofNullable(documentationUnitService.getByUuid(uuid));
        Optional<OidcUser> oidcUser = getOidcUser();
        if (documentationUnit.isPresent() && oidcUser.isPresent()) {
          var procedure = documentationUnit.get().coreData().procedure();
          if (procedure != null) {
            return isProcedureAssignedToUser(procedure, oidcUser.get());
          }
        }
        return false;
      } catch (DocumentationUnitNotExistsException ex) {
        return false;
      }
    };
  }

  /**
   * Creates a Spring bean that checks if a given {@link RisJsonPatch} is allowed for users with the
   * external role.
   *
   * <p>The function filters non-test operations, normalizes the patch paths, and verifies that all
   * modified paths are in the list of allowed paths.
   *
   * @return a {@link Function} that accepts a {@link RisJsonPatch} and returns {@link Boolean#TRUE
   *     true} if all patch operations are allowed, otherwise {@link Boolean#FALSE false}.
   */
  @Bean
  public Function<RisJsonPatch, Boolean> isPatchAllowedForExternalUsers() {
    return patch ->
        patch.patch().getOperations().stream()
            .filter(jsonPatchOperation -> !jsonPatchOperation.getOp().equals("test"))
            .map(JsonPatchOperation::getPath)
            .map(path -> path.replaceAll("/\\d$", "")) // remove index
            .allMatch(allowedPaths::contains);
  }

  private boolean isProcedureAssignedToUser(Procedure procedure, OidcUser oidcUser) {
    var userGroupIdOfUser = userService.getUserGroup(oidcUser).map(UserGroup::id).orElse(null);
    return procedure.userGroupId() != null && procedure.userGroupId().equals(userGroupIdOfUser);
  }

  private boolean userHasWriteAccess(DocumentationUnit documentationUnit) {
    return getUserDocumentationOffice()
        .map(
            userOffice ->
                userHasSameDocOfficeAsDocument(
                        userOffice, documentationUnit.coreData().documentationOffice())
                    || (documentationUnit.status() != null
                        && isPendingStatus(documentationUnit.status())
                        && userHasSameDocOfficeAsDocumentCreator(
                            userOffice, documentationUnit.coreData().creatingDocOffice())))
        .orElse(false);
  }

  @Override
  public boolean userHasWriteAccess(
      OidcUser oidcUser,
      DocumentationOffice creatingDocOffice,
      DocumentationOffice documentationOffice,
      Status status) {
    DocumentationOffice userDocumentationOffice = userService.getDocumentationOffice(oidcUser);
    return userHasSameDocOfficeAsDocument(userDocumentationOffice, documentationOffice)
        || (status != null
            && isPendingStatus(status)
            && userHasSameDocOfficeAsDocumentCreator(userDocumentationOffice, creatingDocOffice));
  }

  private boolean userHasReadAccess(DocumentationUnit documentationUnit) {
    return documentationUnit.status() == null
        || isPublishedStatus(documentationUnit.status())
        || userHasWriteAccess(documentationUnit);
  }

  private boolean isPublishedStatus(Status status) {
    return status != null
        && List.of(PublicationStatus.PUBLISHED, PublicationStatus.PUBLISHING)
            .contains(status.publicationStatus());
  }

  private boolean isPendingStatus(Status status) {
    return status != null
        && PublicationStatus.EXTERNAL_HANDOVER_PENDING.equals(status.publicationStatus());
  }

  private boolean userHasSameDocOfficeAsDocumentCreator(
      DocumentationOffice userDocumentationOffice, DocumentationOffice creatingDocOffice) {
    return creatingDocOffice != null && creatingDocOffice.equals(userDocumentationOffice);
  }

  private boolean userHasSameDocOfficeAsDocument(
      DocumentationOffice userDocumentationOffice, DocumentationOffice documentationOffice) {
    return documentationOffice.equals(userDocumentationOffice);
  }

  private boolean userHasSameDocOfficeAsProcedure(DocumentationOffice documentationOffice) {
    Optional<OidcUser> oidcUser = getOidcUser();
    if (oidcUser.isPresent()) {
      DocumentationOffice documentationOfficeOfUser =
          userService.getDocumentationOffice(oidcUser.get());
      return documentationOffice.equals(documentationOfficeOfUser);
    }
    return false;
  }

  private Optional<OidcUser> getOidcUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof OidcUser principal) {
      return Optional.of(principal);
    }
    return Optional.empty();
  }

  private Optional<DocumentationOffice> getUserDocumentationOffice() {
    return getOidcUser().map(userService::getDocumentationOffice);
  }

  /**
   * Generate an api key for the importer and for the user if the user doesn't have a valid one.
   * <br>
   * In the case of the existence of a key the method throws a {@link ImportApiKeyException}. <br>
   * The key was generated is 64 character long and contains a random sequence of lowercase and
   * uppercase letters and digits. The key is 30 days valid.
   *
   * @param oidcUser current user via openid connect system
   * @return the generated api key
   */
  public ApiKey generateImportApiKey(OidcUser oidcUser) {
    Optional<ApiKeyDTO> apiKeyOptional =
        keyRepository.findByUserAccountAndValidUntilAfter(oidcUser.getEmail(), Instant.now());

    if (apiKeyOptional.isPresent()) {
      log.error(
          "No new import api key couldn't generate because a valid api key for the user exist.");
      throw new ImportApiKeyException(
          "No new import api key couldn't generate because a valid api key for the user exist.");
    }

    char[][] allowedCharacters = {{'a', 'z'}, {'A', 'Z'}, {'0', '9'}};
    // " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
    RandomStringGenerator rsg =
        RandomStringGenerator.builder().withinRange(allowedCharacters).get();

    DocumentationOffice documentationOffice = userService.getDocumentationOffice(oidcUser);
    DocumentationOfficeDTO documentationOfficeDTO = null;
    if (documentationOffice != null) {
      documentationOfficeDTO =
          officeRepository.findByAbbreviation(documentationOffice.abbreviation());
    }

    ApiKeyDTO apiKeyDTO =
        ApiKeyDTO.builder()
            .apiKey(rsg.generate(64))
            .userAccount(oidcUser.getEmail())
            .documentationOffice(documentationOfficeDTO)
            .createdAt(Instant.now())
            .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
            .build();

    keyRepository.save(apiKeyDTO);

    return ApiKeyTransformer.transformToDomain(apiKeyDTO);
  }

  /**
   * Get the api key for the importer and the user.
   *
   * @param oidcUser current user via openid connect system
   * @return the last/current valid api key or null, if no key exists
   */
  public ApiKey getImportApiKey(OidcUser oidcUser) {
    Optional<ApiKeyDTO> apiKeyOptional =
        keyRepository.findFirstByUserAccountOrderByValidUntilDesc(oidcUser.getEmail());
    return apiKeyOptional.map(ApiKeyTransformer::transformToDomain).orElse(null);
  }

  /**
   * Invalidate the api key for the importer and the user. <br>
   * Throws an {@link ImportApiKeyException} if the api key doesn't exist in the database or the api
   * key doesn't belong to the user or after the invalidation no api key exist for the user.
   *
   * @param oidcUser current user via openid connect system
   * @param apiKey api key which should invalidated
   * @return the last/current valid api key or throws an {@link ImportApiKeyException}, if no key
   *     exists
   */
  public ApiKey invalidateImportApiKey(OidcUser oidcUser, String apiKey) {
    Optional<ApiKeyDTO> apiKeyOptional = keyRepository.findByApiKey(apiKey);

    if (apiKeyOptional.isEmpty()) {
      log.error("Can't invalidate api key '{}' because it doesn't exist!", apiKey);
      throw new ImportApiKeyException("Can't invalidate api key because it doesn't exist!");
    }
    if (apiKeyOptional.get().getUserAccount() == null
        || !apiKeyOptional.get().getUserAccount().equals(oidcUser.getEmail())) {

      log.error("api key doesn't belong to user");
      throw new ImportApiKeyException("Api key doesn't belong to user");
    }

    ApiKeyDTO apiKeyDTO =
        apiKeyOptional.get().toBuilder().validUntil(Instant.now()).invalidated(true).build();
    keyRepository.save(apiKeyDTO);

    apiKeyOptional = keyRepository.findFirstByUserAccountOrderByValidUntilDesc(oidcUser.getEmail());
    return apiKeyOptional
        .map(ApiKeyTransformer::transformToDomain)
        .orElseThrow(ImportApiKeyException::new);
  }
}
