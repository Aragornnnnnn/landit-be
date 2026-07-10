// 사용자에게 복습 후보가 된 개선 표현과 학습 상태를 저장한다.
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
import com.landit.landitbe.common.domain.Locale;

@Entity
@Table(name = "user_learning_expression")
public class UserLearningExpression extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_profile_id", nullable = false)
    private Long userProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_locale", nullable = false, length = 35)
    private Locale targetLocale;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_locale", nullable = false, length = 35)
    private Locale baseLocale;

    @Column(name = "session_history_message_feedback_id", nullable = false)
    private Long sessionHistoryMessageFeedbackId;

    @Column(name = "scenario_language_variant_id")
    private Long scenarioLanguageVariantId;

    @Column(name = "original_expression_text", nullable = false, length = 500)
    private String originalExpressionText;

    @Column(name = "better_expression_text", nullable = false, length = 500)
    private String betterExpressionText;

    @Column(name = "base_meaning_text", nullable = false, columnDefinition = "text")
    private String baseMeaningText;

    @Column(name = "usage_context", nullable = false, columnDefinition = "text")
    private String usageContext;

    @Column(name = "correction_reason", nullable = false, columnDefinition = "text")
    private String correctionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserLearningExpressionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_priority_level", nullable = false, length = 20)
    private ReviewPriorityLevel reviewPriorityLevel;

    @Column(name = "repeated_mistake_count", nullable = false)
    private int repeatedMistakeCount;

    @Column(name = "successful_reuse_count", nullable = false)
    private int successfulReuseCount;

    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "session_history_message_id")
    private Long sessionHistoryMessageId;

    protected UserLearningExpression() {
    }
}
