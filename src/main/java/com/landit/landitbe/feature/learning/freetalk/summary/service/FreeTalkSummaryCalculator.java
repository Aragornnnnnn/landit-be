// 세션의 재료로 총평(헤드라인·지난번과 비교·실수 기억 카드)을 계산한다. DB를 읽지 않는 순수 계산이다.

package com.landit.landitbe.feature.learning.freetalk.summary.service;

import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkPatternUsageDraft;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkHeadlinePose;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkHeadlineTrigger;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkSessionSummary;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkGrowthCard;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkHeadline;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSessionMetrics;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSummarySource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * 총평을 계산한다.
 *
 * <p>헤드라인은 기획 1-1의 우선순위대로 위에서부터 처음 해당되는 사실 하나를 고르고, 그 계기의 문구 은행에서 세션 ID로 하나를 돌려 쓴다(같은 세션은 언제 계산해도
 * 같은 문구). 문구 은행에는 우리가 가진 값으로 채울 수 있는 문구만 두었다. 숫자 서식은 한국어로 고정한다.
 */
@Component
public class FreeTalkSummaryCalculator {

  /** 이 백분율 이상 늘어야 "늘었다"로 본다. 그보다 작은 차이는 "비슷하다". 부동소수점 오차를 피하려고 정수로 비교한다. */
  static final int INCREASE_PERCENT = 10;

  /** 직전 스몰톡에서 이 일수 이상 지나면 "오랜만에 돌아옴"으로 본다. */
  static final int RETURN_AFTER_DAYS = 10;

  /** 이 시간 이상 말했을 때만 "N분 넘게 말했어요" 문구를 후보에 둔다. */
  static final long LONG_SPEAKING_MS = 60_000;

  /** 말한 시간이 직전의 이 배수 이상일 때만 "N배예요" 문구를 후보에 둔다. */
  static final double NOTABLE_RATIO = 1.5;

  /** 최장 턴이 직전의 이 배수 이상일 때만 "두 배 길어졌어요" 문구를 후보에 둔다. */
  static final int DOUBLED = 2;

  /**
   * 총평을 계산한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param freeTalkSessionId 총평을 낼 프리톡 세션 ID
   * @param sessionHistoryId 그 세션의 대화 기록 ID
   * @param learningSessionId 그 세션의 학습 세션 ID. 문구를 돌려 쓰는 기준이다
   * @param current 이번 세션의 재료
   * @param previous 직전 완료 스몰톡. 첫 스몰톡이면 null
   * @param previousSource 직전 세션의 재료. 첫 스몰톡이면 null
   * @return 저장할 총평
   */
  public FreeTalkSessionSummary calculate(
      long userProfileId,
      long freeTalkSessionId,
      long sessionHistoryId,
      long learningSessionId,
      FreeTalkSummarySource current,
      FreeTalkSessionSummary.PreviousSession previous,
      FreeTalkSummarySource previousSource) {
    FreeTalkSessionMetrics currentMetrics = metrics(current);
    int correctionCount = current.corrections().size();
    if (previous == null) {
      FreeTalkHeadline headline =
          headline(
              FreeTalkHeadlineTrigger.FIRST_SESSION,
              learningSessionId,
              currentMetrics,
              FreeTalkSessionMetrics.NONE,
              null,
              0,
              FreeTalkHeadlinePose.WAVE_SMILE);
      return FreeTalkSessionSummary.first(
          userProfileId,
          freeTalkSessionId,
          sessionHistoryId,
          headline,
          currentMetrics,
          correctionCount);
    }
    FreeTalkSessionMetrics previousMetrics = metrics(previousSource);
    FreeTalkGrowthCard growth = growthCard(current, previous, previousSource);
    FreeTalkHeadlineTrigger trigger =
        trigger(currentMetrics, previousMetrics, growth, previous.daysSincePrevious());
    FreeTalkHeadlinePose pose =
        growth != null && !growth.succeeded()
            ? FreeTalkHeadlinePose.NORMAL
            : FreeTalkHeadlinePose.POINT;
    FreeTalkHeadline headline =
        headline(
            trigger,
            learningSessionId,
            currentMetrics,
            previousMetrics,
            growth,
            previous.daysSincePrevious(),
            pose);
    return FreeTalkSessionSummary.compared(
        userProfileId,
        freeTalkSessionId,
        sessionHistoryId,
        previous,
        headline,
        currentMetrics,
        previousMetrics,
        growth,
        correctionCount);
  }

  // 말한 시간은 발화 시간의 합(없으면 0), 주고받은 말은 발화 수, 가장 길게 말한 턴은 공백으로 나눈 단어 수의 최댓값이다.
  static FreeTalkSessionMetrics metrics(FreeTalkSummarySource source) {
    long speakingMs = 0;
    int maxWords = 0;
    for (FreeTalkSummarySource.Utterance utterance : source.utterances()) {
      speakingMs += utterance.utteranceDurationMs() == null ? 0 : utterance.utteranceDurationMs();
      maxWords = Math.max(maxWords, wordCount(utterance.content()));
    }
    return new FreeTalkSessionMetrics(speakingMs, source.utterances().size(), maxWords);
  }

  private static int wordCount(String content) {
    String stripped = content == null ? "" : content.strip();
    return stripped.isEmpty() ? 0 : stripped.split("\\s+").length;
  }

  // 직전 세션에서 많이 틀린 순으로 지켜볼 수 있는 패턴을 보고, 오늘 등장한 첫 패턴으로 카드를 만든다. 등장이 없으면 카드가 없다.
  private static FreeTalkGrowthCard growthCard(
      FreeTalkSummarySource current,
      FreeTalkSessionSummary.PreviousSession previous,
      FreeTalkSummarySource previousSource) {
    for (FreeTalkMistakePattern pattern : previousPatternsByFrequency(previousSource)) {
      Optional<Appearance> today = todayAppearance(current, pattern);
      if (today.isEmpty()) {
        continue;
      }
      FreeTalkTurnCorrection.Sentence previousCorrection =
          latestCorrection(previousSource, pattern).orElseThrow();
      return new FreeTalkGrowthCard(
          pattern,
          today.get().correct(),
          previous.date(),
          previousCorrection.originalSentence(),
          previousCorrection.wrongSpan(),
          today.get().sentence(),
          today.get().span());
    }
    return null;
  }

  private static List<FreeTalkMistakePattern> previousPatternsByFrequency(
      FreeTalkSummarySource previousSource) {
    Map<FreeTalkMistakePattern, Integer> counts = new EnumMap<>(FreeTalkMistakePattern.class);
    previousSource.corrections().stream()
        .map(FreeTalkTurnCorrection.Sentence::mistakePattern)
        .filter(pattern -> pattern != null && pattern.isWatchable())
        .forEach(pattern -> counts.merge(pattern, 1, Integer::sum));
    return counts.entrySet().stream()
        .sorted(
            Comparator.comparing(Map.Entry<FreeTalkMistakePattern, Integer>::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey))
        .map(Map.Entry::getKey)
        .toList();
  }

  // 오늘 그 패턴으로 교정받았거나 틀린 사용례가 있으면 "또 틀림"(교정을 먼저), 맞은 사용례만 있으면 "맞음"이다.
  private static Optional<Appearance> todayAppearance(
      FreeTalkSummarySource current, FreeTalkMistakePattern pattern) {
    Optional<Appearance> corrected =
        current.corrections().stream()
            .filter(sentence -> sentence.mistakePattern() == pattern)
            .findFirst()
            .map(
                sentence ->
                    new Appearance(false, sentence.originalSentence(), sentence.wrongSpan()));
    if (corrected.isPresent()) {
      return corrected;
    }
    Optional<Appearance> wrongUsage = usage(current, pattern, usage -> !usage.correct());
    return wrongUsage.isPresent()
        ? wrongUsage
        : usage(current, pattern, FreeTalkPatternUsageDraft::correct);
  }

  private static Optional<Appearance> usage(
      FreeTalkSummarySource current,
      FreeTalkMistakePattern pattern,
      Predicate<FreeTalkPatternUsageDraft> condition) {
    return current.patternUsages().stream()
        .filter(usage -> usage.pattern() == pattern && condition.test(usage))
        .findFirst()
        .map(usage -> new Appearance(usage.correct(), usage.sentence(), usage.span()));
  }

  // 지난 문장은 그 패턴의 교정 중 취소선을 그을 구절이 있는 가장 최근 것, 없으면 가장 최근 것이다.
  private static Optional<FreeTalkTurnCorrection.Sentence> latestCorrection(
      FreeTalkSummarySource previousSource, FreeTalkMistakePattern pattern) {
    List<FreeTalkTurnCorrection.Sentence> ofPattern =
        previousSource.corrections().stream()
            .filter(sentence -> sentence.mistakePattern() == pattern)
            .toList();
    Optional<FreeTalkTurnCorrection.Sentence> withSpan =
        ofPattern.reversed().stream().filter(sentence -> sentence.wrongSpan() != null).findFirst();
    return withSpan.isPresent() ? withSpan : ofPattern.reversed().stream().findFirst();
  }

  // 기획 1-1의 우선순위. 반응 횟수(3)와 배운 표현(6)은 쓰지 않는다.
  private static FreeTalkHeadlineTrigger trigger(
      FreeTalkSessionMetrics current,
      FreeTalkSessionMetrics previous,
      FreeTalkGrowthCard growth,
      int daysSincePrevious) {
    if (daysSincePrevious >= RETURN_AFTER_DAYS) {
      return FreeTalkHeadlineTrigger.RETURN_AFTER_BREAK;
    }
    if (growth != null && growth.succeeded()) {
      return FreeTalkHeadlineTrigger.GROWTH;
    }
    if (increased(current.speakingMs(), previous.speakingMs())) {
      return FreeTalkHeadlineTrigger.SPEAKING_TIME_UP;
    }
    if (increased(current.maxWordsInTurn(), previous.maxWordsInTurn())) {
      return FreeTalkHeadlineTrigger.LONGEST_TURN_UP;
    }
    if (increased(current.turnCount(), previous.turnCount())) {
      return FreeTalkHeadlineTrigger.TURN_COUNT_UP;
    }
    // 둘 다 말한 시간이 없으면(글자로만 대화) 주고받은 말로 비슷한지 본다. 0과 0을 "지난번만큼 말했다"로 부르지 않기 위함이다.
    boolean spoke = current.speakingMs() > 0 || previous.speakingMs() > 0;
    long currentAmount = spoke ? current.speakingMs() : current.turnCount();
    long previousAmount = spoke ? previous.speakingMs() : previous.turnCount();
    if (similar(currentAmount, previousAmount)) {
      return FreeTalkHeadlineTrigger.SIMILAR;
    }
    return FreeTalkHeadlineTrigger.DECREASED;
  }

  // "늘었다"는 기준 비율 이상 늘어난 것이다. 직전이 0이면 하나라도 있으면 늘어난 것이다.
  static boolean increased(long current, long previous) {
    return current > previous && current * 100 >= previous * (100 + INCREASE_PERCENT);
  }

  // "비슷하다"는 말한 시간이 기준 비율 안에서 오르내린 것이다. 둘 다 0이어도 비슷하다.
  static boolean similar(long current, long previous) {
    return current * 100 <= previous * (100 + INCREASE_PERCENT)
        && previous * 100 <= current * (100 + INCREASE_PERCENT);
  }

  // 계기의 문구 은행에서 지금 값으로 채울 수 있는 문구만 남기고, 세션 ID로 하나를 고른다.
  private static FreeTalkHeadline headline(
      FreeTalkHeadlineTrigger trigger,
      long learningSessionId,
      FreeTalkSessionMetrics current,
      FreeTalkSessionMetrics previous,
      FreeTalkGrowthCard growth,
      int daysSincePrevious,
      FreeTalkHeadlinePose pose) {
    List<String[]> candidates = new ArrayList<>();
    switch (trigger) {
      case FIRST_SESSION -> {
        // 한 마디도 안 하고 끝난 세션에 "0번이나"라고 말하지 않는다.
        if (current.turnCount() > 0) {
          candidates.add(
              phrase(
                  "첫 스몰톡, %d번이나 주고받았어요!".formatted(current.turnCount()), "다음부턴 지난번과 비교해서 보여줄게요."));
        }
        candidates.add(phrase("첫 스몰톡 완주 축하해요!", "다음엔 오늘 얘기를 이어서 할 수 있어요."));
      }
      case RETURN_AFTER_BREAK -> {
        candidates.add(
            phrase(
                "%d일 만이네요, 감을 잃지않고 %d번 주고받았어요!".formatted(daysSincePrevious, current.turnCount()),
                "감이 안 죽었어요."));
        if (current.speakingMs() >= LONG_SPEAKING_MS) {
          candidates.add(
              phrase(
                  "오랜만인데도 %s 넘게 말했어요!".formatted(minutesText(current.speakingMs())),
                  "쉰 만큼 굳진 않았어요."));
        }
      }
      case GROWTH -> {
        String label = Objects.requireNonNull(growth).pattern().koreanLabel();
        candidates.add(
            phrase("지난번에 헷갈렸던 %s, 오늘은 다 맞았어요!".formatted(label), "한 번 틀린 걸 고치는 게 제일 어려운 건데요."));
        candidates.add(phrase("%s, 이제 안 헷갈리네요!".formatted(label), "지난 스몰톡이 헛되지 않았어요."));
        candidates.add(phrase("%s 완전 정복한 거 같은데요?".formatted(label), "지난번 얘기를 기억하고 말한 거예요."));
      }
      case SPEAKING_TIME_UP -> {
        candidates.add(
            phrase(
                "지난번보다 %s 더 말했어요!"
                    .formatted(durationText(current.speakingMs() - previous.speakingMs())),
                "할 말이 그만큼 늘었다는 거예요."));
        if (current.speakingMs() >= LONG_SPEAKING_MS) {
          candidates.add(
              phrase(
                  "오늘 %s 넘게 말했어요!".formatted(minutesText(current.speakingMs())),
                  "지난번엔 %s였어요.".formatted(durationText(previous.speakingMs()))));
        }
        if (previous.speakingMs() > 0
            && current.speakingMs() >= previous.speakingMs() * NOTABLE_RATIO) {
          candidates.add(
              phrase(
                  "말한 시간이 지난번의 %s배예요!"
                      .formatted(ratioText(current.speakingMs(), previous.speakingMs())),
                  "막힘이 줄었다는 신호예요."));
        }
      }
      case LONGEST_TURN_UP -> {
        candidates.add(
            phrase(
                "한 번에 %d단어까지 말했어요!".formatted(current.maxWordsInTurn()),
                "지난번 최장은 %d단어였어요.".formatted(previous.maxWordsInTurn())));
        if (previous.maxWordsInTurn() > 0
            && current.maxWordsInTurn() >= previous.maxWordsInTurn() * DOUBLED) {
          candidates.add(phrase("제일 긴 문장이 두 배 길어졌어요!", "막힘 없이 이어 말한 거예요."));
        }
      }
      case TURN_COUNT_UP -> {
        int diff = current.turnCount() - previous.turnCount();
        candidates.add(
            phrase(
                "%d번이나 주고받았어요!".formatted(current.turnCount()),
                "지난번보다 %d번 더 오갔어요.".formatted(diff)));
        candidates.add(phrase("대화가 지난번보다 %d번 더 이어졌어요!".formatted(diff), "끊기지 않고 받아친 거예요."));
      }
      case SIMILAR -> {
        candidates.add(phrase("지난번만큼 얘기했어요!", "꾸준한 게 제일 어려운 거예요."));
        if (current.turnCount() == previous.turnCount() && current.turnCount() > 0) {
          candidates.add(phrase("오늘도 %d번 주고받았어요!".formatted(current.turnCount()), "리듬이 잡혔어요."));
        }
      }
      case DECREASED -> candidates.add(phrase("오늘은 짧게 얘기했어요.", "짧아도 한 번 더 한 게 중요해요."));
      default -> throw new IllegalStateException("문구 은행이 없는 계기: " + trigger);
    }
    String[] chosen = candidates.get((int) Math.floorMod(learningSessionId, candidates.size()));
    return new FreeTalkHeadline(trigger, chosen[0], chosen[1], pose);
  }

  private static String[] phrase(String text, String subline) {
    return new String[] {text, subline};
  }

  // 예: 84000 → "1분 24초", 60000 → "1분", 4000 → "4초"
  static String durationText(long ms) {
    long totalSeconds = Math.round(ms / 1000.0);
    long minutes = totalSeconds / 60;
    long seconds = totalSeconds % 60;
    if (minutes == 0) {
      return seconds + "초";
    }
    return seconds == 0 ? minutes + "분" : minutes + "분 " + seconds + "초";
  }

  // 예: 245000 → "4분"
  static String minutesText(long ms) {
    return (ms / 60_000) + "분";
  }

  // 예: (245000, 161000) → "1.5", (300000, 100000) → "3"
  static String ratioText(long current, long previous) {
    double ratio = Math.floor(current * 10.0 / previous) / 10.0;
    return ratio == Math.floor(ratio)
        ? String.valueOf((long) ratio)
        : String.format(Locale.ROOT, "%.1f", ratio);
  }

  private record Appearance(boolean correct, String sentence, String span) {}
}
