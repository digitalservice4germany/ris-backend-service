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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(
    schema = "incremental_migration",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"juris_id", "notation"})},
    name = "field_of_law")
public class FieldOfLawDTO {
  @Id @GeneratedValue private UUID id;

  @EqualsAndHashCode.Include
  @Column(unique = true, updatable = false, insertable = false)
  private String identifier;

  @EqualsAndHashCode.Include
  @Column(updatable = false, insertable = false)
  private String text;

  // Not used yet
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinTable(
      schema = "incremental_migration",
      name = "field_of_law_field_of_law_navigation_term",
      joinColumns = @JoinColumn(name = "field_of_law_id"),
      inverseJoinColumns = @JoinColumn(name = "field_of_law_navigation_term_id"))
  @Valid
  private NavigationTermDTO navigationTerm;

  // Not used yet
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      schema = "incremental_migration",
      name = "field_of_law_field_of_law_keyword",
      joinColumns = @JoinColumn(name = "field_of_law_id"),
      inverseJoinColumns = @JoinColumn(name = "field_of_law_keyword_id"))
  @Builder.Default
  private List<FieldOfLawKeywordDTO> keywords = new ArrayList<>();

  @OneToMany(
      mappedBy = "fieldOfLaw",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @Valid
  @OrderBy("abbreviation")
  @Builder.Default
  private Set<FieldOfLawNormDTO> norms = new HashSet<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinTable(
      schema = "incremental_migration",
      name = "field_of_law_field_of_law_parent",
      joinColumns = @JoinColumn(name = "field_of_law_id"),
      inverseJoinColumns = @JoinColumn(name = "field_of_law_parent_id"))
  private FieldOfLawDTO parent;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
  @OrderBy("identifier")
  @Builder.Default
  private Set<FieldOfLawDTO> children = new HashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      schema = "incremental_migration",
      name = "field_of_law_field_of_law_text_reference",
      joinColumns = @JoinColumn(name = "field_of_law_id"),
      inverseJoinColumns = @JoinColumn(name = "field_of_law_text_reference_id"))
  @Builder.Default
  private Set<FieldOfLawDTO> fieldOfLawTextReferences = new HashSet<>();

  @Column(updatable = false, name = "juris_id")
  @ToString.Include
  private Integer jurisId;

  @EqualsAndHashCode.Include
  @Column(updatable = false)
  @Enumerated(EnumType.STRING)
  @ToString.Include
  private Notation notation;
}
