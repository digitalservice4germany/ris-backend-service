package de.bund.digitalservice.ris.caselaw.domain;

import lombok.Builder;

@Builder
public record Texts(
    String decisionName,
    String headline,
    String guidingPrinciple,
    String headnote,
    String otherHeadnote,
    String tenor,
    String reasons,
    String caseFacts,
    String decisionReasons,
    String dissentingOpinion,
    String otherLongText,
    String outline) {}
