// UserLearningActivitySummary의 스트릭 날짜 갱신을 검증한다.

package com.landit.landitbe.feature.character.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** UserLearningActivitySummary의 스트릭 날짜 갱신을 검증한다. */
class UserLearningActivitySummaryTest {

  /** 이미 기록한 날짜 또는 과거 날짜는 현재 스트릭을 되돌리지 않는다. */
  @Test
  void ignoresSameOrOlderActivityDateWithoutRewindingStreak() {
    UserLearningActivitySummary summary = UserLearningActivitySummary.initialize(1L);
    LocalDate firstActivityDate = LocalDate.of(2026, 7, 10);
    LocalDate latestActivityDate = LocalDate.of(2026, 7, 11);
    summary.recordActiveDay(firstActivityDate);
    summary.recordActiveDay(latestActivityDate);

    summary.recordActiveDay(latestActivityDate);
    summary.recordActiveDay(firstActivityDate);

    assertThat(summary.getCurrentStreakDays()).isEqualTo(2);
    assertThat(summary.getLongestStreakDays()).isEqualTo(2);
    assertThat(summary.getLastActivityDate()).isEqualTo(latestActivityDate);
  }
}
