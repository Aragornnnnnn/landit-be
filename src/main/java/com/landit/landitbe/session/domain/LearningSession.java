// 진행 중이거나 막 종료된 학습 세션의 생명주기 정보를 저장한다.
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
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "learning_session")
public class LearningSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionType sessionType;

    @Column(name = "ai_tutor_id", nullable = false)
    private Long aiTutorId;

    @Column(name = "target_locale", nullable = false, length = 35)
    private String targetLocale;

    @Column(name = "base_locale", nullable = false, length = 35)
    private String baseLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_mode", nullable = false, length = 20)
    private InputMode inputMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearningSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "ended_by", length = 20)
    private SessionEndActor endedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_reason", length = 30)
    private CompletionReason completionReason;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected LearningSession() {
    }

    private LearningSession(
            Long userProfileId,
            SessionType sessionType,
            Long aiTutorId,
            String targetLocale,
            String baseLocale,
            InputMode inputMode,
            LearningSessionStatus status,
            LocalDateTime startedAt
    ) {
        this.userProfileId = userProfileId;
        this.sessionType = sessionType;
        this.aiTutorId = aiTutorId;
        this.targetLocale = targetLocale;
        this.baseLocale = baseLocale;
        this.inputMode = inputMode;
        this.status = status;
        this.startedAt = startedAt;
    }

    /** 시나리오 학습 세션을 진행 중 상태로 생성한다. */
    public static LearningSession startScenario(
            Long userProfileId,
            Long aiTutorId,
            String targetLocale,
            String baseLocale,
            LocalDateTime startedAt
    ) {
        return new LearningSession(
                userProfileId,
                SessionType.SCENARIO,
                aiTutorId,
                targetLocale,
                baseLocale,
                InputMode.MIXED,
                LearningSessionStatus.IN_PROGRESS,
                startedAt
        );
    }

    /** 사용자가 진행 중인 세션을 중도 종료한다. */
    public void interruptByUser(LocalDateTime endedAt) {
        this.status = LearningSessionStatus.INTERRUPTED;
        this.endedBy = SessionEndActor.USER;
        this.completionReason = CompletionReason.USER_ENDED;
        this.endedAt = endedAt;
    }

    /** 시스템이 목표 달성 또는 최대 턴 도달로 세션을 완료한다. */
    public void completeBySystem(CompletionReason completionReason, LocalDateTime endedAt) {
        this.status = LearningSessionStatus.COMPLETED;
        this.endedBy = SessionEndActor.SYSTEM;
        this.completionReason = completionReason;
        this.endedAt = endedAt;
    }

    /** 세션이 진행 중인지 반환한다. */
    public boolean isInProgress() {
        return status == LearningSessionStatus.IN_PROGRESS;
    }

}
