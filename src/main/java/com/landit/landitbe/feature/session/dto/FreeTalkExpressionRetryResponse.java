// 프리톡 맞춤 표현 생성 재시도 결과를 반환한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;

/**
 * 프리톡 맞춤 표현 생성 재시도 결과를 반환한다.
 *
 * @param sessionId 재시도를 요청한 학습 세션 ID
 * @param expressionGenerationStatus 재시도 요청 뒤의 표현 생성 상태
 */
public record FreeTalkExpressionRetryResponse(
    Long sessionId, ExpressionGenerationStatus expressionGenerationStatus) {}
