package de.bund.digitalservice.ris.caselaw.adapter;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.AttachmentDTO;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.AttachmentRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DatabaseDocumentationUnitRepository;
import de.bund.digitalservice.ris.caselaw.adapter.database.jpa.DocumentationUnitDTO;
import de.bund.digitalservice.ris.caselaw.domain.AttachmentException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@TestPropertySource(properties = "otc.obs.bucket-name:testBucket")
@ExtendWith(SpringExtension.class)
@Import({S3AttachmentService.class})
class S3AttachmentServiceTest {
  @SpyBean S3AttachmentService service;

  @MockBean AttachmentRepository repository;

  @MockBean
  @Qualifier("docxS3Client")
  S3Client s3Client;

  @MockBean DatabaseDocumentationUnitRepository documentationUnitRepository;

  private DocumentationUnitDTO documentationUnitDTO;

  @BeforeEach
  void setup() {
    documentationUnitDTO = DocumentationUnitDTO.builder().id(UUID.randomUUID()).build();
    when(documentationUnitRepository.findById(documentationUnitDTO.getId()))
        .thenReturn(Optional.of(documentationUnitDTO));

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    when(repository.save(any(AttachmentDTO.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void testAttachFileToDocumentationUnit() {
    var byteBuffer = ByteBuffer.wrap(new byte[] {});
    var headerMap = new LinkedMultiValueMap<String, String>();
    headerMap.put("Content-Type", List.of("content/extension"));
    headerMap.put("X-Filename", List.of("testfile.docx"));
    var httpHeaders = HttpHeaders.readOnlyHttpHeaders(headerMap);
    doNothing().when(service).checkDocx(any(ByteBuffer.class));

    service.attachFileToDocumentationUnit(documentationUnitDTO.getId(), byteBuffer, httpHeaders);

    // s3 interaction
    var putObjectRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    var requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
    verify(s3Client).putObject(putObjectRequestCaptor.capture(), requestBodyCaptor.capture());
    assertEquals("testBucket", putObjectRequestCaptor.getValue().bucket());
    assertEquals("content/extension", putObjectRequestCaptor.getValue().contentType());
    var value = requestBodyCaptor.getValue();
    var expected = RequestBody.fromByteBuffer(ByteBuffer.wrap(new byte[] {}));
    assertEquals(
        expected.optionalContentLength().orElse(0L), value.optionalContentLength().orElse(0L));
    assertEquals(expected.contentType(), value.contentType());

    // repo interaction
    var attachmentDtoCaptor = ArgumentCaptor.forClass(AttachmentDTO.class);
    verify(repository).save(attachmentDtoCaptor.capture());
    assertEquals("testfile.docx", attachmentDtoCaptor.getValue().getFilename());
    assertEquals("docx", attachmentDtoCaptor.getValue().getFormat());
  }

  @Test
  void testAttachFileToDocumentationUnit_withoutFileName() {
    var byteBuffer = ByteBuffer.wrap(new byte[] {});
    var headerMap = new LinkedMultiValueMap<String, String>();
    headerMap.put("Content-Type", List.of("content/extension"));
    var httpHeaders = HttpHeaders.readOnlyHttpHeaders(headerMap);
    doNothing().when(service).checkDocx(any(ByteBuffer.class));

    service.attachFileToDocumentationUnit(documentationUnitDTO.getId(), byteBuffer, httpHeaders);

    var attachmentDtoCaptor = ArgumentCaptor.forClass(AttachmentDTO.class);
    verify(repository).save(attachmentDtoCaptor.capture());
    assertEquals("Kein Dateiname gefunden", attachmentDtoCaptor.getValue().getFilename());
  }

  @Test
  void testDeleteByS3Path() {
    var testS3Path = UUID.randomUUID().toString();
    service.deleteByS3Path(testS3Path);

    // bucket interaction
    var deleteObjectRequestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(deleteObjectRequestCaptor.capture());
    assertEquals("testBucket", deleteObjectRequestCaptor.getValue().bucket());
    assertEquals(testS3Path, deleteObjectRequestCaptor.getValue().key());

    // repo interaction
    verify(repository).deleteByS3ObjectPath(testS3Path);
  }

  @ParameterizedTest()
  @ValueSource(strings = {"", " "})
  @NullSource
  void testDeleteByS3Path_withoutS3Path(String s3Path) {
    assertThrows(AttachmentException.class, () -> service.deleteByS3Path(s3Path));

    verifyNoInteractions(s3Client);
    verifyNoInteractions(repository);
  }

  @Test
  void testDeleteAllObjectsFromBucketForDocumentationUnit() {
    when(repository.findAllByDocumentationUnitId(documentationUnitDTO.getId()))
        .thenReturn(
            List.of(
                AttachmentDTO.builder().s3ObjectPath("fooS3Path").build(),
                AttachmentDTO.builder().s3ObjectPath("barS3Path").build()));

    service.deleteAllObjectsFromBucketForDocumentationUnit(documentationUnitDTO.getId());

    // repo interaction
    verify(repository).findAllByDocumentationUnitId(documentationUnitDTO.getId());
    verify(repository, never()).deleteByS3ObjectPath(any(String.class));

    // bucket interaction
    var deleteObjectRequestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client, times(2)).deleteObject(deleteObjectRequestCaptor.capture());
    List<DeleteObjectRequest> capturedRequests = deleteObjectRequestCaptor.getAllValues();
    assertTrue(capturedRequests.stream().anyMatch(request -> request.key().equals("fooS3Path")));
    assertTrue(capturedRequests.stream().anyMatch(request -> request.key().equals("barS3Path")));
  }

  @Test
  void testCheckDocx_withValidDocument() {
    ByteBuffer byteBuffer = buildBuffer("word/document.xml");
    assertDoesNotThrow(() -> service.checkDocx(byteBuffer));
  }

  @Test
  void testCheckDocx_withInvalidFormat() {
    ByteBuffer byteBuffer = buildBuffer("word/document.csv");
    assertThrows(ResponseStatusException.class, () -> service.checkDocx(byteBuffer));
  }

  @Test
  void testCheckDocx_withCorruptedDocx() {
    byte[] corruptedData = new byte[1024];
    new Random().nextBytes(corruptedData);
    ByteBuffer byteBuffer = ByteBuffer.wrap(corruptedData);

    assertThrows(ResponseStatusException.class, () -> service.checkDocx(byteBuffer));
  }

  @Test
  void testCheckDocx_withEmptyBuffer() {
    byte[] emptyData = new byte[] {};
    ByteBuffer byteBuffer = ByteBuffer.wrap(emptyData);

    assertThrows(ResponseStatusException.class, () -> service.checkDocx(byteBuffer));
  }

  @Test
  void testGenerateNewDocumentationUnitAndAttachFile_withExceptionFromBucket() throws S3Exception {
    var byteBuffer = ByteBuffer.wrap(new byte[] {});

    doNothing().when(service).checkDocx(any(ByteBuffer.class));
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(SdkException.create("exception", null));
    var documentationUnitDTOId = documentationUnitDTO.getId();

    assertThrows(
        SdkException.class,
        () ->
            service.attachFileToDocumentationUnit(
                documentationUnitDTOId, byteBuffer, HttpHeaders.EMPTY));

    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  private ByteBuffer buildBuffer(String entry) {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
      ZipEntry zipEntry = new ZipEntry(entry);
      zipOutputStream.putNextEntry(zipEntry);
      zipOutputStream.closeEntry();
      zipOutputStream.finish();

      byte[] zipBytes = byteArrayOutputStream.toByteArray();
      return ByteBuffer.wrap(zipBytes);
    } catch (IOException exception) {
      throw new RuntimeException("Failed to create zip", exception);
    }
  }
}
