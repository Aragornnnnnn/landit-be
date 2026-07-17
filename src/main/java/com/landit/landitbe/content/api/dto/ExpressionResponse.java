// 시나리오별 Writing 표현 조회 응답 항목을 표현한다.

package com.landit.landitbe.content.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 시나리오별 Writing 표현 조회 응답 항목을 표현한다. */
@Schema(description = "시나리오별 Writing 표현 조회 응답 항목")
public record ExpressionResponse(
    @Schema(description = "표현 고유 ID", example = "101") Long expressionId,
    @Schema(description = "시나리오 안 표현 학습 순서 및 해금 순서", example = "1") int displayOrder,
    @Schema(description = "타겟 표현", example = "There is nothing like") String targetExpressionText,
    @Schema(description = "타겟 표현 뜻(한글 해석)", example = "~만 한 게 없다") String baseExpressionMeaningText,
    @Schema(description = "학습 완료 여부", example = "true") boolean completed,
    @Schema(description = "잠김 여부(해금 전 상태)", example = "false") boolean locked) {}
