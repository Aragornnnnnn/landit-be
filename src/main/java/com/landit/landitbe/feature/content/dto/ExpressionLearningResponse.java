// 원어민 표현 학습 시작 시 내려주는 표현 상세 응답을 표현한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.feature.content.domain.WritingExpression;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 원어민 표현 학습 시작 시 내려주는 표현 상세 응답을 표현한다.
 *
 * @param expressionId 표현 고유 ID
 * @param targetExpressionText 타겟 표현
 * @param baseExpressionMeaningText 타겟 표현 뜻
 * @param usageDescription 표현 상세 설명
 * @param representativeQuestionText 작문을 유도하는 대표 질문. 질문형 구성 불가 시 null
 * @param representativeQuestionTranslation 대표 질문의 해석
 * @param representativeSentenceText 대표 예문 텍스트
 * @param representativeSentenceTranslation 대표 예문의 해석
 * @param representativeSentenceWords 정답 예문을 단어 단위로 나눈 배열(정답 순서 유지)
 * @param representativeSentenceWordChoices 정답 단어와 오답 단어를 섞은 선택지 배열(저장된 섞인 순서 그대로)
 * @param representativeImageUrl 대표 예문 이미지 URL
 */
@Schema(description = "원어민 표현 학습 시작 응답")
public record ExpressionLearningResponse(
    @Schema(description = "표현 고유 ID", example = "101") Long expressionId,
    @Schema(description = "타겟 표현", example = "blow my mind") String targetExpressionText,
    @Schema(description = "타겟 표현 뜻", example = "끝내주게 놀랍다") String baseExpressionMeaningText,
    @Schema(description = "표현 상세 설명", example = "blow my mind는 '끝내준다', '충격적으로 대단하다'는 뜻입니다.")
        String usageDescription,
    @Schema(
            description = "작문을 유도하는 대표 질문. 질문형 구성 불가 시 null",
            example = "What should I definitely see in Korea?")
        String representativeQuestionText,
    @Schema(description = "대표 질문의 해석", example = "한국에서 뭘 꼭 봐야 해?")
        String representativeQuestionTranslation,
    @Schema(description = "대표 예문 텍스트", example = "Gyeongbokgung Palace will blow your mind.")
        String representativeSentenceText,
    @Schema(description = "대표 예문의 해석", example = "경복궁은 널 완전 놀라게 할 거야.")
        String representativeSentenceTranslation,
    @Schema(
            description = "정답 예문을 단어 단위로 나눈 배열(정답 순서 유지)",
            example = "[\"Gyeongbokgung\", \"Palace\", \"will\", \"blow\", \"your\", \"mind\"]")
        List<String> representativeSentenceWords,
    @Schema(
            description = "정답 단어와 오답 단어를 섞은 선택지 배열(저장된 섞인 순서 그대로)",
            example =
                "[\"Gyeongbokgung\", \"blow\", \"will\", \"Palace\", \"amazing\", "
                    + "\"have\", \"get\", \"your\", \"mind\"]")
        List<String> representativeSentenceWordChoices,
    @Schema(description = "대표 예문 이미지 URL", example = "https://cdn.example.com/images/101.png")
        String representativeImageUrl) {

  /**
   * 표현 엔티티를 학습 시작 응답으로 변환한다.
   *
   * @param expression 변환할 표현 엔티티
   * @return 표현 학습 시작 응답
   */
  public static ExpressionLearningResponse from(WritingExpression expression) {
    return new ExpressionLearningResponse(
        expression.getId(),
        expression.getTargetExpressionText(),
        expression.getBaseExpressionMeaningText(),
        expression.getUsageDescription(),
        expression.getRepresentativeQuestionText(),
        expression.getRepresentativeQuestionTranslation(),
        expression.getRepresentativeSentenceText(),
        expression.getRepresentativeSentenceTranslation(),
        expression.getRepresentativeSentenceWords(),
        expression.getRepresentativeSentenceWordChoices(),
        expression.getRepresentativeImageUrl());
  }
}
