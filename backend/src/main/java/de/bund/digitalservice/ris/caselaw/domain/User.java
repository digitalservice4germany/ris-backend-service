package de.bund.digitalservice.ris.caselaw.domain;

import java.util.List;
import lombok.Builder;

@Builder
public record User(
    String name, String email, DocumentationOffice documentationOffice, List<String> roles) {}
