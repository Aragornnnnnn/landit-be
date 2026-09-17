// 완료된 프리톡의 목록과 상세 기록을 조회한다.

package com.landit.landitbe.feature.session.freetalk.history.service;

import com.landit.landitbe.feature.content.expression.dto.ExpressionText;
import com.landit.landitbe.feature.content.expression.service.ExpressionContentService;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.dto.LearningSessionSnapshot;
import com.landit.landitbe.feature.session.dto.SessionHistorySnapshot;
import com.landit.landitbe.feature.session.exception.SessionErrorCode;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.freetalk.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.freetalk.expression.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.freetalk.expression.domain.ExpressionLearningStatus;
import com.landit.landitbe.feature.session.freetalk.expression.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.session.freetalk.expression.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.session.freetalk.history.dto.FreeTalkSessionDetailResponse;
import com.landit.landitbe.feature.session.freetalk.history.dto.FreeTalkSessionListResponse;
import com.landit.landitbe.feature.session.freetalk.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.history.service.ConversationMessageService;
import com.landit.landitbe.feature.session.history.service.SessionHistoryService;
import com.landit.landitbe.feature.session.service.LearningSessionService;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 완료된 프리톡의 목록과 상세 기록을 조회한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkHistoryQueryService {

  private final LearningSessionService learningSessionService;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final SessionHistoryService sessionHistoryService;
  private final ConversationMessageService conversationMessageService;
  private final FreeTalkSessionExpressionRepository sessionExpressionRepository;
  private final ExpressionContentService expressionContentService;

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

    // 페이지에 포함된 세션·표현·완료 상태를 일괄 조회해 반복 쿼리를 피한다.
    Map<Long, LearningSessionSnapshot> learningSessionsById =
        learningSessionsById(
            freeTalkSessions.stream().map(FreeTalkSession::getLearningSessionId).toList());
    Map<Long, List<FreeTalkSessionExpression>> expressionsByFreeTalkSessionId =
        expressionsByFreeTalkSessionId(
            freeTalkSessions.stream().map(FreeTalkSession::getId).toList());
    List<FreeTalkSessionExpression> sessionExpressions =
        expressionsByFreeTalkSessionId.values().stream().flatMap(List::stream).toList();
    Map<Long, ExpressionText> writingExpressionsById = writingExpressionsById(sessionExpressions);
    // 일괄 조회한 데이터를 세션별 목록 응답으로 조립한다.
    List<FreeTalkSessionListResponse.Item> items =
        freeTalkSessions.stream()
            .map(
                session ->
                    toListItem(
                        session,
                        learningSessionsById,
                        expressionsByFreeTalkSessionId,
                        writingExpressionsById))
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

    SessionHistorySnapshot history =
        sessionHistoryService
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));

    // 현재 세션의 추천 표현과 완료 상태를 결합해 학습 진행 상태를 계산한다.
    List<FreeTalkSessionExpression> sessionExpressions =
        sessionExpressionRepository.findByFreeTalkSessionIdOrderByDisplayOrderAsc(session.getId());
    Map<Long, LocalDateTime> lastRecommendedAtByExpressionId =
        previousRecommendationTimes(userId, sessionExpressions);
    ExpressionProgress progress =
        expressionProgress(
            session,
            sessionExpressions,
            writingExpressionsById(sessionExpressions),
            lastRecommendedAtByExpressionId);

    // 대화 메시지는 저장 순서대로 API 응답 형태로 변환한다.
    List<FreeTalkSessionDetailResponse.Message> messages =
        conversationMessageService.findAll(history.getId()).stream()
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
        session.getCharacterId(),
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
      Map<Long, LearningSessionSnapshot> learningSessionsById,
      Map<Long, List<FreeTalkSessionExpression>> expressionsByFreeTalkSessionId,
      Map<Long, ExpressionText> writingExpressionsById) {
    LearningSessionSnapshot learningSession =
        learningSessionsById.get(session.getLearningSessionId());
    if (learningSession == null) {
      throw new ApiException(SessionErrorCode.SESSION_NOT_FOUND);
    }
    ExpressionProgress progress =
        expressionProgress(
            session,
            expressionsByFreeTalkSessionId.getOrDefault(session.getId(), List.of()),
            writingExpressionsById,
            Map.of());
    return new FreeTalkSessionListResponse.Item(
        learningSession.getId(),
        session.getTitle(),
        session.getCharacterId(),
        learningSession.getStartedAt(),
        learningSession.getEndedAt(),
        session.getAccumulatedSpeakingDurationMs(),
        session.getExpressionGenerationStatus(),
        progress.learningStatus(),
        progress.expressionCount(),
        progress.completedExpressionCount());
  }

  private CompletedSession requireCompleted(long userId, long learningSessionId) {
    LearningSessionSnapshot learningSession =
        learningSessionService
            .findSession(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
    if (!Long.valueOf(userId).equals(learningSession.getUserProfileId())) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(SessionErrorCode.SESSION_NOT_FOUND));
    if (learningSession.getStatus() != LearningSessionStatus.COMPLETED
        || freeTalkSession.getConversationStatus() != FreeTalkConversationStatus.COMPLETED) {
      throw new ApiException(SessionErrorCode.SESSION_NOT_FOUND);
    }
    return new CompletedSession(learningSession, freeTalkSession);
  }

  // 학습 세션 목록을 ID 기준 조회 맵으로 변환한다.
  private Map<Long, LearningSessionSnapshot> learningSessionsById(List<Long> learningSessionIds) {
    Map<Long, LearningSessionSnapshot> learningSessionsById = new HashMap<>();
    learningSessionService
        .findSessions(learningSessionIds)
        .forEach(session -> learningSessionsById.put(session.getId(), session));
    return learningSessionsById;
  }

  // 추천 표현을 프리톡 세션 ID별로 그룹화한다.
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

  // 세션 추천 표현에 연결된 원어민 표현을 ID 기준으로 조회한다.
  private Map<Long, ExpressionText> writingExpressionsById(
      List<FreeTalkSessionExpression> sessionExpressions) {
    List<Long> expressionIds =
        sessionExpressions.stream().map(FreeTalkSessionExpression::getWritingExpressionId).toList();
    Map<Long, ExpressionText> expressionsById = new HashMap<>();
    expressionContentService
        .findExpressionTexts(expressionIds)
        .forEach(expression -> expressionsById.put(expression.id(), expression));
    return expressionsById;
  }

  // 추천 표현과 완료 이력을 결합해 세션의 학습 진행 상태를 계산한다.
  private ExpressionProgress expressionProgress(
      FreeTalkSession session,
      List<FreeTalkSessionExpression> sessionExpressions,
      Map<Long, ExpressionText> writingExpressionsById,
      Map<Long, LocalDateTime> lastRecommendedAtByExpressionId) {
    if (session.getExpressionGenerationStatus() != ExpressionGenerationStatus.READY) {
      return ExpressionProgress.empty();
    }

    // 추천 표현 본문과 현재 세션 완료 상태를 노출 순서대로 결합한다.
    List<FreeTalkSessionDetailResponse.Expression> expressions =
        sessionExpressions.stream()
            .map(
                sessionExpression ->
                    toExpressionResponse(
                        sessionExpression, writingExpressionsById, lastRecommendedAtByExpressionId))
            .toList();

    int completedCount =
        Math.toIntExact(
            expressions.stream()
                .filter(FreeTalkSessionDetailResponse.Expression::completed)
                .count());

    ExpressionLearningStatus learningStatus = learningStatus(completedCount, expressions.size());
    return new ExpressionProgress(learningStatus, expressions.size(), completedCount, expressions);
  }

  // 세션 추천 표현을 완료 이력이 포함된 상세 응답으로 변환한다.
  private FreeTalkSessionDetailResponse.Expression toExpressionResponse(
      FreeTalkSessionExpression sessionExpression,
      Map<Long, ExpressionText> writingExpressionsById,
      Map<Long, LocalDateTime> lastRecommendedAtByExpressionId) {
    long expressionId = sessionExpression.getWritingExpressionId();
    ExpressionText expression = writingExpressionsById.get(expressionId);
    if (expression == null) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    return new FreeTalkSessionDetailResponse.Expression(
        expressionId,
        sessionExpression.getDisplayOrder(),
        expression.targetExpressionText(),
        expression.baseExpressionMeaningText(),
        sessionExpression.getCompletedAt() != null,
        lastRecommendedAtByExpressionId.get(expressionId));
  }

  // 완료 개수와 전체 표현 개수로 학습 상태를 결정한다.
  private ExpressionLearningStatus learningStatus(int completedCount, int expressionCount) {
    if (completedCount == 0) {
      return ExpressionLearningStatus.NOT_STARTED;
    }
    if (completedCount == expressionCount) {
      return ExpressionLearningStatus.COMPLETED;
    }
    return ExpressionLearningStatus.IN_PROGRESS;
  }

  // 현재 세션보다 이전에 추천한 같은 표현의 마지막 시각을 조회한다.
  private Map<Long, LocalDateTime> previousRecommendationTimes(
      long userId, List<FreeTalkSessionExpression> currentExpressions) {
    List<Long> expressionIds =
        currentExpressions.stream()
            .map(FreeTalkSessionExpression::getWritingExpressionId)
            .distinct()
            .toList();
    if (expressionIds.isEmpty()) {
      return Map.of();
    }
    List<FreeTalkSessionExpression> previousExpressions =
        sessionExpressionRepository.findAllByUserProfileIdAndWritingExpressionIdIn(
            userId, expressionIds);
    Map<Long, LocalDateTime> lastRecommendedAtByExpressionId = new HashMap<>();
    currentExpressions.forEach(
        currentExpression -> {
          LocalDateTime currentRecommendedAt = currentExpression.getCreatedAt();
          if (currentRecommendedAt == null) {
            return;
          }
          previousExpressions.stream()
              .filter(
                  previousExpression ->
                      previousExpression
                              .getWritingExpressionId()
                              .equals(currentExpression.getWritingExpressionId())
                          && previousExpression.getCreatedAt() != null
                          && previousExpression.getCreatedAt().isBefore(currentRecommendedAt))
              .map(FreeTalkSessionExpression::getCreatedAt)
              .max(LocalDateTime::compareTo)
              .ifPresent(
                  lastRecommendedAt ->
                      lastRecommendedAtByExpressionId.put(
                          currentExpression.getWritingExpressionId(), lastRecommendedAt));
        });
    return lastRecommendedAtByExpressionId;
  }

  private record CompletedSession(
      LearningSessionSnapshot learningSession, FreeTalkSession freeTalkSession) {}

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
