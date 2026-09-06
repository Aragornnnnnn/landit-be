// 장기기억 snapshot 재검증과 원자적 상태 적용을 검증한다.

package com.landit.landitbe.feature.memory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.domain.NewConversationMemory;
import com.landit.landitbe.feature.session.client.ai.AiMemoryOperation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** 장기기억 snapshot 재검증과 원자적 상태 적용을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class ConversationMemoryWriteServiceIntegrationTests {

  private static final long USER_ID = 996001L;
  private static final long LEARNING_SESSION_ID = 996002L;
  private static final long FREE_TALK_SESSION_ID = 996003L;
  private static final long SESSION_HISTORY_ID = 996004L;
  private static final long SOURCE_MESSAGE_ID = 996005L;
  private static final long FIRST_OLD_MEMORY_ID = 996101L;
  private static final long SECOND_OLD_MEMORY_ID = 996102L;
  private static final long OTHER_USER_ID = 996006L;
  private static final long OTHER_USER_MEMORY_ID = 996103L;
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 25, 12, 0);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ConversationMemoryWriteService writeService;

  @AfterEach
  void clearFixtures() {
    jdbcTemplate.update(
        "delete from conversation_memory_source where memory_id in "
            + "(select id from conversation_memory where user_profile_id in (?, ?))",
        USER_ID,
        OTHER_USER_ID);
    jdbcTemplate.update(
        "update conversation_memory set status = 'ACTIVE', valid_to = null, "
            + "superseded_at = null, superseded_by_id = null "
            + "where user_profile_id in (?, ?)",
        USER_ID,
        OTHER_USER_ID);
    jdbcTemplate.update("delete from conversation_memory where user_profile_id = ?", USER_ID);
    jdbcTemplate.update("delete from conversation_memory where user_profile_id = ?", OTHER_USER_ID);
    jdbcTemplate.update("delete from free_talk_session where id = ?", FREE_TALK_SESSION_ID);
    jdbcTemplate.update("delete from session_history_message where id = ?", SOURCE_MESSAGE_ID);
    jdbcTemplate.update("delete from session_history where id = ?", SESSION_HISTORY_ID);
    jdbcTemplate.update("delete from learning_session where id = ?", LEARNING_SESSION_ID);
    jdbcTemplate.update("delete from user_profile where id = ?", USER_ID);
    jdbcTemplate.update("delete from user_profile where id = ?", OTHER_USER_ID);
  }

  @Test
  void storesAddAndSourceLineageThenCompletesSessionAtomically() {
    seedCompletedPreparingSession();

    ConversationMemoryWriteService.PersistenceResult result =
        writeService.persistIfSnapshotCurrent(
            LEARNING_SESSION_ID,
            USER_ID,
            List.of(plan(AiMemoryOperation.ADD, List.of(), List.of())));

    assertThat(result).isEqualTo(ConversationMemoryWriteService.PersistenceResult.STORED);
    assertThat(countMemories()).isEqualTo(1);
    assertThat(countSources()).isEqualTo(1);
    assertThat(memoryGenerationStatus()).isEqualTo("READY");
  }

  @Test
  void storesIgnoreAsReadyWithoutAddingMemory() {
    seedCompletedPreparingSession();

    ConversationMemoryWriteService.PersistenceResult result =
        writeService.persistIfSnapshotCurrent(
            LEARNING_SESSION_ID,
            USER_ID,
            List.of(plan(AiMemoryOperation.IGNORE, List.of(), List.of())));

    assertThat(result).isEqualTo(ConversationMemoryWriteService.PersistenceResult.STORED);
    assertThat(countMemories()).isZero();
    assertThat(memoryGenerationStatus()).isEqualTo("READY");
  }

  @Test
  void supersedesMultipleActiveMemoriesAndStoresTheNewSourceLineage() {
    seedCompletedPreparingSession();
    seedMemory(FIRST_OLD_MEMORY_ID, "first old memory", "ACTIVE");
    seedMemory(SECOND_OLD_MEMORY_ID, "second old memory", "ACTIVE");

    ConversationMemoryWriteService.PersistenceResult result =
        writeService.persistIfSnapshotCurrent(
            LEARNING_SESSION_ID,
            USER_ID,
            List.of(
                plan(
                    AiMemoryOperation.SUPERSEDE,
                    List.of(FIRST_OLD_MEMORY_ID, SECOND_OLD_MEMORY_ID),
                    List.of(FIRST_OLD_MEMORY_ID, SECOND_OLD_MEMORY_ID))));

    assertThat(result).isEqualTo(ConversationMemoryWriteService.PersistenceResult.STORED);
    assertThat(countMemories()).isEqualTo(3);
    assertThat(
            jdbcTemplate.queryForList(
                "select status from conversation_memory where id in (?, ?) order by id",
                String.class,
                FIRST_OLD_MEMORY_ID,
                SECOND_OLD_MEMORY_ID))
        .containsExactly("SUPERSEDED", "SUPERSEDED");
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from conversation_memory_source where memory_id not in (?, ?)",
                Integer.class,
                FIRST_OLD_MEMORY_ID,
                SECOND_OLD_MEMORY_ID))
        .isEqualTo(1);
    assertThat(memoryGenerationStatus()).isEqualTo("READY");
  }

  @Test
  void returnsStaleAndWritesNothingWhenOrderedSnapshotIdsChanged() {
    seedCompletedPreparingSession();
    seedMemory(FIRST_OLD_MEMORY_ID, "new comparable memory", "ACTIVE");

    ConversationMemoryWriteService.PersistenceResult result =
        writeService.persistIfSnapshotCurrent(
            LEARNING_SESSION_ID,
            USER_ID,
            List.of(plan(AiMemoryOperation.ADD, List.of(), List.of())));

    assertThat(result).isEqualTo(ConversationMemoryWriteService.PersistenceResult.STALE);
    assertThat(countMemories()).isEqualTo(1);
    assertThat(memoryGenerationStatus()).isEqualTo("PREPARING");
  }

  @Test
  void rollsBackNewMemoryAndEarlierSupersedesWhenOneTargetIsInvalid() {
    seedCompletedPreparingSession();
    seedMemory(FIRST_OLD_MEMORY_ID, "first old memory", "ACTIVE");

    assertThatThrownBy(
            () ->
                writeService.persistIfSnapshotCurrent(
                    LEARNING_SESSION_ID,
                    USER_ID,
                    List.of(
                        plan(
                            AiMemoryOperation.SUPERSEDE,
                            List.of(FIRST_OLD_MEMORY_ID),
                            List.of(FIRST_OLD_MEMORY_ID)),
                        plan(
                            AiMemoryOperation.SUPERSEDE,
                            List.of(FIRST_OLD_MEMORY_ID),
                            List.of(FIRST_OLD_MEMORY_ID)))))
        .isInstanceOf(IllegalStateException.class);

    assertThat(countMemories()).isEqualTo(1);
    assertThat(statusOf(FIRST_OLD_MEMORY_ID)).isEqualTo("ACTIVE");
    assertThat(memoryGenerationStatus()).isEqualTo("PREPARING");
  }

  @Test
  void rejectsSupersedeTargetOutsideSnapshotAndUserScopeBeforeWriting() {
    seedCompletedPreparingSession();
    seedUser(OTHER_USER_ID);
    seedMemory(OTHER_USER_MEMORY_ID, OTHER_USER_ID, "other user's memory", "ACTIVE");

    assertThatThrownBy(
            () ->
                writeService.persistIfSnapshotCurrent(
                    LEARNING_SESSION_ID,
                    USER_ID,
                    List.of(
                        plan(
                            AiMemoryOperation.SUPERSEDE,
                            List.of(OTHER_USER_MEMORY_ID),
                            List.of()))))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(countMemories()).isZero();
    assertThat(statusOf(OTHER_USER_MEMORY_ID)).isEqualTo("ACTIVE");
    assertThat(memoryGenerationStatus()).isEqualTo("PREPARING");
  }

  private ConversationMemoryResolutionPlan plan(
      AiMemoryOperation operation, List<Long> supersededMemoryIds, List<Long> snapshotMemoryIds) {
    return new ConversationMemoryResolutionPlan(
        newMemory(), List.of(SOURCE_MESSAGE_ID), snapshotMemoryIds, operation, supersededMemoryIds);
  }

  private NewConversationMemory newMemory() {
    return new NewConversationMemory(
        USER_ID,
        "chloe",
        ConversationMemoryType.EVENT,
        "new memory",
        Locale.ENGLISH,
        0.8,
        NOW.plusDays(1),
        null,
        NOW,
        NOW,
        "extractor-v1",
        "embedding-v1",
        queryEmbedding());
  }

  private void seedCompletedPreparingSession() {
    Long aiTutorId = jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
            push_permission_status, status, created_at, updated_at)
        values (?, 'memory-write-user', 'EN', 'KR', 1, ?, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        USER_ID,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into learning_session (
            id, user_profile_id, session_type, ai_tutor_id, target_locale, base_locale,
            input_mode, status, ended_by, completion_reason, started_at, ended_at,
            created_at, updated_at)
        values (?, ?, 'FREE_TALK', ?, 'EN', 'KR', 'MIXED', 'COMPLETED', 'TIME_LIMIT',
            'TIME_LIMIT_REACHED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP)
        """,
        LEARNING_SESSION_ID,
        USER_ID,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into free_talk_session (
            id, learning_session_id, start_mode, character_id, conversation_status,
            accumulated_speaking_duration_ms, memory_generation_status,
            memory_generation_started_at, created_at, updated_at)
        values (?, ?, 'USER_FIRST', 'chloe', 'COMPLETED', 0, 'PREPARING',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        FREE_TALK_SESSION_ID,
        LEARNING_SESSION_ID);
    jdbcTemplate.update(
        """
        insert into session_history (
            id, learning_session_id, user_profile_id, session_type, target_locale,
            base_locale, started_at, ended_at, duration_seconds, user_message_count, created_at)
        values (?, ?, ?, 'FREE_TALK', 'EN', 'KR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            0, 1, CURRENT_TIMESTAMP)
        """,
        SESSION_HISTORY_ID,
        LEARNING_SESSION_ID,
        USER_ID);
    jdbcTemplate.update(
        """
        insert into session_history_message (
            id, session_history_id, message_sequence, turn_number, role, content,
            input_type, created_at, updated_at)
        values (?, ?, 1, 1, 'USER', 'hello', 'TEXT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        SOURCE_MESSAGE_ID,
        SESSION_HISTORY_ID);
  }

  private void seedUser(long userId) {
    Long aiTutorId = jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
            push_permission_status, status, created_at, updated_at)
        values (?, 'memory-write-other-user', 'EN', 'KR', 1, ?, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        userId,
        aiTutorId);
  }

  private void seedMemory(long memoryId, String content, String status) {
    seedMemory(memoryId, USER_ID, content, status);
  }

  private void seedMemory(long memoryId, long userId, String content, String status) {
    jdbcTemplate.update(
        """
        insert into conversation_memory (
            id, user_profile_id, character_id, memory_type, content, content_locale,
            confidence, status, valid_from, valid_to, observed_at, recorded_at,
            superseded_at, superseded_by_id, invalidated_at, invalidation_reason,
            extractor_version, embedding_model, embedding)
        values (?, ?, 'chloe', 'EVENT', ?, 'en', 0.8, ?, ?, NULL, ?, ?,
            NULL, NULL, NULL, NULL, 'extractor-v1', 'embedding-v1', CAST(? AS extensions.vector))
        """,
        memoryId,
        userId,
        content,
        status,
        NOW,
        NOW,
        NOW,
        vectorLiteral(queryEmbedding()));
  }

  private long countMemories() {
    return jdbcTemplate.queryForObject(
        "select count(*) from conversation_memory where user_profile_id = ?", Long.class, USER_ID);
  }

  private long countSources() {
    return jdbcTemplate.queryForObject(
        "select count(*) from conversation_memory_source where memory_id in "
            + "(select id from conversation_memory where user_profile_id = ?)",
        Long.class,
        USER_ID);
  }

  private String memoryGenerationStatus() {
    return jdbcTemplate.queryForObject(
        "select memory_generation_status from free_talk_session where id = ?",
        String.class,
        FREE_TALK_SESSION_ID);
  }

  private String statusOf(long memoryId) {
    return jdbcTemplate.queryForObject(
        "select status from conversation_memory where id = ?", String.class, memoryId);
  }

  private static List<Float> queryEmbedding() {
    List<Float> embedding = new ArrayList<>(Collections.nCopies(1536, 0.0f));
    embedding.set(0, 1.0f);
    return embedding;
  }

  private static String vectorLiteral(List<Float> embedding) {
    StringBuilder literal = new StringBuilder("[");
    for (int index = 0; index < embedding.size(); index++) {
      if (index > 0) {
        literal.append(',');
      }
      literal.append(embedding.get(index));
    }
    return literal.append(']').toString();
  }
}
