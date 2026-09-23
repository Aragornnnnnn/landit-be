// 비동기 작업에 요청과 작업 식별자를 전달하고 실행 후 복원한다.

package com.landit.landitbe.config.observability;

import com.landit.landitbe.shared.observability.ObservationContext;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/** 공통 실행기의 요청과 작업 식별자를 전파한다. */
@Configuration
public class ObservationTaskConfiguration {
  /**
   * 허용된 요청과 작업 식별자만 복사하고 실행 후 기존 컨텍스트를 복원한다.
   *
   * @return 관측 식별자 전용 작업 래퍼
   */
  @Bean
  public TaskDecorator observationTaskDecorator() {
    return runnable -> {
      Map<String, String> context = ObservationContext.capture();
      return () -> {
        try (var ignored = ObservationContext.open(context)) {
          try {
            runnable.run();
          } catch (RuntimeException failure) {
            ObservationContext.remember(failure);
            throw failure;
          }
        }
      };
    };
  }
}
