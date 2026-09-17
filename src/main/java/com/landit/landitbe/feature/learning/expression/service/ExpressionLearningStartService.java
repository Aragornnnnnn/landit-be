// 표현 학습 시작의 콘텐츠 검증·권한 발급·응답 조립을 조율한다.

package com.landit.landitbe.feature.learning.expression.service;

import com.landit.landitbe.feature.content.expression.dto.ExpressionLearningResponse;
import com.landit.landitbe.feature.content.expression.pronunciation.dto.ExpressionAudio;
import com.landit.landitbe.feature.content.expression.pronunciation.service.ExpressionPronunciationQueryService;
import com.landit.landitbe.feature.content.expression.service.ExpressionQueryService;
import com.landit.landitbe.feature.learning.progress.service.ExpressionCompletionService;
import com.landit.landitbe.feature.subscription.service.LearningAccessGrantService;
import com.landit.landitbe.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 표현 학습 시작의 콘텐츠 검증·권한 발급·응답 조립을 조율한다. */
@Service
@RequiredArgsConstructor
public class ExpressionLearningStartService {

  private final ExpressionQueryService expressionQueryService;
  private final LearningAccessGrantService accessGrants;
  private final ExpressionPronunciationQueryService pronunciationQueryService;
  private final ExpressionCompletionService expressionCompletionService;

  /**
   * 접근 가능한 표현의 학습 시도를 시작하거나 재개한다.
   *
   * @param userId 학습 사용자
   * @param expressionId 표현 ID
   * @return 콘텐츠와 시작 권한·완료 상태를 합친 응답
   * @throws ApiException 콘텐츠 또는 학습 권한이 유효하지 않을 때
   */
  @Transactional
  public ExpressionLearningResponse startLearning(Long userId, Long expressionId) {
    ExpressionLearningResponse content =
        expressionQueryService.prepareLearningContent(userId, expressionId);
    var attempt = accessGrants.startExpression(userId, expressionId);
    ExpressionAudio audio = pronunciationQueryService.findAudio(userId, expressionId);
    return content
        .withLearningState(
            audio, expressionCompletionService.hasCompletedExpression(userId, expressionId))
        .withAttempt(attempt.id(), attempt.expiresAt());
  }
}
