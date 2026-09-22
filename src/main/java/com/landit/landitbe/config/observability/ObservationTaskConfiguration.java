// 비동기 작업에 안전한 요청 식별자만 전달하고 실행 후 복원한다.

package com.landit.landitbe.config.observability;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/** 공통 실행기의 요청 상관관계 식별자를 전파한다. */
@Configuration
public class ObservationTaskConfiguration {
  /**
   * 스레드 간 사용자 값이나 인증 정보를 복사하지 않는다.
   *
   * @return 요청 ID 전용 작업 래퍼
   */
  @Bean
  TaskDecorator observationTaskDecorator() {
    return runnable -> {
      String requestId = MDC.get("request_id");
      return () -> {
        String previous = MDC.get("request_id");
        if (requestId == null) {
          MDC.remove("request_id");
        } else {
          MDC.put("request_id", requestId);
        }
        try {
          runnable.run();
        } finally {
          if (previous == null) {
            MDC.remove("request_id");
          } else {
            MDC.put("request_id", previous);
          }
        }
      };
    };
  }
}
