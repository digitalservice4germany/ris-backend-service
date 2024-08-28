package de.bund.digitalservice.ris.caselaw.domain;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface UserService {
  User getUser(OidcUser oidcUser);

  DocumentationOffice getDocumentationOffice(OidcUser oidcUser);

  String getEmail(OidcUser oidcUser);

  Boolean isInternal(OidcUser oidcUser);
}
