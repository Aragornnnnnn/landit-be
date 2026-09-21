// 턴 교정 복구 워커의 주기 실행 스위치와 한 건의 실패가 나머지를 막지 않는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.config.session.FreeTalkCorrectionRetryProperties;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkCorrectionAttempt;
import com.landit.landitbe.feature.learning.freetalk.feedback.repository.FreeTalkMessageFeedbackRepository.RecoverableCorrection;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.task.SyncTaskExecutor;

/** 턴 교정 복구 워커의 주기 실행 스위치와 한 건의 실패가 나머지를 막지 않는지 검증한다. */
class FreeTalkCorrectionRecoveryServiceTest {

  private final FreeTalkMessageFeedbackService feedbackService =
      mock(FreeTalkMessageFeedbackService.class);
  private final FreeTalkCorrectionRequestService requestService =
      mock(FreeTalkCorrectionRequestService.class);
  private final AiFreeTalkClient aiClient = mock(AiFreeTalkClient.class);

  @DisplayName("주기 복구가 꺼져 있으면 스케줄이 돌아도 아무것도 찾지 않는다.")
  @Test
  void doesNothingOnScheduleWhenDisabled() {
    service(false, new SyncTaskExecutor()).recoverOnSchedule();

    verify(feedbackService, never()).findRecoverable();
  }

  @DisplayName("주기 복구가 켜져 있으면 스케줄마다 복구 대상을 찾는다.")
  @Test
  void recoversOnScheduleWhenEnabled() {
    when(feedbackService.findRecoverable()).thenReturn(List.of());

    service(true, new SyncTaskExecutor()).recoverOnSchedule();

    verify(feedbackService).findRecoverable();
  }

  @DisplayName("한 건의 선점이 예외로 끝나도 나머지 교정은 계속 복구한다.")
  @Test
  void keepsRecoveringAfterOneClaimFails() {
    // 스텁을 거는 도중에 다른 mock을 스텁하면 안 되므로 복구 대상은 먼저 만들어 둔다.
    List<RecoverableCorrection> candidates = List.of(recoverable(1L, 1), recoverable(2L, 1));
    when(feedbackService.findRecoverable()).thenReturn(candidates);
    when(feedbackService.claimNextAttempt(1L, 1)).thenThrow(new IllegalStateException("db down"));
    FreeTalkCorrectionAttempt second = new FreeTalkCorrectionAttempt(2L, 2, "token-2");
    when(feedbackService.claimNextAttempt(2L, 1)).thenReturn(Optional.of(second));
    when(requestService.rebuild(2L)).thenReturn(Optional.empty());

    assertThatCode(() -> service(true, new SyncTaskExecutor()).recover())
        .doesNotThrowAnyException();

    verify(feedbackService).failUnrebuildableAttempt(second);
  }

  @DisplayName("앞선 복구가 아직 돌고 있으면 새 복구를 시작하지 않아, 복구의 AI 호출이 한 번에 하나만 나간다.")
  @Test
  void doesNotStartAnotherRunWhileOneIsDraining() {
    // 넘겨받은 작업을 돌리지 않고 붙잡아 두는 실행기로 "복구가 아직 도는 중"을 만든다.
    List<Runnable> held = new ArrayList<>();
    FreeTalkCorrectionRecoveryService service = service(true, held::add);
    when(feedbackService.findRecoverable()).thenReturn(List.of());

    service.recover();
    service.recover();

    assertThat(held).hasSize(1);
    // 앞선 복구가 끝나면 다음 주기에는 다시 시작한다.
    held.getFirst().run();
    service.recover();
    assertThat(held).hasSize(2);
  }

  @DisplayName("복구 스레드에 작업을 넘기지 못해도 다음 주기에 다시 시작할 수 있다.")
  @Test
  void canStartAgainAfterExecutorRefusedTheRun() {
    Executor refusing = mock(Executor.class);
    doThrow(new RejectedExecutionException("shutting down")).when(refusing).execute(any());
    FreeTalkCorrectionRecoveryService service = service(true, refusing);

    assertThatCode(service::recover).doesNotThrowAnyException();
    service.recover();

    verify(refusing, times(2)).execute(any());
    verify(feedbackService, never()).findRecoverable();
  }

  @DisplayName("복구 스레드를 만들지 못하는 오류가 나도 도는 중 표시를 풀어, 복구가 흔적 없이 영영 멈추지 않게 한다.")
  @Test
  void canStartAgainAfterExecutorThrewAnError() {
    Executor failing = mock(Executor.class);
    doThrow(new OutOfMemoryError("unable to create native thread")).when(failing).execute(any());
    FreeTalkCorrectionRecoveryService service = service(true, failing);

    assertThatThrownBy(service::recover).isInstanceOf(OutOfMemoryError.class);
    assertThatThrownBy(service::recover).isInstanceOf(OutOfMemoryError.class);

    verify(failing, times(2)).execute(any());
  }

  @DisplayName("복구 대상을 찾다가 예외가 나도 다음 주기에 다시 시작할 수 있다.")
  @Test
  void canStartAgainAfterFindingFailed() {
    when(feedbackService.findRecoverable()).thenThrow(new IllegalStateException("db down"));
    FreeTalkCorrectionRecoveryService service = service(true, new SyncTaskExecutor());

    assertThatCode(service::recover).doesNotThrowAnyException();
    service.recover();

    verify(feedbackService, times(2)).findRecoverable();
  }

  @DisplayName("AI 호출 예외의 메시지에는 사용자 발화가 섞일 수 있어 로그에는 예외 종류만 남긴다.")
  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void logsOnlyExceptionTypeWhenAiCallFails(CapturedOutput output) {
    List<RecoverableCorrection> candidates = List.of(recoverable(1L, 1));
    when(feedbackService.findRecoverable()).thenReturn(candidates);
    when(feedbackService.claimNextAttempt(1L, 1))
        .thenReturn(Optional.of(new FreeTalkCorrectionAttempt(1L, 2, "token-2")));
    AiFreeTalkInnerThoughtRequest request = mock(AiFreeTalkInnerThoughtRequest.class);
    when(requestService.rebuild(1L)).thenReturn(Optional.of(request));
    when(aiClient.generateInnerThought(any())).thenThrow(new IllegalStateException("비밀 발화"));

    service(true, new SyncTaskExecutor()).recover();

    verify(feedbackService).retryOrFail(1L, 2, "token-2");
    assertThat(output.getOut())
        .contains("workflow=free_talk_correction_retry outcome=call_failed messageId=1 attempt=2")
        .contains("error=IllegalStateException")
        .doesNotContain("비밀");
  }

  private FreeTalkCorrectionRecoveryService service(boolean enabled, Executor executor) {
    return new FreeTalkCorrectionRecoveryService(
        feedbackService,
        requestService,
        aiClient,
        new FreeTalkCorrectionRetryProperties(3, List.of(Duration.ZERO), 10, enabled),
        executor);
  }

  private static RecoverableCorrection recoverable(long messageId, int attempts) {
    RecoverableCorrection candidate = mock(RecoverableCorrection.class);
    when(candidate.getSessionHistoryMessageId()).thenReturn(messageId);
    when(candidate.getAttempts()).thenReturn(attempts);
    return candidate;
  }
}
