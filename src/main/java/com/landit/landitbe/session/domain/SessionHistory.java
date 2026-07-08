// 기록 탭과 장기 보관을 위한 학습 결과 이력을 저장한다.
package com.landit.landitbe.session.domain;

import com.landit.landitbe.common.domain.BaseCreatedAtEntity;
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
@Table(name = "session_history")
public class SessionHistory extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "learning_session_id")
    private Long learningSessionId;

    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private SessionType sessionType;

    @Column(name = "target_locale", nullable = false, length = 35)
    private String targetLocale;

    @Column(name = "base_locale", nullable = false, length = 35)
    private String baseLocale;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "user_message_count", nullable = false)
    private int userMessageCount;

    @Column(name = "xp_reward")
    private Integer xpReward;

    protected SessionHistory() {
    }

    private SessionHistory(
            Long learningSessionId,
            Long userProfileId,
            SessionType sessionType,
            String targetLocale,
            String baseLocale,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            int durationSeconds,
            int userMessageCount
    ) {
        this.learningSessionId = learningSessionId;
        this.userProfileId = userProfileId;
        this.sessionType = sessionType;
        this.targetLocale = targetLocale;
        this.baseLocale = baseLocale;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
        this.userMessageCount = userMessageCount;
    }

    /** 시작 메시지를 저장할 세션 히스토리 컨테이너를 생성한다. */
    public static SessionHistory startedScenario(
            Long learningSessionId,
            Long userProfileId,
            String targetLocale,
            String baseLocale,
            LocalDateTime startedAt
    ) {
        return new SessionHistory(
                learningSessionId,
                userProfileId,
                SessionType.SCENARIO,
                targetLocale,
                baseLocale,
                startedAt,
                startedAt,
                0,
                0
        );
    }

}
