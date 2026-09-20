// 요약 작업의 동시 실행과 대기를 제한하고 일반 AI 작업 실행기를 보존한다.

package com.landit.landitbe.feature.learning.freetalk.context.service;

import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/** Executor 타입의 bean을 등록하지 않아 Boot의 공용 실행기 생성을 유지한다. */
@Service
public class FreeTalkContextExecutionService {
  private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

  /** 동시 2개·대기 8개를 넘으면 호출자 스레드에서 실행하지 않고 거부한다. */
  public FreeTalkContextExecutionService() {
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(8);
    executor.setThreadNamePrefix("free-talk-summary-");
    executor.initialize();
  }

  /**
   * 여유가 있을 때 요약 작업을 제출한다.
   *
   * @param task 실행할 작업
   * @throws java.util.concurrent.RejectedExecutionException 실행기가 포화되거나 종료됐을 때
   */
  public void execute(Runnable task) {
    executor.execute(task);
  }

  /** 애플리케이션 종료 시 요약 전용 스레드를 정리한다. */
  @PreDestroy
  public void close() {
    executor.shutdown();
  }
}
