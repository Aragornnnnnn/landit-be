// 완료 프리톡의 장기기억 생성 작업 등록 경계를 제공한다.

package com.landit.landitbe.feature.memory.service;

import com.landit.landitbe.config.memory.MemoryProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

/** 완료 프리톡의 장기기억 생성 작업 등록 경계를 제공한다. */
@Service
public class FreeTalkMemoryGenerationDispatchService {

  private final FreeTalkMemoryGenerationService generationService;
  private final TaskExecutor taskExecutor;
  private final MemoryProperties memoryProperties;

  /** 장기기억 생성 등록에 필요한 실행기와 실패 보상 경계를 주입받는다. */
  public FreeTalkMemoryGenerationDispatchService(
      FreeTalkMemoryGenerationService generationService,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
      MemoryProperties memoryProperties) {
    this.generationService = generationService;
    this.taskExecutor = taskExecutor;
    this.memoryProperties = memoryProperties;
  }

  /**
   * 장기기억 생성 작업 등록이 활성화된 경우에만 후속 작업 경계를 연다. 작업 등록이 거부되면 준비 상태를 실패로 전환해 미완료 작업을 남기지 않는다.
   *
   * @param sessionId 장기기억을 생성할 프리톡 세션 ID
   */
  public void dispatch(long sessionId) {
    if (!memoryProperties.writeEnabled()) {
      return;
    }
    try {
      taskExecutor.execute(() -> generationService.generate(sessionId));
    } catch (TaskRejectedException exception) {
      generationService.markFailed(sessionId);
    }
  }
}
