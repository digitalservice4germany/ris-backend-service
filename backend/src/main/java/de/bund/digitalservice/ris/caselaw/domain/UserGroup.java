package de.bund.digitalservice.ris.caselaw.domain;

import java.util.UUID;
import lombok.Builder;

@Builder
public record UserGroup(
    UUID id, String userGroupPathName, DocumentationOffice docOffice, boolean isInternal) {}
