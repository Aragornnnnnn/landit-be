// 속마음 요청이 지켜볼 실수 패턴을 AI 서버 계약 안에서만 담고 제출한 발화의 원문을 찾는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 속마음 요청이 지켜볼 실수 패턴을 AI 서버 계약 안에서만 담고 제출한 발화의 원문을 찾는지 검증한다. */
class AiFreeTalkInnerThoughtRequestTest {

  @DisplayName("지켜볼 패턴을 주지 않으면 빈 목록이고, 상한을 넘거나 겹치면 거부한다.")
  @Test
  void keepsWatchPatternsWithinContract() {
    assertThat(request(null).watchPatterns()).isEmpty();
    assertThat(
            request(
                    List.of(
                        FreeTalkMistakePattern.TENSE,
                        FreeTalkMistakePattern.ARTICLE,
                        FreeTalkMistakePattern.PLURAL))
                .watchPatterns())
        .hasSize(3);
    assertThatThrownBy(
            () ->
                request(
                    List.of(
                        FreeTalkMistakePattern.TENSE,
                        FreeTalkMistakePattern.ARTICLE,
                        FreeTalkMistakePattern.PLURAL,
                        FreeTalkMistakePattern.PRONOUN)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> request(List.of(FreeTalkMistakePattern.TENSE, FreeTalkMistakePattern.TENSE)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("제출한 발화의 원문은 대화 문맥에서 발화 ID로 찾고, 없으면 null이다.")
  @Test
  void findsSubmittedContentInHistory() {
    assertThat(request(List.of()).submittedContent()).isEqualTo("I went to the gym.");
    assertThat(
            new AiFreeTalkInnerThoughtRequest(
                    300L, "chloe", 9999L, 1, "EN", "KR", null, history(), List.of())
                .submittedContent())
        .isNull();
  }

  private static AiFreeTalkInnerThoughtRequest request(List<FreeTalkMistakePattern> patterns) {
    return new AiFreeTalkInnerThoughtRequest(
        300L, "chloe", 3002L, 1, "EN", "KR", null, history(), List.of(), patterns);
  }

  private static List<AiConversationHistoryMessage> history() {
    return List.of(
        new AiConversationHistoryMessage(3001L, 1, "AI", "What did you do?", null),
        new AiConversationHistoryMessage(3002L, 1, "USER", "I went to the gym.", null));
  }
}
