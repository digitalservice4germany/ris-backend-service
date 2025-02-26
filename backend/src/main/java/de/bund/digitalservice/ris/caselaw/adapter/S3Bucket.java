package de.bund.digitalservice.ris.caselaw.adapter;

import de.bund.digitalservice.ris.caselaw.adapter.exception.BucketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

@Slf4j
public class S3Bucket {

  private final S3Client s3Client;
  private final String bucketName;
  private static final String FILE_COULD_NOT_BE_SAVED_TO_BUCKET =
      "File could not be saved to bucket.";

  public S3Bucket(S3Client s3Client, String bucketName) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
  }

  public List<String> getAllFilenames() {
    return getAllFilenamesByPath("");
  }

  public List<String> getAllFilenamesByPath(String path) {
    List<String> keys = new ArrayList<>();
    ListObjectsV2Response response;
    ListObjectsV2Request request =
        ListObjectsV2Request.builder().bucket(bucketName).prefix(path).build();
    do {
      response = s3Client.listObjectsV2(request);
      keys.addAll(response.contents().stream().map(S3Object::key).toList());
      String token = response.nextContinuationToken();
      request = ListObjectsV2Request.builder().bucket(bucketName).continuationToken(token).build();
    } while (Boolean.TRUE.equals(response.isTruncated()));

    return keys;
  }

  public Optional<String> getFileAsString(String filename) {
    Optional<byte[]> s3Response = get(filename);
    return s3Response.map(bytes -> new String(bytes, StandardCharsets.UTF_8));
  }

  public Optional<byte[]> get(String eli) {
    try {
      var request = GetObjectRequest.builder().bucket(bucketName).key(eli).build();
      var response = s3Client.getObject(request);
      return Optional.of(response.readAllBytes());
    } catch (NoSuchKeyException e) {
      log.error(String.format("Object key %s does not exist", eli), e);
    } catch (S3Exception e) {
      log.error("AWS S3 encountered an issue: {}", e.awsErrorDetails().errorMessage());
    } catch (Exception e) {
      log.error("An error occurred: {}", e.getMessage());
    }
    return Optional.empty();
  }

  /**
   * @return true if the file was deleted, false if the file did not exist
   */
  public boolean delete(String fileName) {
    try {
      s3Client.deleteObject(builder -> builder.bucket(bucketName).key(fileName));
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    }
  }

  public void save(String fileName, String fileContent) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucketName).key(fileName).build();
    try {
      s3Client.putObject(putObjectRequest, RequestBody.fromString(fileContent));
    } catch (S3Exception e) {
      log.error(FILE_COULD_NOT_BE_SAVED_TO_BUCKET, e);
      throw new BucketException(FILE_COULD_NOT_BE_SAVED_TO_BUCKET, e);
    }
  }

  public void saveBytes(String fileName, ByteBuffer buffer) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucketName).key(fileName).build();
    try {
      s3Client.putObject(putObjectRequest, RequestBody.fromByteBuffer(buffer));
    } catch (S3Exception e) {
      log.error(FILE_COULD_NOT_BE_SAVED_TO_BUCKET, e);
      throw new BucketException(FILE_COULD_NOT_BE_SAVED_TO_BUCKET, e);
    }
  }

  public void close() {
    s3Client.close();
  }
}
