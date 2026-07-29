// 로컬 개발과 통합 테스트에서 사용할 결정적 프리톡 AI 대체 클라이언트다.

package com.landit.landitbe.feature.session.client.ai;

import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.feature.session.domain.FreeTalkExpressionSourceType;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    return new AiFreeTalkInnerThoughtResult("사용자가 대화를 자연스럽게 이어가고 있다.", InnerThoughtType.GOOD);
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkClosingResult generateClosing(AiFreeTalkClosingRequest request) {
    return new AiFreeTalkClosingResult(
        "It was nice talking with you.", "이야기해서 좋았어.", CharacterEmotion.NEUTRAL);
  }

  @Override
  public AiFreeTalkExpressionRecommendationsResult recommendExpressions(
      AiFreeTalkExpressionRecommendationsRequest request) {
    return new AiFreeTalkExpressionRecommendationsResult(
        List.of(
            new AiFreeTalkExpressionRecommendation(
                1,
                FreeTalkExpressionSourceType.NEW,
                null,
                "I'm up for that",
                "좋아, 그거 하자",
                "상대의 제안에 흔쾌히 동의할 때 사용")));
  }

  @Override
  public AiFreeTalkExpressionLearningContentResult generateExpressionLearningContent(
      AiFreeTalkExpressionLearningContentRequest request) {
    return new AiFreeTalkExpressionLearningContentResult(
        request.expressions().stream().map(this::learningContent).toList());
  }

  private AiFreeTalkExpressionLearningContent learningContent(
      AiFreeTalkLearningExpression expression) {
    return new AiFreeTalkExpressionLearningContent(
        expression.targetExpressionText(),
        expression.baseExpressionMeaningText(),
        expression.usageSummary(),
        "친근한 대화에서 제안을 자연스럽게 받아들일 때 사용합니다.",
        "Do you want to go hiking this weekend?",
        "이번 주말에 등산 갈래?",
        "I'm up for that.",
        "좋아, 그거 하자.",
        List.of("I'm", "up", "for", "that"),
        List.of("that", "I'm", "up", "for", "to"),
        null,
        List.of(
            practiceExample("I'm up for hiking.", "Want to go hiking?", "등산하는 거 좋아.", "등산 갈래?"),
            practiceExample(
                "I'm up for trying that cafe.",
                "Want to try that cafe?",
                "그 카페 가보는 거 좋아.",
                "그 카페 가볼래?"),
            practiceExample(
                "I'm up for a movie tonight.",
                "Want to watch a movie?",
                "오늘 밤 영화 보는 거 좋아.",
                "영화 볼래?"),
            practiceExample(
                "I'm up for meeting your friends.",
                "Want to meet my friends?",
                "네 친구들 만나는 거 좋아.",
                "내 친구들 만날래?")));
  }

  private AiFreeTalkExpressionPracticeExample practiceExample(
      String sentence, String question, String translation, String questionTranslation) {
    List<String> words = List.of(sentence.replace(".", "").split(" "));
    return new AiFreeTalkExpressionPracticeExample(
        null,
        sentence,
        words,
        "I'm up for",
        question,
        translation,
        shuffledWordChoices(words),
        questionTranslation);
  }

  private List<String> shuffledWordChoices(List<String> words) {
    List<String> choices = new ArrayList<>(words);
    Collections.rotate(choices, 1);
    choices.add("today");
    return choices;
  }
}
