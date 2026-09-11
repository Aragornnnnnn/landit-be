// 세션 기능에 다음 시나리오 질문 정보를 전달한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.feature.content.domain.ResponseDemand;
import com.landit.landitbe.feature.content.repository.projection.ScenarioQuestionProjection;

/**
 * 세션 기능에 다음 시나리오 질문 정보를 전달한다.
 *
 * @param questionId 질문 ID
 * @param sequence 질문 순서
 * @param questionText 질문 본문
 * @param questionTranslation 질문 번역
 * @param questionAudioUrl 질문 음원 URL
 * @param responseDemand 답변에 요구되는 정보량
 * @param requiredResponseElement 수준 평가에 필요한 응답 요소
 */
public record NextQuestionContext(
    Long questionId,
    int sequence,
    String questionText,
    String questionTranslation,
    String questionAudioUrl,
    ResponseDemand responseDemand,
    String requiredResponseElement) {

  /**
   * 질문 조회 Projection을 기능 간 공개 계약으로 변환한다.
   *
   * @param projection 질문 조회 Projection
   * @return 다음 질문 컨텍스트
   */
  public static NextQuestionContext from(ScenarioQuestionProjection projection) {
    return new NextQuestionContext(
        projection.questionId(),
        projection.sequence(),
        projection.questionText(),
        projection.questionTranslation(),
        projection.questionAudioUrl(),
        projection.responseDemand(),
        projection.requiredResponseElement().isBlank()
            ? projection.questionText()
            : projection.requiredResponseElement());
  }
}
