// 세션 기능에 다음 시나리오 질문 정보를 전달한다.

package com.landit.landitbe.feature.content.dto;

import com.landit.landitbe.feature.content.repository.projection.ScenarioQuestionProjection;

/**
 * 세션 기능에 다음 시나리오 질문 정보를 전달한다.
 *
 * @param questionId 질문 ID
 * @param sequence 질문 순서
 * @param questionText 질문 본문
 * @param questionTranslation 질문 번역
 */
public record NextQuestionContext(
    Long questionId, int sequence, String questionText, String questionTranslation) {

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
        projection.questionTranslation());
  }
}
