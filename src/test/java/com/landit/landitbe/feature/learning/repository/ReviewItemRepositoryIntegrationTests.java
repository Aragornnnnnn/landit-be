// 복습 날짜와 상태에 따른 리마인더 대상 사용자 조회를 검증한다.

package com.landit.landitbe.feature.learning.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.learning.domain.ReviewItemStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 복습 날짜와 상태에 따른 리마인더 대상 사용자 조회를 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ReviewItemRepositoryIntegrationTests {

  private static final LocalDate REVIEW_DATE = LocalDate.of(2026, 7, 24);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ReviewItemRepository reviewItemRepository;

  /** 같은 날짜의 READY 항목이 여러 개여도 사용자 ID를 중복 없이 조회한다. */
  @Test
  void findsDistinctUsersWithReadyReviewItemsOnDate() {
    seedLearningHistory(995001L);
    seedLearningHistory(995002L);
    seedReviewItem(9950011L, 995001L, REVIEW_DATE, 1, "READY", null);
    seedReviewItem(9950012L, 995001L, REVIEW_DATE, 2, "READY", null);
    seedReviewItem(9950013L, 995001L, REVIEW_DATE.plusDays(1), 1, "READY", null);
    seedReviewItem(9950021L, 995002L, REVIEW_DATE, 1, "COMPLETED", "2026-07-24 12:00:00");

    assertThat(
            reviewItemRepository.findDistinctUserProfileIdsAfter(
                REVIEW_DATE, ReviewItemStatus.READY, null, PageRequest.of(0, 10)))
        .containsExactly(995001L);
  }

  /** 복습 문항이 참조할 최소 학습 이력을 저장한다. */
  private void seedLearningHistory(long userId) {
    long historyId = userId * 10;
    long messageId = userId * 10 + 1;
    long summaryId = userId * 10 + 2;
    long feedbackId = userId * 10 + 3;
    long expressionId = userId * 10 + 4;
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level,
            push_permission_status, status, created_at, updated_at
        )
        values (?, ?, 'EN', 'KR', 1, 'NOT_DETERMINED', 'ACTIVE',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        "review-user-" + userId);
    jdbcTemplate.update(
        """
        insert into session_history (
            id, user_profile_id, session_type, target_locale, base_locale,
            started_at, ended_at, duration_seconds, user_message_count, created_at
        )
        values (?, ?, 'FREE_TALK', 'EN', 'KR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            60, 1, CURRENT_TIMESTAMP)
        """,
        historyId,
        userId);
    jdbcTemplate.update(
        """
        insert into session_history_message (
            id, session_history_id, message_sequence, turn_number, role, content,
            input_type, created_at, updated_at
        )
        values (?, ?, 1, 1, 'USER', 'message', 'TEXT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        messageId,
        historyId);
    jdbcTemplate.update(
        """
        insert into session_history_summary_feedback (
            id, session_history_id, processing_status, created_at, updated_at
        )
        values (?, ?, 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        summaryId,
        historyId);
    jdbcTemplate.update(
        """
        insert into session_history_message_feedback (
            id, session_history_summary_feedback_id, session_history_message_id,
            target_locale, base_locale, processing_status, created_at, updated_at
        )
        values (?, ?, ?, 'EN', 'KR', 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        feedbackId,
        summaryId,
        messageId);
    jdbcTemplate.update(
        """
        insert into user_learning_expression (
            id, user_profile_id, target_locale, base_locale,
            session_history_message_feedback_id, original_expression_text,
            better_expression_text, base_meaning_text, usage_context, correction_reason,
            status, review_priority_level, repeated_mistake_count, successful_reuse_count,
            first_seen_at, last_seen_at, created_at, updated_at
        )
        values (?, ?, 'EN', 'KR', ?, ?, ?, '뜻', '문맥', '교정 이유',
            'REVIEWING', 'NORMAL', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        expressionId,
        userId,
        feedbackId,
        "original-" + userId,
        "better-" + userId);
  }

  /** 지정한 날짜와 상태의 복습 문항을 저장한다. */
  private void seedReviewItem(
      long reviewItemId,
      long userId,
      LocalDate reviewDate,
      int displayOrder,
      String status,
      String completedAt) {
    jdbcTemplate.update(
        """
        insert into review_item (
            id, user_profile_id, user_learning_expression_id,
            session_history_message_feedback_id, target_locale, base_locale,
            review_date, display_order, quiz_type, question_text, quiz_payload,
            status, completed_at, created_at, updated_at
        )
        values (?, ?, ?, ?, 'EN', 'KR', ?, ?, 'FILL_BLANK', 'question', '{}',
            ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        reviewItemId,
        userId,
        userId * 10 + 4,
        userId * 10 + 3,
        reviewDate,
        displayOrder,
        status,
        completedAt);
  }
}
