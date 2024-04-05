package de.bund.digitalservice.ris.caselaw.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.NormAbbreviation;
import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
// to ignore the validationErrors that the frontend might be sending along
@JsonIgnoreProperties(ignoreUnknown = true)
public record NormReference(
    NormAbbreviation normAbbreviation,
    String normAbbreviationRawValue,
    List<SingleNorm> singleNorms) {}
