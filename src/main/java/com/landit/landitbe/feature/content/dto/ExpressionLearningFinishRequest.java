// 표현 학습 완료 요청의 프리톡 진입 맥락을 전달한다.

package com.landit.landitbe.feature.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 표현 학습 완료 요청의 프리톡 진입 맥락을 전달한다. */
public record ExpressionLearningFinishRequest(
    @Schema(description = "프리톡에서 학습했을 때의 프리톡 세션 ID", example = "123") Long freeTalkSessionId) {}
