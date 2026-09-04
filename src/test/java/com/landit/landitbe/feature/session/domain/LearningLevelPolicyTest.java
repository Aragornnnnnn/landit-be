// 텍스트 평가 결과의 최초 수준 확정과 연속 승급 정책을 검증한다.

package com.landit.landitbe.feature.session.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LearningLevelPolicyTest {

  @Test
  void initializesUnsetLevelFromFirstModelAssessment() {
    assertThat(LearningLevelPolicy.apply(null, 0, new BigDecimal("4.20"), true))
        .isEqualTo(
            new LearningLevelPolicy.Decision(4, 0, LearningLevelPolicy.ChangeType.INITIALIZED));
  }

  @Test
  void promotesOneLevelAfterTwoConsecutiveHigherAssessments() {
    LearningLevelPolicy.Decision first =
        LearningLevelPolicy.apply(3, 0, new BigDecimal("3.70"), true);
    LearningLevelPolicy.Decision second =
        LearningLevelPolicy.apply(
            first.level(), first.promotionStreak(), new BigDecimal("4.10"), true);

    assertThat(first)
        .isEqualTo(
            new LearningLevelPolicy.Decision(3, 1, LearningLevelPolicy.ChangeType.UNCHANGED));
    assertThat(second)
        .isEqualTo(new LearningLevelPolicy.Decision(4, 0, LearningLevelPolicy.ChangeType.PROMOTED));
  }

  @Test
  void fallbackDoesNotChangeLevelOrPromotionStreak() {
    assertThat(LearningLevelPolicy.apply(3, 1, new BigDecimal("5.00"), false))
        .isEqualTo(
            new LearningLevelPolicy.Decision(3, 1, LearningLevelPolicy.ChangeType.UNCHANGED));
  }
}
