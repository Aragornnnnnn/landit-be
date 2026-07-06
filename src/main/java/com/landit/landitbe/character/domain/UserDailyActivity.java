// 사용자별 일별 학습 활동 기록을 저장한다.
package com.landit.landitbe.character.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_daily_activity")
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

    protected UserDailyActivity() {
    }
}
