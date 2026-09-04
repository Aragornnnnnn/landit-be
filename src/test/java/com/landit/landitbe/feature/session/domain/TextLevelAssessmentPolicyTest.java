// 텍스트 회화 수준의 가중 계산과 관찰 상한을 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.domain.ResponseDemand;
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
  void requiresTwoAnswersAndEveryDomainObservation() {
    assertThat(
            TextLevelAssessmentPolicy.calculate(
                List.of(observation(ResponseDemand.HIGH, 5)), ContentLearningLevel.LEVEL_4_TO_5))
        .isEmpty();
  }

  private TextLevelAssessmentPolicy.Observation observation(ResponseDemand demand, int level) {
    return new TextLevelAssessmentPolicy.Observation(demand, level, level, level, level, level);
  }
}
