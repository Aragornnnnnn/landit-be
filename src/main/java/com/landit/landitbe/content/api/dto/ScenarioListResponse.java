// 시나리오 목록 조회 API의 응답 구조를 정의한다.
package com.landit.landitbe.content.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "시나리오 전체 조회 응답")
public record ScenarioListResponse(
        @Schema(description = "카테고리별 시나리오 목록")
        List<CategoryResponse> categories
) {

    @Schema(description = "시나리오 카테고리 응답")
    public record CategoryResponse(
            @Schema(description = "카테고리 ID")
            Long categoryId,
            @Schema(description = "카테고리 이름")
            String categoryName,
            @Schema(description = "카테고리 노출 순서")
            int displayOrder,
            @Schema(description = "카테고리 잠금 여부")
            boolean categoryLocked,
            @Schema(description = "카테고리 잠금 사유")
            String categoryLockReason,
            @Schema(description = "카테고리에 속한 시나리오 목록")
            List<ScenarioResponse> scenarios
    ) {
    }

    @Schema(description = "시나리오 응답")
    public record ScenarioResponse(
            @Schema(description = "시나리오 ID")
            Long scenarioId,
            @Schema(description = "완료한 시나리오의 별점")
            BigDecimal starRating,
            @Schema(description = "카테고리 내 시나리오 노출 순서")
            int displayOrder,
            @Schema(description = "시나리오 제목")
            String scenarioTitle,
            @Schema(description = "시나리오 설명")
            String briefing,
            @Schema(description = "대화 목표")
            String conversationGoal,
            @Schema(description = "난이도")
            String difficulty,
            @Schema(description = "첫 발화자")
            String firstSpeaker,
            @Schema(description = "시나리오 썸네일 URL")
            String thumbnailUrl,
            @Schema(description = "사용자 시나리오 완료 여부")
            boolean completed,
            @Schema(description = "시나리오 잠금 여부")
            boolean locked,
            @Schema(description = "시나리오 잠금 사유")
            String lockReason,
            @Schema(description = "잠기지 않은 시나리오의 시작 메시지 미리보기")
            OpeningPreviewResponse openingPreview
    ) {
    }

    @Schema(description = "시작 메시지 미리보기 응답")
    public record OpeningPreviewResponse(
            @Schema(description = "AI first 시 첫 AI 메시지")
            String aiOpeningMessage,
            @Schema(description = "첫 AI 메시지 번역")
            String aiOpeningMessageTranslation,
            @Schema(description = "USER first 시 사용자 시작 안내")
            String userOpeningInstruction,
            @Schema(description = "첫 화면에 보여줄 상대 역할의 속마음")
            String innerThought,
            @Schema(description = "속마음 유형")
            String innerThoughtType,
            @Schema(description = "시나리오 TTS voice set ID")
            String ttsVoiceSetId
    ) {
    }
}
