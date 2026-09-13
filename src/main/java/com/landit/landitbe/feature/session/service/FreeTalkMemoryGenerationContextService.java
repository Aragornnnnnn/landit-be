// 프리톡 장기기억 생성 상태의 완료 전이를 트랜잭션으로 감싼다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.memory.client.ai.ConversationMemoryHistoryMessage;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryGenerationRequest;
import com.landit.landitbe.feature.memory.service.ConversationMemoryWriteService;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.domain.MemoryGenerationStatus;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.domain.SessionHistoryMessage;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 프리톡 장기기억 생성 상태의 완료 전이를 트랜잭션으로 감싼다. */
@RequiredArgsConstructor
@Service
public class FreeTalkMemoryGenerationContextService {

  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final LearningSessionRepository learningSessionRepository;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;
  private final ConversationMemoryWriteService memoryWriteService;
  private final Clock clock;

  /**
   * 완료 프리톡의 준비 작업을 잠그고 외부 호출용 문맥을 원자적으로 선점한다.
   *
   * @param learningSessionId 선점할 학습 세션 ID
   * @return 선점했으면 불변 문맥, 다른 작업이 선점했거나 실행 대상이 아니면 null
   * @throws ApiException 프리톡 세션 또는 이력 컨테이너를 찾을 수 없을 때
   * @throws IllegalStateException 이력 메시지 또는 장기기억 생성 상태가 유효하지 않을 때
   * @throws IllegalArgumentException 생성 문맥의 ID, 캐릭터 또는 필수 값이 유효하지 않을 때
   */
  @Transactional
  public ConversationMemoryGenerationRequest claim(long learningSessionId) {
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    LearningSession learningSession =
        learningSessionRepository
            .findById(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));

    if (!isEligibleForClaim(learningSession, freeTalkSession)) {
      return null;
    }

    SessionHistory history = loadHistory(learningSessionId);
    List<ConversationMemoryHistoryMessage> historyMessages = loadHistoryMessages(history.getId());
    freeTalkSession.startMemoryGeneration(LocalDateTime.now(clock));
    return new ConversationMemoryGenerationRequest(
        learningSessionId,
        learningSession.getUserProfileId(),
        freeTalkSession.getCharacterId(),
        learningSession.getTargetLocale().name(),
        learningSession.getBaseLocale().name(),
        clock.getZone().getId(),
        historyMessages);
  }

  /** 완료 후 아직 다른 worker가 선점하지 않은 세션만 장기기억 생성 대상이다. */
  private static boolean isEligibleForClaim(
      LearningSession learningSession, FreeTalkSession freeTalkSession) {
    return learningSession.getStatus() == LearningSessionStatus.COMPLETED
        && freeTalkSession.getConversationStatus() == FreeTalkConversationStatus.COMPLETED
        && freeTalkSession.getMemoryGenerationStatus() == MemoryGenerationStatus.PREPARING
        && freeTalkSession.getMemoryGenerationStartedAt() == null;
  }

  /** 생성 대상 세션의 단일 이력 컨테이너를 찾아 원본 기준을 고정한다. */
  private SessionHistory loadHistory(long learningSessionId) {
    return sessionHistoryRepository
        .findByLearningSessionId(learningSessionId)
        .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
  }

  /** 메시지 순서를 보존해 AI가 후보 원본 ID와 관찰 시각을 검증할 수 있게 한다. */
  private List<ConversationMemoryHistoryMessage> loadHistoryMessages(long historyId) {
    return sessionHistoryMessageRepository
        .findBySessionHistoryIdOrderByMessageSequenceAsc(historyId)
        .stream()
        .map(this::toHistoryMessage)
        .toList();
  }

  /** AI 입력에는 원본 메시지의 식별자·순서·시각이 모두 필요하다. */
  private ConversationMemoryHistoryMessage toHistoryMessage(SessionHistoryMessage message) {
    if (message.getId() == null || message.getRole() == null || message.getCreatedAt() == null) {
      throw new IllegalStateException("프리톡 이력 메시지 문맥이 유효하지 않습니다.");
    }
    OffsetDateTime occurredAt = message.getCreatedAt().atZone(clock.getZone()).toOffsetDateTime();
    return new ConversationMemoryHistoryMessage(
        message.getId(),
        message.getTurnNumber(),
        message.getRole().name(),
        message.getContent(),
        message.getTranslatedContent(),
        occurredAt);
  }

  /**
   * 장기기억 저장과 같은 트랜잭션에서 완료 세션을 READY로 전환한다.
   *
   * @param learningSessionId 완료할 프리톡 학습 세션 ID
   * @throws ApiException 프리톡 세션을 찾을 수 없을 때
   * @throws IllegalStateException 세션이 실행 중인 장기기억 작업이 아닐 때
   */
  @Transactional
  public void complete(long learningSessionId) {
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionIdForUpdate(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    freeTalkSession.completeMemoryGeneration();
  }

  /**
   * 장기기억 저장과 완료 상태 전환을 같은 트랜잭션에서 수행한다.
   *
   * @param request 장기기억 생성 문맥
   * @param plans 후보별 저장 계획
   * @return snapshot이 최신이어서 저장과 완료를 수행했으면 STORED, 아니면 STALE
   */
  @Transactional
  public ConversationMemoryWriteService.PersistenceResult persistAndComplete(
      ConversationMemoryGenerationRequest request, List<ConversationMemoryResolutionPlan> plans) {
    ConversationMemoryWriteService.PersistenceResult result =
        memoryWriteService.persistIfSnapshotCurrent(request.userProfileId(), plans);
    if (result == ConversationMemoryWriteService.PersistenceResult.STORED) {
      complete(request.learningSessionId());
    }
    return result;
  }

  /**
   * 실행 중인 장기기억 생성 작업을 조건부 실패 상태로 전환한다.
   *
   * @param learningSessionId 실패 처리할 학습 세션 ID
   * @throws IllegalStateException 장기기억 생성 상태가 완료된 프리톡과 일치하지 않을 때
   */
  @Transactional
  public void fail(long learningSessionId) {
    freeTalkSessionRepository
        .findByLearningSessionIdForUpdate(learningSessionId)
        .filter(session -> session.getMemoryGenerationStatus() == MemoryGenerationStatus.PREPARING)
        .ifPresent(FreeTalkSession::failMemoryGeneration);
  }
}
