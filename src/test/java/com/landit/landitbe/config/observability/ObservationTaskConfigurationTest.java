// 비동기 작업의 관측 ID 전파와 작업 간 컨텍스트 복원을 검증한다.

package com.landit.landitbe.config.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

class ObservationTaskConfigurationTest {
  private final TaskDecorator decorator =
      new ObservationTaskConfiguration().observationTaskDecorator();

  @AfterEach
  void cleanContext() {
    MDC.clear();
  }

  @Test
  void workerReceivesOnlySelectedIdsAndCleansUpAfterExecution() throws Exception {
    MDC.put("request_id", "request-1");
    MDC.put("user_id", "42");
    MDC.put("token", "secret-token");
    Runnable task =
        decorator.decorate(
            () -> {
              assertThat(MDC.get("request_id")).isEqualTo("request-1");
              assertThat(MDC.get("user_id")).isEqualTo("42");
              assertThat(MDC.get("token")).isNull();
            });
    try (var executor = Executors.newSingleThreadExecutor()) {
      executor.submit(task).get();
      executor
          .submit(
              () -> {
                assertThat(MDC.get("request_id")).isNull();
                assertThat(MDC.get("user_id")).isNull();
              })
          .get();
    }
    assertThat(MDC.get("user_id")).isEqualTo("42");
  }

  @Test
  void unattributedTaskClearsWorkerIdsAndRestoresThemEvenOnFailure() {
    Runnable task =
        decorator.decorate(
            () -> {
              assertThat(MDC.get("request_id")).isNull();
              assertThat(MDC.get("user_id")).isNull();
              throw new IllegalStateException("failure");
            });
    MDC.put("request_id", "outer-request");
    MDC.put("user_id", "7");
    assertThatThrownBy(task::run).isInstanceOf(IllegalStateException.class);
    assertThat(MDC.get("request_id")).isEqualTo("outer-request");
    assertThat(MDC.get("user_id")).isEqualTo("7");
  }
}
