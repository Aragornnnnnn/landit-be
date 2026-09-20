// 장기기억 snapshot 재검증과 원자적 상태 적용을 검증한다.

package com.landit.landitbe.feature.memory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.learning.freetalk.memory.service.FreeTalkMemoryGenerationContextService;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryResolutionPlan;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.domain.NewConversationMemory;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryFollowUpDraft;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryGenerationRequest;
import com.landit.landitbe.feature.memory.dto.ConversationMemoryPlanningResult;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryOperation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
  private static final String QUESTION = "저번에 말한 면접 준비, 어떻게 됐어?";
  private static final String INVITE = "다음엔 그 얘기 하자. 궁금해.";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ConversationMemoryWriteService writeService;

  @Autowired private FreeTalkMemoryGenerationContextService contextService;

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
    jdbcTemplate.update(
        "delete from free_talk_follow_up where free_talk_session_id = ?", FREE_TALK_SESSION_ID);
    jdbcTemplate.update("delete from free_talk_session where id = ?", FREE_TALK_SESSION_ID);
    jdbcTemplate.update("delete from session_history_message where id = ?", SOURCE_MESSAGE_ID);
    jdbcTemplate.update("delete from session_history where id = ?", SESSION_HISTORY_ID);
    jdbcTemplate.update("delete from learning_session where id = ?", LEARNING_SESSION_ID);
    jdbcTemplate.update("delete from user_profile where id = ?", USER_ID);
    jdbcTemplate.update("delete from user_profile where id = ?", OTHER_USER_ID);
  }

  @DisplayName("기억 추가와 출처 계보를 원자적으로 저장한다.")
  @Test
  void storesAddAndSourceLineageAtomically() {
    seedCompletedPreparingSession();

    ConversationMemoryWriteService.PersistenceResult result =
        persistAndComplete(
            generationRequest(), List.of(plan(AiMemoryOperation.ADD, List.of(), List.of())));

    assertThat(result).isEqualTo(ConversationMemoryWriteService.PersistenceResult.STORED);
    assertThat(countMemories()).isEqualTo(1);
    assertThat(countSources()).isEqualTo(1);
    assertThat(memoryGenerationStatus()).isEqualTo("READY");
  }

  @DisplayName("기억 무시 계획은 새 기억 없이 준비 완료로 저장한다.")
  @Test
  void storesIgnoreAsReadyWithoutAddingMemory() {
    seedCompletedPreparingSession();

    ConversationMemoryWriteService.PersistenceResult result =
        persistAndComplete(
            generationRequest(), List.of(plan(AiMemoryOperation.IGNORE, List.of(), List.of())));

    assertThat(result).isEqualTo(ConversationMemoryWriteService.PersistenceResult.STORED);
    assertThat(countMemories()).isZero();
    assertThat(memoryGenerationStatus()).isEqualTo("READY");
  }

  @DisplayName("활성 기억 여러 개를 대체하고 새 기억의 출처 계보를 저장한다.")
  @Test
  void supersedesMultipleActiveMemoriesAndStoresTheNewSourceLineage() {
    seedCompletedPreparingSession();
    seedMemory(FIRST_OLD_MEMORY_ID, "first old memory", "ACTIVE");
    seedMemory(SECOND_OLD_MEMORY_ID, "second old memory", "ACTIVE");

    ConversationMemoryWriteService.PersistenceResult result =
        persistAndComplete(
            generationRequest(),
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

  @DisplayName("조회했던 기억 ID와 순서가 바뀌면 오래된 계획으로 판단하고 아무것도 저장하지 않는다.")
  @Test
  void returnsStaleAndWritesNothingWhenOrderedSnapshotIdsChanged() {
    seedCompletedPreparingSession();
    seedMemory(FIRST_OLD_MEMORY_ID, "new comparable memory", "ACTIVE");

    ConversationMemoryWriteService.PersistenceResult result =
        persistAndComplete(
            generationRequest(), List.of(plan(AiMemoryOperation.ADD, List.of(), List.of())));

    assertThat(result).isEqualTo(ConversationMemoryWriteService.PersistenceResult.STALE);
    assertThat(countMemories()).isEqualTo(1);
    assertThat(memoryGenerationStatus()).isEqualTo("PREPARING");
  }

  @DisplayName("대체 대상 하나라도 잘못되면 새 기억과 앞선 대체 작업을 모두 롤백한다.")
  @Test
  void rollsBackNewMemoryAndEarlierSupersedesWhenOneTargetIsInvalid() {
    seedCompletedPreparingSession();
    seedMemory(FIRST_OLD_MEMORY_ID, "first old memory", "ACTIVE");

    assertThatThrownBy(
            () ->
                writeService.persistIfSnapshotCurrent(
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
  }

  @DisplayName("기억 저장 후 세션 완료 처리에 실패하면 기억 저장도 롤백한다.")
  @Test
  void rollsBackMemoryWhenSessionCompletionFailsAfterMemoryWrite() {
    seedCompletedPreparingSession();
    jdbcTemplate.update(
        "update free_talk_session set memory_generation_status = 'READY', "
            + "memory_generation_started_at = null where id = ?",
        FREE_TALK_SESSION_ID);

    ConversationMemoryGenerationRequest request =
        new ConversationMemoryGenerationRequest(
            LEARNING_SESSION_ID, USER_ID, "chloe", "EN", "KR", "Asia/Seoul", List.of());

    assertThatThrownBy(
            () ->
                persistAndComplete(
                    request, List.of(plan(AiMemoryOperation.ADD, List.of(), List.of()))))
        .isInstanceOf(IllegalStateException.class);

    assertThat(countMemories()).isZero();
    assertThat(memoryGenerationStatus()).isEqualTo("READY");
  }

  @DisplayName("스냅샷이나 사용자 범위를 벗어난 대체 대상은 저장 전에 거부한다.")
  @Test
  void rejectsSupersedeTargetOutsideSnapshotAndUserScopeBeforeWriting() {
    seedCompletedPreparingSession();
    seedUser(OTHER_USER_ID);
    seedMemory(OTHER_USER_MEMORY_ID, OTHER_USER_ID, "other user's memory", "ACTIVE");

    assertThatThrownBy(
            () ->
                writeService.persistIfSnapshotCurrent(
                    USER_ID,
                    List.of(
                        plan(
                            AiMemoryOperation.SUPERSEDE,
                            List.of(OTHER_USER_MEMORY_ID),
                            List.of()))))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(countMemories()).isZero();
    assertThat(statusOf(OTHER_USER_MEMORY_ID)).isEqualTo("ACTIVE");
  }

  @DisplayName("이번 후보를 근거로 한 후속 질문은 방금 저장한 새 기억 ID와 함께 저장한다.")
  @Test
  void storesFollowUpWithTheNewMemoryIdOfItsSourcePlan() {
    seedCompletedPreparingSession();

    persistAndComplete(
        generationRequest(),
        List.of(
            plan(AiMemoryOperation.IGNORE, List.of(), List.of()),
            plan(AiMemoryOperation.ADD, List.of(), List.of())),
        new ConversationMemoryFollowUpDraft(null, 1, "CONCERN", QUESTION, INVITE));

    Long newMemoryId =
        jdbcTemplate.queryForObject(
            "select id from conversation_memory where user_profile_id = ?", Long.class, USER_ID);
    assertThat(followUpRow())
        .containsEntry("USER_PROFILE_ID", USER_ID)
        .containsEntry("MEMORY_ID", newMemoryId)
        .containsEntry("TRIGGER_TYPE", "CONCERN")
        .containsEntry("QUESTION", QUESTION)
        .containsEntry("INVITE", INVITE);
  }

  @DisplayName("기존 기억을 근거로 한 후속 질문은 그 기억 ID를 그대로 저장한다.")
  @Test
  void storesFollowUpWithTheExistingMemoryId() {
    seedCompletedPreparingSession();

    persistAndComplete(
        generationRequest(),
        List.of(),
        new ConversationMemoryFollowUpDraft(5504L, null, "GOAL", QUESTION, INVITE));

    assertThat(followUpRow())
        .containsEntry("MEMORY_ID", 5504L)
        .containsEntry("TRIGGER_TYPE", "GOAL");
    assertThat(memoryGenerationStatus()).isEqualTo("READY");
  }

  @DisplayName("근거 후보가 기억으로 저장되지 않았으면 문구는 남기고 근거 기억만 비운다.")
  @Test
  void storesFollowUpWithoutMemoryWhenItsSourcePlanIsIgnored() {
    seedCompletedPreparingSession();

    persistAndComplete(
        generationRequest(),
        List.of(plan(AiMemoryOperation.IGNORE, List.of(), List.of())),
        new ConversationMemoryFollowUpDraft(null, 0, "HOBBY", QUESTION, INVITE));

    assertThat(followUpRow())
        .containsEntry("MEMORY_ID", null)
        .containsEntry("TRIGGER_TYPE", "HOBBY")
        .containsEntry("QUESTION", QUESTION);
  }

  @DisplayName("근거가 없는 기본 문구도 저장한다.")
  @Test
  void storesDefaultFollowUpWithoutMemory() {
    seedCompletedPreparingSession();

    persistAndComplete(
        generationRequest(),
        List.of(),
        new ConversationMemoryFollowUpDraft(null, null, "NONE", QUESTION, INVITE));

    assertThat(followUpRow())
        .containsEntry("MEMORY_ID", null)
        .containsEntry("TRIGGER_TYPE", "NONE");
  }

  @DisplayName("모르는 계기의 후속 질문은 건너뛰고 기억 저장과 완료는 그대로 한다.")
  @Test
  void skipsFollowUpWithUnknownTriggerTypeButStillStoresMemory() {
    seedCompletedPreparingSession();

    ConversationMemoryWriteService.PersistenceResult result =
        persistAndComplete(
            generationRequest(),
            List.of(plan(AiMemoryOperation.ADD, List.of(), List.of())),
            new ConversationMemoryFollowUpDraft(null, 0, "BIRTHDAY", QUESTION, INVITE));

    assertThat(result).isEqualTo(ConversationMemoryWriteService.PersistenceResult.STORED);
    assertThat(countFollowUps()).isZero();
    assertThat(countMemories()).isEqualTo(1);
    assertThat(memoryGenerationStatus()).isEqualTo("READY");
  }

  @DisplayName("오래된 계획이라 기억을 저장하지 않았으면 후속 질문도 저장하지 않는다.")
  @Test
  void storesNoFollowUpWhenSnapshotIsStale() {
    seedCompletedPreparingSession();
    seedMemory(FIRST_OLD_MEMORY_ID, "new comparable memory", "ACTIVE");

    persistAndComplete(
        generationRequest(),
        List.of(plan(AiMemoryOperation.ADD, List.of(), List.of())),
        new ConversationMemoryFollowUpDraft(null, 0, "CONCERN", QUESTION, INVITE));

    assertThat(countFollowUps()).isZero();
  }

  @DisplayName("세션 완료 처리에 실패하면 후속 질문도 남지 않는다.")
  @Test
  void storesNoFollowUpWhenSessionCompletionFails() {
    seedCompletedPreparingSession();
    jdbcTemplate.update(
        "update free_talk_session set memory_generation_status = 'READY', "
            + "memory_generation_started_at = null where id = ?",
        FREE_TALK_SESSION_ID);

    assertThatThrownBy(
            () ->
                persistAndComplete(
                    generationRequest(),
                    List.of(),
                    new ConversationMemoryFollowUpDraft(null, null, "NONE", QUESTION, INVITE)))
        .isInstanceOf(IllegalStateException.class);

    assertThat(countFollowUps()).isZero();
  }

  private ConversationMemoryWriteService.PersistenceResult persistAndComplete(
      ConversationMemoryGenerationRequest request, List<ConversationMemoryResolutionPlan> plans) {
    return persistAndComplete(request, plans, null);
  }

  private ConversationMemoryWriteService.PersistenceResult persistAndComplete(
      ConversationMemoryGenerationRequest request,
      List<ConversationMemoryResolutionPlan> plans,
      ConversationMemoryFollowUpDraft followUp) {
    return contextService.persistAndComplete(
        request, new ConversationMemoryPlanningResult(plans, followUp));
  }

  private Map<String, Object> followUpRow() {
    return jdbcTemplate.queryForMap(
        "select user_profile_id, memory_id, trigger_type, question, invite "
            + "from free_talk_follow_up where free_talk_session_id = ?",
        FREE_TALK_SESSION_ID);
  }

  private int countFollowUps() {
    return jdbcTemplate.queryForObject(
        "select count(*) from free_talk_follow_up where free_talk_session_id = ?",
        Integer.class,
        FREE_TALK_SESSION_ID);
  }

  private ConversationMemoryResolutionPlan plan(
      AiMemoryOperation operation, List<Long> supersededMemoryIds, List<Long> snapshotMemoryIds) {
    return new ConversationMemoryResolutionPlan(
        newMemory(), List.of(SOURCE_MESSAGE_ID), snapshotMemoryIds, operation, supersededMemoryIds);
  }

  private ConversationMemoryGenerationRequest generationRequest() {
    return new ConversationMemoryGenerationRequest(
        LEARNING_SESSION_ID, USER_ID, "chloe", "EN", "KR", "Asia/Seoul", List.of());
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
