package de.bund.digitalservice.ris.caselaw.domain;

import lombok.Builder;

@Builder
public record Source(SourceValue value, String sourceRawValue) {}
