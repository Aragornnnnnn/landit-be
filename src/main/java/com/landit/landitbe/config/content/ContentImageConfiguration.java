// 콘텐츠 이미지 업로드에 사용하는 AWS 클라이언트를 구성한다.

package com.landit.landitbe.config.content;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** 콘텐츠 이미지 업로드에 사용하는 AWS 클라이언트를 구성한다. */
@Configuration
public class ContentImageConfiguration {

  /**
   * ECS Task Role을 사용하는 S3 presigner를 생성한다.
   *
   * @param properties 콘텐츠 이미지 런타임 설정
   * @return S3 presigner
   */
  @Bean
  S3Presigner contentImageS3Presigner(ContentImageProperties properties) {
    return S3Presigner.builder().region(Region.of(properties.region())).build();
  }
}
