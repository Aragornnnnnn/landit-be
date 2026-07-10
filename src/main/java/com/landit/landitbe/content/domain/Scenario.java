// 언어와 무관한 시나리오 원형 정보를 저장한다.
package com.landit.landitbe.content.domain;

import com.landit.landitbe.common.domain.ActiveStatus;
import com.landit.landitbe.common.domain.BaseTimeEntity;
import com.landit.landitbe.common.domain.ConversationSpeaker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scenario")
public class Scenario extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "ai_role", nullable = false, length = 80)
    private String aiRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScenarioDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "first_speaker", nullable = false, length = 20)
    private ConversationSpeaker firstSpeaker;

    @Column(name = "total_question_count", nullable = false)
    private int totalQuestionCount;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActiveStatus status;

    protected Scenario() {
    }
}
