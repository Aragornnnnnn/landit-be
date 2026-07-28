// 프리톡 맞춤 표현 생성 재시도 결과를 반환한다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.ExpressionGenerationStatus;

/** 프리톡 맞춤 표현 생성 재시도 결과를 반환한다. */
public record FreeTalkExpressionRetryResponse(
    Long sessionId, ExpressionGenerationStatus expressionGenerationStatus) {}
