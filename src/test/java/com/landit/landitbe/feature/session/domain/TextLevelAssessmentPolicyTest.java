// 텍스트 회화 수준의 가중 계산과 관찰 상한을 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.domain.ResponseDemand;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextLevelAssessmentPolicyTest {

  @Test
  void calculatesDemandWeightedDomainsAndCapsOnlyOverallScore() {
    TextLevelAssessmentPolicy.Score score =
        TextLevelAssessmentPolicy.calculate(
                List.of(observation(ResponseDemand.LOW, 5), observation(ResponseDemand.HIGH, 3)),
                ContentLearningLevel.LEVEL_1)
            .orElseThrow();

    assertThat(score.situationPerformance().score()).isEqualByComparingTo("3.52");
    assertThat(score.situationPerformance().confidence()).isEqualByComparingTo("1.00");
    assertThat(score.grammar().score()).isEqualByComparingTo("3.52");
    assertThat(score.overallScore()).isEqualByComparingTo("2.00");
    assertThat(score.assessedLevel()).isEqualTo(2);
  }

  @Test
  void preservesSingleAnswerForDisplayWithoutTreatingItAsSufficient() {
    var score =
        TextLevelAssessmentPolicy.calculate(
                List.of(observation(ResponseDemand.HIGH, 5)), ContentLearningLevel.LEVEL_4_TO_5)
            .orElseThrow();
    assertThat(score.grammar().score()).isEqualByComparingTo("5.00");
    assertThat(score.sufficientEvidence()).isFalse();
  }

  @Test
  void preservesObservedDomainsWhenPragmaticsWasNotObserved() {
    var answer = new TextLevelAssessmentPolicy.Observation(ResponseDemand.HIGH, 3, 3, 3, 3, null);
    var score =
        TextLevelAssessmentPolicy.calculate(
                List.of(answer, answer), ContentLearningLevel.LEVEL_4_TO_5)
            .orElseThrow();
    assertThat(score.grammar().score()).isEqualByComparingTo("3.00");
    assertThat(score.interactionPragmatics().score()).isNull();
    assertThat(score.overallScore()).isNull();
    assertThat(score.assessedLevel()).isNull();
    assertThat(score.sufficientEvidence()).isFalse();
  }

  @Test
  void calculatesOverallConfidenceFromObservedAnswerWeight() {
    TextLevelAssessmentPolicy.Score score =
        TextLevelAssessmentPolicy.calculate(
                List.of(
                    observation(ResponseDemand.HIGH, 4),
                    new TextLevelAssessmentPolicy.Observation(
                        ResponseDemand.HIGH, null, null, null, null, null)),
                ContentLearningLevel.LEVEL_4_TO_5)
            .orElseThrow();

    assertThat(score.overallConfidence()).isEqualByComparingTo(new BigDecimal("0.50"));
    assertThat(score.sufficientEvidence()).isFalse();
  }

  @Test
  void requiresEnoughObservationsInEachDomainRatherThanOnlyOverallCoverage() {
    var complete = observation(ResponseDemand.HIGH, 4);
    var partial = new TextLevelAssessmentPolicy.Observation(ResponseDemand.HIGH, 4, 4, 4, 4, null);
    var score =
        TextLevelAssessmentPolicy.calculate(
                List.of(complete, partial), ContentLearningLevel.LEVEL_4_TO_5)
            .orElseThrow();
    assertThat(score.overallConfidence()).isEqualByComparingTo("0.93");
    assertThat(score.sufficientEvidence()).isFalse();
    assertThat(
            TextLevelAssessmentPolicy.calculate(
                    List.of(complete, complete), ContentLearningLevel.LEVEL_4_TO_5)
                .orElseThrow()
                .sufficientEvidence())
        .isTrue();
  }

  private TextLevelAssessmentPolicy.Observation observation(ResponseDemand demand, int level) {
    return new TextLevelAssessmentPolicy.Observation(demand, level, level, level, level, level);
  }
}
