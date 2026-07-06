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
}
