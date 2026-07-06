// 사용자별 누적 학습 통계와 스트릭 상태를 저장한다.
package com.landit.landitbe.character.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "user_learning_activity_summary")
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

    protected UserLearningActivitySummary() {
    }
}
