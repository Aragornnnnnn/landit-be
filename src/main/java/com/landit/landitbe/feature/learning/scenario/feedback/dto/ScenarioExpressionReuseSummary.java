// 시나리오 총평의 배운 표현 재사용 상태와 결과를 전달한다.

package com.landit.landitbe.feature.learning.scenario.feedback.dto;

import java.util.List;

/**
 * 분석 중 여부와 실제로 다시 쓴 표현을 제공한다.
 *
 * @param pending 분석이 진행 중이면 true
 * @param items 실제 사용이 확인된 표현 목록. 비어 있으면 카드를 표시하지 않는다
 */
public record ScenarioExpressionReuseSummary(boolean pending, List<Item> items) {

  /**
   * 항목을 불변으로 보관한다.
   *
   * @param pending 분석이 진행 중이면 true
   * @param items 실제 사용이 확인된 표현 목록
   */
  public ScenarioExpressionReuseSummary {
    items = List.copyOf(items);
  }

  /**
   * 이번 시나리오에서 다시 쓴 표현 하나다.
   *
   * @param expressionId 학습 표현 ID
   * @param text 학습 표현 원문
   * @param meaning 표현의 기준 언어 뜻
   * @param sourceLabel 학습 날짜와 출처 기능
   * @param messageId 표현을 사용한 사용자 메시지 ID
   * @param quotedSentence 표현을 포함한 문장
   * @param matchedText 문장에서 강조할 정확한 구절
   */
  public record Item(
      long expressionId,
      String text,
      String meaning,
      String sourceLabel,
      long messageId,
      String quotedSentence,
      String matchedText) {}
}
