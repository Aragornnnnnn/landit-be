// 프리톡 표현 생성 작업 제출 실패 처리를 검증한다.

package com.landit.landitbe.feature.session.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

/** 프리톡 표현 생성 작업 제출 실패 처리를 검증한다. */
class FreeTalkExpressionGenerationDispatcherTest {

  /** 실행기 제출이 거부되면 호출자에게 전파하지 않고 생성 상태를 실패로 전환한다. */
  @Test
  void marksGenerationFailedWhenTaskExecutorRejectsSubmission() {
    FreeTalkExpressionGenerationService generationService =
        org.mockito.Mockito.mock(FreeTalkExpressionGenerationService.class);
    TaskExecutor rejectingExecutor =
        task -> {
          throw new TaskRejectedException("queue full");
        };
    FreeTalkExpressionGenerationDispatcher dispatcher =
        new FreeTalkExpressionGenerationDispatcher(generationService, rejectingExecutor);

    dispatcher.dispatch(10L);

    verify(generationService).markFailed(10L);
  }
}
