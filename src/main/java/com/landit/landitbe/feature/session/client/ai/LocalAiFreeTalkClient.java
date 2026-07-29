// 로컬 개발과 통합 테스트에서 사용할 결정적 프리톡 AI 대체 클라이언트다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 로컬 개발과 통합 테스트에서 사용할 결정적 프리톡 AI 대체 클라이언트다. */
@Component
@ConditionalOnProperty(
    prefix = "landit.ai",
    name = "client-mode",
    havingValue = "local",
    matchIfMissing = true)
public class LocalAiFreeTalkClient implements AiFreeTalkClient {

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkOpeningResult generateOpening(AiFreeTalkOpeningRequest request) {
    return new AiFreeTalkOpeningResult(
        "What would you like to talk about today?", "오늘은 무슨 이야기를 하고 싶어?", CharacterEmotion.HAPPY);
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkTurnResult generateTurn(AiFreeTalkTurnRequest request) {
    return new AiFreeTalkTurnResult(
        false,
        request.isFirstUserTurn() ? "오늘의 프리톡" : null,
        "That sounds interesting. Tell me more.",
        "흥미롭다. 조금 더 이야기해줘.",
        CharacterEmotion.HAPPY);
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkInnerThoughtResult generateInnerThought(AiFreeTalkInnerThoughtRequest request) {
    return new AiFreeTalkInnerThoughtResult(
        "사용자가 대화를 자연스럽게 이어가고 있다.", com.landit.landitbe.shared.domain.InnerThoughtType.GOOD);
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkClosingResult generateClosing(AiFreeTalkClosingRequest request) {
    return new AiFreeTalkClosingResult(
        "It was nice talking with you.", "이야기해서 좋았어.", CharacterEmotion.NEUTRAL);
  }
}
