// 질문별 관찰값을 텍스트 회화 수준 점수로 계산한다.

package com.landit.landitbe.feature.learning.scenario.assessment.domain;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.scenario.question.domain.ResponseDemand;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** 질문 난이도와 영역 가중치를 적용해 세션의 텍스트 수준을 계산한다. */
public final class TextLevelAssessmentPolicy {

  private TextLevelAssessmentPolicy() {}

  /** 한 질문에서 관찰된 다섯 영역 수준이다. 관찰하지 못한 영역은 {@code null}이다. */
  public record Observation(
      ResponseDemand responseDemand,
      Integer situationPerformance,
      Integer grammar,
      Integer vocabulary,
      Integer discourse,
      Integer interactionPragmatics) {}

  /** 사용자에게 제공할 세션 단위 다섯 영역 점수와 종합 수준이다. */
  public record DomainScore(BigDecimal score, BigDecimal confidence, int observedCount) {
    boolean sufficientEvidence() {
      return observedCount >= 2 && confidence.compareTo(new BigDecimal("0.75")) >= 0;
    }
  }

  /** 사용자에게 제공할 세션 단위 다섯 영역 점수와 종합 수준이다. */
  public record Score(
      DomainScore situationPerformance,
      DomainScore grammar,
      DomainScore vocabulary,
      DomainScore discourse,
      DomainScore interactionPragmatics,
      BigDecimal overallScore,
      BigDecimal overallConfidence,
      Integer assessedLevel) {
    /** 모든 영역에 서로 다른 답변 두 개 이상과 충분한 관찰 비율이 있는지 반환한다. */
    public boolean sufficientEvidence() {
      return List.of(situationPerformance, grammar, vocabulary, discourse, interactionPragmatics)
          .stream()
          .allMatch(DomainScore::sufficientEvidence);
    }
  }

  /**
   * 관찰된 영역을 보존하고, 다섯 영역이 관찰됐을 때만 100점 종합 점수를 계산한다.
   *
   * @param observations 질문 요구 난이도와 영역별 1~100점 관찰값
   * @param questionLevelGroup 종합 점수의 관찰 상한을 결정하는 질문 그룹
   * @return 소수 둘째 자리로 반올림한 평가. 관찰 목록이 없으면 빈 값
   */
  public static Optional<Score> calculate(
      List<Observation> observations, ContentLearningLevel questionLevelGroup) {
    return calculate(observations, questionLevelGroup, AssessmentScale.SCORE);
  }

  /**
   * 실제 응답 척도의 집계와 관찰 상한을 적용한다.
   *
   * @param observations 질문별 관찰값
   * @param questionLevelGroup 질문 난이도 그룹
   * @param scale 응답 척도
   * @return 해당 척도를 유지한 집계 결과
   */
  public static Optional<Score> calculate(
      List<Observation> observations,
      ContentLearningLevel questionLevelGroup,
      AssessmentScale scale) {
    if (observations == null || observations.isEmpty()) {
      return Optional.empty();
    }
    DomainScore situation = average(observations, Observation::situationPerformance, scale);
    DomainScore grammar = average(observations, Observation::grammar, scale);
    DomainScore vocabulary = average(observations, Observation::vocabulary, scale);
    DomainScore discourse = average(observations, Observation::discourse, scale);
    DomainScore interaction = average(observations, Observation::interactionPragmatics, scale);
    boolean complete =
        List.of(situation, grammar, vocabulary, discourse, interaction).stream()
            .allMatch(domain -> domain.score() != null);
    BigDecimal rawOverall =
        !complete
            ? null
            : situation
                .score()
                .multiply(new BigDecimal("0.30"))
                .add(grammar.score().multiply(new BigDecimal("0.20")))
                .add(vocabulary.score().multiply(new BigDecimal("0.20")))
                .add(discourse.score().multiply(new BigDecimal("0.15")))
                .add(interaction.score().multiply(new BigDecimal("0.15")));
    BigDecimal overall =
        rawOverall == null
            ? null
            : rawOverall
                .min(scale.observationCap(questionLevelGroup))
                .setScale(2, RoundingMode.HALF_UP);
    BigDecimal overallConfidence =
        situation
            .confidence()
            .multiply(new BigDecimal("0.30"))
            .add(grammar.confidence().multiply(new BigDecimal("0.20")))
            .add(vocabulary.confidence().multiply(new BigDecimal("0.20")))
            .add(discourse.confidence().multiply(new BigDecimal("0.15")))
            .add(interaction.confidence().multiply(new BigDecimal("0.15")))
            .setScale(2, RoundingMode.HALF_UP);
    Integer assessedLevel = overall == null ? null : scale.levelForScore(overall);
    return Optional.of(
        new Score(
            situation,
            grammar,
            vocabulary,
            discourse,
            interaction,
            overall,
            overallConfidence,
            assessedLevel));
  }

  /**
   * 유효 점수만 난이도로 가중 평균하되 미관찰 질문도 근거 비율의 분모에 포함한다.
   *
   * @param observations 질문별 영역 관찰값
   * @param level 집계할 영역의 점수 추출 함수
   * @param scale 평가 척도
   * @return 영역 점수, 관찰 난이도 비율 및 유효 관찰 수
   */
  private static DomainScore average(
      List<Observation> observations, Function<Observation, Integer> level, AssessmentScale scale) {
    BigDecimal weightedLevels = BigDecimal.ZERO;
    BigDecimal observedWeights = BigDecimal.ZERO;
    BigDecimal totalWeights = BigDecimal.ZERO;
    int observedCount = 0;
    for (Observation observation : observations) {
      if (observation.responseDemand() == null) {
        continue;
      }
      totalWeights = totalWeights.add(observation.responseDemand().weight());
      Integer value = level.apply(observation);
      if (value == null || value < 1 || value > scale.maximum()) {
        continue;
      }
      weightedLevels =
          weightedLevels.add(
              observation.responseDemand().weight().multiply(BigDecimal.valueOf(value)));
      observedWeights = observedWeights.add(observation.responseDemand().weight());
      observedCount++;
    }
    return observedWeights.signum() == 0
        ? new DomainScore(null, BigDecimal.ZERO.setScale(2), 0)
        : new DomainScore(
            weightedLevels.divide(observedWeights, 2, RoundingMode.HALF_UP),
            observedWeights.divide(totalWeights, 2, RoundingMode.HALF_UP),
            observedCount);
  }
}
