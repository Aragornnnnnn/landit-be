// 표현 학습 완료 요청의 프리톡 진입 맥락을 전달한다.

package com.landit.landitbe.feature.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 표현 학습 완료 요청의 프리톡 진입 맥락을 전달한다.
 *
 * @param freeTalkSessionId 프리톡 학습 세션 ID. 시나리오 학습이면 null
 */
public record ExpressionLearningFinishRequest(
    @Schema(description = "프리톡 학습 세션 ID", example = "123") Long freeTalkSessionId) {}
