// 비동기 작업에 요청 ID와 인증된 내부 사용자 ID를 전달하고 실행 후 복원한다.

package com.landit.landitbe.config.observability;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/** 공통 실행기의 요청과 사용자 식별자를 전파한다. */
@Configuration
public class ObservationTaskConfiguration {
  /**
   * 요청 ID와 내부 사용자 ID만 복사하고 실행 후 기존 컨텍스트를 복원한다.
   *
   * @return 관측 식별자 전용 작업 래퍼
   */
  @Bean
  public TaskDecorator observationTaskDecorator() {
    return runnable -> {
      Map<String, String> context = capture();
      return () -> {
        Map<String, String> previous = capture();
        restore(context);
        try {
          runnable.run();
        } finally {
          restore(previous);
        }
      };
    };
  }

  private Map<String, String> capture() {
    Map<String, String> context = new HashMap<>();
    Set.of("request_id", "user_id").forEach(key -> context.put(key, MDC.get(key)));
    return context;
  }

  private void restore(Map<String, String> context) {
    context.forEach(
        (key, value) -> {
          if (value == null) {
            MDC.remove(key);
          } else {
            MDC.put(key, value);
          }
        });
  }
}
