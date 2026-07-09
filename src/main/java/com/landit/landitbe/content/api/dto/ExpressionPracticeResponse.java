// 원어민 표현 학습 중 추가 예문 조회 응답을 표현한다.
package com.landit.landitbe.content.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "원어민 표현 추가 예문 조회 응답")
public record ExpressionPracticeResponse(
        @Schema(description = "타겟 표현", example = "blow my mind")
        String targetExpressionText,

        @Schema(description = "타겟 표현 뜻", example = "끝내주게 놀랍다")
        String baseExpressionMeaningText,

        @Schema(description = "표현 상세 설명", example = "강렬한 인상을 받았을 때 최고의 리액션이에요.")
        String usageDescription,

        @Schema(description = "추가 예문 목록")
        List<PracticeSentenceResponse> practiceSentence,

        @Schema(description = "작문 연습에 사용할 문제. practiceSentence 중 랜덤 1개")
        WritingSentenceResponse writingSentence
) {
}
