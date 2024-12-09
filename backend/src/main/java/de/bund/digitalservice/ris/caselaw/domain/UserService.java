package de.bund.digitalservice.ris.caselaw.domain;

import java.util.Optional;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface UserService {
  User getUser(OidcUser oidcUser);

  Optional<UserGroup> getUserGroup(OidcUser oidcUser);

  DocumentationOffice getDocumentationOffice(OidcUser oidcUser);

  String getEmail(OidcUser oidcUser);

  Boolean isInternal(OidcUser oidcUser);
}
