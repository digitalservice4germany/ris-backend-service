package de.bund.digitalservice.ris.caselaw.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface ProcedureService {
  Slice<Procedure> search(
      Optional<String> query,
      DocumentationOffice documentationOffice,
      Pageable pageable,
      Optional<Boolean> withDocUnits,
      OidcUser oidcUser);

  List<DocumentationUnitListItem> getDocumentationUnits(UUID procedureId, OidcUser oidcUser);

  DocumentationOffice getDocumentationOfficeByUUID(UUID uuid);

  String assignUserGroup(UUID procedureUUID, UUID userGroupId);

  String unassignUserGroup(UUID procedureUUID);

  void delete(UUID procedureId);
}
