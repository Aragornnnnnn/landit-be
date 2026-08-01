// 완료된 프리톡의 목록과 상세 기록을 조회한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.learning.domain.ExpressionLearningSource;
import com.landit.landitbe.feature.learning.domain.UserWritingExpressionCompletion;
import com.landit.landitbe.feature.learning.repository.UserWritingExpressionCompletionRepository;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.ExpressionLearningStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionDetailResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionListResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 완료된 프리톡의 목록과 상세 기록을 조회한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkHistoryQueryService {

  private final LearningSessionRepository learningSessionRepository;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;
  private final FreeTalkSessionExpressionRepository sessionExpressionRepository;
  private final WritingExpressionRepository writingExpressionRepository;
  private final UserWritingExpressionCompletionRepository expressionCompletionRepository;

  /**
   * 완료 프리톡을 최신순 페이지로 조회한다.
   *
   * @param userId 조회할 사용자 ID
   * @param page 0부터 시작하는 페이지 번호
   * @param size 페이지 크기
   * @return 완료된 프리톡 목록
   */
  @Transactional(readOnly = true)
  public FreeTalkSessionListResponse getSessions(long userId, int page, int size) {
    Page<FreeTalkSession> sessions =
        freeTalkSessionRepository.findCompletedByUserProfileId(userId, PageRequest.of(page, size));
    List<FreeTalkSession> freeTalkSessions = sessions.getContent();

    // 페이지에 포함된 세션·표현·완료 이력을 일괄 조회해 반복 쿼리를 피한다.
    Map<Long, LearningSession> learningSessionsById =
        learningSessionsById(
            freeTalkSessions.stream().map(FreeTalkSession::getLearningSessionId).toList());
    Map<Long, List<FreeTalkSessionExpression>> expressionsByFreeTalkSessionId =
        expressionsByFreeTalkSessionId(
            freeTalkSessions.stream().map(FreeTalkSession::getId).toList());
    List<FreeTalkSessionExpression> sessionExpressions =
        expressionsByFreeTalkSessionId.values().stream().flatMap(List::stream).toList();
    Map<Long, WritingExpression> writingExpressionsById =
        writingExpressionsById(sessionExpressions);
    ExpressionCompletionLookup completionLookup =
        expressionCompletionLookup(userId, sessionExpressions);

    // 일괄 조회한 데이터를 세션별 목록 응답으로 조립한다.
    List<FreeTalkSessionListResponse.Item> items =
        freeTalkSessions.stream()
            .map(
                session ->
                    toListItem(
                        session,
                        learningSessionsById,
                        expressionsByFreeTalkSessionId,
                        writingExpressionsById,
                        completionLookup))
            .toList();
    return new FreeTalkSessionListResponse(items, page, size, sessions.hasNext());
  }

  /**
   * 사용자가 소유한 완료 프리톡의 상세 대화와 표현을 조회한다.
   *
   * @param userId 조회할 사용자 ID
   * @param learningSessionId 프리톡 학습 세션 ID
   * @return 지난 프리톡 상세 정보
   * @throws ApiException 세션이 없거나 소유자가 아니거나 아직 완료되지 않았을 때
   */
  @Transactional(readOnly = true)
  public FreeTalkSessionDetailResponse getSession(long userId, long learningSessionId) {
    CompletedSession completedSession = requireCompleted(userId, learningSessionId);
    FreeTalkSession session = completedSession.freeTalkSession();

    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));

    // 추천 표현과 사용자의 완료 이력을 결합해 현재 학습 진행 상태를 계산한다.
    List<FreeTalkSessionExpression> sessionExpressions =
        sessionExpressionRepository.findByFreeTalkSessionIdOrderByDisplayOrderAsc(session.getId());
    ExpressionProgress progress =
        expressionProgress(
            session,
            sessionExpressions,
            writingExpressionsById(sessionExpressions),
            expressionCompletionLookup(userId, sessionExpressions));

    // 대화 메시지는 저장 순서대로 API 응답 형태로 변환한다.
    List<FreeTalkSessionDetailResponse.Message> messages =
        sessionHistoryMessageRepository
            .findBySessionHistoryIdOrderByMessageSequenceAsc(history.getId())
            .stream()
            .map(
                message ->
                    new FreeTalkSessionDetailResponse.Message(
                        message.getId(),
                        message.getTurnNumber(),
                        message.getMessageSequence(),
                        message.getRole().name(),
                        message.getContent(),
                        message.getTranslatedContent(),
                        message.getEmotion(),
                        message.getInnerThought(),
                        message.getInnerThoughtType()))
            .toList();

    return new FreeTalkSessionDetailResponse(
        learningSessionId,
        session.getTitle(),
        completedSession.learningSession().getStartedAt(),
        completedSession.learningSession().getEndedAt(),
        session.getAccumulatedSpeakingDurationMs(),
        messages,
        session.getExpressionGenerationStatus(),
        progress.learningStatus(),
        progress.expressions());
  }

  private FreeTalkSessionListResponse.Item toListItem(
      FreeTalkSession session,
      Map<Long, LearningSession> learningSessionsById,
      Map<Long, List<FreeTalkSessionExpression>> expressionsByFreeTalkSessionId,
      Map<Long, WritingExpression> writingExpressionsById,
      ExpressionCompletionLookup completionLookup) {
    LearningSession learningSession = learningSessionsById.get(session.getLearningSessionId());
    if (learningSession == null) {
      throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
    }
    ExpressionProgress progress =
        expressionProgress(
            session,
            expressionsByFreeTalkSessionId.getOrDefault(session.getId(), List.of()),
            writingExpressionsById,
            completionLookup);
    return new FreeTalkSessionListResponse.Item(
        learningSession.getId(),
        session.getTitle(),
        learningSession.getStartedAt(),
        learningSession.getEndedAt(),
        session.getAccumulatedSpeakingDurationMs(),
        session.getExpressionGenerationStatus(),
        progress.learningStatus(),
        progress.expressionCount(),
        progress.completedExpressionCount());
  }

  private CompletedSession requireCompleted(long userId, long learningSessionId) {
    LearningSession learningSession =
        learningSessionRepository
            .findById(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    if (!Long.valueOf(userId).equals(learningSession.getUserProfileId())) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    if (learningSession.getStatus() != LearningSessionStatus.COMPLETED
        || freeTalkSession.getConversationStatus() != FreeTalkConversationStatus.COMPLETED) {
      throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
    }
    return new CompletedSession(learningSession, freeTalkSession);
  }

  private Map<Long, LearningSession> learningSessionsById(List<Long> learningSessionIds) {
    Map<Long, LearningSession> learningSessionsById = new HashMap<>();
    learningSessionRepository
        .findAllById(learningSessionIds)
        .forEach(session -> learningSessionsById.put(session.getId(), session));
    return learningSessionsById;
  }

  private Map<Long, List<FreeTalkSessionExpression>> expressionsByFreeTalkSessionId(
      List<Long> freeTalkSessionIds) {
    Map<Long, List<FreeTalkSessionExpression>> expressionsByFreeTalkSessionId = new HashMap<>();
    sessionExpressionRepository
        .findByFreeTalkSessionIdInOrderByFreeTalkSessionIdAscDisplayOrderAsc(freeTalkSessionIds)
        .forEach(
            expression ->
                expressionsByFreeTalkSessionId
                    .computeIfAbsent(
                        expression.getFreeTalkSessionId(), ignored -> new ArrayList<>())
                    .add(expression));
    return expressionsByFreeTalkSessionId;
  }

  private Map<Long, WritingExpression> writingExpressionsById(
      List<FreeTalkSessionExpression> sessionExpressions) {
    List<Long> expressionIds =
        sessionExpressions.stream().map(FreeTalkSessionExpression::getWritingExpressionId).toList();
    Map<Long, WritingExpression> expressionsById = new HashMap<>();
    writingExpressionRepository
        .findAllById(expressionIds)
        .forEach(expression -> expressionsById.put(expression.getId(), expression));
    return expressionsById;
  }

  private ExpressionProgress expressionProgress(
      FreeTalkSession session,
      List<FreeTalkSessionExpression> sessionExpressions,
      Map<Long, WritingExpression> writingExpressionsById,
      ExpressionCompletionLookup completionLookup) {
    if (session.getExpressionGenerationStatus() != ExpressionGenerationStatus.READY) {
      return ExpressionProgress.empty();
    }

    // 추천 표현 본문과 출처별 완료 이력을 노출 순서대로 결합한다.
    List<FreeTalkSessionDetailResponse.Expression> expressions =
        sessionExpressions.stream()
            .map(
                sessionExpression ->
                    toExpressionResponse(
                        sessionExpression, writingExpressionsById, completionLookup))
            .toList();

    int completedCount =
        Math.toIntExact(
            expressions.stream()
                .filter(FreeTalkSessionDetailResponse.Expression::completed)
                .count());

    ExpressionLearningStatus learningStatus = learningStatus(completedCount, expressions.size());
    return new ExpressionProgress(learningStatus, expressions.size(), completedCount, expressions);
  }

  private FreeTalkSessionDetailResponse.Expression toExpressionResponse(
      FreeTalkSessionExpression sessionExpression,
      Map<Long, WritingExpression> writingExpressionsById,
      ExpressionCompletionLookup completionLookup) {
    long expressionId = sessionExpression.getWritingExpressionId();
    WritingExpression expression = writingExpressionsById.get(expressionId);
    if (expression == null) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    return new FreeTalkSessionDetailResponse.Expression(
        expressionId,
        sessionExpression.getDisplayOrder(),
        expression.getTargetExpressionText(),
        expression.getBaseExpressionMeaningText(),
        completionLookup.freeTalkCompletedExpressionIds().contains(expressionId),
        completionLookup.lastCompletedAtByExpressionId().get(expressionId));
  }

  private ExpressionLearningStatus learningStatus(int completedCount, int expressionCount) {
    if (completedCount == 0) {
      return ExpressionLearningStatus.NOT_STARTED;
    }
    if (completedCount == expressionCount) {
      return ExpressionLearningStatus.COMPLETED;
    }
    return ExpressionLearningStatus.IN_PROGRESS;
  }

  private ExpressionCompletionLookup expressionCompletionLookup(
      long userId, List<FreeTalkSessionExpression> sessionExpressions) {
    List<Long> expressionIds =
        sessionExpressions.stream().map(FreeTalkSessionExpression::getWritingExpressionId).toList();
    if (expressionIds.isEmpty()) {
      return ExpressionCompletionLookup.empty();
    }
    List<UserWritingExpressionCompletion> completions =
        expressionCompletionRepository.findAllByUserProfileIdAndWritingExpressionIdIn(
            userId, expressionIds);
    Set<Long> freeTalkCompletedExpressionIds = new HashSet<>();
    Map<Long, LocalDateTime> lastCompletedAtByExpressionId = new HashMap<>();
    completions.forEach(
        completion -> {
          if (completion.getLearningSource() == ExpressionLearningSource.FREE_TALK) {
            freeTalkCompletedExpressionIds.add(completion.getWritingExpressionId());
          }
          lastCompletedAtByExpressionId.merge(
              completion.getWritingExpressionId(),
              completion.getLastCompletedAt(),
              (first, second) -> first.isAfter(second) ? first : second);
        });
    return new ExpressionCompletionLookup(
        freeTalkCompletedExpressionIds, lastCompletedAtByExpressionId);
  }

  private record CompletedSession(
      LearningSession learningSession, FreeTalkSession freeTalkSession) {}

  private record ExpressionCompletionLookup(
      Set<Long> freeTalkCompletedExpressionIds,
      Map<Long, LocalDateTime> lastCompletedAtByExpressionId) {
    static ExpressionCompletionLookup empty() {
      return new ExpressionCompletionLookup(Set.of(), Map.of());
    }
  }

  private record ExpressionProgress(
      ExpressionLearningStatus learningStatus,
      int expressionCount,
      int completedExpressionCount,
      List<FreeTalkSessionDetailResponse.Expression> expressions) {
    static ExpressionProgress empty() {
      return new ExpressionProgress(null, 0, 0, List.of());
    }
  }
}
