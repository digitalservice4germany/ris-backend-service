package de.bund.digitalservice.ris.caselaw;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import de.bund.digitalservice.ris.caselaw.adapter.KeycloakUserService;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.UserGroup;
import de.bund.digitalservice.ris.caselaw.domain.UserGroupService;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

public class AuthUtils {

  public static OidcLoginRequestPostProcessor getMockLogin() {
    return getMockLoginWithDocOffice("/DS", "Internal");
  }

  public static OidcLoginRequestPostProcessor getMockLoginExternal() {
    return getMockLoginWithDocOffice("/DS/Extern", "External");
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
    doReturn(true).when(userService).isInternal(any());
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

  public static DocumentationOffice buildDSDocOffice() {
    return DocumentationOffice.builder()
        .abbreviation("DS")
        .uuid(UUID.fromString("ba90a851-3c54-4858-b4fa-7742ffbe8f05"))
        .build();
  }

  public static DocumentationOffice buildCCRisDocOffice() {
    return DocumentationOffice.builder()
        .abbreviation("CC-RIS")
        .uuid(UUID.fromString("f13c2fdb-5323-49aa-bc6d-09fa68c3acb9"))
        .build();
  }

  public static DocumentationOffice buildBGHDocOffice() {
    return DocumentationOffice.builder()
        .abbreviation("BGH")
        .uuid(UUID.fromString("41e62dbc-e5b6-414f-91e2-0cfe559447d1"))
        .build();
  }

  public static void mockUserGroups(UserGroupService userGroupService) {
    doReturn(
            List.of(
                UserGroup.builder()
                    .docOffice(buildDSDocOffice())
                    .userGroupPathName("/DS")
                    .isInternal(true)
                    .build(),
                UserGroup.builder()
                    .id(UUID.fromString("2b733549-d2cc-40f0-b7f3-9bfa9f3c1b89"))
                    .docOffice(buildDSDocOffice())
                    .userGroupPathName("/DS/Extern")
                    .isInternal(false)
                    .build(),
                UserGroup.builder()
                    .docOffice(buildBGHDocOffice())
                    .userGroupPathName("/BGH")
                    .isInternal(true)
                    .build(),
                UserGroup.builder()
                    .id(UUID.fromString("3b733549-d2cc-40f0-b7f3-9bfa9f3c1b89"))
                    .docOffice(buildBGHDocOffice())
                    .userGroupPathName("/BGH/Extern")
                    .isInternal(true)
                    .build(),
                UserGroup.builder()
                    .docOffice(buildCCRisDocOffice())
                    .userGroupPathName("/CC-RIS")
                    .isInternal(true)
                    .build()))
        .when(userGroupService)
        .getAllUserGroups();
  }
}
