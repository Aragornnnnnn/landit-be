// 실수 패턴 사용례가 필수 값을 갖출 때만 만들어지는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.feedback.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 실수 패턴 사용례가 필수 값을 갖출 때만 만들어지는지 검증한다. */
class FreeTalkPatternUsageTest {

  @DisplayName("패턴·문장·구절·맞음 여부를 판정 시점 값 그대로 담는다.")
  @Test
  void keepsJudgedValues() {
    FreeTalkPatternUsage usage =
        FreeTalkPatternUsage.of(
            55020L, 3100L, FreeTalkMistakePattern.TENSE, "I went to the gym.", "went", true);

    assertThat(usage.getSessionHistoryMessageId()).isEqualTo(55020L);
    assertThat(usage.getSessionHistoryId()).isEqualTo(3100L);
    assertThat(usage.getPattern()).isEqualTo(FreeTalkMistakePattern.TENSE);
    assertThat(usage.getSentence()).isEqualTo("I went to the gym.");
    assertThat(usage.getSpan()).isEqualTo("went");
    assertThat(usage.isCorrect()).isTrue();
  }

  @DisplayName("문장이나 구절이 비면 만들지 않는다.")
  @ParameterizedTest
  @CsvSource(
      value = {"' '|went", "I went to the gym.|' '", "NULL|went", "I went to the gym.|NULL"},
      delimiter = '|',
      nullValues = "NULL")
  void rejectsBlankText(String sentence, String span) {
    assertThatThrownBy(
            () ->
                FreeTalkPatternUsage.of(
                    55020L, 3100L, FreeTalkMistakePattern.TENSE, sentence, span, false))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("패턴이 없으면 만들지 않는다.")
  @Test
  void rejectsMissingPattern() {
    assertThatThrownBy(
            () -> FreeTalkPatternUsage.of(55020L, 3100L, null, "I went to the gym.", "went", true))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
