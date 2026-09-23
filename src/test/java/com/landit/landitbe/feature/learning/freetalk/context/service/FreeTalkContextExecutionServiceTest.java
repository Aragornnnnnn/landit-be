// 요약 실행기 포화가 호출자나 공용 실행기로 전파되지 않는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.context.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class FreeTalkContextExecutionServiceTest {
  @Test
  void rejectsEleventhTaskWithoutCallerExecution() throws Exception {
    var service = new FreeTalkContextExecutionService();
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
}
