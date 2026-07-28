// 완료된 프리톡의 목록과 상세 기록을 조회한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.ExpressionLearningStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkExpression;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.domain.SessionHistory;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionDetailResponse;
import com.landit.landitbe.feature.session.dto.FreeTalkSessionListResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkExpressionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryMessageRepository;
import com.landit.landitbe.feature.session.repository.SessionHistoryRepository;
import com.landit.landitbe.feature.session.repository.UserFreeTalkExpressionCompletionRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
  private final FreeTalkExpressionRepository expressionRepository;
  private final UserFreeTalkExpressionCompletionRepository completionRepository;

  /** 완료 프리톡을 최신순 페이지로 조회한다. */
  @Transactional(readOnly = true)
  public FreeTalkSessionListResponse getSessions(long userId, int page, int size) {
    Page<FreeTalkSession> sessions =
        freeTalkSessionRepository.findCompletedByUserProfileId(userId, PageRequest.of(page, size));
    List<FreeTalkSessionListResponse.Item> items =
        sessions.getContent().stream().map(session -> toListItem(userId, session)).toList();
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
    ExpressionProgress progress = expressionProgress(userId, session.getId());
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

  private FreeTalkSessionListResponse.Item toListItem(long userId, FreeTalkSession session) {
    LearningSession learningSession =
        learningSessionRepository.findById(session.getLearningSessionId()).orElseThrow();
    ExpressionProgress progress = expressionProgress(userId, session.getId());
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

  private ExpressionProgress expressionProgress(long userId, long freeTalkSessionId) {
    ExpressionGenerationStatus generationStatus =
        freeTalkSessionRepository
            .findById(freeTalkSessionId)
            .orElseThrow()
            .getExpressionGenerationStatus();
    if (generationStatus != ExpressionGenerationStatus.READY) {
      return ExpressionProgress.empty();
    }
    List<FreeTalkSessionExpression> sessionExpressions =
        sessionExpressionRepository.findByFreeTalkSessionIdOrderByDisplayOrderAsc(
            freeTalkSessionId);
    Map<Long, FreeTalkExpression> expressionsById =
        expressionRepository
            .findAllById(
                sessionExpressions.stream()
                    .map(FreeTalkSessionExpression::getFreeTalkExpressionId)
                    .toList())
            .stream()
            .collect(
                java.util.stream.Collectors.toMap(FreeTalkExpression::getId, Function.identity()));
    Set<Long> completedExpressionIds =
        new HashSet<>(
            completionRepository
                .findByUserProfileIdAndFreeTalkExpressionIdIn(userId, expressionsById.keySet())
                .stream()
                .map(completion -> completion.getFreeTalkExpressionId())
                .toList());
    List<FreeTalkSessionDetailResponse.Expression> expressions =
        sessionExpressions.stream()
            .map(
                sessionExpression -> {
                  FreeTalkExpression expression =
                      expressionsById.get(sessionExpression.getFreeTalkExpressionId());
                  return new FreeTalkSessionDetailResponse.Expression(
                      sessionExpression.getId(),
                      sessionExpression.getDisplayOrder(),
                      expression.getTargetExpressionText(),
                      expression.getBaseExpressionMeaningText(),
                      new FreeTalkSessionDetailResponse.ContextualExample(
                          sessionExpression.getPersonalizedExampleText(),
                          sessionExpression.getPersonalizedExampleTranslation()),
                      completedExpressionIds.contains(expression.getId()));
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

  private record CompletedSession(
      LearningSession learningSession, FreeTalkSession freeTalkSession) {}

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
