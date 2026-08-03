// 사용자별 누적 학습 통계와 스트릭 상태를 저장한다.

package com.landit.landitbe.feature.character.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;

/** 사용자별 누적 학습 통계와 스트릭 상태를 저장한다. */
@Entity
@Table(name = "user_learning_activity_summary")
@Getter
public class UserLearningActivitySummary extends BaseTimeEntity {

  @Id
  @Column(name = "user_profile_id")
  private Long userProfileId;

  @Column(name = "total_session_count", nullable = false)
  private int totalSessionCount;

  @Column(name = "completed_scenario_count", nullable = false)
  private int completedScenarioCount;

  @Column(name = "completed_free_talk_count", nullable = false)
  private int completedFreeTalkCount;

  @Column(name = "completed_review_count", nullable = false)
  private int completedReviewCount;

  @Column(name = "total_turn_count", nullable = false)
  private int totalTurnCount;

  @Column(name = "total_study_seconds", nullable = false)
  private int totalStudySeconds;

  @Column(name = "learned_expression_count", nullable = false)
  private int learnedExpressionCount;

  @Column(name = "average_native_score")
  private BigDecimal averageNativeScore;

  @Column(name = "current_streak_days", nullable = false)
  private int currentStreakDays;

  @Column(name = "longest_streak_days", nullable = false)
  private int longestStreakDays;

  @Column(name = "last_activity_date")
  private LocalDate lastActivityDate;

  /** JPA에서 사용하는 기본 생성자다. */
  protected UserLearningActivitySummary() {}

  // 스트릭 기록을 시작할 사용자의 학습 요약을 초기화한다.
  private UserLearningActivitySummary(Long userProfileId) {
    this.userProfileId = userProfileId;
    this.totalSessionCount = 0;
    this.completedScenarioCount = 0;
    this.completedFreeTalkCount = 0;
    this.completedReviewCount = 0;
    this.totalTurnCount = 0;
    this.totalStudySeconds = 0;
    this.learnedExpressionCount = 0;
    this.currentStreakDays = 0;
    this.longestStreakDays = 0;
  }

  /**
   * 스트릭을 처음 기록할 사용자의 요약 행을 생성한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @return 초기화한 학습 활동 요약
   */
  public static UserLearningActivitySummary initialize(Long userProfileId) {
    return new UserLearningActivitySummary(userProfileId);
  }

  /**
   * 새로운 활동일을 반영해 현재·최장 스트릭과 마지막 활동일을 갱신한다.
   *
   * @param activityDate 새 활동 날짜
   */
  public void recordActiveDay(LocalDate activityDate) {
    if (lastActivityDate != null && !activityDate.isAfter(lastActivityDate)) {
      return;
    }
    if (lastActivityDate != null && lastActivityDate.equals(activityDate.minusDays(1))) {
      currentStreakDays++;
    } else {
      currentStreakDays = 1;
    }
    longestStreakDays = Math.max(longestStreakDays, currentStreakDays);
    lastActivityDate = activityDate;
  }
}
