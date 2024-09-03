package de.bund.digitalservice.ris.caselaw;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

public class AuthUtils {

  public static OidcLoginRequestPostProcessor getMockLogin() {
    return getMockLoginWithDocOffice("/DS");
  }

  public static OidcLoginRequestPostProcessor getMockLoginInternal() {
    return getMockLoginWithDocOffice("/DS/Intern", "Internal");
  }

  public static OidcLoginRequestPostProcessor getMockLoginExternal() {
    return getMockLoginWithDocOffice("/DS/Extern", "External");
  }

  public static OidcLoginRequestPostProcessor getMockLoginWithDocOffice(String docOfficeGroup) {
    return oidcLogin()
        .idToken(
            token ->
                token.claims(
                    claims -> {
                      claims.put("groups", Collections.singletonList(docOfficeGroup));
                      claims.put("name", "testUser");
                      claims.put("email", "test@test.com");
                    }));
  }

  public static OidcLoginRequestPostProcessor getMockLoginWithDocOffice(
      String docOfficeGroup, String role) {
    return oidcLogin()
        .idToken(
            token ->
                token.claims(
                    claims -> {
                      claims.put("groups", Collections.singletonList(docOfficeGroup));
                      claims.put("roles", Collections.singletonList(role));
                      claims.put("name", "testUser");
                      claims.put("email", "test@test.com");
                    }));
  }

  public static void setUpDocumentationOfficeMocks(
      KeycloakUserService userService,
      DocumentationOffice docOffice1,
      String docOffice1Group,
      DocumentationOffice docOffice2,
      String docOffice2Group) {
    doReturn(docOffice1)
        .when(userService)
        .getDocumentationOffice(
            argThat(
                (OidcUser user) -> {
                  List<String> groups = user.getAttribute("groups");
                  return Objects.requireNonNull(groups).get(0).equals(docOffice1Group);
                }));
    doReturn(docOffice2)
        .when(userService)
        .getDocumentationOffice(
            argThat(
                (OidcUser user) -> {
                  List<String> groups = user.getAttribute("groups");
                  return Objects.requireNonNull(groups).get(0).equals(docOffice2Group);
                }));
  }

  public static DocumentationOffice buildDocOffice(String abbreviation) {
    return DocumentationOffice.builder().abbreviation(abbreviation).build();
  }

  public static DocumentationOffice buildDefaultDocOffice() {
    return DocumentationOffice.builder().abbreviation("DS").build();
  }
}
