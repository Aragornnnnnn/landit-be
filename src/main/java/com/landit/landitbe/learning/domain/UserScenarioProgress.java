// 사용자별 시나리오 진행 상태와 최고 성과를 저장한다.
package com.landit.landitbe.learning.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

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

    @Column(name = "target_locale", nullable = false, length = 35)
    private String targetLocale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserScenarioProgressStatus status;

    @Column(name = "best_star_rating")
    private Integer bestStarRating;

    @Column(name = "best_native_score")
    private Integer bestNativeScore;

    @Column(name = "completed_count", nullable = false)
    private int completedCount;

    @Column(name = "first_cleared_at")
    private LocalDateTime firstClearedAt;

    @Column(name = "last_played_at")
    private LocalDateTime lastPlayedAt;

    protected UserScenarioProgress() {
    }
}
