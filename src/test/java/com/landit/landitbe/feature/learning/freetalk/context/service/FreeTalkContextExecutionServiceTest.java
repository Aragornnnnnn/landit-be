// 요약 실행기 포화가 호출자나 공용 실행기로 전파되지 않는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.context.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.landit.landitbe.config.observability.ObservationTaskConfiguration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class FreeTalkContextExecutionServiceTest {
  @Test
  void rejectsEleventhTaskWithoutCallerExecution() throws Exception {
    var service =
        new FreeTalkContextExecutionService(
            new ObservationTaskConfiguration().observationTaskDecorator());
    var started = new CountDownLatch(2);
    var release = new CountDownLatch(1);
    var rejectedRan = new AtomicBoolean();
    try {
      for (int i = 0; i < 10; i++) {
        service.execute(
            () -> {
              started.countDown();
              try {
                release.await(5, TimeUnit.SECONDS);
              } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
              }
            });
      }
      assertTrue(started.await(5, TimeUnit.SECONDS));
      assertThrows(
          RejectedExecutionException.class, () -> service.execute(() -> rejectedRan.set(true)));
      assertFalse(rejectedRan.get());
    } finally {
      release.countDown();
      service.close();
    }
  }

  @Test
  void summaryWorkerRetainsRequestAndUserIds() throws Exception {
    var service =
        new FreeTalkContextExecutionService(
            new ObservationTaskConfiguration().observationTaskDecorator());
    var observed = new CompletableFuture<String>();
    MDC.put("request_id", "request-1");
    MDC.put("user_id", "42");
    try {
      service.execute(() -> observed.complete(MDC.get("request_id") + "/" + MDC.get("user_id")));
      assertThat(observed.get(5, TimeUnit.SECONDS)).isEqualTo("request-1/42");
    } finally {
      MDC.clear();
      service.close();
    }
  }
}
