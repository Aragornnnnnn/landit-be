// 완료된 프리톡의 목록과 상세 기록을 조회한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.domain.WritingExpression;
import com.landit.landitbe.feature.content.repository.WritingExpressionRepository;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.ExpressionLearningStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkExpressionSourceType;
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

  private final LearningSessionRepository learningSessionRepository;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final SessionHistoryRepository sessionHistoryRepository;
  private final SessionHistoryMessageRepository sessionHistoryMessageRepository;
  private final FreeTalkSessionExpressionRepository sessionExpressionRepository;
  private final WritingExpressionRepository writingExpressionRepository;

  /** 완료 프리톡을 최신순 페이지로 조회한다. */
  @Transactional(readOnly = true)
  public FreeTalkSessionListResponse getSessions(long userId, int page, int size) {
    Page<FreeTalkSession> sessions =
        freeTalkSessionRepository.findCompletedByUserProfileId(userId, PageRequest.of(page, size));
    List<FreeTalkSession> freeTalkSessions = sessions.getContent();
    Map<Long, LearningSession> learningSessionsById =
        learningSessionsById(
            freeTalkSessions.stream().map(FreeTalkSession::getLearningSessionId).toList());
    Map<Long, List<FreeTalkSessionExpression>> expressionsByFreeTalkSessionId =
        expressionsByFreeTalkSessionId(
            freeTalkSessions.stream().map(FreeTalkSession::getId).toList());
    Map<Long, WritingExpression> existingExpressionsById =
        existingExpressionsById(
            expressionsByFreeTalkSessionId.values().stream().flatMap(List::stream).toList());
    List<FreeTalkSessionListResponse.Item> items =
        freeTalkSessions.stream()
            .map(
                session ->
                    toListItem(
                        session,
                        learningSessionsById,
                        expressionsByFreeTalkSessionId,
                        existingExpressionsById))
            .toList();
    return new FreeTalkSessionListResponse(items, page, size, sessions.hasNext());
  }

  /** 사용자가 소유한 완료 프리톡의 상세 대화와 표현을 조회한다. */
  @Transactional(readOnly = true)
  public FreeTalkSessionDetailResponse getSession(long userId, long learningSessionId) {
    CompletedSession completedSession = requireCompleted(userId, learningSessionId);
    FreeTalkSession session = completedSession.freeTalkSession();
    SessionHistory history =
        sessionHistoryRepository
            .findByLearningSessionId(learningSessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.SESSION_NOT_FOUND));
    List<FreeTalkSessionExpression> sessionExpressions =
        sessionExpressionRepository.findByFreeTalkSessionIdOrderByDisplayOrderAsc(session.getId());
    ExpressionProgress progress =
        expressionProgress(
            session, sessionExpressions, existingExpressionsById(sessionExpressions));
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
      Map<Long, WritingExpression> existingExpressionsById) {
    LearningSession learningSession = learningSessionsById.get(session.getLearningSessionId());
    if (learningSession == null) {
      throw new ApiException(ErrorCode.SESSION_NOT_FOUND);
    }
    ExpressionProgress progress =
        expressionProgress(
            session,
            expressionsByFreeTalkSessionId.getOrDefault(session.getId(), List.of()),
            existingExpressionsById);
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
                        expression.getFreeTalkSessionId(), ignored -> new java.util.ArrayList<>())
                    .add(expression));
    return expressionsByFreeTalkSessionId;
  }

  private Map<Long, WritingExpression> existingExpressionsById(
      List<FreeTalkSessionExpression> sessionExpressions) {
    List<Long> expressionIds =
        sessionExpressions.stream()
            .filter(
                expression -> expression.getSourceType() == FreeTalkExpressionSourceType.EXISTING)
            .map(FreeTalkSessionExpression::getWritingExpressionId)
            .toList();
    Map<Long, WritingExpression> expressionsById = new HashMap<>();
    writingExpressionRepository
        .findAllById(expressionIds)
        .forEach(expression -> expressionsById.put(expression.getId(), expression));
    return expressionsById;
  }

  private ExpressionProgress expressionProgress(
      FreeTalkSession session,
      List<FreeTalkSessionExpression> sessionExpressions,
      Map<Long, WritingExpression> existingExpressionsById) {
    if (session.getExpressionGenerationStatus() != ExpressionGenerationStatus.READY) {
      return ExpressionProgress.empty();
    }
    List<FreeTalkSessionDetailResponse.Expression> expressions =
        sessionExpressions.stream()
            .map(
                sessionExpression -> {
                  ExpressionSummary expression =
                      expressionSummary(sessionExpression, existingExpressionsById);
                  return new FreeTalkSessionDetailResponse.Expression(
                      sessionExpression.getId(),
                      sessionExpression.getDisplayOrder(),
                      expression.targetExpressionText(),
                      expression.baseExpressionMeaningText(),
                      new FreeTalkSessionDetailResponse.ContextualExample(
                          sessionExpression.getPersonalizedExampleText(),
                          sessionExpression.getPersonalizedExampleTranslation()),
                      sessionExpression.isCompleted());
                })
            .toList();
    int completedCount =
        Math.toIntExact(
            expressions.stream()
                .filter(FreeTalkSessionDetailResponse.Expression::completed)
                .count());
    ExpressionLearningStatus learningStatus =
        completedCount == 0
            ? ExpressionLearningStatus.NOT_STARTED
            : completedCount == expressions.size()
                ? ExpressionLearningStatus.COMPLETED
                : ExpressionLearningStatus.IN_PROGRESS;
    return new ExpressionProgress(learningStatus, expressions.size(), completedCount, expressions);
  }

  private ExpressionSummary expressionSummary(
      FreeTalkSessionExpression sessionExpression,
      Map<Long, WritingExpression> existingExpressionsById) {
    if (sessionExpression.getSourceType() == FreeTalkExpressionSourceType.EXISTING) {
      WritingExpression expression =
          existingExpressionsById.get(sessionExpression.getWritingExpressionId());
      if (expression == null) {
        throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
      }
      return new ExpressionSummary(
          expression.getTargetExpressionText(), expression.getBaseExpressionMeaningText());
    }
    return new ExpressionSummary(
        sessionExpression.getGeneratedContentPayload().path("targetExpressionText").asText(),
        sessionExpression.getGeneratedContentPayload().path("baseExpressionMeaningText").asText());
  }

  private record CompletedSession(
      LearningSession learningSession, FreeTalkSession freeTalkSession) {}

  private record ExpressionSummary(String targetExpressionText, String baseExpressionMeaningText) {}

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
