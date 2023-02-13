package de.bund.digitalservice.ris.caselaw.domain.lookuptable.subjectfield;

import java.util.List;
import lombok.Builder;

@Builder
public record SubjectField(
    Long id,
    String subjectFieldNumber,
    String subjectFieldText,
    String navigationTerm,
    List<Keyword> keywords,
    List<Norm> norms) {}
