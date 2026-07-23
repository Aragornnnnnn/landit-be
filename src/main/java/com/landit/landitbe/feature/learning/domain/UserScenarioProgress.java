// 사용자별 시나리오 진행 상태와 최고 성과를 저장한다.

package com.landit.landitbe.feature.learning.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import com.landit.landitbe.shared.domain.Locale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;

/** 사용자별 시나리오 진행 상태와 최고 성과를 저장한다. */
@Getter
@Entity
@Table(name = "user_scenario_progress")
public class UserScenarioProgress extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(name = "scenario_id", nullable = false)
  private Long scenarioId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_locale", nullable = false, length = 35)
  private Locale targetLocale;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserScenarioProgressStatus status;

  @Column(name = "best_star_rating")
  private BigDecimal bestStarRating;

  @Column(name = "best_native_score")
  private Integer bestNativeScore;

  @Column(name = "completed_count", nullable = false)
  private int completedCount;

  @Column(name = "first_cleared_at")
  private LocalDateTime firstClearedAt;

  @Column(name = "last_played_at")
  private LocalDateTime lastPlayedAt;

  /** 동작을 수행한다. */
  protected UserScenarioProgress() {}

  private UserScenarioProgress(
      Long userProfileId,
      Long scenarioId,
      Locale targetLocale,
      UserScenarioProgressStatus status,
      int completedCount,
      LocalDateTime lastPlayedAt) {
    this.userProfileId = userProfileId;
    this.scenarioId = scenarioId;
    this.targetLocale = targetLocale;
    this.status = status;
    this.completedCount = completedCount;
    this.lastPlayedAt = lastPlayedAt;
  }

  /** 사용자가 시나리오를 처음 시작한 진행도 row를 생성한다. */
  public static UserScenarioProgress start(
      Long userProfileId, Long scenarioId, Locale targetLocale, LocalDateTime startedAt) {
    return new UserScenarioProgress(
        userProfileId,
        scenarioId,
        targetLocale,
        UserScenarioProgressStatus.IN_PROGRESS,
        0,
        startedAt);
  }

  /** 재시도 시작 시 기존 최고 성과를 유지하면서 최근 플레이 시간을 갱신한다. */
  public void markStarted(LocalDateTime startedAt) {
    if (status != UserScenarioProgressStatus.CLEARED) {
      status = UserScenarioProgressStatus.IN_PROGRESS;
    }
    lastPlayedAt = startedAt;
  }

  /** 시나리오 완료 결과로 진행도와 최고 성과를 갱신한다. */
  public void complete(BigDecimal starRating, int nativeScore, LocalDateTime endedAt) {
    status = UserScenarioProgressStatus.CLEARED;
    completedCount += 1;
    if (firstClearedAt == null) {
      firstClearedAt = endedAt;
    }
    lastPlayedAt = endedAt;
    if (bestNativeScore == null || nativeScore > bestNativeScore) {
      bestNativeScore = nativeScore;
      bestStarRating = starRating;
    }
  }
}
