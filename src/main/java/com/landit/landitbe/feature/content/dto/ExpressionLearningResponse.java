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
 * @param representativeSentenceAudioUrl 대표 예문 원어민 TTS URL. 발음 자산이 없으면 null
 * @param targetExpressionAudioUrl 타겟 표현만 읽은 원어민 TTS URL. 자산 미준비이거나 패턴형 표현이면 null
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
        String representativeImageUrl,
    @Schema(
            description =
                "대표 예문 원어민 TTS URL. 발음 페이지의 '원어민 발음 듣기' 재생용."
                    + " 사용자의 AI 튜터 억양 기준이며, 발음 자산이 아직 준비되지 않은 표현은 null"
                    + " (앱은 null이면 발음 듣기·발화 파트를 숨긴다)",
            example = "https://cdn.landit.com/content/expression-pronunciation-audio/101/abc.mp3")
        String representativeSentenceAudioUrl,
    @Schema(
            description =
                "타겟 표현만 읽은 원어민 TTS URL. 표현 듣기 재생용."
                    + " 사용자의 AI 튜터 억양 기준이며, 발음 자산이 아직 준비되지 않았거나"
                    + " 패턴형 표현(발화 불가)이면 null (앱은 null이면 표현 듣기 버튼을 숨긴다)",
            example = "https://cdn.landit.com/content/expression-pronunciation-audio/101/expr.mp3")
        String targetExpressionAudioUrl) {

  /**
   * 표현 엔티티와 원어민 TTS URL들을 학습 시작 응답으로 변환한다.
   *
   * @param expression 변환할 표현 엔티티
   * @param representativeSentenceAudioUrl 대표 예문 원어민 TTS URL. 발음 자산이 없으면 null
   * @param targetExpressionAudioUrl 타겟 표현만 읽은 원어민 TTS URL. 자산 미준비이거나 패턴형 표현이면 null
   * @return 표현 학습 시작 응답
   */
  public static ExpressionLearningResponse from(
      WritingExpression expression,
      String representativeSentenceAudioUrl,
      String targetExpressionAudioUrl) {
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
        expression.getRepresentativeImageUrl(),
        representativeSentenceAudioUrl,
        targetExpressionAudioUrl);
  }
}
