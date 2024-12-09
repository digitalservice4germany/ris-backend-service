package de.bund.digitalservice.ris.caselaw.domain.lookuptable;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ParticipatingJudge(UUID id, String name, String referencedOpinions) {}
