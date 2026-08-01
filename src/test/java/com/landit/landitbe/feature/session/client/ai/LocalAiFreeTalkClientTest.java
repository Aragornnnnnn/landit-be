// 로컬 프리톡 AI 클라이언트의 결정적 응답 계약을 검증한다.

package com.landit.landitbe.feature.session.client.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 로컬 프리톡 AI 클라이언트의 결정적 응답 계약을 검증한다. */
class LocalAiFreeTalkClientTest {

  private final LocalAiFreeTalkClient client = new LocalAiFreeTalkClient();

  @Test
  void returnsDeterministicTurnContract() {
    AiFreeTalkTurnResult turn = client.generateTurn(turnRequest());

    assertThat(turn.userExitIntentDetected()).isFalse();
    assertThat(turn.inferredTitle()).isEqualTo("오늘의 프리톡");
    assertThat(turn.aiMessage()).isNotBlank();
    assertThat(turn.translatedMessage()).isNotBlank();
    assertThat(turn.emotion()).isNotNull();
    assertThat(turn.innerThought()).isNotBlank();
    assertThat(turn.innerThoughtType()).isNotNull();
  }

  private AiFreeTalkTurnRequest turnRequest() {
    return new AiFreeTalkTurnRequest(
        300L,
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
}
