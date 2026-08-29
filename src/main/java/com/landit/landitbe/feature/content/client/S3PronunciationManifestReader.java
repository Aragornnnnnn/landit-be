// S3 콘텐츠 버킷에서 발음 평가 자산 매니페스트를 내려받는다.

package com.landit.landitbe.feature.content.client;

import com.landit.landitbe.config.content.ContentImageProperties;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * S3 콘텐츠 버킷에서 발음 평가 자산 매니페스트를 내려받는다.
 *
 * <p>ECS Task Role의 자격증명을 사용한다. 버킷·리전은 콘텐츠 이미지와 동일한 설정을 재사용한다.
 */
@Component
@ConditionalOnProperty(
    prefix = "landit.pronunciation-asset",
    name = "manifest-mode",
    havingValue = "s3",
    matchIfMissing = true)
public class S3PronunciationManifestReader implements PronunciationManifestReadable {

  private final S3Client s3Client;
  private final ContentImageProperties properties;

  /**
   * 콘텐츠 버킷 설정으로 S3 클라이언트를 구성한다.
   *
   * @param properties 콘텐츠 버킷 런타임 설정
   */
  public S3PronunciationManifestReader(ContentImageProperties properties) {
    this.properties = properties;
    this.s3Client = S3Client.builder().region(Region.of(properties.region())).build();
  }

  /** {@inheritDoc} */
  @Override
  public String read(String manifestKey) {
    GetObjectRequest request =
        GetObjectRequest.builder().bucket(properties.bucketName()).key(manifestKey).build();
    try {
      return new String(s3Client.getObjectAsBytes(request).asByteArray(), StandardCharsets.UTF_8);
    } catch (NoSuchKeyException exception) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
  }
}
