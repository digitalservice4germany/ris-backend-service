package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "lookuptable_field_of_law")
@Deprecated
public class JPAFieldOfLawDTO {

  @Id Long id;

  @ManyToOne
  @JoinColumn(name = "parent_id", referencedColumnName = "id")
  JPAFieldOfLawDTO parentFieldOfLaw;

  @Column(name = "change_date_mail")
  String changeDateMail;

  @Column(name = "change_date_client")
  String changeDateClient;

  @Column(name = "change_indicator")
  char changeIndicator;

  String version;

  @Column(name = "identifier")
  String identifier;

  @Column(name = "text")
  String text;

  @Column(name = "navigation_term")
  String navigationTerm;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "field_of_law_id")
  Set<JPAKeywordDTO> keywords;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "field_of_law_id")
  Set<JPANormDTO> norms;

  @Column(name = "children_count")
  Integer childrenCount;

  public String getIdentifierOfParent() {
    int lastIndexOf = identifier.lastIndexOf('-');

    if (lastIndexOf == -1) {
      return null;
    }

    return identifier.substring(0, lastIndexOf);
  }
}
