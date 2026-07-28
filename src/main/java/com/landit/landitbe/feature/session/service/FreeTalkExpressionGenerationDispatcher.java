// 완료 프리톡의 표현 생성 작업을 애플리케이션 실행기로 넘긴다.

package com.landit.landitbe.feature.session.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/** 완료 프리톡의 표현 생성 작업을 애플리케이션 실행기로 넘긴다. */
@Service
public class FreeTalkExpressionGenerationDispatcher {

  private final FreeTalkExpressionGenerationService generationService;
  private final TaskExecutor taskExecutor;

  FreeTalkExpressionGenerationDispatcher(
      FreeTalkExpressionGenerationService generationService,
      @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
    this.generationService = generationService;
    this.taskExecutor = taskExecutor;
  }

  /** 표현 생성 작업을 비동기로 제출한다. */
  public void dispatch(long learningSessionId) {
    taskExecutor.execute(() -> generationService.generate(learningSessionId));
  }
}
