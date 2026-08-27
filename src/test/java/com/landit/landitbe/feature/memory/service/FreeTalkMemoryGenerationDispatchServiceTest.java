// 프리톡 장기기억 생성 작업 제출 조건과 거부 처리를 검증한다.

package com.landit.landitbe.feature.memory.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.landit.landitbe.config.memory.MemoryProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

/** 프리톡 장기기억 생성 작업 제출 조건과 거부 처리를 검증한다. */
class FreeTalkMemoryGenerationDispatchServiceTest {

  @Test
  void doesNotSubmitWhenMemoryWritingIsDisabled() {
    FreeTalkMemoryGenerationService generationService =
        Mockito.mock(FreeTalkMemoryGenerationService.class);
    TaskExecutor taskExecutor = Mockito.mock(TaskExecutor.class);
    FreeTalkMemoryGenerationDispatchService dispatchService =
        new FreeTalkMemoryGenerationDispatchService(
            generationService, taskExecutor, new MemoryProperties(false));

    dispatchService.dispatch(10L);

    verify(taskExecutor, never()).execute(Mockito.any());
    verify(generationService, never()).generate(10L);
  }

  @Test
  void marksRegisteredJobFailedWhenExecutorRejectsSubmission() {
    FreeTalkMemoryGenerationService generationService =
        Mockito.mock(FreeTalkMemoryGenerationService.class);
    TaskExecutor rejectingExecutor =
        task -> {
          throw new TaskRejectedException("queue full");
        };
    FreeTalkMemoryGenerationDispatchService dispatchService =
        new FreeTalkMemoryGenerationDispatchService(
            generationService, rejectingExecutor, new MemoryProperties(true));

    dispatchService.dispatch(10L);

    verify(generationService).markFailed(10L);
  }

  @Test
  void submitsEnabledJobToExecutor() {
    FreeTalkMemoryGenerationService generationService =
        Mockito.mock(FreeTalkMemoryGenerationService.class);
    TaskExecutor taskExecutor = Mockito.mock(TaskExecutor.class);
    FreeTalkMemoryGenerationDispatchService dispatchService =
        new FreeTalkMemoryGenerationDispatchService(
            generationService, taskExecutor, new MemoryProperties(true));

    dispatchService.dispatch(10L);

    verify(taskExecutor).execute(Mockito.any());
  }
}
