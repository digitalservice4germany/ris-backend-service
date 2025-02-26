package de.bund.digitalservice.ris.caselaw.adapter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

@Component
public class PortalPrototypeBucket extends S3Bucket {

  public PortalPrototypeBucket(
      @Qualifier("portalPrototypeS3Client") S3Client s3Client,
      @Value("${s3.file-storage.case-law-prototype.bucket-name:no-bucket}") String bucketName) {
    super(s3Client, bucketName);
  }
}
