package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.domain.ApiKey;
import de.bund.digitalservice.ris.caselaw.domain.User;
import de.bund.digitalservice.ris.caselaw.domain.UserService;
import de.bund.digitalservice.ris.caselaw.domain.exception.ImportApiKeyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

  private final UserService userService;
  private final OAuthService oAuthService;

  public AuthController(UserService userService, OAuthService oAuthService) {
    this.userService = userService;
    this.oAuthService = oAuthService;
  }

  @GetMapping(value = "me", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<User> getUser(@AuthenticationPrincipal OidcUser oidcUser) {
    return ResponseEntity.ok(userService.getUser(oidcUser));
  }

  /**
   * Get the api key for the importer and the user.
   *
   * @param oidcUser current user via openid connect system
   * @return the last/current api key for the user or null if no api key exist
   */
  @GetMapping(value = "api-key/import", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiKey> getImportApiKey(@AuthenticationPrincipal OidcUser oidcUser) {
    ApiKey apiKey = oAuthService.getImportApiKey(oidcUser);
    return ResponseEntity.ok(apiKey);
  }

  /**
   * Generate an api key for the importer and the user. <br>
   * Returns a bad response (HTTP status code: 400) if the generation throws an {@link
   * ImportApiKeyException}. This will happen if a valid key for the user exist.
   *
   * @param oidcUser current user via openid connect system
   * @return the new generated api key
   */
  @PutMapping(value = "api-key/import", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiKey> generateImportApiKey(@AuthenticationPrincipal OidcUser oidcUser) {

    ApiKey apiKey = oAuthService.generateImportApiKey(oidcUser);

    return ResponseEntity.ok(apiKey);
  }

  /**
   * Invalidate an existing api key for the importer and the user. <br>
   * Returns a bad response (HTTP status code: 400) if no api key in the header or the key doesn't
   * exist or doesn't belong to the user.
   *
   * @param oidcUser current user via openid connect system
   * @param request http request to get the header information
   * @return the last/current api key for the user
   */
  @PostMapping(value = "api-key/import/invalidate", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiKey> invalidateImportApiKey(
      @AuthenticationPrincipal OidcUser oidcUser, HttpServletRequest request) {
    String apiKey = request.getHeaders("X-API-KEY").nextElement();

    if (apiKey == null) {
      throw new ImportApiKeyException("No api key set.");
    }

    ApiKey lastApiKey = oAuthService.invalidateImportApiKey(oidcUser, apiKey);

    return ResponseEntity.ok(lastApiKey);
  }
}
