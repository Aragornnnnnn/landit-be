// 사용자별 일별 학습 활동 기록을 저장한다.

package com.landit.landitbe.feature.character.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

/** 사용자별 일별 학습 활동 기록을 저장한다. */
@Entity
@Table(name = "user_daily_activity")
@Getter
public class UserDailyActivity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_profile_id", nullable = false)
  private Long userProfileId;

  @Column(name = "activity_date", nullable = false)
  private LocalDate activityDate;

  @Column(name = "completed_session_count", nullable = false)
  private int completedSessionCount;

  @Column(name = "completed_review_count", nullable = false)
  private int completedReviewCount;

  @Column(name = "study_seconds", nullable = false)
  private int studySeconds;

  @Column(name = "review_all_correct_reward_xp", nullable = false)
  private int reviewAllCorrectRewardXp;

  @Column(name = "review_all_correct_reward_granted_at")
  private LocalDateTime reviewAllCorrectRewardGrantedAt;

  @Column(name = "active_day", nullable = false)
  private boolean activeDay;

  /** JPA에서 사용하는 기본 생성자다. */
  protected UserDailyActivity() {}

  private UserDailyActivity(Long userProfileId, LocalDate activityDate) {
    this.userProfileId = userProfileId;
    this.activityDate = activityDate;
    this.completedSessionCount = 1;
    this.completedReviewCount = 0;
    this.studySeconds = 0;
    this.reviewAllCorrectRewardXp = 0;
    this.activeDay = true;
  }

  /**
   * 사용자의 첫 일별 대화 완료 활동을 생성한다.
   *
   * @param userProfileId 사용자 프로필 ID
   * @param activityDate 활동 날짜
   * @return 첫 완료를 반영한 일별 활동
   */
  public static UserDailyActivity startActiveDay(Long userProfileId, LocalDate activityDate) {
    return new UserDailyActivity(userProfileId, activityDate);
  }

  /** 같은 날짜에 정상 완료한 대화 횟수를 증가시킨다. */
  public void completeSession() {
    completedSessionCount++;
    activeDay = true;
  }
}
