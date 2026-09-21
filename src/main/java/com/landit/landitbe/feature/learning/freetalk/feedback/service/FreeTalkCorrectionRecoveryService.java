// 끝나지 못한 프리톡 턴 교정을 넘겨받아 다시 시도하고, 시도를 다 쓰면 실패로 확정한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import com.landit.landitbe.config.session.FreeTalkCorrectionRetryProperties;
import com.landit.landitbe.feature.learning.freetalk.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkCorrectionAttempt;
import com.landit.landitbe.feature.learning.freetalk.feedback.repository.FreeTalkMessageFeedbackRepository.RecoverableCorrection;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import jakarta.annotation.PreDestroy;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 끝나지 못한 턴 교정의 복구 워커다.
 *
 * <p>교정은 서버 메모리 안의 비동기 콜백이 저장한다. 서버가 재시작되면 콜백이 사라지고, AI가 판정을 돌려주지 못하면 교정이 비어 버린다. 프론트는 교정을 폴링하지
 * 않으므로 서버가 스스로 끝내야 한다. 준비 상태이면서 임대가 끝난 교정을 주기적으로 넘겨받아 같은 대화로 다시 요청한다.
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
  private final Executor executor;
  private final AtomicBoolean draining = new AtomicBoolean();

  /**
   * 복구 전용 스레드 하나로 워커를 만든다.
   *
   * <p>실시간 속마음 호출과 같은 실행기를 쓰지 않는다. AI가 느려져 다시 시도할 교정이 늘어날수록 복구가 실시간 요청의 자리를 차지하게 되기 때문이다. 스레드가 하나라
   * 복구의 AI 호출은 한 번에 하나만 나간다.
   */
  @Autowired
  public FreeTalkCorrectionRecoveryService(
      FreeTalkMessageFeedbackService feedbackService,
      FreeTalkCorrectionRequestService requestService,
      AiFreeTalkClient aiFreeTalkClient,
      FreeTalkCorrectionRetryProperties retryProperties) {
    this(
        feedbackService,
        requestService,
        aiFreeTalkClient,
        retryProperties,
        Executors.newSingleThreadExecutor(
            task -> {
              Thread thread = new Thread(task, "free-talk-correction-recovery");
              thread.setDaemon(true);
              return thread;
            }));
  }

  /** 테스트에서 복구를 같은 스레드에서 끝까지 돌릴 수 있게 실행기를 받는다. */
  FreeTalkCorrectionRecoveryService(
      FreeTalkMessageFeedbackService feedbackService,
      FreeTalkCorrectionRequestService requestService,
      AiFreeTalkClient aiFreeTalkClient,
      FreeTalkCorrectionRetryProperties retryProperties,
      Executor executor) {
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

  /**
   * 임대가 끝난 교정을 복구 스레드에서 하나씩 다시 시도한다. 앞선 복구가 아직 돌고 있으면 새로 시작하지 않는다.
   *
   * <p>AI 호출이 스케줄러 스레드를 붙잡지 않도록 복구 스레드로 넘긴다.
   */
  public void recover() {
    if (!draining.compareAndSet(false, true)) {
      return;
    }
    try {
      executor.execute(this::drain);
    } catch (RuntimeException exception) {
      draining.set(false);
      log.warn(
          "workflow=free_talk_correction_retry outcome=not_started error={}",
          exception.getClass().getSimpleName());
    } catch (Error error) {
      // 복구 스레드를 만들지 못하는 경우(메모리 부족 등)에도 표시를 풀어야 한다. 풀지 않으면 복구가 흔적 없이 영영 멈춘다.
      draining.set(false);
      throw error;
    }
  }

  /** 서버가 내려갈 때 복구 스레드를 정리한다. 처리 중이던 시도는 임대가 끝난 뒤 다른 서버가 넘겨받는다. */
  @PreDestroy
  void shutdown() {
    if (executor instanceof ExecutorService service) {
      service.shutdownNow();
    }
  }

  // 찾은 교정을 하나씩 "선점 → 호출 → 저장"한다. 선점을 호출 직전에 해야 임대가 줄을 서서 기다리는 동안 끝나 버리지 않는다.
  // 한 건의 실패가 나머지 복구를 막지 않는다.
  private void drain() {
    try {
      for (RecoverableCorrection candidate : feedbackService.findRecoverable()) {
        try {
          feedbackService
              .claimNextAttempt(candidate.getSessionHistoryMessageId(), candidate.getAttempts())
              .ifPresent(this::run);
        } catch (RuntimeException exception) {
          log.warn(
              "workflow=free_talk_correction_retry outcome=claim_error messageId={} error={}",
              candidate.getSessionHistoryMessageId(),
              exception.getClass().getSimpleName());
        }
      }
    } catch (RuntimeException exception) {
      log.warn(
          "workflow=free_talk_correction_retry outcome=find_error error={}",
          exception.getClass().getSimpleName());
    } finally {
      draining.set(false);
    }
  }

  // 선점한 시도 하나를 끝까지 처리한다. 어떤 예외도 밖으로 내보내지 않는다.
  private void run(FreeTalkCorrectionAttempt attempt) {
    try {
      Optional<AiFreeTalkInnerThoughtRequest> request = requestService.rebuild(attempt.messageId());
      if (request.isEmpty()) {
        feedbackService.failUnrebuildableAttempt(attempt);
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
