// 최초 1회 수준 확정과 이후 연속 승급·무효 결과 보존 정책을 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LearningLevelPolicyTest {

  @Test
  void initializesUnsetLevelFromFirstModelAssessment() {
    assertThat(
            LearningLevelPolicy.apply(null, 0, new BigDecimal("4.20"), BigDecimal.ONE, true, false))
        .isEqualTo(
            new LearningLevelPolicy.Decision(4, 0, LearningLevelPolicy.ChangeType.INITIALIZED));
  }

  @ParameterizedTest
  @CsvSource({"4, 2.84, 3", "5, 1.20, 1", "1, 4.70, 5", "3, 3.50, 4", "3, 3.49, 3"})
  void firstAssessmentReplacesSelectedLevelRegardlessOfDirection(
      int previous, BigDecimal score, int expected) {
    assertThat(LearningLevelPolicy.apply(previous, 1, score, new BigDecimal("0.75"), true, false))
        .isEqualTo(
            new LearningLevelPolicy.Decision(
                expected, 0, LearningLevelPolicy.ChangeType.INITIALIZED));
  }

  @ParameterizedTest
  @CsvSource({
    "3, 0, 3.70, 3, 1, UNCHANGED",
    "3, 1, 3.70, 4, 0, PROMOTED",
    "1, 1, 5.00, 2, 0, PROMOTED",
    "3, 1, 3.69, 3, 0, UNCHANGED",
    "4, 1, 1.00, 4, 0, UNCHANGED",
    "5, 1, 5.00, 5, 0, UNCHANGED"
  })
  void subsequentAssessmentUsesOriginalPromotionPolicy(
      int current,
      int streak,
      BigDecimal score,
      int expected,
      int expectedStreak,
      LearningLevelPolicy.ChangeType changeType) {
    assertThat(
            LearningLevelPolicy.apply(current, streak, score, new BigDecimal("0.75"), true, true))
        .isEqualTo(new LearningLevelPolicy.Decision(expected, expectedStreak, changeType));
  }

  @Test
  void fallbackDoesNotChangeLevelOrPromotionStreak() {
    assertThat(
            LearningLevelPolicy.apply(3, 1, new BigDecimal("5.00"), BigDecimal.ZERO, false, true))
        .isEqualTo(
            new LearningLevelPolicy.Decision(3, 1, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }

  @Test
  void fallbackDoesNotInitializeUnsetLevel() {
    assertThat(
            LearningLevelPolicy.apply(
                null, 0, new BigDecimal("3.00"), BigDecimal.ZERO, false, false))
        .isEqualTo(
            new LearningLevelPolicy.Decision(null, 0, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }

  @Test
  void lowConfidenceAssessmentDoesNotAdvancePromotionStreak() {
    assertThat(
            LearningLevelPolicy.apply(
                3, 0, new BigDecimal("4.00"), new BigDecimal("0.74"), true, true))
        .isEqualTo(
            new LearningLevelPolicy.Decision(3, 0, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }

  @Test
  void lowCoverageCannotInitializeLevel() {
    assertThat(
            LearningLevelPolicy.apply(
                null, 0, new BigDecimal("5.00"), new BigDecimal("0.50"), true, false))
        .isEqualTo(
            new LearningLevelPolicy.Decision(null, 0, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }

  @Test
  void insufficientEvidencePreservesExistingPromotionSignal() {
    assertThat(
            LearningLevelPolicy.apply(
                3, 1, new BigDecimal("5.00"), new BigDecimal("0.50"), true, true))
        .isEqualTo(
            new LearningLevelPolicy.Decision(3, 1, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }

  @Test
  void nullConfidenceDoesNotReplaceSelectedLevel() {
    assertThat(LearningLevelPolicy.apply(4, 0, new BigDecimal("3.00"), null, true, false))
        .isEqualTo(
            new LearningLevelPolicy.Decision(4, 0, LearningLevelPolicy.ChangeType.NOT_APPLIED));
  }
}
