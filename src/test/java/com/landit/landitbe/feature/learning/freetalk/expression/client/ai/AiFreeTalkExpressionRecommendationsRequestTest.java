// 표현 추천 요청이 배운 표현 후보를 AI 서버 계약 안에서만 담는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.expression.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 표현 추천 요청이 배운 표현 후보를 AI 서버 계약 안에서만 담는지 검증한다. */
class AiFreeTalkExpressionRecommendationsRequestTest {

  @DisplayName("배운 표현 후보를 주지 않거나 null로 주면 빈 목록으로 보관한다.")
  @Test
  void keepsEmptyLearnedExpressionsWhenAbsent() {
    assertThat(request(null).learnedExpressions()).isEmpty();
    assertThat(
            new AiFreeTalkExpressionRecommendationsRequest(300L, "EN", "KR", List.of(), List.of())
                .learnedExpressions())
        .isEmpty();
  }

  @DisplayName("배운 표현 후보는 상한까지 담고, 하나라도 넘으면 거부한다.")
  @Test
  void rejectsLearnedExpressionsOverLimit() {
    int limit = AiFreeTalkExpressionRecommendationsRequest.MAX_LEARNED_EXPRESSIONS;

    assertThat(request(learned(limit)).learnedExpressions()).hasSize(limit);
    assertThatThrownBy(() -> request(learned(limit + 1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("표현 ID가 겹치는 배운 표현 후보는 거부한다.")
  @Test
  void rejectsDuplicateLearnedExpressionIds() {
    List<AiFreeTalkLearnedExpression> duplicated =
        List.of(
            new AiFreeTalkLearnedExpression(812L, "grab a coffee", "커피 한잔하다"),
            new AiFreeTalkLearnedExpression(812L, "grab a coffee", "커피 한잔하다"));

    assertThatThrownBy(() -> request(duplicated)).isInstanceOf(IllegalArgumentException.class);
  }

  private static AiFreeTalkExpressionRecommendationsRequest request(
      List<AiFreeTalkLearnedExpression> learnedExpressions) {
    return new AiFreeTalkExpressionRecommendationsRequest(
        300L, "EN", "KR", List.of(), List.of(), learnedExpressions);
  }

  private static List<AiFreeTalkLearnedExpression> learned(int count) {
    return LongStream.rangeClosed(1, count)
        .mapToObj(id -> new AiFreeTalkLearnedExpression(id, "expression " + id, "뜻 " + id))
        .toList();
  }
}
