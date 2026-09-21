// 끝나지 못한 프리톡 턴 교정을 넘겨받아 다시 시도하고, 시도를 다 쓰면 실패로 확정한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import com.landit.landitbe.config.session.FreeTalkCorrectionRetryProperties;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkCorrectionAttempt;
import com.landit.landitbe.feature.learning.freetalk.feedback.repository.FreeTalkMessageFeedbackRepository.RecoverableCorrection;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 끝나지 못한 턴 교정의 복구 워커다.
 *
 * <p>교정은 서버 메모리 안의 비동기 콜백이 저장한다. 서버가 재시작되면 콜백이 사라지고, AI가 판정을 돌려주지 못하면 교정이 비어 버린다. 프론트는 교정을 폴링하지
 * 않으므로 서버가 스스로 끝내야 한다. 준비 상태이면서 임대가 끝난 교정을 주기적으로 넘겨받아 같은 입력으로 다시 요청한다.
 *
 * <p>이 워커는 세션 잠금 밖에서 교정 행을 쓴다. 그래서 행을 읽어서 고치지 않고 조건부 갱신으로만 쓴다. 다시 요청한 응답의 속마음은 버린다. 속마음은 대화 중에만 의미가
 * 있고 이미 끝나 있다.
 */
@Slf4j
@Service
public class FreeTalkCorrectionRecoveryService {

  private final FreeTalkMessageFeedbackService feedbackService;
  private final FreeTalkCorrectionRequestService requestService;
  private final AiFreeTalkClient aiFreeTalkClient;
  private final FreeTalkCorrectionRetryProperties retryProperties;
  private final TaskExecutor executor;

  /** AI 호출은 스케줄러 스레드를 붙잡지 않도록 작업 실행기로 넘긴다. */
  public FreeTalkCorrectionRecoveryService(
      FreeTalkMessageFeedbackService feedbackService,
      FreeTalkCorrectionRequestService requestService,
      AiFreeTalkClient aiFreeTalkClient,
      FreeTalkCorrectionRetryProperties retryProperties,
      @Qualifier("applicationTaskExecutor") TaskExecutor executor) {
    this.feedbackService = feedbackService;
    this.requestService = requestService;
    this.aiFreeTalkClient = aiFreeTalkClient;
    this.retryProperties = retryProperties;
    this.executor = executor;
  }

  /** 설정으로 켜져 있을 때만 주기 복구를 돌린다. */
  @Scheduled(
      fixedDelayString = "${landit.free-talk.correction-retry.recovery-interval-ms:30000}",
      initialDelayString = "${landit.free-talk.correction-retry.recovery-interval-ms:30000}")
  public void recoverOnSchedule() {
    if (retryProperties.schedulingEnabled()) {
      recover();
    }
  }

  /** 임대가 끝난 교정을 선점해 다시 시도한다. 한 건의 실패가 나머지 복구를 막지 않는다. */
  public void recover() {
    for (RecoverableCorrection candidate : feedbackService.findRecoverable()) {
      try {
        feedbackService
            .claimNextAttempt(candidate.getSessionHistoryMessageId(), candidate.getAttempts())
            .ifPresent(this::dispatch);
      } catch (RuntimeException exception) {
        log.warn(
            "workflow=free_talk_correction_retry outcome=claim_error messageId={} error={}",
            candidate.getSessionHistoryMessageId(),
            exception.getClass().getSimpleName());
      }
    }
  }

  // 실행기가 작업을 받지 못하면 이번 시도를 실패한 시도로 끝내 다음 주기에 다시 집히게 한다.
  private void dispatch(FreeTalkCorrectionAttempt attempt) {
    try {
      executor.execute(() -> run(attempt));
    } catch (RuntimeException exception) {
      log.warn(
          "workflow=free_talk_correction_retry outcome=rejected messageId={} attempt={} error={}",
          attempt.messageId(),
          attempt.attempt(),
          exception.getClass().getSimpleName());
      feedbackService.retryOrFail(attempt.messageId(), attempt.attempt(), attempt.token());
    }
  }

  // 선점한 시도 하나를 끝까지 처리한다. 어떤 예외도 밖으로 내보내지 않는다.
  private void run(FreeTalkCorrectionAttempt attempt) {
    try {
      Optional<AiFreeTalkInnerThoughtRequest> request = requestService.rebuild(attempt.messageId());
      if (request.isEmpty()) {
        feedbackService.abandonAttempt(attempt);
        return;
      }
      feedbackService.completeAttempt(
          attempt, aiFreeTalkClient.generateInnerThought(request.get()).correction());
    } catch (RuntimeException exception) {
      // 예외 메시지에 사용자 발화가 섞일 수 있어 예외 종류만 남긴다.
      log.warn(
          "workflow=free_talk_correction_retry outcome=call_failed messageId={} attempt={}"
              + " error={}",
          attempt.messageId(),
          attempt.attempt(),
          exception.getClass().getSimpleName());
      releaseSafely(attempt);
    }
  }

  private void releaseSafely(FreeTalkCorrectionAttempt attempt) {
    try {
      feedbackService.retryOrFail(attempt.messageId(), attempt.attempt(), attempt.token());
    } catch (RuntimeException exception) {
      // 여기서도 실패하면 임대가 끝난 뒤 다음 주기가 다시 집는다.
      log.warn(
          "workflow=free_talk_correction_retry outcome=release_error messageId={} attempt={}"
              + " error={}",
          attempt.messageId(),
          attempt.attempt(),
          exception.getClass().getSimpleName());
    }
  }
}
