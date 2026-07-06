// 사용자에게 할당된 퀘스트와 진행 상태를 저장한다.
package com.landit.landitbe.quest.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.landit.landitbe.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_quest")
public class UserQuest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    @Column(name = "quest_template_id", nullable = false)
    private Long questTemplateId;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(name = "current_count", nullable = false)
    private int currentCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "progress_payload", columnDefinition = "jsonb")
    private JsonNode progressPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserQuestStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "reward_granted_at")
    private LocalDateTime rewardGrantedAt;

    protected UserQuest() {
    }
}
