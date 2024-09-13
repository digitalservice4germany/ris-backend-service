package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.UserGroupDTO;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationOffice;
import de.bund.digitalservice.ris.caselaw.domain.UserGroup;

public class UserGroupTransformer {
  private UserGroupTransformer() {}

  public static UserGroup transformToDomain(UserGroupDTO group) {
    if (group == null) {
      return null;
    }

    return UserGroup.builder()
        .id(group.getId())
        .userGroupPathName(group.getUserGroupPathName())
        .isInternal(group.isInternal())
        .docOffice(
            DocumentationOffice.builder()
                .uuid(group.getDocumentationOffice().getId())
                .abbreviation(group.getDocumentationOffice().getAbbreviation())
                .build())
        .build();
  }
}
