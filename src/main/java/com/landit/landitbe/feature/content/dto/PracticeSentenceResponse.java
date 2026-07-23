// 추가 예문 조회 응답의 예문 1건을 표현한다.

package com.landit.landitbe.feature.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 추가 예문 조회 응답의 예문 1건을 표현한다. */
@Schema(description = "추가 예문 항목")
public record PracticeSentenceResponse(
    @Schema(description = "예문 텍스트", example = "Her voice blows my mind every time.")
        String sentenceText,
    @Schema(description = "예문 중 강조 표시할 부분(타겟 표현이 활용된 구간)", example = "blows my mind")
        String highlightingPart,
    @Schema(description = "예문 해석", example = "그녀 목소리는 들을 때마다 소름 돋아.") String sentenceTranslation,
    @Schema(description = "예문을 유도하는 연습 질문", example = "What do you think of her singing?")
        String practiceQuestion,
    @Schema(description = "연습 질문의 해석", example = "걔 노래 어때?") String practiceQuestionTranslation,
    @Schema(
            description = "예문 이미지 URL. 없으면 null",
            example = "https://cdn.landit.com/writing/examples/001.png")
        String imageUrl) {}
