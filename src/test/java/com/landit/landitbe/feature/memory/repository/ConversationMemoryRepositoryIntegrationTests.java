// 장기기억과 원문 source의 원자적 저장 계약을 검증한다.

package com.landit.landitbe.feature.memory.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.domain.NewConversationMemory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** 장기기억과 원문 source의 원자적 저장 계약을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class ConversationMemoryRepositoryIntegrationTests {

  private static final long USER_ID = 995001L;
  private static final long LEARNING_SESSION_ID = 995002L;
  private static final long FREE_TALK_SESSION_ID = 995005L;
  private static final long SESSION_HISTORY_ID = 995003L;
  private static final long SOURCE_MESSAGE_ID = 995004L;
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 25, 12, 0);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ConversationMemoryRepository repository;

  @AfterEach
  void clearFixtures() {
    jdbcTemplate.update(
        "delete from free_talk_memory_retrieval where free_talk_session_id = ?",
        FREE_TALK_SESSION_ID);
    jdbcTemplate.update("delete from free_talk_session where id = ?", FREE_TALK_SESSION_ID);
    jdbcTemplate.update(
        "delete from conversation_memory_source where memory_id in "
            + "(select id from conversation_memory where user_profile_id between ? and ?)",
        USER_ID,
        USER_ID + 2);
    jdbcTemplate.update(
        "update conversation_memory set status = 'ACTIVE', valid_to = null, "
            + "superseded_at = null, superseded_by_id = null "
            + "where user_profile_id between ? and ?",
        USER_ID,
        USER_ID + 2);
    jdbcTemplate.update(
        "delete from conversation_memory where user_profile_id between ? and ?",
        USER_ID,
        USER_ID + 2);
    jdbcTemplate.update(
        "delete from session_history_message where id between ? and ?",
        SOURCE_MESSAGE_ID,
        SOURCE_MESSAGE_ID + 2);
    jdbcTemplate.update(
        "delete from session_history where id between ? and ?",
        SESSION_HISTORY_ID,
        SESSION_HISTORY_ID + 2);
    jdbcTemplate.update(
        "delete from learning_session where id between ? and ?",
        LEARNING_SESSION_ID,
        LEARNING_SESSION_ID + 2);
    jdbcTemplate.update("delete from user_profile where id between ? and ?", USER_ID, USER_ID + 2);
  }

  @Test
  void savesActiveMemoryAndItsSourceWithValidityAndNullableStateUnset() {
    seedConversation(USER_ID, LEARNING_SESSION_ID, SESSION_HISTORY_ID, SOURCE_MESSAGE_ID);

    long memoryId = repository.save(validEventMemory(), List.of(SOURCE_MESSAGE_ID));

    assertThat(
            jdbcTemplate.queryForObject(
                "select status from conversation_memory where id = ?", String.class, memoryId))
        .isEqualTo("ACTIVE");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from conversation_memory_source where memory_id = ?",
                Integer.class,
                memoryId))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select valid_to from conversation_memory where id = ?",
                java.sql.Timestamp.class,
                memoryId))
        .isEqualTo(java.sql.Timestamp.valueOf(NOW.plusDays(1)));
    assertThat(
            jdbcTemplate.queryForObject(
                "select superseded_at is null and superseded_by_id is null "
                    + "and invalidated_at is null and invalidation_reason is null "
                    + "from conversation_memory where id = ?",
                Boolean.class,
                memoryId))
        .isTrue();
  }

  @Test
  void rejectsEmptyDuplicateAndNonPositiveSourceIdsBeforeInsert() {
    seedConversation(
        USER_ID + 1, LEARNING_SESSION_ID + 1, SESSION_HISTORY_ID + 1, SOURCE_MESSAGE_ID + 1);
    long before = countMemoriesForUser(USER_ID + 1);

    assertThatThrownBy(() -> repository.save(validEventMemory(USER_ID + 1), List.of()))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                repository.save(
                    validEventMemory(USER_ID + 1),
                    List.of(SOURCE_MESSAGE_ID + 1, SOURCE_MESSAGE_ID + 1)))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> repository.save(validEventMemory(USER_ID + 1), List.of(0L)))
        .hasRootCauseInstanceOf(IllegalArgumentException.class);

    assertThat(countMemoriesForUser(USER_ID + 1)).isEqualTo(before);
  }

  @Test
  void rollsBackMemoryWhenSourceForeignKeyFails() {
    seedConversation(
        USER_ID + 2, LEARNING_SESSION_ID + 2, SESSION_HISTORY_ID + 2, SOURCE_MESSAGE_ID + 2);

    assertThatThrownBy(
            () -> repository.save(validEventMemory(USER_ID + 2), List.of(SOURCE_MESSAGE_ID + 999)))
        .isInstanceOf(DataIntegrityViolationException.class);

    assertThat(countMemoriesForUser(USER_ID + 2)).isZero();
  }

  @Test
  void deletesMemorySourceAndFreeTalkRetrievalTraceOnUserWithdrawal() {
    seedConversation(USER_ID, LEARNING_SESSION_ID, SESSION_HISTORY_ID, SOURCE_MESSAGE_ID);
    long memoryId = repository.save(validEventMemory(), List.of(SOURCE_MESSAGE_ID));
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from conversation_memory_source where memory_id = ?",
                Long.class,
                memoryId))
        .isEqualTo(1L);
    jdbcTemplate.update(
        """
        insert into free_talk_memory_retrieval (
            free_talk_session_id, retrieval_stage, candidate_rank, policy_version, used, created_at)
        values (?, 'OPENING', 0, 'memory-retrieval-v1', false, CURRENT_TIMESTAMP)
        """,
        FREE_TALK_SESSION_ID);

    repository.deleteAllByUserProfileId(USER_ID);

    assertThat(countMemoriesForUser(USER_ID)).isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from conversation_memory_source where memory_id = ?",
                Long.class,
                memoryId))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from free_talk_memory_retrieval where free_talk_session_id = ?",
                Long.class,
                FREE_TALK_SESSION_ID))
        .isZero();
  }

  @Test
  void supersedesOnlyActiveMemoryAndClosesItsValidityAtNewMemoryStart() {
    seedConversation(USER_ID, LEARNING_SESSION_ID, SESSION_HISTORY_ID, SOURCE_MESSAGE_ID);
    long oldMemoryId = repository.save(validEventMemory(), List.of(SOURCE_MESSAGE_ID));
    long newMemoryId =
        repository.save(validEventMemory(USER_ID, NOW.plusDays(1)), List.of(SOURCE_MESSAGE_ID));

    assertThat(
            repository.supersedeActive(
                oldMemoryId, newMemoryId, NOW.plusDays(1), NOW.plusDays(1).plusMinutes(1)))
        .isTrue();
    assertThat(
            jdbcTemplate.queryForMap(
                "select status, valid_to, superseded_by_id from conversation_memory where id = ?",
                oldMemoryId))
        .containsEntry("STATUS", "SUPERSEDED")
        .containsEntry("SUPERSEDED_BY_ID", newMemoryId);
    assertThat(
            jdbcTemplate.queryForObject(
                "select valid_to from conversation_memory where id = ?",
                LocalDateTime.class,
                oldMemoryId))
        .isEqualTo(NOW.plusDays(1));
    assertThat(
            repository.supersedeActive(
                oldMemoryId, newMemoryId, NOW.plusDays(1), NOW.plusDays(1).plusMinutes(2)))
        .isFalse();
  }

  private NewConversationMemory validEventMemory() {
    return validEventMemory(USER_ID);
  }

  private NewConversationMemory validEventMemory(long userProfileId) {
    return validEventMemory(userProfileId, NOW);
  }

  private NewConversationMemory validEventMemory(long userProfileId, LocalDateTime validFrom) {
    return new NewConversationMemory(
        userProfileId,
        "chloe",
        ConversationMemoryType.EVENT,
        "remembered content",
        Locale.ENGLISH,
        0.8,
        validFrom,
        validFrom.plusDays(1),
        NOW,
        NOW,
        "extractor-v1",
        "embedding-v1",
        validEmbedding());
  }

  private long countMemoriesForUser(long userProfileId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from conversation_memory where user_profile_id = ?",
        Long.class,
        userProfileId);
  }

  private void seedConversation(
      long userProfileId, long learningSessionId, long sessionHistoryId, long sourceMessageId) {
    Long aiTutorId = jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
            push_permission_status, status, created_at, updated_at)
        values (?, 'memory-repository-user', 'EN', 'KR', 1, ?, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userProfileId,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into learning_session (
            id, user_profile_id, session_type, ai_tutor_id, target_locale, base_locale,
            input_mode, status, started_at, created_at, updated_at)
        values (?, ?, 'FREE_TALK', ?, 'EN', 'KR', 'MIXED', 'IN_PROGRESS',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        learningSessionId,
        userProfileId,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into free_talk_session (
            id, learning_session_id, start_mode, character_id, conversation_status,
            accumulated_speaking_duration_ms, created_at, updated_at)
        values (?, ?, 'USER_FIRST', 'chloe', 'COMPLETED', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        FREE_TALK_SESSION_ID,
        learningSessionId);
    jdbcTemplate.update(
        """
        insert into session_history (
            id, learning_session_id, user_profile_id, session_type, target_locale,
            base_locale, started_at, ended_at, duration_seconds, user_message_count, created_at)
        values (?, ?, ?, 'FREE_TALK', 'EN', 'KR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            0, 1, CURRENT_TIMESTAMP)
        """,
        sessionHistoryId,
        learningSessionId,
        userProfileId);
    jdbcTemplate.update(
        """
        insert into session_history_message (
            id, session_history_id, message_sequence, turn_number, role, content,
            input_type, created_at, updated_at)
        values (?, ?, 1, 1, 'USER', 'hello', 'TEXT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        sourceMessageId,
        sessionHistoryId);
  }

  private static List<Float> validEmbedding() {
    return new ArrayList<>(Collections.nCopies(1536, 0.1f));
  }
}
