// 프리톡 사용자 발화의 턴 교정을 준비·확정하고 대화 기록 단위로 조회한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.service;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.config.session.FreeTalkCorrectionRetryProperties;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMessageFeedback;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.feedback.repository.FreeTalkMessageFeedbackRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 턴 교정의 저장과 조회를 담당한다. */
@Slf4j
@RequiredArgsConstructor
@Service
public class FreeTalkMessageFeedbackService {

  // AI 응답을 기다려 주는 시간에 더하는 여유. 시나리오 메시지 피드백 작업과 같은 규칙이다.
  private static final Duration LEASE_MARGIN = Duration.ofSeconds(30);
  private static final int FIRST_ATTEMPT = 1;

  private final FreeTalkMessageFeedbackRepository feedbackRepository;
  private final ConversationMessageService conversationMessageService;
  private final AiClientProperties aiClientProperties;
  private final FreeTalkCorrectionRetryProperties retryProperties;
  private final MeterRegistry meterRegistry;
  private final Clock clock;

  /**
   * 속마음 준비를 건 같은 트랜잭션에서 교정 판정을 기다리는 행을 만든다.
   *
   * <p>행을 만들 때 첫 시도(attempts = 1)와 그 응답을 기다려 줄 시각(lease)을 함께 적는다. 시각은 애플리케이션 Clock으로만 찍는다.
   *
   * <p>교정은 부가 기능이라 준비가 턴 확정을 막아서는 안 된다. 교정 도입 전에 저장되어 실패 행이 먼저 채워진 발화가 도입 뒤에 확정되면 새 행을 만들지 않고 그 행을
   * 준비 상태로 되돌린다. 대화 기록 ID는 호출자에게 받지 않고 발화에서 읽어 둘이 어긋나지 않게 한다.
   *
   * @param messageId 교정 대상 사용자 발화 ID
   * @throws org.springframework.transaction.IllegalTransactionStateException 턴 확정 트랜잭션 밖에서 호출했을 때
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void prepareCorrection(long messageId) {
    LocalDateTime leaseUntil = firstAttemptLeaseUntil();
    feedbackRepository
        .findBySessionHistoryMessageId(messageId)
        .ifPresentOrElse(
            feedback -> feedback.restartPreparing(leaseUntil),
            () ->
                feedbackRepository.save(
                    FreeTalkMessageFeedback.preparing(
                        messageId,
                        conversationMessageService.require(messageId).getSessionHistoryId(),
                        leaseUntil)));
  }

  // 첫 시도의 응답을 기다려 주는 시각. 이 시각이 지나도 준비 상태면 콜백이 사라진 것으로 보고 복구 워커가 넘겨받는다.
  private LocalDateTime firstAttemptLeaseUntil() {
    return LocalDateTime.now(clock).plus(aiClientProperties.requestTimeout()).plus(LEASE_MARGIN);
  }

  /**
   * 준비 상태인 교정에만 판정 결과를 반영한다. 같은 발화에 두 번 호출해도 한 번만 저장된다.
   *
   * @param messageId 교정 대상 사용자 발화 ID
   * @param correction 교정 판정 결과
   * @return 갱신된 row 수. 이미 판정이 끝난 교정이면 0
   */
  @Transactional
  public int completeIfPreparing(long messageId, FreeTalkTurnCorrection correction) {
    FreeTalkTurnCorrection.Sentence sentence = correction.sentence();
    return feedbackRepository.updateIfPreparing(
        messageId,
        correction.status(),
        correction.reactedToPartner(),
        sentence == null ? null : sentence.originalSentence(),
        sentence == null ? null : sentence.betterSentence(),
        sentence == null ? null : sentence.reason(),
        sentence == null ? null : sentence.mistakePattern(),
        sentence == null ? null : sentence.usedMemoryId(),
        sentence == null ? null : sentence.memoryObservedOn(),
        sentence == null ? null : sentence.memoryLabel(),
        ProcessingStatus.PREPARING);
  }

  /**
   * 첫 시도의 판정 결과를 반영한다. AI가 판정을 돌려주지 못했으면 실패로 끝내지 않고 다음 시도를 기다리게 한다.
   *
   * @param messageId 교정 대상 사용자 발화 ID
   * @param correction 첫 시도의 교정 판정 결과
   */
  @Transactional
  public void completeFirstAttempt(long messageId, FreeTalkTurnCorrection correction) {
    if (correction.retryable()) {
      retryOrFail(messageId, FIRST_ATTEMPT, null);
      return;
    }
    completeIfPreparing(messageId, correction);
  }

  /**
   * AI 호출 자체가 실패한 첫 시도를 끝낸다. 실패로 확정하지 않고 다음 시도를 기다리게 한다.
   *
   * @param messageId 교정 대상 사용자 발화 ID
   */
  @Transactional
  public void failFirstAttempt(long messageId) {
    retryOrFail(messageId, FIRST_ATTEMPT, null);
  }

  /**
   * 다시 해 볼 실패로 끝난 시도를 정리한다. 시도가 남았으면 준비 상태를 유지한 채 다음 시도 시각을 적고, 다 썼으면 실패로 확정한다.
   *
   * <p>그 시도를 시작한 쪽의 결과만 반영한다. 응답이 늦어 복구 워커가 이미 넘겨받은 교정은 건드리지 않는다.
   *
   * @param messageId 교정 대상 사용자 발화 ID
   * @param attempt 방금 실패한 시도의 순번(첫 시도가 1)
   * @param attemptToken 그 시도의 선점 식별자. 첫 시도는 null
   */
  @Transactional
  public void retryOrFail(long messageId, int attempt, String attemptToken) {
    String token = attemptToken == null ? "" : attemptToken;
    if (attempt >= retryProperties.maxAttempts()) {
      int failed =
          feedbackRepository.failAttempt(
              messageId, attempt, token, ProcessingStatus.FAILED, ProcessingStatus.PREPARING);
      report(failed == 1 ? "exhausted" : "ignored", messageId, attempt);
      return;
    }
    int released =
        feedbackRepository.releaseForRetry(
            messageId,
            attempt,
            token,
            LocalDateTime.now(clock).plus(retryProperties.delayAfter(attempt)),
            ProcessingStatus.PREPARING);
    report(released == 1 ? "scheduled" : "ignored", messageId, attempt);
  }

  // 교정 문장은 사용자 발화라 남기지 않고, 어느 발화의 몇 번째 시도가 어떻게 끝났는지만 남긴다.
  private void report(String outcome, long messageId, int attempt) {
    meterRegistry.counter("landit.free_talk.correction.retry", "outcome", outcome).increment();
    log.info(
        "workflow=free_talk_correction_retry outcome={} messageId={} attempt={}",
        outcome,
        messageId,
        attempt);
  }

  /**
   * 대화 기록 하나의 교정을 사용자 발화 ID로 묶어 돌려준다.
   *
   * @param sessionHistoryId 대화 기록 ID
   * @return 사용자 발화 ID별 교정 판정. 교정 대상이 아닌 메시지는 들어 있지 않다
   */
  @Transactional(readOnly = true)
  public Map<Long, FreeTalkTurnCorrection> findBySessionHistoryId(long sessionHistoryId) {
    return feedbackRepository.findBySessionHistoryId(sessionHistoryId).stream()
        .peek(FreeTalkMessageFeedbackService::warnInconsistentMemory)
        .collect(
            Collectors.toMap(
                FreeTalkMessageFeedback::getSessionHistoryMessageId,
                FreeTalkMessageFeedback::toCorrection));
  }

  // DB 제약이 막는 상태라 정상적으로는 없다. 있으면 교정은 내려주되 근거 기억 태그만 빠지므로 흔적을 남긴다.
  private static void warnInconsistentMemory(FreeTalkMessageFeedback feedback) {
    if (!feedback.hasConsistentMemory()) {
      log.warn(
          "프리톡 턴 교정의 근거 기억 값이 어긋나 태그 없이 내려줍니다. workflow=free_talk_turn_correction_memory"
              + " reason=inconsistent_stored_memory messageId={}",
          feedback.getSessionHistoryMessageId());
    }
  }
}
