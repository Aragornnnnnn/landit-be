// 총평이 첫 스몰톡과 비교 스몰톡의 값 묶음을 짝이 맞게 담고, 값 객체가 잘못된 값을 거부하는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.summary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkGrowthCard;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkHeadline;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSessionMetrics;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 총평이 첫 스몰톡과 비교 스몰톡의 값 묶음을 짝이 맞게 담고, 값 객체가 잘못된 값을 거부하는지 검증한다. */
class FreeTalkSessionSummaryTest {

  private static final FreeTalkHeadline HEADLINE =
      new FreeTalkHeadline(
          FreeTalkHeadlineTrigger.SPEAKING_TIME_UP,
          "지난번보다 1분 24초 더 말했어요!",
          "할 말이 그만큼 늘었다는 거예요.",
          FreeTalkHeadlinePose.POINT);
  private static final FreeTalkSessionMetrics CURRENT = new FreeTalkSessionMetrics(245000, 18, 23);
  private static final FreeTalkSessionMetrics PREVIOUS = new FreeTalkSessionMetrics(161000, 14, 12);
  private static final LocalDate PREVIOUS_DATE = LocalDate.of(2026, 9, 10);

  @DisplayName("첫 스몰톡 총평은 직전 값이 없고 직전 지표가 0이며 실수 기억 카드가 없다.")
  @Test
  void firstSessionHasNoPreviousValues() {
    FreeTalkSessionSummary summary =
        FreeTalkSessionSummary.first(1207L, 30L, 3100L, HEADLINE, CURRENT, 3);

    assertThat(summary.isFirstSession()).isTrue();
    assertThat(summary.getPreviousLearningSessionId()).isNull();
    assertThat(summary.getPreviousDate()).isNull();
    assertThat(summary.getDaysSincePrevious()).isNull();
    assertThat(summary.getPreviousSpeakingMs()).isZero();
    assertThat(summary.getPreviousTurnCount()).isZero();
    assertThat(summary.getPreviousMaxWordsInTurn()).isZero();
    assertThat(summary.getGrowthPattern()).isNull();
    assertThat(summary.getGrowthSucceeded()).isNull();
    assertThat(summary.getCurrentSpeakingMs()).isEqualTo(245000);
    assertThat(summary.getHeadlineText()).isEqualTo("지난번보다 1분 24초 더 말했어요!");
    assertThat(summary.getCorrectionCount()).isEqualTo(3);
  }

  @DisplayName("비교 총평은 직전 세션·직전 지표·실수 기억 카드를 계산 시점 값 그대로 담는다.")
  @Test
  void comparedSessionKeepsPreviousValuesAndGrowthCard() {
    FreeTalkGrowthCard growth =
        new FreeTalkGrowthCard(
            FreeTalkMistakePattern.TENSE,
            true,
            PREVIOUS_DATE,
            "I go to gym with my friend.",
            "go",
            "I went to the gym with my friend.",
            null);

    FreeTalkSessionSummary summary =
        FreeTalkSessionSummary.compared(
            1207L,
            30L,
            3100L,
            new FreeTalkSessionSummary.PreviousSession(1201L, PREVIOUS_DATE, 5),
            HEADLINE,
            CURRENT,
            PREVIOUS,
            growth,
            3);

    assertThat(summary.isFirstSession()).isFalse();
    assertThat(summary.getPreviousLearningSessionId()).isEqualTo(1201L);
    assertThat(summary.getPreviousDate()).isEqualTo(PREVIOUS_DATE);
    assertThat(summary.getDaysSincePrevious()).isEqualTo(5);
    assertThat(summary.getPreviousSpeakingMs()).isEqualTo(161000);
    assertThat(summary.getGrowthPattern()).isEqualTo(FreeTalkMistakePattern.TENSE);
    assertThat(summary.getGrowthSucceeded()).isTrue();
    assertThat(summary.getGrowthPreviousWrongSpan()).isEqualTo("go");
    assertThat(summary.getGrowthCurrentSpan()).isNull();
  }

  @DisplayName("비교 총평에 실수 기억 카드가 없으면 카드 값은 전부 null이다.")
  @Test
  void comparedSessionWithoutGrowthLeavesCardEmpty() {
    FreeTalkSessionSummary summary =
        FreeTalkSessionSummary.compared(
            1207L,
            30L,
            3100L,
            new FreeTalkSessionSummary.PreviousSession(1201L, PREVIOUS_DATE, 5),
            HEADLINE,
            CURRENT,
            PREVIOUS,
            null,
            0);

    assertThat(summary.getGrowthPattern()).isNull();
    assertThat(summary.getGrowthPreviousSentence()).isNull();
    assertThat(summary.getGrowthCurrentSentence()).isNull();
  }

  @DisplayName("값 객체는 지켜볼 수 없는 패턴, 빈 구절, 음수 지표, 빈 헤드라인을 거부한다.")
  @Test
  void valueObjectsRejectInvalidValues() {
    assertThatThrownBy(
            () ->
                new FreeTalkGrowthCard(
                    FreeTalkMistakePattern.WORD_CHOICE, true, PREVIOUS_DATE, "a", null, "b", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new FreeTalkGrowthCard(
                    FreeTalkMistakePattern.TENSE, true, PREVIOUS_DATE, "a", " ", "b", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new FreeTalkGrowthCard(
                    FreeTalkMistakePattern.TENSE, true, null, "a", null, "b", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FreeTalkSessionMetrics(-1, 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new FreeTalkHeadline(
                    FreeTalkHeadlineTrigger.SIMILAR, " ", "리듬이 잡혔어요.", FreeTalkHeadlinePose.POINT))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FreeTalkSessionSummary.PreviousSession(1201L, PREVIOUS_DATE, -1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> FreeTalkSessionSummary.first(1207L, 30L, 3100L, HEADLINE, CURRENT, -1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
