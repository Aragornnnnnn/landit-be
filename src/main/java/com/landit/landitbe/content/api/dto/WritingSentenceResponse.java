// 추가 예문 중 랜덤으로 선택된 작문 연습 문제를 표현한다.
package com.landit.landitbe.content.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "작문 연습 문제. practiceSentence 중 서버에서 랜덤으로 1개를 선택해 내려줌")
public record WritingSentenceResponse(
    @Schema(
            description = "작문용으로 선택된 예문 영어 텍스트(정답 비교용)",
            example = "The special effects blew my mind.")
        String writingSentenceText,
    @Schema(description = "선택된 예문의 해석", example = "특수효과가 끝내줬어.") String writingSentenceTranslation,
    @Schema(description = "선택된 예문의 연습 질문", example = "How was the musical?") String writingQuestion,
    @Schema(description = "선택된 연습 질문의 해석", example = "뮤지컬 어땠어?")
        String writingQuestionTranslation) {}
