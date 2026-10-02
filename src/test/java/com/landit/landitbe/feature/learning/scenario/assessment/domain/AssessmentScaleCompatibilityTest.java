// 기존 평가 반올림과 관찰 상한 및 승급 경계를 보존하는지 검증한다.

package com.landit.landitbe.feature.learning.scenario.assessment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.scenario.question.domain.ResponseDemand;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AssessmentScaleCompatibilityTest {
  @DisplayName("기존 척도는 3.2점을 레벨 3으로 반올림하고 신규 척도와 혼동하지 않는다.")
  @Test
  void legacyAggregationPreservesOriginalRounding() {
    var observation = new TextLevelAssessmentPolicy.Observation(ResponseDemand.HIGH, 3, 4, 3, 3, 3);
    var result =
        TextLevelAssessmentPolicy.calculate(
                List.of(observation, observation),
                ContentLearningLevel.DIAGNOSTIC,
                AssessmentScale.LEGACY)
            .orElseThrow();
    assertThat(result.overallScore()).isEqualByComparingTo("3.20");
    assertThat(result.assessedLevel()).isEqualTo(3);
    assertThat(result.sufficientEvidence()).isTrue();
    var caps =
        java.util.Map.of(
            ContentLearningLevel.LEVEL_1, 2,
            ContentLearningLevel.LEVEL_2_TO_3, 4,
            ContentLearningLevel.LEVEL_4_TO_5, 5,
            ContentLearningLevel.DIAGNOSTIC, 5);
    for (ContentLearningLevel group : ContentLearningLevel.values()) {
      var high = new TextLevelAssessmentPolicy.Observation(ResponseDemand.HIGH, 5, 5, 5, 5, 5);
      var capped =
          TextLevelAssessmentPolicy.calculate(List.of(high, high), group, AssessmentScale.LEGACY)
              .orElseThrow();
      assertThat(capped.overallScore()).isEqualByComparingTo(BigDecimal.valueOf(caps.get(group)));
    }
  }

  @DisplayName("척도마다 기존 승급 경계와 2회 연속 규칙을 유지한다.")
  @ParameterizedTest
  @CsvSource({"LEGACY,3.69,false", "LEGACY,3.70,true", "SCORE,63.99,false", "SCORE,64,true"})
  void promotionBoundaryIsScaleSpecific(AssessmentScale scale, BigDecimal score, boolean promoted) {
    var decision = LearningLevelPolicy.apply(3, 1, score, BigDecimal.ONE, true, true, scale);
    assertThat(decision.changeType())
        .isEqualTo(
            promoted
                ? LearningLevelPolicy.ChangeType.PROMOTED
                : LearningLevelPolicy.ChangeType.UNCHANGED);
    assertThat(decision.level()).isEqualTo(promoted ? 4 : 3);
  }
}
