// 완료된 프리톡의 맞춤 표현 학습 조회와 완료 처리를 담당한다.

package com.landit.landitbe.feature.session.service;

import com.landit.landitbe.feature.content.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.content.dto.ExpressionPracticeResponse;
import com.landit.landitbe.feature.content.service.ExpressionQueryService;
import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkConversationStatus;
import com.landit.landitbe.feature.session.domain.FreeTalkExpression;
import com.landit.landitbe.feature.session.domain.FreeTalkExpressionSourceType;
import com.landit.landitbe.feature.session.domain.FreeTalkSession;
import com.landit.landitbe.feature.session.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.session.domain.LearningSession;
import com.landit.landitbe.feature.session.domain.LearningSessionStatus;
import com.landit.landitbe.feature.session.domain.UserFreeTalkExpressionCompletion;
import com.landit.landitbe.feature.session.dto.FreeTalkExpressionLearningResponse;
import com.landit.landitbe.feature.session.repository.FreeTalkExpressionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionRepository;
import com.landit.landitbe.feature.session.repository.LearningSessionRepository;
import com.landit.landitbe.feature.session.repository.UserFreeTalkExpressionCompletionRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 완료된 프리톡의 맞춤 표현 학습 조회와 완료 처리를 담당한다. */
@RequiredArgsConstructor
@Service
public class FreeTalkExpressionLearningService {

  private final FreeTalkSessionExpressionRepository sessionExpressionRepository;
  private final FreeTalkSessionRepository freeTalkSessionRepository;
  private final LearningSessionRepository learningSessionRepository;
  private final FreeTalkExpressionRepository expressionRepository;
  private final UserFreeTalkExpressionCompletionRepository completionRepository;
  private final ExpressionQueryService expressionQueryService;

  /** 맞춤 표현의 학습 시작 콘텐츠와 사용자 완료 여부를 조회한다. */
  @Transactional(readOnly = true)
  public FreeTalkExpressionLearningResponse getLearningContent(
      long userId, long sessionExpressionId) {
    FreeTalkExpression expression = requireAccessible(userId, sessionExpressionId);
    boolean completed =
        completionRepository.existsByUserProfileIdAndFreeTalkExpressionId(
            userId, expression.getId());

    if (expression.getSourceType() == FreeTalkExpressionSourceType.EXISTING) {
      ExpressionLearningResponse response =
          expressionQueryService.getExpressionForLearning(expression.getWritingExpressionId());
      return FreeTalkExpressionLearningResponse.fromExisting(
          sessionExpressionId, response, completed);
    }
    return FreeTalkExpressionLearningResponse.fromNew(sessionExpressionId, expression, completed);
  }

  /** 맞춤 표현의 추가 예문과 작문 문제를 조회한다. */
  @Transactional(readOnly = true)
  public ExpressionPracticeResponse getPractice(long userId, long sessionExpressionId) {
    FreeTalkExpression expression = requireAccessible(userId, sessionExpressionId);
    if (expression.getSourceType() == FreeTalkExpressionSourceType.EXISTING) {
      return expressionQueryService.getExtraPracticeExamples(expression.getWritingExpressionId());
    }
    return expressionQueryService.buildPracticeResponse(
        expression.getId(),
        expression.getTargetExpressionText(),
        expression.getBaseExpressionMeaningText(),
        expression.getUsageDescription(),
        expression.getPracticeExamplesPayload());
  }

  /** 맞춤 표현 학습을 완료한다. 이미 완료한 표현이면 기존 기록을 유지한다. */
  @Transactional
  public void complete(long userId, long sessionExpressionId) {
    long expressionId = requireAccessible(userId, sessionExpressionId).getId();
    if (completionRepository.existsByUserProfileIdAndFreeTalkExpressionId(userId, expressionId)) {
      return;
    }
    completionRepository.save(
        UserFreeTalkExpressionCompletion.complete(userId, expressionId, LocalDateTime.now()));
  }

  private FreeTalkExpression requireAccessible(long userId, long sessionExpressionId) {
    FreeTalkSessionExpression sessionExpression =
        sessionExpressionRepository
            .findById(sessionExpressionId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    FreeTalkSession freeTalkSession =
        freeTalkSessionRepository
            .findById(sessionExpression.getFreeTalkSessionId())
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    LearningSession learningSession =
        learningSessionRepository
            .findById(freeTalkSession.getLearningSessionId())
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
    if (!Long.valueOf(userId).equals(learningSession.getUserProfileId())) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    if (learningSession.getStatus() != LearningSessionStatus.COMPLETED
        || freeTalkSession.getConversationStatus() != FreeTalkConversationStatus.COMPLETED
        || freeTalkSession.getExpressionGenerationStatus() != ExpressionGenerationStatus.READY) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
    }
    return expressionRepository
        .findById(sessionExpression.getFreeTalkExpressionId())
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }
}
