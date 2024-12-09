package de.bund.digitalservice.ris.caselaw.config;

import de.bund.digitalservice.ris.caselaw.adapter.S3MockClient;
import de.bund.digitalservice.ris.caselaw.adapter.S3NoOpClient;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class OtcObsConfig {
  @Value("${otc.obs.endpoint:https://obs.eu-de.otc.t-systems.com}")
  private String endpoint;

  @Value("${otc.obs.accessKeyId:test}")
  private String docxAccessKeyId;

  @Value("${otc.obs.secretAccessKey:test}")
  private String docxSecretAccessKey;

  @Value("${s3.file-storage.case-law.access-key-id:test}")
  private String ldmlAccessKeyId;

  @Value("${s3.file-storage.case-law.secret-access-key:test}")
  private String ldmlSecretAccessKey;

  @Bean(name = "docxS3Client")
  @Profile({"staging", "production", "uat"})
  public S3Client docxS3Client() throws URISyntaxException {
    return S3Client.builder()
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(docxAccessKeyId, docxSecretAccessKey)))
        .endpointOverride(new URI(endpoint))
        .region(Region.of("eu-de"))
        .build();
  }

  @Bean(name = "ldmlS3Client")
  @Profile({"staging"})
  public S3Client ldmlS3Client() throws URISyntaxException {
    return S3Client.builder()
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ldmlAccessKeyId, ldmlSecretAccessKey)))
        .endpointOverride(new URI(endpoint))
        .region(Region.of("eu-de"))
        .build();
  }

  @Bean(name = "ldmlS3Client")
  @Profile({"production", "uat"})
  public S3Client ldmlS3NoopClient() {
    return new S3NoOpClient();
  }

  @Bean(name = "docxS3Client")
  @Profile({"!production & !staging & !uat"})
  public S3Client docxS3MockClient() {
    return new S3MockClient();
  }

  @Bean(name = "ldmlS3Client")
  @Profile({"!production & !staging & !uat"})
  public S3Client ldmlS3MockClient() {
    return new S3MockClient();
  }
}
