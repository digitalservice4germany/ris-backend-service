package de.bund.digitalservice.ris.caselaw.adapter.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.CourtDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentTypeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationOfficeDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitListItemDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.FileNumberDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.SourceDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.StatusDTO;
import de.bund.digitalservice.ris.caselaw.domain.DocumentationUnitListItem;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import de.bund.digitalservice.ris.caselaw.domain.court.Court;
import de.bund.digitalservice.ris.caselaw.domain.lookuptable.documenttype.DocumentType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentationUnitListItemTransformerTest {
  @Test
  void testTransformToDomain_shouldTransformAllFields() {
    UUID id = UUID.randomUUID();
    DocumentationUnitListItemDTO currentDto =
        DocumentationUnitDTO.builder()
            .id(id)
            .note("a note")
            .court(CourtDTO.builder().type("LG").location("Berlin").build())
            .documentType(DocumentTypeDTO.builder().abbreviation("Urt").build())
            .fileNumbers(List.of(FileNumberDTO.builder().value("1 BvR 1234/19").build()))
            .decisionDate(LocalDate.parse("2021-01-01"))
            .scheduledPublicationDateTime(LocalDateTime.parse("2022-01-23T18:25:14"))
            .lastPublicationDateTime(LocalDateTime.parse("2022-01-22T18:27:18"))
            .judicialBody("1. Senat")
            .headnote("headnote")
            .creatingDocumentationOffice(
                DocumentationOfficeDTO.builder().abbreviation("DS").build())
            .source(
                List.of(
                    SourceDTO.builder().value("NJW").build(),
                    SourceDTO.builder().value("o").build()))
            .status(
                StatusDTO.builder()
                    .createdAt(Instant.now())
                    .publicationStatus(PublicationStatus.PUBLISHED)
                    .withError(false)
                    .build())
            .build();

    DocumentationUnitListItem documentationUnitListItem =
        DocumentationUnitListItemTransformer.transformToDomain(currentDto);

    // basic data
    assertThat(documentationUnitListItem.referencedDocumentationUnitId()).isEqualTo(id);
    assertThat(documentationUnitListItem.note()).isEqualTo("a note");
    assertThat(documentationUnitListItem.court())
        .isEqualTo(Court.builder().type("LG").location("Berlin").label("LG Berlin").build());
    assertThat(documentationUnitListItem.documentType())
        .isEqualTo(DocumentType.builder().jurisShortcut("Urt").build());
    assertThat(documentationUnitListItem.fileNumber()).isEqualTo("1 BvR 1234/19");
    assertThat(documentationUnitListItem.decisionDate()).isEqualTo(LocalDate.parse("2021-01-01"));
    assertThat(documentationUnitListItem.scheduledPublicationDateTime())
        .isEqualTo(LocalDateTime.parse("2022-01-23T18:25:14"));
    assertThat(documentationUnitListItem.lastPublicationDateTime())
        .isEqualTo(LocalDateTime.parse("2022-01-22T18:27:18"));
    assertThat(documentationUnitListItem.appraisalBody()).isEqualTo("1. Senat");
    assertThat(documentationUnitListItem.hasHeadnoteOrPrinciple()).isTrue();
    // source and creating doc office
    assertThat(documentationUnitListItem.creatingDocumentationOffice().abbreviation())
        .isEqualTo("DS");
    assertThat(documentationUnitListItem.source()).isEqualTo("NJW, o");
    // status
    assertThat(documentationUnitListItem.status().publicationStatus())
        .isEqualTo(PublicationStatus.PUBLISHED);
    assertThat(documentationUnitListItem.status().withError()).isFalse();
  }

  @Test
  void testTransformToDomain_withoutStatus_shouldTransformToNullStatus() {
    UUID id = UUID.randomUUID();

    DocumentationUnitListItemDTO currentDto =
        DocumentationUnitDTO.builder().id(id).status(null).build();

    DocumentationUnitListItem documentationUnitListItem =
        DocumentationUnitListItemTransformer.transformToDomain(currentDto);

    assertThat(documentationUnitListItem.referencedDocumentationUnitId()).isEqualTo(id);
    assertThat(documentationUnitListItem.status()).isNull();
  }

  @Test
  void testTransformToDomain_withoutNote_shouldHaveNoNote() {
    UUID id = UUID.randomUUID();
    DocumentationUnitListItemDTO currentDto =
        DocumentationUnitDTO.builder().id(id).note(null).build();

    DocumentationUnitListItem documentationUnitListItem =
        DocumentationUnitListItemTransformer.transformToDomain(currentDto);

    assertThat(documentationUnitListItem.referencedDocumentationUnitId()).isEqualTo(id);
    assertThat(documentationUnitListItem.note()).isNull();
  }

  @Test
  void testTransformToDomain_withoutEmptyNote_shouldHaveNoNote() {
    UUID id = UUID.randomUUID();
    DocumentationUnitListItemDTO currentDto =
        DocumentationUnitDTO.builder().id(id).note("").build();

    DocumentationUnitListItem documentationUnitListItem =
        DocumentationUnitListItemTransformer.transformToDomain(currentDto);

    assertThat(documentationUnitListItem.referencedDocumentationUnitId()).isEqualTo(id);
  }
}
