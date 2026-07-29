// 로컬 프리톡 AI 클라이언트의 결정적 응답 계약을 검증한다.

package com.landit.landitbe.feature.session.client.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.session.domain.FreeTalkExpressionSourceType;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 로컬 프리톡 AI 클라이언트의 결정적 응답 계약을 검증한다. */
class LocalAiFreeTalkClientTest {

  private final LocalAiFreeTalkClient client = new LocalAiFreeTalkClient();

  @Test
  void returnsDeterministicTurnAndNewExpressionRecommendationContracts() {
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
    assertThat(recommendation.sourceType()).isEqualTo(FreeTalkExpressionSourceType.NEW);
    assertThat(recommendation.existingExpressionId()).isNull();
    assertThat(recommendation.displayOrder()).isEqualTo(1);
  }

  @Test
  void returnsFourValidPracticeExamplesForEachLearningExpression() {
    AiFreeTalkExpressionLearningContent content =
        client
            .generateExpressionLearningContent(
                new AiFreeTalkExpressionLearningContentRequest(
                    300L,
                    "EN",
                    "KR",
                    List.of(
                        new AiFreeTalkLearningExpression(
                            "I'm up for that", "좋아, 그거 하자", "제안에 동의할 때 사용"))))
            .expressions()
            .getFirst();

    assertThat(content.practiceExamples()).hasSize(4);
    assertThat(content.representativeImageUrl()).isNull();
    assertThat(content.representativeSentenceWordChoices())
        .containsAll(content.representativeSentenceWords())
        .hasSizeGreaterThan(content.representativeSentenceWords().size())
        .isNotEqualTo(content.representativeSentenceWords());
    content
        .practiceExamples()
        .forEach(
            example -> {
              assertThat(example.imageUrl()).isNull();
              assertThat(String.join(" ", example.sentenceWords()))
                  .isEqualTo(example.sentenceText().replace(".", ""));
              assertThat(example.sentenceWordChoices())
                  .containsAll(example.sentenceWords())
                  .hasSizeGreaterThan(example.sentenceWords().size())
                  .isNotEqualTo(example.sentenceWords());
              assertThat(example.sentenceText()).contains(example.highlightingPart());
            });
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

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest() {
    return new AiFreeTalkInnerThoughtRequest(
        300L,
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
        List.of());
  }
}
