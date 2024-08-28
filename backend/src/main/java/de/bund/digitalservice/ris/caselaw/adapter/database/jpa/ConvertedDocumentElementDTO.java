package de.bund.digitalservice.ris.caselaw.adapter.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Database object for the converted object of a part (paragraph) of the original document */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "public", name = "converted_document_element")
public class ConvertedDocumentElementDTO {

  /** id of the element */
  @Id @GeneratedValue private UUID id;

  /** reference id to the documentation unit */
  @Column(name = "documentation_unit_id")
  private UUID documentationUnitId;

  /**
   * xml string representation of the concrete converted document element {@link
   * de.bund.digitalservice.ris.caselaw.domain.docx.DocumentationUnitDocx}
   */
  private String content;

  /** value ordering for ordering of the elements */
  private Long rank;
}
