// 비공개 문의 첨부의 S3 클라이언트와 호출 제한을 구성한다.

package com.landit.landitbe.config.mailbox;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/** 비공개 문의 첨부의 S3 클라이언트와 호출 제한을 구성한다. */
@Configuration
public class MailboxAttachmentConfiguration {

  /**
   * ECS Task Role을 사용하고 호출 시간을 제한한 클라이언트를 생성한다.
   *
   * @param properties 첨부 저장소 설정
   * @return 애플리케이션 종료 시 닫을 S3 클라이언트
   */
  @Bean
  S3Client mailboxAttachmentS3Client(MailboxAttachmentProperties properties) {
    return S3Client.builder()
        .region(Region.of(properties.region()))
        .overrideConfiguration(
            config ->
                config
                    .apiCallTimeout(Duration.ofSeconds(10))
                    .apiCallAttemptTimeout(Duration.ofSeconds(5)))
        .build();
  }
}
