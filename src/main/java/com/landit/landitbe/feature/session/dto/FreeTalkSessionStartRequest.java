// 프리톡 세션 시작 방식과 선택 주제를 받는다.

package com.landit.landitbe.feature.session.dto;

import com.landit.landitbe.feature.session.domain.FreeTalkStartMode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 프리톡 세션 시작 방식과 선택 주제를 받는다.
 *
 * @param startMode 첫 발화 주체
 * @param topicId AI 선시작에서 선택한 추천 주제 ID
 */
@Schema(description = "프리톡 세션 시작 요청")
public record FreeTalkSessionStartRequest(
    @Schema(description = "첫 발화 주체", example = "AI_FIRST") FreeTalkStartMode startMode,
    @Schema(description = "AI 선시작에서 선택한 활성 추천 주제 ID", example = "2") Long topicId) {}
