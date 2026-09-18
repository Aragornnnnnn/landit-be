// 텍스트 회화 수준의 가중 계산과 관찰 상한을 검증한다.

package com.landit.landitbe.feature.learning.scenario.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.scenario.question.domain.ResponseDemand;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextLevelAssessmentPolicyTest {

  @DisplayName("진단 질문으로 전체 학습 수준 범위를 평가할 수 있다.")
  @Test
  void diagnosticQuestionsCanAssessTheFullLevelRange() {
    for (int level = 1; level <= 5; level++) {
      var score =
          TextLevelAssessmentPolicy.calculate(
                  List.of(
                      observation(ResponseDemand.MEDIUM, level),
                      observation(ResponseDemand.MEDIUM, level),
                      observation(ResponseDemand.HIGH, level),
                      observation(ResponseDemand.HIGH, level)),
                  ContentLearningLevel.DIAGNOSTIC)
              .orElseThrow();
      assertThat(score.assessedLevel()).isEqualTo(level);
      assertThat(score.sufficientEvidence()).isTrue();
    }
  }

  @DisplayName("요구 난이도로 영역별 점수를 가중 계산하고 전체 점수에만 상한을 적용한다.")
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

  @DisplayName("답변 하나의 평가는 화면 표시용으로 보존하되 충분한 근거로 보지 않는다.")
  @Test
  void preservesSingleAnswerForDisplayWithoutTreatingItAsSufficient() {
    var score =
        TextLevelAssessmentPolicy.calculate(
                List.of(observation(ResponseDemand.HIGH, 5)), ContentLearningLevel.LEVEL_4_TO_5)
            .orElseThrow();
    assertThat(score.grammar().score()).isEqualByComparingTo("5.00");
    assertThat(score.sufficientEvidence()).isFalse();
  }

  @DisplayName("화용 영역을 관측하지 못해도 다른 관측 영역의 결과를 보존한다.")
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

  @DisplayName("관측한 답변의 가중치로 전체 평가 신뢰도를 계산한다.")
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

  @DisplayName("전체 관측률뿐 아니라 각 영역에도 충분한 관측 수를 요구한다.")
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
