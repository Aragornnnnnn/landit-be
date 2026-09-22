// 어느 세션의 직전 완료 프리톡을 같은 사용자의, 그 세션이 시작하기 전에 끝난 것 중 가장 최근으로 고르는지 실제 DB에서 검증한다.

package com.landit.landitbe.feature.learning.freetalk.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.learning.freetalk.domain.FreeTalkSession;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** 어느 세션의 직전 완료 프리톡을 같은 사용자의, 그 세션이 시작하기 전에 끝난 것 중 가장 최근으로 고르는지 실제 DB에서 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class FreeTalkSessionRepositoryIntegrationTests {

  private static final long USER_ID = 993001L;
  private static final long OTHER_USER_ID = 993002L;
  // 기준 ID가 학습 세션, +1이 프리톡 세션이다.
  private static final long CURRENT = 993100L;
  private static final long LATEST_BEFORE = 993200L;
  private static final long OLDER = 993300L;
  private static final long ENDED_AFTER_START = 993400L;
  private static final long IN_PROGRESS = 993500L;
  private static final long OTHER_USER = 993600L;
  private static final long ABANDONED = 993700L;
  private static final List<Long> SESSIONS =
      List.of(CURRENT, LATEST_BEFORE, OLDER, ENDED_AFTER_START, IN_PROGRESS, OTHER_USER, ABANDONED);
  private static final LocalDateTime CURRENT_STARTED_AT = LocalDateTime.of(2026, 9, 15, 10, 0);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FreeTalkSessionRepository repository;

  @Autowired
  private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

  @BeforeEach
  void seed() {
    seedUser(USER_ID);
    seedUser(OTHER_USER_ID);
    seedSession(CURRENT, USER_ID, "IN_PROGRESS", "IN_PROGRESS", CURRENT_STARTED_AT, null);
    seedSession(LATEST_BEFORE, USER_ID, "COMPLETED", "COMPLETED", day(10, 9), day(10, 10));
    seedSession(OLDER, USER_ID, "COMPLETED", "COMPLETED", day(1, 9), day(1, 10));
    // 기준 세션이 시작한 뒤에 끝난 세션은 직전이 아니다(교정을 다시 요청할 때도 첫 시도와 같은 직전을 보게).
    seedSession(ENDED_AFTER_START, USER_ID, "COMPLETED", "COMPLETED", day(14, 9), day(15, 11));
    seedSession(IN_PROGRESS, USER_ID, "IN_PROGRESS", "IN_PROGRESS", day(12, 9), null);
    seedSession(OTHER_USER, OTHER_USER_ID, "COMPLETED", "COMPLETED", day(14, 9), day(14, 10));
    // 학습 세션은 끝났지만 프리톡 대화가 완료가 아닌 세션도 직전이 아니다.
    seedSession(ABANDONED, USER_ID, "COMPLETED", "IN_PROGRESS", day(13, 9), day(13, 10));
  }

  @AfterEach
  void clearFixtures() {
    for (long baseId : SESSIONS) {
      jdbcTemplate.update("delete from free_talk_session where id = ?", baseId + 1);
      jdbcTemplate.update("delete from learning_session where id = ?", baseId);
    }
    jdbcTemplate.update("delete from user_profile where id in (?, ?)", USER_ID, OTHER_USER_ID);
  }

  @DisplayName("같은 사용자의 완료 프리톡 중 기준 세션이 시작하기 전에 끝난 가장 최근 것을 고르고, 기준 세션이 끝난 뒤 다시 물어도 같다.")
  @Test
  void findsLatestCompletedSessionEndedBeforeCurrentStarted() {
    assertThat(previousOf(CURRENT)).containsExactly(LATEST_BEFORE + 1, OLDER + 1);

    // 시작과 같은 시각에 끝난 것으로 기록돼도(테스트 픽스처가 흔히 그렇다) 자기 자신은 직전이 아니다.
    jdbcTemplate.update(
        "update learning_session set status = 'COMPLETED', ended_at = ?, ended_by = 'USER',"
            + " completion_reason = 'USER_ENDED' where id = ?",
        Timestamp.valueOf(CURRENT_STARTED_AT),
        CURRENT);
    jdbcTemplate.update(
        "update free_talk_session set conversation_status = 'COMPLETED' where id = ?", CURRENT + 1);
    assertThat(previousOf(CURRENT)).containsExactly(LATEST_BEFORE + 1, OLDER + 1);
  }

  @DisplayName("첫 프리톡이면 비어 있다.")
  @Test
  void returnsEmptyForFirstSession() {
    assertThat(previousOf(OLDER)).isEmpty();
  }

  @DisplayName("멈춘 장기기억 작업은 준비 상태일 때만 실패로 확정하고 시작 시각을 비우며, 이미 끝난 작업은 건드리지 않는다.")
  @Test
  void failsStaleMemoryGenerationOnlyWhilePreparing() {
    jdbcTemplate.update(
        "update free_talk_session set memory_generation_status = 'PREPARING',"
            + " memory_generation_started_at = CURRENT_TIMESTAMP where id = ?",
        LATEST_BEFORE + 1);
    jdbcTemplate.update(
        "update free_talk_session set memory_generation_status = 'READY' where id = ?", OLDER + 1);

    assertThat(failStale(LATEST_BEFORE + 1)).isEqualTo(1);
    assertThat(failStale(LATEST_BEFORE + 1)).isZero();
    assertThat(failStale(OLDER + 1)).isZero();
    assertThat(
            jdbcTemplate.queryForMap(
                "select memory_generation_status, memory_generation_started_at"
                    + " from free_talk_session where id = ?",
                LATEST_BEFORE + 1))
        .containsEntry("MEMORY_GENERATION_STATUS", "FAILED")
        .containsEntry("MEMORY_GENERATION_STARTED_AT", null);
    assertThat(
            jdbcTemplate.queryForObject(
                "select memory_generation_status from free_talk_session where id = ?",
                String.class,
                OLDER + 1))
        .isEqualTo("READY");
  }

  private int failStale(long freeTalkSessionId) {
    return transactionTemplate.execute(
        status -> repository.failStaleMemoryGeneration(freeTalkSessionId));
  }

  private List<Long> previousOf(long learningSessionId) {
    return repository.findPreviousCompleted(learningSessionId, PageRequest.of(0, 5)).stream()
        .map(FreeTalkSession::getId)
        .toList();
  }

  private static LocalDateTime day(int dayOfMonth, int hour) {
    return LocalDateTime.of(2026, 9, dayOfMonth, hour, 0);
  }

  private void seedUser(long userId) {
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
            push_permission_status, status, created_at, updated_at)
        values (?, ?, 'EN', 'KR', 1, ?, 'NOT_DETERMINED', 'ACTIVE', CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP)
        """,
        userId,
        "previous-" + userId,
        aiTutorId());
  }

  private void seedSession(
      long baseId,
      long userId,
      String learningStatus,
      String conversationStatus,
      LocalDateTime startedAt,
      LocalDateTime endedAt) {
    jdbcTemplate.update(
        """
        insert into learning_session (
            id, user_profile_id, session_type, ai_tutor_id, target_locale, base_locale,
            input_mode, status, started_at, ended_at, ended_by, completion_reason,
            created_at, updated_at)
        values (?, ?, 'FREE_TALK', ?, 'EN', 'KR', 'MIXED', ?, ?, ?, ?, ?, CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP)
        """,
        baseId,
        userId,
        aiTutorId(),
        learningStatus,
        Timestamp.valueOf(startedAt),
        endedAt == null ? null : Timestamp.valueOf(endedAt),
        endedAt == null ? null : "USER",
        endedAt == null ? null : "USER_ENDED");
    jdbcTemplate.update(
        """
        insert into free_talk_session (
            id, learning_session_id, start_mode, character_id, conversation_status,
            accumulated_speaking_duration_ms, created_at, updated_at)
        values (?, ?, 'USER_FIRST', 'chloe', ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        baseId + 1,
        baseId,
        conversationStatus);
  }

  private Long aiTutorId() {
    return jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
  }
}
