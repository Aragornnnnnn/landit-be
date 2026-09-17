// SES 요청의 제한 시간과 자동 재발송 금지를 구성한다.

package com.landit.landitbe.config.notification;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/** SES API 클라이언트를 구성한다. */
@Configuration
public class EmailConfiguration {
  /**
   * 서버 역할 자격증명을 사용하며 불확실한 전송을 SDK에서 재시도하지 않는다.
   *
   * @param region SES 발신 도메인을 인증한 리전
   * @return SES 클라이언트
   */
  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean
  public SesV2Client emailClient(@Value("${AWS_REGION:ap-northeast-2}") String region) {
    return SesV2Client.builder()
        .region(Region.of(region))
        .overrideConfiguration(
            c ->
                c.apiCallTimeout(Duration.ofSeconds(10))
                    .apiCallAttemptTimeout(Duration.ofSeconds(10))
                    .retryStrategy(StandardRetryStrategy.builder().maxAttempts(1).build()))
        .build();
  }
}
