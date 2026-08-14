// AWS S3 콘텐츠 이미지 업로드용 presigned URL을 발급한다.

package com.landit.landitbe.feature.contentimage.client;

import com.landit.landitbe.config.content.ContentImageProperties;
import java.net.URI;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** AWS S3 콘텐츠 이미지 업로드용 presigned URL을 발급한다. */
@Component
public class S3ContentImageUploadClient implements ContentImageUploadClient {

  private final S3Presigner presigner;
  private final ContentImageProperties properties;

  /**
   * S3 presigner와 콘텐츠 이미지 설정을 주입받는다.
   *
   * @param presigner S3 presigner
   * @param properties 콘텐츠 이미지 설정
   */
  public S3ContentImageUploadClient(S3Presigner presigner, ContentImageProperties properties) {
    this.presigner = presigner;
    this.properties = properties;
  }

  /** {@inheritDoc} */
  @Override
  public URI presign(ContentImageUploadCommand command) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(properties.bucketName())
            .key(command.objectKey())
            .contentType(command.contentType())
            .cacheControl(command.cacheControl())
            .ifNoneMatch(command.ifNoneMatch())
            .build();
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(command.expiresIn())
            .putObjectRequest(putObjectRequest)
            .build();
    return URI.create(presigner.presignPutObject(presignRequest).url().toString());
  }
}
