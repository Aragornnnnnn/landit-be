// 업무 간에 전달할 ExpressionProgress 값을 정의한다.

package com.landit.landitbe.feature.learning.expression.progress.dto;

/**
 * 시나리오에 속한 활성 표현 수와 사용자의 완료 표현 수를 담는다.
 *
 * @param expressionCount 활성 표현 수
 * @param completedExpressionCount 완료한 활성 표현 수
 */
public record ExpressionProgress(int expressionCount, int completedExpressionCount) {}
