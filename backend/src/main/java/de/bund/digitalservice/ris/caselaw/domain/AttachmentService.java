package de.bund.digitalservice.ris.caselaw.domain;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.springframework.http.HttpHeaders;

public interface AttachmentService {
  Attachment attachFileToDocumentationUnit(
      UUID documentationUnitId, ByteBuffer byteBuffer, HttpHeaders httpHeaders);

  void deleteByS3Path(String s3Path);

  void deleteAllObjectsFromBucketForDocumentationUnit(UUID documentationUnitId);
}
