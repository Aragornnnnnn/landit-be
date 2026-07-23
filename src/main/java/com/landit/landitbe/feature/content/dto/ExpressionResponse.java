// 시나리오별 Writing 표현 조회 응답 항목을 표현한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.feature.content.domain.WritingExpression;
import io.swagger.v3.oas.annotations.media.Schema;

/** 시나리오별 Writing 표현 조회 응답 항목을 표현한다. */
@Schema(description = "시나리오별 Writing 표현 조회 응답 항목")
public record ExpressionResponse(
    @Schema(description = "표현 고유 ID", example = "101") Long expressionId,
    @Schema(description = "시나리오 안 표현 학습 순서 및 해금 순서", example = "1") int displayOrder,
    @Schema(description = "타겟 표현", example = "There is nothing like") String targetExpressionText,
    @Schema(description = "타겟 표현 뜻(한글 해석)", example = "~만 한 게 없다") String baseExpressionMeaningText,
    @Schema(description = "학습 완료 여부", example = "true") boolean completed,
    @Schema(description = "잠김 여부(해금 전 상태)", example = "false") boolean locked) {

  /** 표현 엔티티와 사용자 진행 상태를 목록 응답 항목으로 변환한다. */
  public static ExpressionResponse from(
      WritingExpression expression, boolean completed, boolean locked) {
    return new ExpressionResponse(
        expression.getId(),
        expression.getDisplayOrder(),
        expression.getTargetExpressionText(),
        expression.getBaseExpressionMeaningText(),
        completed,
        locked);
  }
}
