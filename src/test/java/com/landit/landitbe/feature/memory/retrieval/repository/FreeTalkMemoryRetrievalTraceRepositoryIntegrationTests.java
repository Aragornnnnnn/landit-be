// 세션에서 이미 검색한 장기기억을 새 검색 없이 다시 읽는 조회 계약을 검증한다.

package com.landit.landitbe.feature.memory.retrieval.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.retrieval.domain.MemoryRetrievalStage;
import com.landit.landitbe.feature.memory.retrieval.dto.ConversationMemoryMatch;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** 세션에서 이미 검색한 장기기억을 새 검색 없이 다시 읽는 조회 계약을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class FreeTalkMemoryRetrievalTraceRepositoryIntegrationTests {

  private static final long USER_ID = 996001L;
  private static final long OTHER_USER_ID = 996002L;
  private static final long LEARNING_SESSION_ID = 996010L;
  private static final long FREE_TALK_SESSION_ID = 996020L;
  private static final long OTHER_LEARNING_SESSION_ID = 996011L;
  private static final long OTHER_FREE_TALK_SESSION_ID = 996021L;
  private static final String POLICY_VERSION = "memory-retrieval-test";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FreeTalkMemoryRetrievalTraceRepository traceRepository;

  @AfterEach
  void clearFixtures() {
    jdbcTemplate.update(
        "delete from free_talk_memory_retrieval where free_talk_session_id in (?, ?)",
        FREE_TALK_SESSION_ID,
        OTHER_FREE_TALK_SESSION_ID);
    jdbcTemplate.update(
        "delete from free_talk_session where id in (?, ?)",
        FREE_TALK_SESSION_ID,
        OTHER_FREE_TALK_SESSION_ID);
    jdbcTemplate.update(
        "delete from learning_session where id in (?, ?)",
        LEARNING_SESSION_ID,
        OTHER_LEARNING_SESSION_ID);
    jdbcTemplate.update(
        "delete from conversation_memory where user_profile_id between ? and ?",
        USER_ID,
        OTHER_USER_ID);
    jdbcTemplate.update(
        "delete from user_profile where id between ? and ?", USER_ID, OTHER_USER_ID);
  }

  @DisplayName("두 검색 단계의 기억을 한 번씩만 담아 가까운 순으로 상한까지 돌려준다.")
  @Test
  void returnsDistinctRetrievedMemoriesOrderedByBestDistanceUpToLimit() {
    seedSession();
    seedMemory(996101L, USER_ID, "ACTIVE");
    seedMemory(996102L, USER_ID, "ACTIVE");
    seedMemory(996103L, USER_ID, "ACTIVE");
    seedMemory(996104L, USER_ID, "ACTIVE");
    traceRepository.saveCandidates(
        FREE_TALK_SESSION_ID,
        MemoryRetrievalStage.OPENING,
        List.of(match(996102L, 0.2), match(996101L, 0.3), match(996104L, 0.6)),
        POLICY_VERSION);
    traceRepository.saveCandidates(
        FREE_TALK_SESSION_ID,
        MemoryRetrievalStage.FIRST_USER_TURN,
        List.of(match(996101L, 0.1), match(996103L, 0.5)),
        POLICY_VERSION);

    List<AiFreeTalkMemoryContext> contexts =
        traceRepository.findRetrievedContexts(FREE_TALK_SESSION_ID, USER_ID, 3);

    assertThat(contexts)
        .extracting(AiFreeTalkMemoryContext::memoryId)
        .containsExactly(996101L, 996102L, 996103L);
    assertThat(contexts.getFirst().memoryType()).isEqualTo(ConversationMemoryType.PROFILE);
    assertThat(contexts.getFirst().content()).isEqualTo("사용자는 집 앞 헬스장에 다닌다.");
    assertThat(contexts.getFirst().observedAt()).isNotNull();
  }

  @DisplayName("다른 사용자의 기억과 그사이 비활성화된 기억은 돌려주지 않는다.")
  @Test
  void excludesOtherUsersAndInactiveMemories() {
    seedSession();
    seedMemory(996101L, USER_ID, "ACTIVE");
    seedMemory(996105L, USER_ID, "INVALIDATED");
    seedMemory(996106L, OTHER_USER_ID, "ACTIVE");
    traceRepository.saveCandidates(
        FREE_TALK_SESSION_ID,
        MemoryRetrievalStage.OPENING,
        List.of(match(996106L, 0.01), match(996105L, 0.05), match(996101L, 0.3)),
        POLICY_VERSION);

    assertThat(traceRepository.findRetrievedContexts(FREE_TALK_SESSION_ID, USER_ID, 3))
        .extracting(AiFreeTalkMemoryContext::memoryId)
        .containsExactly(996101L);
  }

  @DisplayName("다른 세션에서 검색한 기억은 돌려주지 않고, 기억을 말한 시각은 저장된 값 그대로 돌려준다.")
  @Test
  void excludesOtherSessionsAndKeepsStoredObservedTime() {
    seedSession();
    seedMemory(996101L, USER_ID, "ACTIVE");
    seedMemory(996102L, USER_ID, "ACTIVE");
    jdbcTemplate.update(
        "update conversation_memory set observed_at = TIMESTAMP '2026-09-13 23:30:00' where id = ?",
        996101L);
    traceRepository.saveCandidates(
        FREE_TALK_SESSION_ID,
        MemoryRetrievalStage.OPENING,
        List.of(match(996101L, 0.3)),
        POLICY_VERSION);
    // 같은 사용자의 다른 세션이 찾은 기억은 이 세션의 교정 근거로 쓰지 않는다.
    jdbcTemplate.update(
        """
        insert into free_talk_session (
            id, learning_session_id, start_mode, character_id, conversation_status,
            accumulated_speaking_duration_ms, created_at, updated_at)
        values (?, ?, 'AI_FIRST', 'chloe', 'IN_PROGRESS', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        OTHER_FREE_TALK_SESSION_ID,
        OTHER_LEARNING_SESSION_ID);
    traceRepository.saveCandidates(
        OTHER_FREE_TALK_SESSION_ID,
        MemoryRetrievalStage.OPENING,
        List.of(match(996102L, 0.1)),
        POLICY_VERSION);

    List<AiFreeTalkMemoryContext> contexts =
        traceRepository.findRetrievedContexts(FREE_TALK_SESSION_ID, USER_ID, 3);

    assertThat(contexts).extracting(AiFreeTalkMemoryContext::memoryId).containsExactly(996101L);
    // 자정 직전에 말한 기억도 그날 날짜로 태그에 쓰인다.
    assertThat(contexts.getFirst().observedAt().toLocalDate())
        .isEqualTo(java.time.LocalDate.of(2026, 9, 13));
  }

  @DisplayName("검색 선점 기록만 있고 후보가 없는 세션은 빈 목록을 돌려준다.")
  @Test
  void returnsEmptyWhenSessionHasOnlyClaimMarker() {
    seedSession();
    traceRepository.claim(FREE_TALK_SESSION_ID, MemoryRetrievalStage.OPENING, POLICY_VERSION);

    assertThat(traceRepository.findRetrievedContexts(FREE_TALK_SESSION_ID, USER_ID, 3)).isEmpty();
  }

  private static ConversationMemoryMatch match(long memoryId, double distance) {
    return new ConversationMemoryMatch(
        memoryId, ConversationMemoryType.PROFILE, "unused", null, null, null, distance);
  }

  private void seedSession() {
    Long aiTutorId = jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
    for (long userId : List.of(USER_ID, OTHER_USER_ID)) {
      jdbcTemplate.update(
          """
          insert into user_profile (
              id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
              push_permission_status, status, created_at, updated_at)
          values (?, 'memory-trace-user', 'EN', 'KR', 1, ?, 'NOT_DETERMINED',
              'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          """,
          userId,
          aiTutorId);
    }
    jdbcTemplate.update(
        """
        insert into learning_session (
            id, user_profile_id, session_type, ai_tutor_id, target_locale, base_locale,
            input_mode, status, started_at, created_at, updated_at)
        values (?, ?, 'FREE_TALK', ?, 'EN', 'KR', 'MIXED', 'IN_PROGRESS',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        LEARNING_SESSION_ID,
        USER_ID,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into learning_session (
            id, user_profile_id, session_type, ai_tutor_id, target_locale, base_locale,
            input_mode, status, started_at, created_at, updated_at)
        values (?, ?, 'FREE_TALK', ?, 'EN', 'KR', 'MIXED', 'IN_PROGRESS',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        OTHER_LEARNING_SESSION_ID,
        USER_ID,
        aiTutorId);
    jdbcTemplate.update(
        """
        insert into free_talk_session (
            id, learning_session_id, start_mode, character_id, conversation_status,
            accumulated_speaking_duration_ms, created_at, updated_at)
        values (?, ?, 'AI_FIRST', 'chloe', 'IN_PROGRESS', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        FREE_TALK_SESSION_ID,
        LEARNING_SESSION_ID);
  }

  private void seedMemory(long memoryId, long userId, String status) {
    boolean invalidated = "INVALIDATED".equals(status);
    jdbcTemplate.update(
        """
        insert into conversation_memory (
            id, user_profile_id, character_id, memory_type, content, content_locale,
            confidence, status, valid_from, valid_to, observed_at, recorded_at,
            superseded_at, superseded_by_id, invalidated_at, invalidation_reason,
            extractor_version, embedding_model, embedding)
        values (?, ?, NULL, 'PROFILE', '사용자는 집 앞 헬스장에 다닌다.', 'KR', 0.8, ?,
            CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, %s, %s,
            'extractor-v1', 'embedding-v1', CAST(? AS extensions.vector))
        """
            .formatted(
                invalidated ? "CURRENT_TIMESTAMP" : "NULL", invalidated ? "'policy'" : "NULL"),
        memoryId,
        userId,
        status,
        "[" + String.join(",", Collections.nCopies(1536, "0.1")) + "]");
  }
}
