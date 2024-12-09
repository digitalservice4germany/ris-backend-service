package de.bund.digitalservice.ris.caselaw.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record ShortTexts(
    String decisionName,
    String headline,
    String guidingPrinciple,
    String headnote,
    String otherHeadnote) {}
