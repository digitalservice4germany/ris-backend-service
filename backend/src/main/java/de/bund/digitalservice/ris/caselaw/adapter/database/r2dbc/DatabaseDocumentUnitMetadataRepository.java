package de.bund.digitalservice.ris.caselaw.adapter.database.r2dbc;

import de.bund.digitalservice.ris.caselaw.domain.DataSource;
import de.bund.digitalservice.ris.caselaw.domain.PublicationStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@SuppressWarnings("java:S1192")
public interface DatabaseDocumentUnitMetadataRepository
    extends R2dbcRepository<DocumentUnitMetadataDTO, Long> {

  String OVERVIEW_SEARCH_QUERY =
      "LEFT JOIN ( "
          + "    SELECT DISTINCT ON (document_unit_id) document_unit_id, publication_status "
          + "    FROM public.status "
          + "    ORDER BY document_unit_id, created_at DESC "
          + ") AS status_subquery ON uuid = status_subquery.document_unit_id "
          + "LEFT JOIN ( "
          + "    SELECT DISTINCT ON (document_unit_id) document_unit_id, file_number, is_deviating AS filenumber_is_deviating "
          + "    FROM public.file_number "
          + ") AS file_number_subquery ON doc_unit.id = file_number_subquery.document_unit_id "
          + "WHERE "
          + "(:courtType IS NULL OR gerichtstyp = :courtType) AND "
          + "(:courtLocation IS NULL OR gerichtssitz = :courtLocation) AND "
          + "(:decisionDate IS NULL OR decision_date = :decisionDate) AND "
          + "(:status IS NULL OR status_subquery.publication_status = :status) AND "
          + "(:documentNumberOrFileNumber IS NULL OR"
          + "   (UPPER(CONCAT(documentnumber, ' ', file_number_subquery.file_number)) LIKE UPPER('%'||:documentNumberOrFileNumber||'%'))) AND "
          + "data_source in ('NEURIS', 'MIGRATION') AND ("
          + "   documentation_office_id = :documentationOfficeId OR"
          + "   (status_subquery.publication_status IS NULL OR status_subquery.publication_status IN ('PUBLISHED', 'PUBLISHING')) "
          + ")";
  String SEARCH_QUERY =
      "LEFT JOIN ( "
          + "    SELECT DISTINCT ON (document_unit_id) document_unit_id, publication_status "
          + "    FROM public.status "
          + "    ORDER BY document_unit_id, created_at DESC "
          + ") status ON uuid = status.document_unit_id "
          + "WHERE "
          + "(:courtType IS NULL OR gerichtstyp = :courtType) AND "
          + "(:courtLocation IS NULL OR gerichtssitz = :courtLocation) AND"
          + "(:decisionDate IS NULL OR decision_date = :decisionDate) AND"
          + "(:docUnitIds IS NULL OR id = ANY(:docUnitIds)) AND "
          + "(:docTypeId IS NULL OR document_type_id = :docTypeId) AND "
          + "(status.publication_status IS NULL OR status.publication_status IN ('PUBLISHED', 'PUBLISHING')) AND "
          + "data_source in ('NEURIS', 'MIGRATION') ";
  String ALL_QUERY =
      "LEFT JOIN ( "
          + "    SELECT DISTINCT ON (document_unit_id) document_unit_id, publication_status "
          + "    FROM public.status "
          + "    ORDER BY document_unit_id, created_at DESC "
          + ") status ON uuid = status.document_unit_id "
          + "WHERE data_source = :dataSource AND ( "
          + "    documentation_office_id = :documentationOfficeId OR"
          + "    status.publication_status IS NULL OR "
          + "    status.publication_status IN ('PUBLISHED', 'PUBLISHING') )";
  String ORDER_BY_DOCUMENTNUMBER =
      "ORDER BY "
          + "SUBSTRING(documentnumber, LENGTH(documentnumber) - 8, 4) DESC, "
          + "RIGHT(documentnumber, 5) DESC ";

  Mono<DocumentUnitMetadataDTO> findByUuid(UUID documentUnitUuid);

  @Query(
      "SELECT * FROM doc_unit "
          + ALL_QUERY
          + ORDER_BY_DOCUMENTNUMBER
          + "LIMIT :pageSize OFFSET :offset")
  Flux<DocumentUnitMetadataDTO> getAllDocumentUnitListEntries(
      String dataSource, UUID documentationOfficeId, Integer pageSize, Long offset);

  @Query(
      "SELECT * FROM doc_unit "
          + OVERVIEW_SEARCH_QUERY
          + ORDER_BY_DOCUMENTNUMBER
          + "LIMIT :pageSize OFFSET :offset")
  Flux<DocumentUnitMetadataDTO> searchByDocumentUnitListEntry(
      UUID documentationOfficeId,
      Integer pageSize,
      Long offset,
      String documentNumberOrFileNumber,
      String courtType,
      String courtLocation,
      Instant decisionDate,
      PublicationStatus status);

  @Query(
      "SELECT * FROM doc_unit "
          + SEARCH_QUERY
          + "ORDER BY decision_date DESC, id DESC "
          + "LIMIT :pageSize OFFSET :offset")
  Flux<DocumentUnitMetadataDTO> searchByLinkedDocumentationUnit(
      String courtType,
      String courtLocation,
      Instant decisionDate,
      Long[] docUnitIds,
      Long docTypeId,
      Integer pageSize,
      Long offset);

  @Query("SELECT COUNT(*) FROM doc_unit " + SEARCH_QUERY)
  Mono<Long> countSearchByLinkedDocumentationUnit(
      String courtType,
      String courtLocation,
      Instant decisionDate,
      Long[] docUnitIds,
      Long docTypeId);

  @Query("SELECT COUNT(*) FROM doc_unit " + ALL_QUERY)
  Mono<Long> countGetAllDocumentUnitListEntries(DataSource dataSource, UUID documentationOfficeId);

  @Query("SELECT COUNT(*) FROM doc_unit " + OVERVIEW_SEARCH_QUERY)
  Mono<Long> countSearchByDocumentUnitListEntry(
      UUID documentationOfficeId,
      String documentNumberOrFileNumber,
      String courtType,
      String courtLocation,
      Instant decisionDate,
      PublicationStatus status);
}
