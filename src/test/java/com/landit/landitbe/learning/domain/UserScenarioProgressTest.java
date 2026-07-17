// 사용자 시나리오 진행도의 완료 성과 갱신을 검증한다.

package com.landit.landitbe.learning.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.common.domain.Locale;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** 사용자 시나리오 진행도의 완료 성과 갱신을 검증한다. */
class UserScenarioProgressTest {

  @Test
  void completeSetsFirstClearAndBestPerformance() {
    UserScenarioProgress progress =
        UserScenarioProgress.start(1L, 2L, Locale.EN, LocalDateTime.of(2026, 7, 12, 10, 0));
    LocalDateTime endedAt = LocalDateTime.of(2026, 7, 12, 10, 15);

    progress.complete(new BigDecimal("2.5"), 75, endedAt);

    assertThat(progress.getStatus()).isEqualTo(UserScenarioProgressStatus.CLEARED);
    assertThat(progress.getCompletedCount()).isEqualTo(1);
    assertThat(progress.getFirstClearedAt()).isEqualTo(endedAt);
    assertThat(progress.getLastPlayedAt()).isEqualTo(endedAt);
    assertThat(progress.getBestNativeScore()).isEqualTo(75);
    assertThat(progress.getBestStarRating()).isEqualByComparingTo("2.5");
  }

  @Test
  void completeKeepsFirstClearAndBestPerformanceWhenScoreIsLower() {
    UserScenarioProgress progress =
        UserScenarioProgress.start(1L, 2L, Locale.EN, LocalDateTime.of(2026, 7, 12, 10, 0));
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 7, 12, 10, 15);
    LocalDateTime secondEndedAt = LocalDateTime.of(2026, 7, 12, 11, 15);
    progress.complete(new BigDecimal("3.0"), 90, firstEndedAt);

    progress.complete(new BigDecimal("2.0"), 60, secondEndedAt);

    assertThat(progress.getCompletedCount()).isEqualTo(2);
    assertThat(progress.getFirstClearedAt()).isEqualTo(firstEndedAt);
    assertThat(progress.getLastPlayedAt()).isEqualTo(secondEndedAt);
    assertThat(progress.getBestNativeScore()).isEqualTo(90);
    assertThat(progress.getBestStarRating()).isEqualByComparingTo("3.0");
  }

  @Test
  void completeKeepsExistingBestStarRatingWhenScoreIsEqual() {
    UserScenarioProgress progress =
        UserScenarioProgress.start(1L, 2L, Locale.EN, LocalDateTime.of(2026, 7, 12, 10, 0));
    progress.complete(new BigDecimal("3.0"), 90, LocalDateTime.of(2026, 7, 12, 10, 15));

    progress.complete(new BigDecimal("2.5"), 90, LocalDateTime.of(2026, 7, 12, 11, 15));

    assertThat(progress.getBestNativeScore()).isEqualTo(90);
    assertThat(progress.getBestStarRating()).isEqualByComparingTo("3.0");
  }
}
