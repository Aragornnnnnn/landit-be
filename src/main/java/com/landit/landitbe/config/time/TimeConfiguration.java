// 서비스 기준 시간을 서울 시간대로 제공하는 Clock 설정을 정의한다.

package com.landit.landitbe.config.time;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 일일 학습 정책에 사용할 서울 시간대 Clock을 제공한다. */
@Configuration
public class TimeConfiguration {

  private static final ZoneId SERVICE_ZONE_ID = ZoneId.of("Asia/Seoul");

  /** 서울 시간대의 현재 시각을 제공하는 Clock을 생성한다. */
  @Bean
  Clock clock() {
    return Clock.system(SERVICE_ZONE_ID);
  }
}
