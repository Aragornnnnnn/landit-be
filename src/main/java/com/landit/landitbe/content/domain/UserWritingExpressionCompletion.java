// 사용자가 완료한 Writing 표현 기록을 저장한다.
package com.landit.landitbe.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Getter
@Table(name = "user_writing_expression_completion")
public class UserWritingExpressionCompletion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "writing_expression_id", nullable = false)
    private Long writingExpressionId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "last_completed_at", nullable = false)
    private LocalDateTime lastCompletedAt;

    protected UserWritingExpressionCompletion() {
    }

    /** 사용자가 특정 시나리오의 표현 학습을 완료했음을 기록하는 엔티티를 CREATE한다. */
    public UserWritingExpressionCompletion(Long userProfileId, Long scenarioId, Long writingExpressionId) {
        LocalDateTime now = LocalDateTime.now();
        this.userProfileId = userProfileId;
        this.scenarioId = scenarioId;
        this.writingExpressionId = writingExpressionId;
        this.completedAt = now;
        this.lastCompletedAt = now;
    }

    /** 이미 완료한 표현을 다시 완료했을 때 마지막 완료 시각을 현재 시각으로 UPDATE한다. (최초 완료 시각 completedAt은 유지) */
    public void markCompletedAgain() {
        this.lastCompletedAt = LocalDateTime.now();
    }
}
