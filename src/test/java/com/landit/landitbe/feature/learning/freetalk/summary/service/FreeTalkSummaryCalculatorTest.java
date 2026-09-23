// 총평 계산기가 지표·실수 기억 카드·헤드라인을 기획 규칙대로 고르는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.summary.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkPatternUsageDraft;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkHeadlinePose;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkHeadlineTrigger;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkSessionSummary;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSummarySource;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSummarySource.Utterance;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 총평 계산기가 지표·실수 기억 카드·헤드라인을 기획 규칙대로 고르는지 검증한다. */
class FreeTalkSummaryCalculatorTest {

  private static final LocalDate PREVIOUS_DATE = LocalDate.of(2026, 9, 10);
  private static final FreeTalkSessionSummary.PreviousSession PREVIOUS =
      new FreeTalkSessionSummary.PreviousSession(1201L, PREVIOUS_DATE, 5);

  private final FreeTalkSummaryCalculator calculator = new FreeTalkSummaryCalculator();

  @DisplayName("말한 시간은 발화 시간의 합(없으면 0), 주고받은 말은 발화 수, 가장 길게 말한 턴은 공백 기준 단어 수의 최댓값이다.")
  @Test
  void computesMetrics() {
    FreeTalkSummarySource source =
        source(
            List.of(
                new Utterance("I went to the gym.", 4200L),
                new Utterance("  It was   really fun and I liked it  ", null),
                new Utterance("", 800L)),
            List.of(),
            List.of());

    FreeTalkSessionSummary summary = calculator.calculate(1L, 30L, 3100L, 300L, source, null, null);

    assertThat(summary.getCurrentSpeakingMs()).isEqualTo(5000);
    assertThat(summary.getCurrentTurnCount()).isEqualTo(3);
    assertThat(summary.getCurrentMaxWordsInTurn()).isEqualTo(8);
    assertThat(summary.getCorrectionCount()).isZero();
  }

  @DisplayName("첫 스몰톡은 직전 값 없이 첫 스몰톡 예외 문구와 손 흔드는 포즈다.")
  @Test
  void firstSessionUsesFirstSessionHeadline() {
    FreeTalkSummarySource source =
        source(utterances(18), List.of(correction(FreeTalkMistakePattern.TENSE)), List.of());

    FreeTalkSessionSummary summary = calculator.calculate(1L, 30L, 3100L, 300L, source, null, null);

    assertThat(summary.isFirstSession()).isTrue();
    assertThat(summary.getHeadlineTrigger()).isEqualTo(FreeTalkHeadlineTrigger.FIRST_SESSION);
    assertThat(summary.getHeadlineText()).isEqualTo("첫 스몰톡, 18번이나 주고받았어요!");
    assertThat(summary.getHeadlinePose()).isEqualTo(FreeTalkHeadlinePose.WAVE_SMILE);
    assertThat(summary.getCorrectionCount()).isEqualTo(1);
    assertThat(summary.getGrowthPattern()).isNull();
  }

  @DisplayName("한 마디도 안 한 첫 스몰톡은 \"0번이나\"라고 말하지 않고, 둘 다 말한 시간이 없는 세션은 주고받은 말로 비슷한지 본다.")
  @Test
  void avoidsMisleadingPhrasesForEmptySessions() {
    FreeTalkSessionSummary emptyFirst =
        calculator.calculate(
            1L, 30L, 3100L, 300L, source(List.of(), List.of(), List.of()), null, null);
    assertThat(emptyFirst.getHeadlineText()).isEqualTo("첫 스몰톡 완주 축하해요!");

    FreeTalkSummarySource textOnly =
        source(
            List.of(new Utterance("hi", null), new Utterance("bye", null)), List.of(), List.of());
    FreeTalkSummarySource textOnlyShorter =
        source(List.of(new Utterance("hi", null)), List.of(), List.of());
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 300L, textOnly, PREVIOUS, textOnly)
                .getHeadlineTrigger())
        .isEqualTo(FreeTalkHeadlineTrigger.SIMILAR);
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 300L, textOnlyShorter, PREVIOUS, textOnly)
                .getHeadlineTrigger())
        .isEqualTo(FreeTalkHeadlineTrigger.DECREASED);
  }

  @DisplayName("같은 세션은 언제 계산해도 같은 문구이고, 세션 ID가 다르면 은행의 다른 문구를 돌려 쓴다.")
  @Test
  void rotatesPhrasesBySessionIdDeterministically() {
    FreeTalkSummarySource source = source(utterances(18), List.of(), List.of());

    FreeTalkSessionSummary odd = calculator.calculate(1L, 30L, 3100L, 301L, source, null, null);
    FreeTalkSessionSummary oddAgain =
        calculator.calculate(1L, 30L, 3100L, 301L, source, null, null);
    FreeTalkSessionSummary even = calculator.calculate(1L, 30L, 3100L, 302L, source, null, null);

    assertThat(odd.getHeadlineText())
        .isEqualTo(oddAgain.getHeadlineText())
        .isEqualTo("첫 스몰톡 완주 축하해요!");
    assertThat(even.getHeadlineText()).isEqualTo("첫 스몰톡, 18번이나 주고받았어요!");
  }

  @DisplayName("실수 기억 카드는 직전에 가장 많이 틀린 패턴 중 오늘 등장한 것으로 만들고, 맞은 사용례만 있으면 성장 성공이다.")
  @Test
  void buildsSucceededGrowthCardFromMostFrequentPatternSeenToday() {
    FreeTalkSummarySource previous =
        source(
            utterances(10),
            List.of(
                correction(FreeTalkMistakePattern.ARTICLE, "I eat a apple.", "a apple"),
                correction(FreeTalkMistakePattern.TENSE, "I go yesterday.", "go"),
                correction(FreeTalkMistakePattern.ARTICLE, "at a gym", "a gym"),
                correction(FreeTalkMistakePattern.ARTICLE, "in a school", null),
                // 지켜볼 수 없는 유형은 아무리 많아도 카드가 되지 않는다.
                correction(FreeTalkMistakePattern.WORD_CHOICE),
                correction(FreeTalkMistakePattern.WORD_CHOICE),
                correction(FreeTalkMistakePattern.WORD_CHOICE),
                correction(FreeTalkMistakePattern.WORD_CHOICE)),
            List.of());
    FreeTalkSummarySource current =
        source(
            utterances(10),
            List.of(),
            List.of(
                usage(FreeTalkMistakePattern.TENSE, "I went there.", "went", true),
                usage(FreeTalkMistakePattern.ARTICLE, "I like the movie.", "the movie", true),
                usage(FreeTalkMistakePattern.ARTICLE, "at the gym", "the gym", true)));

    FreeTalkSessionSummary summary =
        calculator.calculate(1L, 30L, 3100L, 300L, current, PREVIOUS, previous);

    // ARTICLE(3회)이 TENSE(1회)보다 먼저다. 지난 문장은 구절이 있는 가장 최근 교정, 오늘 문장은 첫 사용례다.
    assertThat(summary.getGrowthPattern()).isEqualTo(FreeTalkMistakePattern.ARTICLE);
    assertThat(summary.getGrowthSucceeded()).isTrue();
    assertThat(summary.getGrowthPreviousDate()).isEqualTo(PREVIOUS_DATE);
    assertThat(summary.getGrowthPreviousSentence()).isEqualTo("at a gym");
    assertThat(summary.getGrowthPreviousWrongSpan()).isEqualTo("a gym");
    assertThat(summary.getGrowthCurrentSentence()).isEqualTo("I like the movie.");
    assertThat(summary.getGrowthCurrentSpan()).isEqualTo("the movie");
    assertThat(summary.getHeadlineTrigger()).isEqualTo(FreeTalkHeadlineTrigger.GROWTH);
    assertThat(summary.getHeadlineText()).isEqualTo("지난번에 헷갈렸던 관사, 오늘은 다 맞았어요!");
    assertThat(summary.getHeadlinePose()).isEqualTo(FreeTalkHeadlinePose.POINT);
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 302L, current, PREVIOUS, previous)
                .getHeadlineText())
        .isEqualTo("관사 완전 정복한 거 같은데요?");
  }

  @DisplayName("오늘 그 패턴으로 또 교정받았으면 반복 카드이고, 포즈는 normal이며 헤드라인은 성장을 말하지 않는다.")
  @Test
  void buildsRepeatedGrowthCardWhenCorrectedAgainToday() {
    FreeTalkSummarySource previous =
        source(
            utterances(10),
            List.of(correction(FreeTalkMistakePattern.TENSE, "I go yesterday.", "go")),
            List.of());
    FreeTalkSummarySource current =
        source(
            utterances(10),
            List.of(correction(FreeTalkMistakePattern.TENSE, "I see him last week.", "see")),
            // 맞은 사용례가 있어도 교정이 있으면 반복이다.
            List.of(usage(FreeTalkMistakePattern.TENSE, "I went home.", "went", true)));

    FreeTalkSessionSummary summary =
        calculator.calculate(1L, 30L, 3100L, 300L, current, PREVIOUS, previous);

    assertThat(summary.getGrowthSucceeded()).isFalse();
    assertThat(summary.getGrowthCurrentSentence()).isEqualTo("I see him last week.");
    assertThat(summary.getGrowthCurrentSpan()).isEqualTo("see");
    assertThat(summary.getHeadlinePose()).isEqualTo(FreeTalkHeadlinePose.NORMAL);
    assertThat(summary.getHeadlineTrigger()).isEqualTo(FreeTalkHeadlineTrigger.SIMILAR);
  }

  @DisplayName("교정은 없고 틀린 사용례가 있으면 반복 카드이며, 지난 교정에 구절이 없었으면 취소선 구절 없이 만든다.")
  @Test
  void usesWrongUsageAsRepeatAndAllowsMissingPreviousSpan() {
    FreeTalkSummarySource previous =
        source(
            utterances(10),
            List.of(correction(FreeTalkMistakePattern.PLURAL, "two cat", null)),
            List.of());
    FreeTalkSummarySource current =
        source(
            utterances(10),
            List.of(),
            List.of(
                usage(FreeTalkMistakePattern.PLURAL, "many dogs", "dogs", true),
                usage(FreeTalkMistakePattern.PLURAL, "three book", "book", false)));

    FreeTalkSessionSummary summary =
        calculator.calculate(1L, 30L, 3100L, 300L, current, PREVIOUS, previous);

    assertThat(summary.getGrowthSucceeded()).isFalse();
    assertThat(summary.getGrowthPreviousWrongSpan()).isNull();
    assertThat(summary.getGrowthCurrentSentence()).isEqualTo("three book");
  }

  @DisplayName("지켜볼 수 없는 유형은 직전에도 오늘도 교정받았어도 카드가 되지 않는다.")
  @Test
  void neverBuildsCardForUnwatchablePattern() {
    FreeTalkSummarySource previous =
        source(utterances(10), List.of(correction(FreeTalkMistakePattern.WORD_CHOICE)), List.of());
    FreeTalkSummarySource current =
        source(utterances(10), List.of(correction(FreeTalkMistakePattern.WORD_CHOICE)), List.of());

    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 300L, current, PREVIOUS, previous)
                .getGrowthPattern())
        .isNull();
  }

  @DisplayName("직전에 틀린 패턴이 오늘 등장하지 않으면 카드가 없고, 더 많이 틀린 패턴이 안 나왔으면 다음 패턴으로 만든다.")
  @Test
  void skipsPatternsNotSeenToday() {
    FreeTalkSummarySource previous =
        source(
            utterances(10),
            List.of(
                correction(FreeTalkMistakePattern.ARTICLE),
                correction(FreeTalkMistakePattern.ARTICLE),
                correction(FreeTalkMistakePattern.TENSE, "I go.", "go")),
            List.of());

    assertThat(
            calculator
                .calculate(
                    1L,
                    30L,
                    3100L,
                    300L,
                    source(utterances(10), List.of(), List.of()),
                    PREVIOUS,
                    previous)
                .getGrowthPattern())
        .isNull();
    assertThat(
            calculator
                .calculate(
                    1L,
                    30L,
                    3100L,
                    300L,
                    source(
                        utterances(10),
                        List.of(),
                        List.of(usage(FreeTalkMistakePattern.TENSE, "I went.", "went", true))),
                    PREVIOUS,
                    previous)
                .getGrowthPattern())
        .isEqualTo(FreeTalkMistakePattern.TENSE);
  }

  @DisplayName("헤드라인은 오랜만 복귀 → 성장 → 말한 시간 → 최장 턴 → 주고받은 말 → 비슷 → 줄어듦 순으로 처음 해당되는 것이다.")
  @ParameterizedTest
  @CsvSource({
    // days | cur ms,turns,words | prev ms,turns,words | trigger
    "10, 100000,10,10, 100000,10,10, RETURN_AFTER_BREAK",
    "9, 110000,10,10, 100000,10,10, SPEAKING_TIME_UP",
    // 9% 증가는 "늘었다"가 아니고, 우선순위상 주고받은 말 증가가 비슷함보다 앞선다.
    "5, 109000,20,10, 100000,10,10, TURN_COUNT_UP",
    "5, 100000,10,20, 100000,10,10, LONGEST_TURN_UP",
    "5, 100000,11,10, 100000,10,10, TURN_COUNT_UP",
    "5, 105000,10,10, 100000,10,10, SIMILAR",
    "5, 95000,10,10, 100000,10,10, SIMILAR",
    "5, 80000,10,10, 100000,10,10, DECREASED",
    // 비슷함의 아래 경계: 직전의 100/110 이상이면 비슷, 그 아래는 줄어듦.
    "5, 90910,10,10, 100000,10,10, SIMILAR",
    "5, 90909,10,10, 100000,10,10, DECREASED",
    "5, 0,0,0, 0,0,0, SIMILAR",
    "5, 1000,1,1, 0,0,0, SPEAKING_TIME_UP",
    // 비율로는 늘었어도 1초가 안 되는 차이는 "0초 더 말했어요"가 되므로 비슷함이다.
    "5, 3400,10,10, 3000,10,10, SIMILAR",
    "5, 999,1,1, 0,1,1, SIMILAR",
    "5, 4000,10,10, 3000,10,10, SPEAKING_TIME_UP",
  })
  void picksHeadlineTriggerByPriority(
      int days,
      long currentMs,
      int currentTurns,
      int currentWords,
      long previousMs,
      int previousTurns,
      int previousWords,
      String expected) {
    FreeTalkSessionSummary summary =
        calculator.calculate(
            1L,
            30L,
            3100L,
            300L,
            metricsSource(currentMs, currentTurns, currentWords),
            new FreeTalkSessionSummary.PreviousSession(1201L, PREVIOUS_DATE, days),
            metricsSource(previousMs, previousTurns, previousWords));

    assertThat(summary.getHeadlineTrigger()).isEqualTo(FreeTalkHeadlineTrigger.valueOf(expected));
  }

  @DisplayName("오랜만 복귀는 성장 성공보다 먼저이고, 성장 성공은 지표가 늘었어도 그보다 먼저다.")
  @Test
  void ordersReturnBeforeGrowthAndGrowthBeforeMetrics() {
    FreeTalkSummarySource previous =
        source(
            utterances(10),
            List.of(correction(FreeTalkMistakePattern.TENSE, "I go.", "go")),
            List.of());
    FreeTalkSummarySource current =
        source(
            java.util.Collections.nCopies(30, new Utterance("I went.", 9000L)),
            List.of(),
            List.of(usage(FreeTalkMistakePattern.TENSE, "I went.", "went", true)));

    assertThat(
            calculator
                .calculate(
                    1L,
                    30L,
                    3100L,
                    300L,
                    current,
                    new FreeTalkSessionSummary.PreviousSession(1201L, PREVIOUS_DATE, 10),
                    previous)
                .getHeadlineTrigger())
        .isEqualTo(FreeTalkHeadlineTrigger.RETURN_AFTER_BREAK);
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 300L, current, PREVIOUS, previous)
                .getHeadlineTrigger())
        .isEqualTo(FreeTalkHeadlineTrigger.GROWTH);
  }

  @DisplayName("문구 은행의 둘째·셋째 문구도 세션 ID로 고를 수 있고, 조건이 안 맞으면 후보에서 빠진다.")
  @Test
  void reachesEveryPhraseAndDropsUnfillableOnes() {
    FreeTalkSummarySource previous = metricsSource(100000, 10, 10);
    // 오랜만 복귀 둘째 문구: 1분 이상 말했을 때만.
    FreeTalkSessionSummary.PreviousSession longAgo =
        new FreeTalkSessionSummary.PreviousSession(1201L, PREVIOUS_DATE, 20);
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 301L, metricsSource(245000, 14, 10), longAgo, previous)
                .getHeadlineText())
        .isEqualTo("오랜만인데도 4분 넘게 말했어요!");
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 301L, metricsSource(50000, 14, 10), longAgo, previous)
                .getHeadlineText())
        .isEqualTo("20일 만이네요, 감을 잃지않고 14번 주고받았어요!");
    // 주고받은 말 첫 문구.
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 300L, metricsSource(100000, 18, 10), PREVIOUS, previous)
                .getHeadlineText())
        .isEqualTo("18번이나 주고받았어요!");
    // 비슷함 둘째 문구는 턴 수가 같고 0이 아닐 때만. 0턴이면 첫 문구뿐이다.
    assertThat(
            calculator
                .calculate(
                    1L, 30L, 3100L, 301L, metricsSource(0, 0, 0), PREVIOUS, metricsSource(0, 0, 0))
                .getHeadlineText())
        .isEqualTo("지난번만큼 얘기했어요!");
    // 성장 성공 둘째 문구.
    FreeTalkSummarySource corrected =
        source(
            utterances(10),
            List.of(correction(FreeTalkMistakePattern.ARTICLE, "a gym", "a gym")),
            List.of());
    FreeTalkSummarySource fixed =
        source(
            utterances(10),
            List.of(),
            List.of(usage(FreeTalkMistakePattern.ARTICLE, "the gym", "the gym", true)));
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 301L, fixed, PREVIOUS, corrected)
                .getHeadlineText())
        .isEqualTo("관사, 이제 안 헷갈리네요!");
  }

  @DisplayName("직전 교정에 실수 패턴이 없으면 세지 않는다.")
  @Test
  void ignoresPreviousCorrectionsWithoutPattern() {
    List<FreeTalkTurnCorrection.Sentence> unpatterned =
        List.of(new FreeTalkTurnCorrection.Sentence("a", "b", "c", null));

    assertThat(
            calculator
                .calculate(
                    1L,
                    30L,
                    3100L,
                    300L,
                    // 오늘 TENSE 사용례가 있어도, 직전에 패턴 없는 교정뿐이면 카드가 없다.
                    source(
                        utterances(10),
                        List.of(),
                        List.of(usage(FreeTalkMistakePattern.TENSE, "I went.", "went", true))),
                    PREVIOUS,
                    source(utterances(10), unpatterned, List.of()))
                .getGrowthPattern())
        .isNull();
  }

  @DisplayName("말한 시간 헤드라인은 차이·총량·배수 문구를 값이 채워질 때만 후보로 두고 숫자를 한국어 서식으로 쓴다.")
  @Test
  void fillsSpeakingTimePhrasesOnlyWhenValuesAllow() {
    FreeTalkSummarySource previous = metricsSource(161000, 14, 12);
    FreeTalkSummarySource current = metricsSource(245000, 18, 23);

    FreeTalkSessionSummary diff =
        calculator.calculate(1L, 30L, 3100L, 300L, current, PREVIOUS, previous);
    FreeTalkSessionSummary total =
        calculator.calculate(1L, 30L, 3100L, 301L, current, PREVIOUS, previous);
    FreeTalkSessionSummary ratio =
        calculator.calculate(1L, 30L, 3100L, 302L, current, PREVIOUS, previous);

    assertThat(diff.getHeadlineText()).isEqualTo("지난번보다 1분 24초 더 말했어요!");
    assertThat(total.getHeadlineText()).isEqualTo("오늘 4분 넘게 말했어요!");
    assertThat(total.getHeadlineSubline()).isEqualTo("지난번엔 2분 41초였어요.");
    assertThat(ratio.getHeadlineText()).isEqualTo("말한 시간이 지난번의 1.5배예요!");

    // 1분이 안 되고 1.5배도 안 되면 차이 문구 하나뿐이라 어떤 세션 ID여도 그 문구다.
    FreeTalkSessionSummary onlyDiff =
        calculator.calculate(
            1L,
            30L,
            3100L,
            301L,
            metricsSource(50000, 10, 10),
            PREVIOUS,
            metricsSource(40000, 10, 10));
    assertThat(onlyDiff.getHeadlineText()).isEqualTo("지난번보다 10초 더 말했어요!");
  }

  @DisplayName("최장 턴·주고받은 말·비슷함·오랜만 복귀 문구에 지금 값을 채운다.")
  @Test
  void fillsOtherTriggerPhrases() {
    assertThat(
            calculator
                .calculate(
                    1L,
                    30L,
                    3100L,
                    300L,
                    metricsSource(100000, 10, 23),
                    PREVIOUS,
                    metricsSource(100000, 10, 12))
                .getHeadlineText())
        .isEqualTo("한 번에 23단어까지 말했어요!");
    assertThat(
            calculator
                .calculate(
                    1L,
                    30L,
                    3100L,
                    301L,
                    metricsSource(100000, 10, 24),
                    PREVIOUS,
                    metricsSource(100000, 10, 12))
                .getHeadlineText())
        .isEqualTo("제일 긴 문장이 두 배 길어졌어요!");
    FreeTalkSessionSummary turns =
        calculator.calculate(
            1L,
            30L,
            3100L,
            301L,
            metricsSource(100000, 18, 10),
            PREVIOUS,
            metricsSource(100000, 14, 10));
    assertThat(turns.getHeadlineText()).isEqualTo("대화가 지난번보다 4번 더 이어졌어요!");
    assertThat(
            calculator
                .calculate(
                    1L,
                    30L,
                    3100L,
                    301L,
                    metricsSource(100000, 18, 10),
                    PREVIOUS,
                    metricsSource(100000, 18, 10))
                .getHeadlineText())
        .isEqualTo("오늘도 18번 주고받았어요!");
    FreeTalkSessionSummary returned =
        calculator.calculate(
            1L,
            30L,
            3100L,
            300L,
            metricsSource(245000, 14, 10),
            new FreeTalkSessionSummary.PreviousSession(1201L, PREVIOUS_DATE, 20),
            metricsSource(100000, 10, 10));
    assertThat(returned.getHeadlineText()).isEqualTo("20일 만이네요, 감을 잃지않고 14번 주고받았어요!");
    assertThat(
            calculator
                .calculate(
                    1L,
                    30L,
                    3100L,
                    300L,
                    metricsSource(80000, 10, 10),
                    PREVIOUS,
                    metricsSource(100000, 10, 10))
                .getHeadlineText())
        .isEqualTo("오늘은 짧게 얘기했어요.");
  }

  @DisplayName("교정이 다 끝나지 않은 채 확정하면 맞은 사용례만 있는 패턴은 건너뛰고, 또 틀린 근거가 있는 패턴만 카드가 된다.")
  @Test
  void doesNotClaimSuccessWhileCorrectionsRemain() {
    FreeTalkSummarySource previous =
        source(
            utterances(10),
            List.of(
                correction(FreeTalkMistakePattern.ARTICLE, "a gym", "a gym"),
                correction(FreeTalkMistakePattern.ARTICLE, "a apple", "a apple"),
                correction(FreeTalkMistakePattern.TENSE, "I go yesterday.", "go")),
            List.of());
    List<FreeTalkPatternUsageDraft> usages =
        List.of(
            usage(FreeTalkMistakePattern.ARTICLE, "the gym", "the gym", true),
            usage(FreeTalkMistakePattern.TENSE, "I go there.", "go", false));
    FreeTalkSummarySource complete = source(utterances(10), List.of(), usages);
    FreeTalkSummarySource incomplete =
        new FreeTalkSummarySource(utterances(10), List.of(), usages, false);

    FreeTalkSessionSummary settled =
        calculator.calculate(1L, 30L, 3100L, 300L, complete, PREVIOUS, previous);
    FreeTalkSessionSummary early =
        calculator.calculate(1L, 30L, 3100L, 300L, incomplete, PREVIOUS, previous);

    // 교정이 다 끝났으면 가장 많이 틀린 관사의 성공 카드다.
    assertThat(settled.getGrowthPattern()).isEqualTo(FreeTalkMistakePattern.ARTICLE);
    assertThat(settled.getGrowthSucceeded()).isTrue();
    assertThat(settled.getHeadlineTrigger()).isEqualTo(FreeTalkHeadlineTrigger.GROWTH);
    // 남은 교정이 관사일 수 있으므로 성공은 주장하지 않고, 또 틀린 근거가 있는 시제 카드로 넘어간다.
    assertThat(early.getGrowthPattern()).isEqualTo(FreeTalkMistakePattern.TENSE);
    assertThat(early.getGrowthSucceeded()).isFalse();
    assertThat(early.getGrowthCurrentSentence()).isEqualTo("I go there.");
    assertThat(early.getHeadlineTrigger()).isNotEqualTo(FreeTalkHeadlineTrigger.GROWTH);
    assertThat(early.getHeadlinePose()).isEqualTo(FreeTalkHeadlinePose.NORMAL);

    // 맞은 사용례뿐이면 카드가 없고 헤드라인도 성장을 말하지 않는다.
    FreeTalkSummarySource onlyCorrect =
        new FreeTalkSummarySource(
            utterances(10),
            List.of(),
            List.of(usage(FreeTalkMistakePattern.ARTICLE, "the gym", "the gym", true)),
            false);
    FreeTalkSessionSummary none =
        calculator.calculate(1L, 30L, 3100L, 300L, onlyCorrect, PREVIOUS, previous);
    assertThat(none.getGrowthPattern()).isNull();
    assertThat(none.getGrowthSucceeded()).isNull();
    assertThat(none.getHeadlineTrigger()).isNotEqualTo(FreeTalkHeadlineTrigger.GROWTH);
  }

  @DisplayName("실수 기억 카드 후보는 턴마다 지켜본 상위 3개 패턴뿐이라, 네 번째 패턴은 오늘 맞게 썼어도 또 틀렸어도 카드가 되지 않는다.")
  @Test
  void limitsCardCandidatesToWatchedPatterns() {
    FreeTalkSummarySource previous =
        source(
            utterances(10),
            List.of(
                correction(FreeTalkMistakePattern.TENSE),
                correction(FreeTalkMistakePattern.TENSE),
                correction(FreeTalkMistakePattern.TENSE),
                correction(FreeTalkMistakePattern.ARTICLE),
                correction(FreeTalkMistakePattern.ARTICLE),
                correction(FreeTalkMistakePattern.PLURAL),
                correction(FreeTalkMistakePattern.PLURAL),
                correction(FreeTalkMistakePattern.PRONOUN)),
            List.of());
    FreeTalkSummarySource fixedFourth =
        source(
            utterances(10),
            List.of(),
            List.of(usage(FreeTalkMistakePattern.PRONOUN, "her bag", "her", true)));
    FreeTalkSummarySource repeatedFourth =
        source(
            utterances(10),
            List.of(correction(FreeTalkMistakePattern.PRONOUN, "him bag", "him")),
            List.of());
    FreeTalkSummarySource fixedThird =
        source(
            utterances(10),
            List.of(),
            List.of(usage(FreeTalkMistakePattern.PLURAL, "two cats", "cats", true)));

    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 300L, fixedFourth, PREVIOUS, previous)
                .getGrowthPattern())
        .isNull();
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 300L, repeatedFourth, PREVIOUS, previous)
                .getGrowthPattern())
        .isNull();
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 300L, fixedThird, PREVIOUS, previous)
                .getGrowthPattern())
        .isEqualTo(FreeTalkMistakePattern.PLURAL);
  }

  @DisplayName(
      "\"N분 넘게\"는 정확히 N분이면 후보에서 빠지고, \"N번이나\"는 1번이면 쓰지 않으며, 줄어듦 문구 셋과 성장 셋째 문구의 의미 줄을 돌려 쓴다.")
  @Test
  void gatesBoundaryPhrasesAndRotatesDecreasedPhrases() {
    FreeTalkSummarySource previous = metricsSource(161000, 14, 12);
    // 정확히 4분: "4분 넘게"가 거짓이라 빠지고, 1.5배도 안 되어 차이 문구뿐이다.
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 301L, metricsSource(240000, 14, 12), PREVIOUS, previous)
                .getHeadlineText())
        .isEqualTo("지난번보다 1분 19초 더 말했어요!");
    FreeTalkSessionSummary.PreviousSession longAgo =
        new FreeTalkSessionSummary.PreviousSession(1201L, PREVIOUS_DATE, 20);
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 301L, metricsSource(120000, 14, 12), longAgo, previous)
                .getHeadlineText())
        .isEqualTo("20일 만이네요, 감을 잃지않고 14번 주고받았어요!");
    // 1번: "1번이나"를 쓰지 않는다.
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 300L, metricsSource(5000, 1, 3), null, null)
                .getHeadlineText())
        .isEqualTo("첫 스몰톡 완주 축하해요!");
    assertThat(
            calculator
                .calculate(
                    1L, 30L, 3100L, 300L, metricsSource(0, 1, 0), PREVIOUS, metricsSource(0, 0, 0))
                .getHeadlineText())
        .isEqualTo("대화가 지난번보다 1번 더 이어졌어요!");
    // 줄어듦 문구 셋.
    FreeTalkSummarySource shorter = metricsSource(80000, 14, 12);
    assertThat(
            calculator
                .calculate(1L, 30L, 3100L, 300L, shorter, PREVIOUS, previous)
                .getHeadlineText())
        .isEqualTo("오늘은 짧게 얘기했어요.");
    FreeTalkSessionSummary second =
        calculator.calculate(1L, 30L, 3100L, 301L, shorter, PREVIOUS, previous);
    assertThat(second.getHeadlineText()).isEqualTo("지난번보다 짧게 대화했어요.");
    assertThat(second.getHeadlineSubline()).isEqualTo("오늘도 대화했다는 사실이 중요하죠.");
    FreeTalkSessionSummary third =
        calculator.calculate(1L, 30L, 3100L, 302L, shorter, PREVIOUS, previous);
    assertThat(third.getHeadlineText()).isEqualTo("오늘은 짧게 얘기해서 아쉬워요.");
    assertThat(third.getHeadlineSubline()).isEqualTo("다음엔 더 길게 얘기하고 싶어요.");
    // 성장 셋째 문구의 의미 줄.
    FreeTalkSummarySource corrected =
        source(
            utterances(10),
            List.of(correction(FreeTalkMistakePattern.ARTICLE, "a gym", "a gym")),
            List.of());
    FreeTalkSummarySource fixed =
        source(
            utterances(10),
            List.of(),
            List.of(usage(FreeTalkMistakePattern.ARTICLE, "the gym", "the gym", true)));
    FreeTalkSessionSummary mastered =
        calculator.calculate(1L, 30L, 3100L, 302L, fixed, PREVIOUS, corrected);
    assertThat(mastered.getHeadlineText()).isEqualTo("관사 완전 정복한 거 같은데요?");
    assertThat(mastered.getHeadlineSubline()).isEqualTo("점점 실력이 늘어가고 있어요.");
  }

  @DisplayName("\"N분 넘게\" 판정은 1분 이상이면서 정확히 N분이 아닐 때만 참이다.")
  @ParameterizedTest
  @CsvSource({"245000, true", "240000, false", "60000, false", "60001, true", "59999, false"})
  void judgesOverWholeMinute(long ms, boolean expected) {
    assertThat(FreeTalkSummaryCalculator.overWholeMinute(ms)).isEqualTo(expected);
  }

  @DisplayName("시간·배수 서식은 0초·정확한 분·소수 배수를 한국어로 쓴다.")
  @ParameterizedTest
  @CsvSource({
    "84000, 1분 24초",
    "60000, 1분",
    "4000, 4초",
    "0, 0초",
    "1499, 1초",
    "1500, 2초",
    "3600000, 60분"
  })
  void formatsDuration(long ms, String expected) {
    assertThat(FreeTalkSummaryCalculator.durationText(ms)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({"245000, 161000, 1.5", "300000, 100000, 3", "199999, 100000, 1.9"})
  void formatsRatio(long current, long previous, String expected) {
    assertThat(FreeTalkSummaryCalculator.ratioText(current, previous)).isEqualTo(expected);
  }

  private static FreeTalkSummarySource metricsSource(long speakingMs, int turns, int maxWords) {
    List<Utterance> utterances = new java.util.ArrayList<>();
    for (int index = 0; index < turns; index++) {
      String content = index == 0 ? "w ".repeat(maxWords).strip() : "w";
      utterances.add(new Utterance(content, index == 0 ? speakingMs : 0L));
    }
    return source(utterances, List.of(), List.of());
  }

  private static FreeTalkSummarySource source(
      List<Utterance> utterances,
      List<FreeTalkTurnCorrection.Sentence> corrections,
      List<FreeTalkPatternUsageDraft> usages) {
    return new FreeTalkSummarySource(utterances, corrections, usages);
  }

  private static List<Utterance> utterances(int count) {
    return java.util.Collections.nCopies(count, new Utterance("I like it.", 1000L));
  }

  private static FreeTalkTurnCorrection.Sentence correction(FreeTalkMistakePattern pattern) {
    return correction(pattern, "wrong", null);
  }

  private static FreeTalkTurnCorrection.Sentence correction(
      FreeTalkMistakePattern pattern, String original, String wrongSpan) {
    return new FreeTalkTurnCorrection.Sentence(original, "better", "이유", pattern, wrongSpan, null);
  }

  private static FreeTalkPatternUsageDraft usage(
      FreeTalkMistakePattern pattern, String sentence, String span, boolean correct) {
    return new FreeTalkPatternUsageDraft(pattern, sentence, span, correct);
  }
}
