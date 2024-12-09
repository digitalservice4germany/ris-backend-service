package de.bund.digitalservice.ris.caselaw.adapter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

@Component
public class LdmlBucket extends S3Bucket {

  public LdmlBucket(
      @Qualifier("ldmlS3Client") S3Client s3Client,
      @Value("${s3.file-storage.case-law.bucket-name:no-bucket}") String bucketName) {
    super(s3Client, bucketName);
  }
}
