package de.bund.digitalservice.ris.caselaw.domain;

import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import lombok.Builder;
import org.hibernate.validator.constraints.UniqueElements;

@Builder(toBuilder = true)
public record CoreData(
    @UniqueElements List<String> fileNumbers,
    @UniqueElements List<String> deviatingFileNumbers,
    Court court,
    @UniqueElements List<String> deviatingCourts,
    DocumentType documentType,
    Procedure procedure,
    List<String> previousProcedures,
    String ecli,
    @UniqueElements List<String> deviatingEclis,
    String appraisalBody,
    @PastOrPresent LocalDate decisionDate,
    @UniqueElements List<LocalDate> deviatingDecisionDates,
    String legalEffect,
    List<String> inputTypes,
    DocumentationOffice documentationOffice,
    DocumentationOffice creatingDocOffice,
    String region,
    @UniqueElements List<String> leadingDecisionNormReferences,
    List<@PastOrPresent Year> yearsOfDispute) {}
