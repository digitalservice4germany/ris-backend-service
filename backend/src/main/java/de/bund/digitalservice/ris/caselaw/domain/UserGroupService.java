package de.bund.digitalservice.ris.caselaw.domain;

import java.util.List;

/**
 * The user groups represent the groups in Bare.ID / Keycloak, however, there is no guarantee that
 * the two sets of groups are in sync.
 */
public interface UserGroupService {
  /** Get all user groups from memory. (Will only be updated after server restart.) */
  List<UserGroup> getAllUserGroups();

  List<UserGroup> getExternalUserGroups(DocumentationOffice documentationOffice);
}
