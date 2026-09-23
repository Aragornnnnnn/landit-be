// 프리톡 맞춤 표현 추천 결과를 담는다.

package com.landit.landitbe.feature.learning.freetalk.expression.client.ai;

import java.util.List;

/**
 * 프리톡 맞춤 표현 추천 결과를 담는다.
 *
 * @param recommendations 대화에 맞는 표현 추천 목록
 * @param usedExpressions AI가 다시 썼다고 판정한 배운 표현. 판정하지 못했거나 쓴 표현이 없으면 비어 있다. 둘은 구분되지 않는다
 */
public record AiFreeTalkExpressionRecommendationsResult(
    List<AiFreeTalkExpressionRecommendation> recommendations,
    List<AiFreeTalkUsedExpression> usedExpressions) {

  /** 다시 쓴 표현 목록을 null 없이 불변으로 보관한다. */
  public AiFreeTalkExpressionRecommendationsResult {
    usedExpressions = usedExpressions == null ? List.of() : List.copyOf(usedExpressions);
  }

  /** 다시 쓴 표현 없이 추천만 담는다. */
  public AiFreeTalkExpressionRecommendationsResult(
      List<AiFreeTalkExpressionRecommendation> recommendations) {
    this(recommendations, List.of());
  }
}
