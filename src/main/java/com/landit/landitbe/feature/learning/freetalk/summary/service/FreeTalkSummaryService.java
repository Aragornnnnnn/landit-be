// 완료된 스몰톡의 요약을 조회하고, 아직 없는 총평은 이 세션의 턴 교정이 끝났을 때 한 번 계산해 저장한다.

package com.landit.landitbe.feature.learning.freetalk.summary.service;

import com.landit.landitbe.feature.learning.conversation.domain.LearningSessionStatus;
import com.landit.landitbe.feature.learning.conversation.domain.ProcessingStatus;
import com.landit.landitbe.feature.learning.conversation.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistoryMessageSnapshot;
import com.landit.landitbe.feature.learning.conversation.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.learning.conversation.exception.SessionErrorCode;
import com.landit.landitbe.feature.learning.conversation.history.service.ConversationMessageService;
import com.landit.landitbe.feature.learning.conversation.history.service.SessionHistoryService;
import com.landit.landitbe.feature.learning.conversation.service.LearningSessionService;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.learning.freetalk.expression.reuse.service.FreeTalkExpressionReuseQueryService;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkPatternUsage;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkPatternUsageDraft;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.feedback.repository.FreeTalkPatternUsageRepository;
import com.landit.landitbe.feature.learning.freetalk.feedback.service.FreeTalkMessageFeedbackService;
import com.landit.landitbe.feature.learning.freetalk.followup.service.FreeTalkFollowUpService;
import com.landit.landitbe.feature.learning.freetalk.memory.domain.MemoryGenerationStatus;
import com.landit.landitbe.feature.learning.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkSessionSummary;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSessionSummaryResponse;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSummarySource;
import com.landit.landitbe.feature.learning.freetalk.summary.repository.FreeTalkSessionSummaryRepository;
import com.landit.landitbe.shared.domain.ConversationSpeaker;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 오늘의 스몰톡 요약을 조회한다.
 *
 * <p>총평은 이 세션의 턴 교정이 모두 끝난 뒤 한 번 계산해 저장하고, 그 뒤로는 저장된 값만 돌려준다. 교정이 아직 끝나지 않았으면 세션 종료 후 짧은 상한까지는
 * {@code pending}으로 알리고, 상한이 지나면 남은 교정을 빼고 확정한다. 이 서비스는 트랜잭션을 열지 않는다. 읽기는 각 서비스가 제 트랜잭션에서 하고, 쓰기
 * 둘(총평 저장, 멈춘 장기기억 확정)은 각각 짧은 트랜잭션으로 분리해 실패가 조회를 오염시키지 않게 한다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FreeTalkSummaryService {

  /** 세션 종료 후 턴 교정이 끝나기를 기다려 주는 상한. 교정은 보통 몇 초 안에 끝나고, 재시도까지 기다리면 요약 화면이 멈춘다. */
  static final Duration CORRECTION_WAIT = Duration.ofSeconds(30);

  /**
   * 장기기억 작업을 기다려 주는 상한. 작업이 선점된 뒤에는 선점 시각부터, 아직 선점되지 않았으면 세션 종료부터 센다. 넘기면 멈춘 것으로 보고 실패로 확정한다. 종료
   * 시각만 기준으로 하면 실행기 대기로 늦게 선점된 정상 작업을 죽이고, 그 작업이 저장하는 기억과 후속 질문까지 함께 잃는다.
   */
  static final Duration MEMORY_WAIT = Duration.ofMinutes(5);

  private final LearningSessionService learningSessionService;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final SessionHistoryService sessionHistoryService;
  private final ConversationMessageService conversationMessageService;
  private final FreeTalkMessageFeedbackService messageFeedbackService;
  private final FreeTalkPatternUsageRepository patternUsageRepository;
  private final FreeTalkSessionSummaryRepository summaryRepository;
  private final FreeTalkSummaryCalculator calculator;
  private final FreeTalkExpressionReuseQueryService expressionReuseQueryService;
  private final FreeTalkFollowUpService followUpService;
  private final PlatformTransactionManager transactionManager;
  private final Clock clock;

  /**
   * 사용자가 소유한 완료 스몰톡의 요약을 조회한다.
   *
   * @param userId 조회하는 사용자 ID
   * @param learningSessionId 프리톡 학습 세션 ID
   * @return 요약. 총평 계산 전이면 {@code pending}
   * @throws ApiException 세션이 없거나(404) 소유자가 아니거나(403) 아직 완료되지 않았을 때(409)
   */
  public FreeTalkSessionSummaryResponse getSummary(long userId, long learningSessionId) {
    LearningSessionSnapshot learningSession =
        learningSessionService
            .findSession(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
    if (!Objects.equals(learningSession.getUserProfileId(), userId)) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    FreeTalkSession session =
        freeTalkSessionRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
    if (learningSession.getStatus() != LearningSessionStatus.COMPLETED
        || session.getConversationStatus() != FreeTalkConversationStatus.COMPLETED) {
      throw new ApiException(SessionErrorCode.SESSION_NOT_COMPLETED);
    }
    SessionHistorySnapshot history =
        sessionHistoryService
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));

    Optional<FreeTalkSessionSummary> summary =
        summaryRepository
            .findByFreeTalkSessionId(session.getId())
            .or(() -> settle(learningSession, session, history));
    FreeTalkSession current = confirmStaleMemoryGeneration(learningSession, session);
    var reused =
        expressionReuseQueryService.findSummary(
            current.getId(), current.getExpressionGenerationStatus());
    var followUp =
        followUpService.findSummary(current.getId(), current.getMemoryGenerationStatus());
    return summary
        .map(
            stored ->
                FreeTalkSessionSummaryResponse.of(
                    learningSessionId, current.getTitle(), stored, reused, followUp))
        .orElseGet(
            () ->
                FreeTalkSessionSummaryResponse.pending(
                    learningSessionId, current.getTitle(), reused, followUp));
  }

  // 교정이 남아 있고 상한 전이면 비워 둔다. 아니면 계산해 저장한다. 동시에 두 조회가 저장하면 UNIQUE에 진 쪽이 저장된 행을 다시 읽는다.
  private Optional<FreeTalkSessionSummary> settle(
      LearningSessionSnapshot learningSession,
      FreeTalkSession session,
      SessionHistorySnapshot history) {
    Map<Long, FreeTalkTurnCorrection> corrections =
        messageFeedbackService.findBySessionHistoryId(history.getId());
    boolean correcting =
        corrections.values().stream()
            .anyMatch(correction -> correction.status() == ProcessingStatus.PREPARING);
    if (correcting && before(learningSession.getEndedAt().plus(CORRECTION_WAIT))) {
      return Optional.empty();
    }
    FreeTalkSessionSummary calculated = calculate(learningSession, session, history, corrections);
    try {
      return Optional.of(
          new TransactionTemplate(transactionManager)
              .execute(status -> summaryRepository.save(calculated)));
    } catch (DataIntegrityViolationException exception) {
      log.info(
          "workflow=free_talk_summary outcome=already_settled learningSessionId={}",
          learningSession.getId());
      return summaryRepository.findByFreeTalkSessionId(session.getId());
    }
  }

  private FreeTalkSessionSummary calculate(
      LearningSessionSnapshot learningSession,
      FreeTalkSession session,
      SessionHistorySnapshot history,
      Map<Long, FreeTalkTurnCorrection> corrections) {
    FreeTalkSummarySource current = source(history.getId(), corrections);
    Optional<FreeTalkSession> previousSession =
        freeTalkSessionRepository
            .findPreviousCompleted(learningSession.getId(), PageRequest.of(0, 1))
            .stream()
            .findFirst();
    if (previousSession.isEmpty()) {
      return calculator.calculate(
          learningSession.getUserProfileId(),
          session.getId(),
          history.getId(),
          learningSession.getId(),
          current,
          null,
          null);
    }
    long previousLearningSessionId = previousSession.get().getLearningSessionId();
    LearningSessionSnapshot previousLearning =
        learningSessionService
            .findSession(previousLearningSessionId)
            .orElseThrow(() -> missingPrevious("학습 세션", previousLearningSessionId));
    SessionHistorySnapshot previousHistory =
        sessionHistoryService
            .findByLearningSessionId(previousLearningSessionId)
            .orElseThrow(() -> missingPrevious("대화 기록", previousLearningSessionId));
    FreeTalkSummarySource previousSource =
        source(
            previousHistory.getId(),
            messageFeedbackService.findBySessionHistoryId(previousHistory.getId()));
    FreeTalkSessionSummary.PreviousSession previous =
        new FreeTalkSessionSummary.PreviousSession(
            previousLearningSessionId,
            previousLearning.getEndedAt().toLocalDate(),
            (int)
                ChronoUnit.DAYS.between(
                    previousLearning.getEndedAt().toLocalDate(),
                    learningSession.getStartedAt().toLocalDate()));
    return calculator.calculate(
        learningSession.getUserProfileId(),
        session.getId(),
        history.getId(),
        learningSession.getId(),
        current,
        previous,
        previousSource);
  }

  // 발화 순서대로 사용자 발화와 그 발화의 교정(판정을 마쳐 문장이 있는 것만)을 모은다. 상한을 넘겨 아직 준비 상태인 교정은 문장이 없어 빠지고,
  // 그 사실을 함께 넘겨 계산기가 "오늘 맞게 썼다"를 섣불리 주장하지 않게 한다.
  private FreeTalkSummarySource source(
      long sessionHistoryId, Map<Long, FreeTalkTurnCorrection> corrections) {
    List<FreeTalkSummarySource.Utterance> utterances = new ArrayList<>();
    List<FreeTalkTurnCorrection.Sentence> sentences = new ArrayList<>();
    for (SessionHistoryMessageSnapshot message :
        conversationMessageService.findAll(sessionHistoryId)) {
      if (message.getRole() != ConversationSpeaker.USER) {
        continue;
      }
      utterances.add(
          new FreeTalkSummarySource.Utterance(
              message.getContent(), message.getUtteranceDurationMs()));
      FreeTalkTurnCorrection correction = corrections.get(message.getId());
      if (correction != null && correction.sentence() != null) {
        sentences.add(correction.sentence());
      }
    }
    List<FreeTalkPatternUsageDraft> usages =
        patternUsageRepository.findBySessionHistoryIdOrderByIdAsc(sessionHistoryId).stream()
            .map(FreeTalkSummaryService::draft)
            .toList();
    boolean correctionsComplete =
        corrections.values().stream()
            .noneMatch(correction -> correction.status() == ProcessingStatus.PREPARING);
    return new FreeTalkSummarySource(utterances, sentences, usages, correctionsComplete);
  }

  private static IllegalStateException missingPrevious(String what, long learningSessionId) {
    return new IllegalStateException(
        "직전 스몰톡의 " + what + "이 없습니다. learningSessionId=" + learningSessionId);
  }

  private static FreeTalkPatternUsageDraft draft(FreeTalkPatternUsage usage) {
    return new FreeTalkPatternUsageDraft(
        usage.getPattern(), usage.getSentence(), usage.getSpan(), usage.isCorrect());
  }

  // 상한을 넘긴 장기기억 작업은 멈춘 것으로 보고 DB에 실패로 확정한다. 그사이 끝났으면 0건이라 그대로 두고, 어느 쪽이든 최신 상태를 다시 읽는다.
  private FreeTalkSession confirmStaleMemoryGeneration(
      LearningSessionSnapshot learningSession, FreeTalkSession session) {
    if (session.getMemoryGenerationStatus() != MemoryGenerationStatus.PREPARING) {
      return session;
    }
    LocalDateTime anchor =
        session.getMemoryGenerationStartedAt() == null
            ? learningSession.getEndedAt()
            : session.getMemoryGenerationStartedAt();
    if (before(anchor.plus(MEMORY_WAIT))) {
      return session;
    }
    Integer failed =
        new TransactionTemplate(transactionManager)
            .execute(
                status ->
                    freeTalkSessionRepository.failStaleMemoryGeneration(
                        session.getId(), LocalDateTime.now(clock)));
    if (Objects.equals(failed, 1)) {
      log.warn(
          "workflow=free_talk_summary outcome=memory_generation_timed_out learningSessionId={}",
          learningSession.getId());
    } else {
      log.info(
          "workflow=free_talk_summary outcome=memory_generation_settled_meanwhile"
              + " learningSessionId={}",
          learningSession.getId());
    }
    // 이 메서드는 트랜잭션 밖이라 새 영속성 컨텍스트로 읽어 방금 UPDATE한 값이 보인다. 트랜잭션 안에서 부르게 되면 지워진 캐시가 아니라 옛 엔티티를 볼 수 있다.
    return freeTalkSessionRepository.findById(session.getId()).orElse(session);
  }

  private boolean before(LocalDateTime deadline) {
    return LocalDateTime.now(clock).isBefore(deadline);
  }
}
