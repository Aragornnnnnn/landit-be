// 최신 평가가 기존 수준을 즉시 대체하고 무효 결과는 보존하는지 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LearningLevelPolicyTest {

  @Test
  void initializesUnsetLevelFromFirstModelAssessment() {
    assertThat(LearningLevelPolicy.apply(null, 0, new BigDecimal("4.20"), BigDecimal.ONE, true))
        .isEqualTo(
            new LearningLevelPolicy.Decision(4, 0, LearningLevelPolicy.ChangeType.INITIALIZED));
  }

  @ParameterizedTest
  @CsvSource({
    "4, 2.84, 3, DEMOTED",
    "5, 1.20, 1, DEMOTED",
    "1, 4.70, 5, PROMOTED",
    "3, 3.50, 4, PROMOTED",
    "3, 3.49, 3, UNCHANGED"
  })
  void replacesExistingLevelWithoutWaitingForConsecutiveEvidence(
      int previous, BigDecimal score, int expected, LearningLevelPolicy.ChangeType changeType) {
    assertThat(LearningLevelPolicy.apply(previous, 1, score, new BigDecimal("0.75"), true))
        .isEqualTo(new LearningLevelPolicy.Decision(expected, 0, changeType));
  }

  @Test
  void fallbackDoesNotChangeLevelOrPromotionStreak() {
    assertThat(LearningLevelPolicy.apply(3, 1, new BigDecimal("5.00"), BigDecimal.ZERO, false))
        .isEqualTo(
            new LearningLevelPolicy.Decision(3, 1, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }

  @Test
  void fallbackDoesNotInitializeUnsetLevel() {
    assertThat(LearningLevelPolicy.apply(null, 0, new BigDecimal("3.00"), BigDecimal.ZERO, false))
        .isEqualTo(
            new LearningLevelPolicy.Decision(null, 0, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }

  @Test
  void lowConfidenceAssessmentDoesNotAdvancePromotionStreak() {
    assertThat(
            LearningLevelPolicy.apply(3, 0, new BigDecimal("4.00"), new BigDecimal("0.74"), true))
        .isEqualTo(
            new LearningLevelPolicy.Decision(3, 0, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }

  @Test
  void lowCoverageCannotInitializeLevel() {
    assertThat(
            LearningLevelPolicy.apply(
                null, 0, new BigDecimal("5.00"), new BigDecimal("0.50"), true))
        .isEqualTo(
            new LearningLevelPolicy.Decision(null, 0, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }

  @Test
  void insufficientEvidencePreservesExistingPromotionSignal() {
    assertThat(
            LearningLevelPolicy.apply(3, 1, new BigDecimal("5.00"), new BigDecimal("0.50"), true))
        .isEqualTo(
            new LearningLevelPolicy.Decision(3, 1, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }
}
