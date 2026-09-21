// 표현 재사용 기록이 필수 값과 문구를 갖출 때만 만들어지는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.expression.reuse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 표현 재사용 기록이 필수 값과 문구를 갖출 때만 만들어지는지 검증한다. */
class FreeTalkExpressionReuseTest {

  private static final LocalDate LEARNED_ON = LocalDate.of(2026, 9, 10);

  @DisplayName("표현과 출처, 발화에서 찾은 조각을 저장 시점의 값 그대로 담는다.")
  @Test
  void keepsSnapshotValues() {
    FreeTalkExpressionReuse reuse =
        FreeTalkExpressionReuse.of(
            1207L,
            30L,
            55020L,
            812L,
            "grab a coffee",
            "커피 한잔하다",
            FreeTalkExpressionReuseSource.SCENARIO,
            "주말 계획",
            LEARNED_ON,
            "grab a coffee",
            "Let's grab a coffee after work.");

    assertThat(reuse.getUserProfileId()).isEqualTo(1207L);
    assertThat(reuse.getFreeTalkSessionId()).isEqualTo(30L);
    assertThat(reuse.getSessionHistoryMessageId()).isEqualTo(55020L);
    assertThat(reuse.getWritingExpressionId()).isEqualTo(812L);
    assertThat(reuse.getExpressionText()).isEqualTo("grab a coffee");
    assertThat(reuse.getExpressionMeaning()).isEqualTo("커피 한잔하다");
    assertThat(reuse.getSourceType()).isEqualTo(FreeTalkExpressionReuseSource.SCENARIO);
    assertThat(reuse.getSourceTitle()).isEqualTo("주말 계획");
    assertThat(reuse.getSourceLearnedOn()).isEqualTo(LEARNED_ON);
    assertThat(reuse.getMatchedText()).isEqualTo("grab a coffee");
    assertThat(reuse.getQuotedSentence()).isEqualTo("Let's grab a coffee after work.");
  }

  @DisplayName("출처 제목을 찾지 못했으면 빈 문자열이 아니라 없는 값으로 남긴다.")
  @Test
  void storesMissingSourceTitleAsNull() {
    assertThat(reuse("grab a coffee", "커피 한잔하다", "  ", "grab a coffee", "문장").getSourceTitle())
        .isNull();
    assertThat(reuse("grab a coffee", "커피 한잔하다", null, "grab a coffee", "문장").getSourceTitle())
        .isNull();
  }

  @DisplayName("표현 원문·뜻·발화에서 찾은 조각·인용 문장 중 하나라도 비면 만들지 않는다.")
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      nullValues = "NULL",
      value = {
        "'  ' | 커피 한잔하다 | grab a coffee | 문장",
        "grab a coffee | NULL | grab a coffee | 문장",
        "grab a coffee | 커피 한잔하다 | '' | 문장",
        "grab a coffee | 커피 한잔하다 | grab a coffee | NULL"
      })
  void rejectsBlankText(String text, String meaning, String matchedText, String quotedSentence) {
    assertThatThrownBy(() -> reuse(text, meaning, "주말 계획", matchedText, quotedSentence))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("출처 종류나 배운 날이 없으면 만들지 않는다.")
  @Test
  void rejectsMissingSource() {
    assertThatThrownBy(
            () ->
                FreeTalkExpressionReuse.of(
                    1L, 2L, 3L, 4L, "a", "b", null, "제목", LEARNED_ON, "a", "a."))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                FreeTalkExpressionReuse.of(
                    1L,
                    2L,
                    3L,
                    4L,
                    "a",
                    "b",
                    FreeTalkExpressionReuseSource.FREE_TALK,
                    "제목",
                    null,
                    "a",
                    "a."))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static FreeTalkExpressionReuse reuse(
      String text, String meaning, String sourceTitle, String matchedText, String sentence) {
    return FreeTalkExpressionReuse.of(
        1207L,
        30L,
        55020L,
        812L,
        text,
        meaning,
        FreeTalkExpressionReuseSource.SCENARIO,
        sourceTitle,
        LEARNED_ON,
        matchedText,
        sentence);
  }
}
