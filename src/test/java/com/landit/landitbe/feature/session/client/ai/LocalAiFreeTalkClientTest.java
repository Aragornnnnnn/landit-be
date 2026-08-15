// 로컬 프리톡 AI 클라이언트의 결정적 응답 계약을 검증한다.

package com.landit.landitbe.feature.session.client.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 로컬 프리톡 AI 클라이언트의 결정적 응답 계약을 검증한다. */
class LocalAiFreeTalkClientTest {

  private final LocalAiFreeTalkClient client = new LocalAiFreeTalkClient();

  @Test
  void returnsDeterministicTurnAndExistingExpressionRecommendationContracts() {
    AiFreeTalkTurnResult turn = client.generateTurn(turnRequest());
    AiFreeTalkExpressionRecommendation recommendation =
        client.recommendExpressions(recommendationsRequest()).recommendations().getFirst();

    assertThat(turn.userExitIntentDetected()).isFalse();
    assertThat(turn.inferredTitle()).isEqualTo("오늘의 프리톡");
    assertThat(turn.aiMessage()).isNotBlank();
    assertThat(turn.translatedMessage()).isNotBlank();
    assertThat(turn.emotion()).isNotNull();
    assertThat(client.generateInnerThought(innerThoughtRequest()).innerThought()).isNotBlank();
    assertThat(client.generateInnerThought(innerThoughtRequest()).innerThoughtType()).isNotNull();
    assertThat(recommendation.existingExpressionId()).isEqualTo(7L);
    assertThat(recommendation.displayOrder()).isEqualTo(1);
  }

  private AiFreeTalkTurnRequest turnRequest() {
    return new AiFreeTalkTurnRequest(
        300L,
        "chloe",
        3002L,
        1,
        "EN",
        "KR",
        AiFreeTalkResponseMode.NORMAL,
        true,
        null,
        List.of(
            new AiConversationHistoryMessage(
                3002L, 1, "USER", "I'm going hiking with friends.", null)));
  }

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest() {
    return new AiFreeTalkInnerThoughtRequest(
        300L,
        "chloe",
        3002L,
        1,
        "EN",
        "KR",
        null,
        List.of(
            new AiConversationHistoryMessage(
                3002L, 1, "USER", "I'm going hiking with friends.", null)));
  }

  private AiFreeTalkExpressionRecommendationsRequest recommendationsRequest() {
    return new AiFreeTalkExpressionRecommendationsRequest(
        300L,
        "EN",
        "KR",
        List.of(
            new AiConversationHistoryMessage(
                3002L, 1, "USER", "I'm going hiking with friends.", null)),
        List.of(new AiFreeTalkExistingExpression(7L, "I'm up for that", "좋아", "제안에 동의")));
  }
}
