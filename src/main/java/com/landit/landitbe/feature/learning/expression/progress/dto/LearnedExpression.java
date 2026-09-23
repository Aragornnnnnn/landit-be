// 사용자가 학습을 마친 표현 하나와 그 출처를 전달한다.

package com.landit.landitbe.feature.learning.expression.progress.dto;

import com.landit.landitbe.feature.learning.expression.progress.domain.ExpressionLearningSource;
import java.time.LocalDateTime;

/**
 * 사용자가 학습을 마친 표현이다.
 *
 * @param expressionId 표현 ID. 예: 812
 * @param learningSource 배운 곳. 예: SCENARIO
 * @param scenarioId 배운 시나리오 ID. 스몰톡에서 배웠으면 null. 예: 41
 * @param completedAt 처음 학습을 마친 시각. 예: 2026-09-10T21:04:00
 * @param lastCompletedAt 가장 최근에 학습을 마친 시각. 예: 2026-09-18T08:30:00
 */
public record LearnedExpression(
    long expressionId,
    ExpressionLearningSource learningSource,
    Long scenarioId,
    LocalDateTime completedAt,
    LocalDateTime lastCompletedAt) {}
