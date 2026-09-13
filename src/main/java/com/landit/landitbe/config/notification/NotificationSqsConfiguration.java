// Push 발행과 소비가 공용으로 사용하는 SQS Client를 구성한다.

package com.landit.landitbe.config.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/** Push Queue 접근에 필요한 AWS SQS Client를 제공한다. */
@Configuration
public class NotificationSqsConfiguration {

  /**
   * 관리자 일회성 예약 클라이언트를 구성한다.
   *
   * @param region AWS 리전
   * @return 제한 시간이 설정된 Scheduler 클라이언트
   */
  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean
  public software.amazon.awssdk.services.scheduler.SchedulerClient notificationSchedulerClient(
      @Value("${AWS_REGION:ap-northeast-2}") String region) {
    return software.amazon.awssdk.services.scheduler.SchedulerClient.builder()
        .region(Region.of(region))
        .overrideConfiguration(
            configuration ->
                configuration
                    .apiCallTimeout(java.time.Duration.ofSeconds(10))
                    .apiCallAttemptTimeout(java.time.Duration.ofSeconds(5)))
        .build();
  }

  /**
   * 애플리케이션 AWS 리전에 연결할 비동기 SQS Client를 생성한다.
   *
   * @param region AWS 리전
   * @return 공용 SQS Client
   */
  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean
  public SqsAsyncClient notificationSqsAsyncClient(
      @Value("${AWS_REGION:ap-northeast-2}") String region) {
    return SqsAsyncClient.builder().region(Region.of(region)).build();
  }
}
