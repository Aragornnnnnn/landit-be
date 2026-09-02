// 원어민 표현 학습 중 추가 예문 조회 응답을 표현한다.

package com.landit.landitbe.feature.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 원어민 표현 학습 중 추가 예문 조회 응답을 표현한다.
 *
 * @param targetExpressionText 타겟 표현
 * @param baseExpressionMeaningText 타겟 표현 뜻
 * @param usageDescription 표현 상세 설명
 * @param practiceSentence 추가 예문 목록
 * @param writingSentence 작문 연습 문제 2건. 영어 문제와 한국어 문제가 한 건씩이다
 */
@Schema(description = "원어민 표현 추가 예문 조회 응답")
public record ExpressionPracticeResponse(
    @Schema(description = "타겟 표현", example = "blow my mind") String targetExpressionText,
    @Schema(description = "타겟 표현 뜻", example = "끝내주게 놀랍다") String baseExpressionMeaningText,
    @Schema(description = "표현 상세 설명", example = "강렬한 인상을 받았을 때 최고의 리액션이에요.")
        String usageDescription,
    @Schema(description = "문제 안 풀고, 눈으로 익히는 추가 예문 2건") List<PracticeSentenceResponse> practiceSentence,
    @Schema(description = "직접 푸는 작문 문제 2건. 영어 문제와 한국어 문제가 한 건씩이다")
        List<WritingSentenceResponse> writingSentence) {}
