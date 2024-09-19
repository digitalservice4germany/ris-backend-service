package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.lookuptable.ParticipatingJudge;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.fieldoflaw.FieldOfLaw;
import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
public record ContentRelatedIndexing(
    List<String> keywords,
    List<String> jobProfiles,
    List<FieldOfLaw> fieldsOfLaw,
    List<NormReference> norms,
    List<ActiveCitation> activeCitations,
    List<String> dismissalGrounds,
    List<String> dismissalTypes,
    boolean hasLegislativeMandate,
    List<ParticipatingJudge> participatingJudges) {}
