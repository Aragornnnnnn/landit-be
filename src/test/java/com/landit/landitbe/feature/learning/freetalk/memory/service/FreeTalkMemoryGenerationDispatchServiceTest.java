// 프리톡 장기기억 생성 작업 제출 조건과 거부 처리를 검증한다.

package com.landit.landitbe.feature.learning.freetalk.memory.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.landit.landitbe.config.memory.MemoryProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

/** 프리톡 장기기억 생성 작업 제출 조건과 거부 처리를 검증한다. */
class FreeTalkMemoryGenerationDispatchServiceTest {

  @DisplayName("기억 저장 기능이 꺼져 있으면 생성 작업을 제출하지 않는다.")
  @Test
  void doesNotSubmitWhenMemoryWritingIsDisabled() {
    FreeTalkMemoryGenerationService generationService =
        Mockito.mock(FreeTalkMemoryGenerationService.class);
    TaskExecutor taskExecutor = Mockito.mock(TaskExecutor.class);
    FreeTalkMemoryGenerationDispatchService dispatchService =
        new FreeTalkMemoryGenerationDispatchService(
            generationService, taskExecutor, new MemoryProperties(false, false));

    dispatchService.dispatch(10L);

    verify(taskExecutor, never()).execute(Mockito.any());
    verify(generationService, never()).generate(10L);
  }

  @DisplayName("실행기가 작업 제출을 거부하면 등록된 기억 생성 작업을 실패로 바꾼다.")
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
            generationService, rejectingExecutor, new MemoryProperties(true, false));

    dispatchService.dispatch(10L);

    verify(generationService).markFailed(10L);
  }

  @DisplayName("기억 저장 기능이 켜져 있으면 생성 작업을 실행기에 제출한다.")
  @Test
  void submitsEnabledJobToExecutor() {
    FreeTalkMemoryGenerationService generationService =
        Mockito.mock(FreeTalkMemoryGenerationService.class);
    TaskExecutor taskExecutor = Mockito.mock(TaskExecutor.class);
    FreeTalkMemoryGenerationDispatchService dispatchService =
        new FreeTalkMemoryGenerationDispatchService(
            generationService, taskExecutor, new MemoryProperties(true, false));

    dispatchService.dispatch(10L);

    verify(taskExecutor).execute(Mockito.any());
  }
}
