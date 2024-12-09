package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Include;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "documentation_unit", schema = "incremental_migration")
@SuppressWarnings(
    "java:S6539") // This class depends on many classes, because it's the key part and merging
// everything.
public class DocumentationUnitDTO implements DocumentationUnitListItemDTO {

  @Id @GeneratedValue @Include private UUID id;

  private Long version;

  @Column(name = "case_facts")
  private String caseFacts;

  @Column(name = "decision_date")
  private LocalDate decisionDate;

  @Column(name = "last_publication_date_time")
  private LocalDateTime lastPublicationDateTime;

  @Column(name = "scheduled_publication_date_time")
  private LocalDateTime scheduledPublicationDateTime;

  @Column(name = "scheduled_by_email")
  private String scheduledByEmail;

  @Column(name = "decision_grounds")
  private String decisionGrounds;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  private List<DecisionNameDTO> decisionNames = new ArrayList<>();

  @Column(nullable = false, unique = true, updatable = false, name = "document_number")
  @NotBlank
  @Include
  private String documentNumber;

  @ManyToOne
  @JoinColumn(name = "document_type_id")
  private DocumentTypeDTO documentType;

  @Column private String ecli;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<FileNumberDTO> fileNumbers = new ArrayList<>();

  @Column private String grounds;

  @Column(name = "guiding_principle")
  private String guidingPrinciple;

  @Column private String headline;

  @Column private String headnote;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<InputTypeDTO> inputTypes = new ArrayList<>();

  @Column(name = "judicial_body")
  private String judicialBody;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<NormReferenceDTO> normReferences = new ArrayList<>();

  @Builder.Default
  @OneToMany(
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      mappedBy = "documentationUnit")
  @OrderBy("uploadTimestamp asc")
  private List<AttachmentDTO> attachments = new ArrayList<>();

  @Column(name = "other_long_text")
  String otherLongText;

  @Column(name = "outline")
  String outline;

  @Column(name = "other_headnote")
  String otherHeadnote;

  @Column(name = "dissenting_opinion")
  private String dissentingOpinion;

  @OneToMany(
      mappedBy = "documentationUnit",
      orphanRemoval = true,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OrderBy("rank")
  @Builder.Default
  private List<DocumentationUnitProcedureDTO> procedures = new ArrayList<>();

  @ManyToMany(
      cascade = {CascadeType.MERGE},
      fetch = FetchType.LAZY)
  @JoinTable(
      name = "documentation_unit_region",
      schema = "incremental_migration",
      joinColumns = @JoinColumn(name = "documentation_unit_id"),
      inverseJoinColumns = @JoinColumn(name = "region_id"))
  @Builder.Default
  private List<RegionDTO> regions = new ArrayList<>();

  @OneToMany(
      mappedBy = "documentationUnit",
      orphanRemoval = true,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OrderBy("rank")
  @Builder.Default
  private List<DocumentationUnitFieldOfLawDTO> documentationUnitFieldsOfLaw = new ArrayList<>();

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<SourceDTO> source = new ArrayList<>();

  @Column private String tenor;

  @Column(name = "legal_effect")
  @Enumerated(EnumType.STRING)
  private LegalEffectDTO legalEffect;

  @ManyToOne(optional = false)
  @NotNull
  @JoinColumn(name = "documentation_office_id", referencedColumnName = "id")
  private DocumentationOfficeDTO documentationOffice;

  @ManyToOne()
  @JoinColumn(name = "creating_documentation_office_id", referencedColumnName = "id")
  private DocumentationOfficeDTO creatingDocumentationOffice;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(
      name = "documentation_unit_id",
      nullable = false,
      insertable = false,
      updatable = false)
  @Builder.Default
  @OrderBy("createdAt desc")
  private List<StatusDTO> statusHistory = new ArrayList<>();

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "current_status_id")
  private StatusDTO status;

  // Gericht
  @ManyToOne
  @JoinColumn(name = "court_id", referencedColumnName = "id")
  private CourtDTO court;

  // Aktivzitierung
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<ActiveCitationDTO> activeCitations = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<DeviatingDateDTO> deviatingDates = new ArrayList<>();

  //
  //  @OneToMany(
  //      mappedBy = "documentationUnit",
  //      cascade = CascadeType.ALL,
  //      cascade = CascadeType.ALL,
  //      fetch = FetchType.EAGER,
  //      orphanRemoval = true)
  //  @Builder.Default
  //  private Set<DeviatingDocumentNumber> deviatingDocumentNumbers = new HashSet<>();
  //
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<DeviatingEcliDTO> deviatingEclis = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<DeviatingFileNumberDTO> deviatingFileNumbers = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<DeviatingCourtDTO> deviatingCourts = new ArrayList<>();

  @OneToMany(
      mappedBy = "documentationUnit",
      orphanRemoval = true,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OrderBy("rank")
  @Builder.Default
  private List<DocumentationUnitKeywordDTO> documentationUnitKeywordDTOs = new ArrayList<>();

  // Nachgehende Entscheidungen
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<EnsuingDecisionDTO> ensuingDecisions = new ArrayList<>();

  // Nachgehende Entscheidungen mit Prädikat anhängig
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<PendingDecisionDTO> pendingDecisions = new ArrayList<>();

  // Vorgehende Entscheidungen
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<PreviousDecisionDTO> previousDecisions = new ArrayList<>();

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private List<LeadingDecisionNormReferenceDTO> leadingDecisionNormReferences = new ArrayList<>();

  @Column private String note;

  // Streitjahr
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @Builder.Default
  @OrderBy("rank")
  private Set<YearOfDisputeDTO> yearsOfDispute = new HashSet<>();

  // Fundstellen
  @OneToMany(mappedBy = "documentationUnit", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @OrderBy("rank")
  private List<ReferenceDTO> references = new ArrayList<>();

  @OneToMany(mappedBy = "documentationUnit", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @OrderBy("rank")
  private List<DependentLiteratureCitationDTO> dependentLiteratureCitations = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @OrderBy("rank")
  @Builder.Default
  private List<JobProfileDTO> jobProfiles = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @OrderBy("rank")
  @Builder.Default
  private List<DismissalGroundsDTO> dismissalGrounds = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @OrderBy("rank")
  @Builder.Default
  private List<DismissalTypesDTO> dismissalTypes = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @OrderBy("rank")
  @Builder.Default
  private List<CollectiveAgreementDTO> collectiveAgreements = new ArrayList<>();

  @Column(name = "legislative_mandate")
  private boolean hasLegislativeMandate;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "documentation_unit_id", nullable = false)
  @OrderBy("rank")
  @Builder.Default
  private List<ParticipatingJudgeDTO> participatingJudges = new ArrayList<>();
}
