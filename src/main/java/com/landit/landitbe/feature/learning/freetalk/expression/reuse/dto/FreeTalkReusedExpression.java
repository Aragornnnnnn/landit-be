// 지난 스몰톡의 한 발화에서 다시 쓴 배운 표현을 전달한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.dto;

import com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain.FreeTalkExpressionReuse;

/**
 * 한 발화에서 다시 쓴 배운 표현이다. 저장된 값을 그대로 담고 조회할 때 표현 테이블을 다시 읽지 않는다.
 *
 * @param expressionId 표현 ID. 예: 812
 * @param text 저장 시점의 표현 원문. 예: "grab a coffee"
 * @param matchedText 발화 원문에서 밑줄을 그을 조각. 예: "grabbed a coffee"
 */
public record FreeTalkReusedExpression(long expressionId, String text, String matchedText) {

  /** 저장된 재사용 기록을 그대로 옮긴다. */
  public static FreeTalkReusedExpression of(FreeTalkExpressionReuse reuse) {
    return new FreeTalkReusedExpression(
        reuse.getWritingExpressionId(), reuse.getExpressionText(), reuse.getMatchedText());
  }
}
