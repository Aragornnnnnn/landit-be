// 같은 세션의 총평을 두 번 저장하면 서비스가 잡는 바로 그 예외 타입이 실제 DB에서 나는지 검증한다.

package com.landit.landitbe.feature.learning.freetalk.summary.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkHeadlinePose;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkHeadlineTrigger;
import com.landit.landitbe.feature.learning.freetalk.summary.domain.FreeTalkSessionSummary;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkHeadline;
import com.landit.landitbe.feature.learning.freetalk.summary.dto.FreeTalkSessionMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/** 같은 세션의 총평을 두 번 저장하면 서비스가 잡는 바로 그 예외 타입이 실제 DB에서 나는지 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class FreeTalkSessionSummaryRepositoryIntegrationTests {

  private static final long USER_ID = 989001L;
  private static final long LEARNING_SESSION_ID = 989100L;
  private static final long FREE_TALK_SESSION_ID = 989101L;
  private static final long HISTORY_ID = 989102L;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FreeTalkSessionSummaryRepository repository;

  @Autowired private TransactionTemplate transactionTemplate;

  @BeforeEach
  void seed() {
    Long aiTutorId = jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
            push_permission_status, status, created_at, updated_at)
        values (?, 'summary-repo-user', 'EN', 'KR', 1, ?, 'NOT_DETERMINED', 'ACTIVE',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        USER_ID,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into learning_session (
            id, user_profile_id, session_type, ai_tutor_id, target_locale, base_locale,
            input_mode, status, ended_by, completion_reason, started_at, ended_at,
            created_at, updated_at)
        values (?, ?, 'FREE_TALK', ?, 'EN', 'KR', 'MIXED', 'COMPLETED', 'USER', 'USER_ENDED',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        LEARNING_SESSION_ID,
        USER_ID,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into free_talk_session (
            id, learning_session_id, start_mode, character_id, conversation_status,
            accumulated_speaking_duration_ms, created_at, updated_at)
        values (?, ?, 'USER_FIRST', 'chloe', 'COMPLETED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        FREE_TALK_SESSION_ID,
        LEARNING_SESSION_ID);
    jdbcTemplate.update(
        """
        insert into session_history (
            id, learning_session_id, user_profile_id, session_type, target_locale,
            base_locale, started_at, ended_at, duration_seconds, user_message_count, created_at)
        values (?, ?, ?, 'FREE_TALK', 'EN', 'KR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0,
            CURRENT_TIMESTAMP)
        """,
        HISTORY_ID,
        LEARNING_SESSION_ID,
        USER_ID);
  }

  @AfterEach
  void clearFixtures() {
    jdbcTemplate.update(
        "delete from free_talk_session_summary where free_talk_session_id = ?",
        FREE_TALK_SESSION_ID);
    jdbcTemplate.update("delete from free_talk_session where id = ?", FREE_TALK_SESSION_ID);
    jdbcTemplate.update("delete from session_history where id = ?", HISTORY_ID);
    jdbcTemplate.update("delete from learning_session where id = ?", LEARNING_SESSION_ID);
    jdbcTemplate.update("delete from user_profile where id = ?", USER_ID);
  }

  @DisplayName("같은 세션의 총평을 두 번 저장하면 두 번째 저장이 DataIntegrityViolationException으로 실패하고 첫 행이 남는다.")
  @Test
  void secondSaveForSameSessionFailsWithDataIntegrityViolation() {
    transactionTemplate.executeWithoutResult(status -> repository.save(summary("첫 번째")));

    assertThatThrownBy(
            () ->
                transactionTemplate.executeWithoutResult(
                    status -> repository.save(summary("두 번째"))))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(repository.findByFreeTalkSessionId(FREE_TALK_SESSION_ID))
        .map(FreeTalkSessionSummary::getHeadlineText)
        .contains("첫 번째");
  }

  private static FreeTalkSessionSummary summary(String headlineText) {
    return FreeTalkSessionSummary.first(
        USER_ID,
        FREE_TALK_SESSION_ID,
        HISTORY_ID,
        new FreeTalkHeadline(
            FreeTalkHeadlineTrigger.FIRST_SESSION,
            headlineText,
            "부제",
            FreeTalkHeadlinePose.WAVE_SMILE),
        new FreeTalkSessionMetrics(1000, 1, 1),
        0);
  }
}
