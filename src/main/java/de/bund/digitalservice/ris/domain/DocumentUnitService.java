package de.bund.digitalservice.ris.domain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Service
@Slf4j
public class DocumentUnitService {
  private final DocumentUnitRepository repository;
  private final DocumentUnitListEntryRepository listEntryRepository;
  private final DocumentNumberCounterRepository counterRepository;
  private final PreviousDecisionRepository previousDecisionRepository;
  private final S3AsyncClient s3AsyncClient;
  private final EmailPublishService publishService;

  @Value("${otc.obs.bucket-name}")
  private String bucketName;

  public DocumentUnitService(
      DocumentUnitRepository repository,
      DocumentUnitListEntryRepository listEntryRepository,
      DocumentNumberCounterRepository counterRepository,
      PreviousDecisionRepository previousDecisionRepository,
      S3AsyncClient s3AsyncClient,
      EmailPublishService publishService) {
    this.repository = repository;
    this.listEntryRepository = listEntryRepository;
    this.counterRepository = counterRepository;
    this.s3AsyncClient = s3AsyncClient;
    this.publishService = publishService;
    this.previousDecisionRepository = previousDecisionRepository;
  }

  public Mono<DocumentUnitDTO> generateNewDocUnit(
      DocumentUnitCreationInfo documentUnitCreationInfo) {
    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    return counterRepository
        .getDocumentNumberCounterEntry()
        .flatMap(
            outdatedDocumentNumberCounter -> {
              // this is the switch happening when the first new DocUnit in a new year gets
              // created
              if (outdatedDocumentNumberCounter.currentyear != currentYear) {
                outdatedDocumentNumberCounter.currentyear = currentYear;
                outdatedDocumentNumberCounter.nextnumber = 1;
              }
              outdatedDocumentNumberCounter.nextnumber += 1;
              return counterRepository.save(outdatedDocumentNumberCounter);
            })
        .flatMap(
            updatedDocumentNumberCounter ->
                repository.save(
                    DocumentUnitDTO.createNew(
                        documentUnitCreationInfo, updatedDocumentNumberCounter.nextnumber - 1)))
        .doOnError(ex -> log.error("Couldn't create empty doc unit", ex));
  }

  public Mono<DocumentUnitDTO> attachFileToDocUnit(
      UUID docUnitId, ByteBuffer byteBufferFlux, HttpHeaders httpHeaders) {
    var fileUuid = UUID.randomUUID().toString();
    checkDocx(byteBufferFlux);
    return putObjectIntoBucket(fileUuid, byteBufferFlux, httpHeaders)
        .doOnNext(putObjectResponse -> log.debug("generate doc unit for {}", fileUuid))
        .flatMap(
            putObjectResponse ->
                repository
                    .findByUuid(docUnitId)
                    .map(
                        docUnit -> {
                          docUnit.setFileuploadtimestamp(Instant.now());
                          docUnit.setS3path(fileUuid);
                          docUnit.setFiletype("docx");
                          docUnit.setFilename(
                              httpHeaders.containsKey("X-Filename")
                                  ? httpHeaders.getFirst("X-Filename")
                                  : "Kein Dateiname gefunden");
                          return docUnit;
                        }))
        .doOnNext(docUnit -> log.debug("save doc unit"))
        .flatMap(repository::save);
  }

  public Mono<ResponseEntity<DocumentUnit>> removeFileFromDocUnit(UUID docUnitId) {
    return repository
        .findByUuid(docUnitId)
        .flatMap(
            docUnit -> {
              var fileUuid = docUnit.getS3path();
              return deleteObjectFromBucket(fileUuid)
                  .doOnNext(
                      deleteObjectResponse -> log.debug("deleted file {} in bucket", fileUuid))
                  .map(
                      deleteObjectResponse -> {
                        docUnit.setS3path(null);
                        docUnit.setFilename(null);
                        docUnit.setFileuploadtimestamp(null);
                        return docUnit;
                      });
            })
        .doOnNext(docUnit -> log.debug("removed file from DocUnit {}", docUnitId))
        .flatMap(repository::save)
        .map(
            documentUnitDTO ->
                ResponseEntity.status(HttpStatus.OK)
                    .body(DocumentUnitBuilder.newInstance().setDocUnitDTO(documentUnitDTO).build()))
        .doOnError(ex -> log.error("Couldn't remove the file from the DocUnit", ex))
        .onErrorReturn(
            ResponseEntity.internalServerError().body(DocumentUnitBuilder.newInstance().build()));
  }

  void checkDocx(ByteBuffer byteBufferFlux) {
    var zip = new ZipInputStream(new ByteArrayInputStream(byteBufferFlux.array()));
    ZipEntry entry;
    try {
      while ((entry = zip.getNextEntry()) != null) {
        if (entry.getName().equals("word/document.xml")) {
          return;
        }
      }
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
    throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  private Mono<PutObjectResponse> putObjectIntoBucket(
      String fileUuid, ByteBuffer byteBuffer, HttpHeaders httpHeaders) {

    var contentLength = httpHeaders.getContentLength();

    Map<String, String> metadata = new HashMap<>();
    MediaType mediaType = httpHeaders.getContentType();
    if (mediaType == null) {
      mediaType = MediaType.APPLICATION_OCTET_STREAM;
    }

    log.debug("upload header information: mediaType{}, contentLength={}", mediaType, contentLength);

    var asyncRequestBody = AsyncRequestBody.fromPublisher(Mono.just(byteBuffer));
    var putObjectRequestBuilder =
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(fileUuid)
            .contentType(mediaType.toString())
            .metadata(metadata);

    if (contentLength >= 0) {
      putObjectRequestBuilder.contentLength(contentLength);
    }

    var putObjectRequest = putObjectRequestBuilder.build();

    return Mono.fromCallable(
            () -> Mono.fromFuture(s3AsyncClient.putObject(putObjectRequest, asyncRequestBody)))
        .flatMap(Function.identity());
  }

  private Mono<DeleteObjectResponse> deleteObjectFromBucket(String fileUuid) {
    var deleteObjectRequest =
        DeleteObjectRequest.builder().bucket(bucketName).key(fileUuid).build();
    return Mono.fromCallable(() -> Mono.fromFuture(s3AsyncClient.deleteObject(deleteObjectRequest)))
        .flatMap(Function.identity());
  }

  public Mono<ResponseEntity<Flux<DocumentUnitListEntry>>> getAll() {
    return Mono.just(
        ResponseEntity.ok(listEntryRepository.findAll(Sort.by(Order.desc("documentnumber")))));
  }

  public Mono<ResponseEntity<DocumentUnit>> getByDocumentnumber(String documentnumber) {
    return repository
        .findByDocumentnumber(documentnumber)
        .flatMap(
            documentUnitDTO ->
                previousDecisionRepository
                    .findAllByDocumentnumber(documentnumber)
                    .collectList()
                    .flatMap(
                        previousDecisions ->
                            Mono.just(documentUnitDTO.setPreviousDecisions(previousDecisions))))
        .map(
            documentUnitDTO ->
                ResponseEntity.ok(
                    DocumentUnitBuilder.newInstance().setDocUnitDTO(documentUnitDTO).build()));
  }

  public Mono<ResponseEntity<String>> deleteByUuid(UUID docUnitId) {
    return repository
        .findByUuid(docUnitId)
        .flatMap(
            docUnit -> {
              if (docUnit.hasFileAttached()) {
                var fileUuid = docUnit.getS3path();
                return deleteObjectFromBucket(fileUuid)
                    .doOnNext(
                        deleteObjectResponse -> log.debug("deleted file {} in bucket", fileUuid))
                    .flatMap(deleteObjectResponse -> repository.delete(docUnit));
              }
              return repository.delete(docUnit);
            })
        .doOnNext(v -> log.debug("deleted doc unit"))
        .map(v -> ResponseEntity.status(HttpStatus.OK).body("done"))
        .doOnError(ex -> log.error("Couldn't delete the DocUnit", ex))
        .onErrorReturn(ResponseEntity.internalServerError().body("Couldn't delete the DocUnit"));
  }

  public Mono<ResponseEntity<DocumentUnit>> updateDocUnit(DocumentUnit documentUnit) {
    DocumentUnitDTO documentUnitDTO = DocumentUnitDTO.buildFromDocumentUnit(documentUnit);
    if (documentUnitDTO.previousDecisions == null)
      return repository
          .save(documentUnitDTO)
          .map(
              duDTO ->
                  ResponseEntity.status(HttpStatus.OK)
                      .body(DocumentUnitBuilder.newInstance().setDocUnitDTO(duDTO).build()))
          .doOnError(ex -> log.error("Couldn't update the DocUnit", ex))
          .onErrorReturn(
              ResponseEntity.internalServerError().body(DocumentUnitBuilder.newInstance().build()));

    /* Passing foreign key to object */
    List<PreviousDecision> previousDecisionsList =
        documentUnitDTO.previousDecisions.stream()
            .map(
                previousDecision ->
                    previousDecision.setDocumentnumber(documentUnitDTO.documentnumber))
            .toList();
    /* Get all id of previous decisions from font-end */
    List<Long> incomingIds =
        previousDecisionsList.stream()
            .filter(previousDecision -> previousDecision.id != null)
            .map(previousDecision -> previousDecision.id)
            .toList();
    /* Create mono publisher object to loop through */
    Mono<List<PreviousDecision>> listPreviousDecisionMonoObj = Mono.just(previousDecisionsList);
    return listPreviousDecisionMonoObj
        .flatMap(
            previousDecisions ->
                previousDecisionRepository
                    .getAllIdsByDocumentnumber(documentUnitDTO.documentnumber)
                    .collectList()
                    .flatMap(
                        inDatabaseIds -> {
                          /* Get all id of deleted decisions */
                          List<Long> deletedIndexes =
                              getDeletedPreviousDecisionIds(incomingIds, inDatabaseIds);
                          /* Insert/Update decisions */
                          return previousDecisionRepository
                              .saveAll(
                                  previousDecisions.stream()
                                      .filter(
                                          previousDecision ->
                                              !deletedIndexes.contains(previousDecision.id))
                                      .toList())
                              .then(
                                  /* Delete decisions */
                                  previousDecisionRepository.deleteAllById(
                                      deletedIndexes.stream().map(String::valueOf).toList()));
                        }))
        .then(repository.save(documentUnitDTO))
        .map(
            duDTO ->
                ResponseEntity.status(HttpStatus.OK)
                    .body(DocumentUnitBuilder.newInstance().setDocUnitDTO(duDTO).build()))
        .doOnError(ex -> log.error("Couldn't update the DocUnit", ex))
        .onErrorReturn(
            ResponseEntity.internalServerError().body(DocumentUnitBuilder.newInstance().build()));
  }

  private List<Long> getDeletedPreviousDecisionIds(
      List<Long> incomingIds, List<Long> inDatabaseIds) {
    /* Return all ids in database but not in incoming Id from front end */
    return inDatabaseIds.stream().filter(index -> !incomingIds.contains(index)).toList();
  }

  public Mono<MailResponse> publishAsEmail(UUID documentUnitUuid, String receiverAddress) {
    return repository
        .findByUuid(documentUnitUuid)
        .flatMap(
            documentUnit ->
                previousDecisionRepository
                    .findAllByDocumentnumber(documentUnit.documentnumber)
                    .collectList()
                    .flatMap(
                        previousDecisions ->
                            publishService.publish(
                                documentUnit.setPreviousDecisions(previousDecisions),
                                receiverAddress)));
  }

  public Mono<MailResponse> getLastPublishedXmlMail(UUID documentUuid) {
    return repository
        .findByUuid(documentUuid)
        .flatMap(
            documentUnit -> publishService.getLastPublishedXml(documentUnit.getId(), documentUuid));
  }
}
