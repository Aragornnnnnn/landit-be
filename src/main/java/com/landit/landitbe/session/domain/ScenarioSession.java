// 시나리오 세션에만 필요한 정보를 저장한다.
package com.landit.landitbe.session.domain;

import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scenario_session")
public class ScenarioSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "learning_session_id", nullable = false)
    private Long learningSessionId;

    @Column(name = "scenario_language_variant_id", nullable = false)
    private Long scenarioLanguageVariantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_completion_status", length = 20)
    private GoalCompletionStatus goalCompletionStatus;

    protected ScenarioSession() {
    }

    private ScenarioSession(
            Long learningSessionId,
            Long scenarioLanguageVariantId,
            GoalCompletionStatus goalCompletionStatus
    ) {
        this.learningSessionId = learningSessionId;
        this.scenarioLanguageVariantId = scenarioLanguageVariantId;
        this.goalCompletionStatus = goalCompletionStatus;
    }

    /** 새 시나리오 세션 보조 정보를 생성한다. */
    public static ScenarioSession start(Long learningSessionId, Long scenarioLanguageVariantId) {
        return new ScenarioSession(
                learningSessionId,
                scenarioLanguageVariantId,
                GoalCompletionStatus.NOT_STARTED
        );
    }
}
