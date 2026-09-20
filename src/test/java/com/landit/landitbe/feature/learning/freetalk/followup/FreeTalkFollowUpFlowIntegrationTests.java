// 세션 종료 뒤 후속 질문이 저장되고 다음 세션의 질문 생성에 반영되는 흐름을 검증한다.

package com.landit.landitbe.feature.learning.freetalk.followup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landit.landitbe.feature.learning.freetalk.followup.domain.FreeTalkFollowUpTriggerType;
import com.landit.landitbe.feature.learning.freetalk.followup.dto.FreeTalkFollowUpSummary;
import com.landit.landitbe.feature.learning.freetalk.followup.service.FreeTalkFollowUpService;
import com.landit.landitbe.feature.learning.freetalk.memory.domain.MemoryGenerationStatus;
import com.landit.landitbe.feature.learning.freetalk.memory.service.FreeTalkMemoryGenerationService;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.client.ai.AiMemoryClient;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.memory.planning.client.ai.AiMemoryCandidatesResult;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 세션 종료 뒤 후속 질문이 저장되고 다음 세션의 질문 생성에 반영되는 흐름을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
class FreeTalkFollowUpFlowIntegrationTests {

  private static final long USER_ID = 997001L;
  private static final long FIRST_SESSION_BASE_ID = 997100L;
  private static final long SECOND_SESSION_BASE_ID = 997200L;
  private static final String QUESTION = "저번에 말한 면접 준비, 어떻게 됐어?";
  private static final String INVITE = "다음엔 그 얘기 하자. 궁금해.";

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FreeTalkMemoryGenerationService generationService;

  @Autowired private FreeTalkFollowUpService followUpService;

  @MockitoBean private AiMemoryClient aiMemoryClient;

  @AfterEach
  void clearFixtures() {
    jdbcTemplate.update("delete from free_talk_follow_up where user_profile_id = ?", USER_ID);
    jdbcTemplate.update(
        "delete from conversation_memory_source where memory_id in "
            + "(select id from conversation_memory where user_profile_id = ?)",
        USER_ID);
    jdbcTemplate.update("delete from conversation_memory where user_profile_id = ?", USER_ID);
    for (long baseId : List.of(FIRST_SESSION_BASE_ID, SECOND_SESSION_BASE_ID)) {
      jdbcTemplate.update("delete from free_talk_session where id = ?", baseId + 1);
      jdbcTemplate.update("delete from session_history_message where id = ?", baseId + 3);
      jdbcTemplate.update("delete from session_history where id = ?", baseId + 2);
      jdbcTemplate.update("delete from learning_session where id = ?", baseId);
    }
    jdbcTemplate.update("delete from user_profile where id = ?", USER_ID);
  }

  @DisplayName("세션이 끝나 저장된 후속 질문은 요약에 그대로 나오고, 근거 기억은 다음 세션의 질문 생성에서 빠지도록 전달된다.")
  @Test
  void storesFollowUpAfterSessionAndExcludesItsMemoryFromTheNextSession() {
    seedUser();
    seedCompletedSession(FIRST_SESSION_BASE_ID, "TIME_LIMIT", "TIME_LIMIT_REACHED");
    when(aiMemoryClient.extractMemoryCandidates(any()))
        .thenReturn(
            new AiMemoryCandidatesResult(
                "extractor-v1",
                List.of(candidate(FIRST_SESSION_BASE_ID + 3)),
                new AiMemoryCandidatesResult.FollowUpQuestion(
                    null, 0, "CONCERN", QUESTION, INVITE)));

    assertThat(followUpService.findSummary(FIRST_SESSION_BASE_ID + 1, memoryStatusOfFirst()))
        .isEqualTo(FreeTalkFollowUpSummary.waiting());

    generationService.generate(FIRST_SESSION_BASE_ID);

    Long newMemoryId =
        jdbcTemplate.queryForObject(
            "select id from conversation_memory where user_profile_id = ?", Long.class, USER_ID);
    assertThat(followUpService.findSummary(FIRST_SESSION_BASE_ID + 1, memoryStatusOfFirst()))
        .isEqualTo(
            new FreeTalkFollowUpSummary(
                false, FreeTalkFollowUpTriggerType.CONCERN, QUESTION, INVITE));
    assertThat(
            jdbcTemplate.queryForObject(
                "select memory_id from free_talk_follow_up where free_talk_session_id = ?",
                Long.class,
                FIRST_SESSION_BASE_ID + 1))
        .isEqualTo(newMemoryId);

    seedCompletedSession(SECOND_SESSION_BASE_ID, "USER", "USER_ENDED");
    when(aiMemoryClient.extractMemoryCandidates(any()))
        .thenReturn(new AiMemoryCandidatesResult("extractor-v1", List.of()));

    generationService.generate(SECOND_SESSION_BASE_ID);

    ArgumentCaptor<AiMemoryCandidatesRequest> requests =
        ArgumentCaptor.forClass(AiMemoryCandidatesRequest.class);
    verify(aiMemoryClient, times(2)).extractMemoryCandidates(requests.capture());
    AiMemoryCandidatesRequest first = requests.getAllValues().get(0);
    assertThat(first.askedMemoryIds()).isEmpty();
    assertThat(first.existingMemories()).isEmpty();
    assertThat(first.sessionEndedBy()).isEqualTo("TIME_LIMIT_REACHED");
    AiMemoryCandidatesRequest second = requests.getAllValues().get(1);
    assertThat(second.askedMemoryIds()).containsExactly(newMemoryId);
    assertThat(second.existingMemories())
        .extracting(AiFreeTalkMemoryContext::memoryId)
        .containsExactly(newMemoryId);
    assertThat(second.sessionEndedBy()).isEqualTo("USER_CONFIRMED");
    assertThat(
            followUpService.findSummary(SECOND_SESSION_BASE_ID + 1, MemoryGenerationStatus.READY))
        .isEqualTo(FreeTalkFollowUpSummary.none());
  }

  private MemoryGenerationStatus memoryStatusOfFirst() {
    return MemoryGenerationStatus.valueOf(
        jdbcTemplate.queryForObject(
            "select memory_generation_status from free_talk_session where id = ?",
            String.class,
            FIRST_SESSION_BASE_ID + 1));
  }

  private AiMemoryCandidatesResult.Candidate candidate(long sourceMessageId) {
    List<Float> embedding = new ArrayList<>(Collections.nCopies(1536, 0.0f));
    embedding.set(0, 1.0f);
    return new AiMemoryCandidatesResult.Candidate(
        0,
        ConversationMemoryType.EVENT,
        "면접을 준비하고 있다",
        "KR",
        List.of(sourceMessageId),
        0.8,
        OffsetDateTime.of(2026, 8, 25, 10, 0, 0, 0, ZoneOffset.ofHours(9)),
        null,
        "openai/text-embedding-3-small",
        embedding);
  }

  private void seedUser() {
    jdbcTemplate.update(
        """
        insert into user_profile (
            id, nickname, target_locale, base_locale, current_level, ai_tutor_id,
            push_permission_status, status, created_at, updated_at)
        values (?, 'follow-up-user', 'EN', 'KR', 1, ?, 'NOT_DETERMINED',
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        USER_ID,
        aiTutorId());
  }

  // 기준 ID에서 학습 세션, +1 프리톡 세션, +2 대화 기록, +3 사용자 발화를 만든다.
  private void seedCompletedSession(long baseId, String endedBy, String completionReason) {
    jdbcTemplate.update(
        """
        insert into learning_session (
            id, user_profile_id, session_type, ai_tutor_id, target_locale, base_locale,
            input_mode, status, ended_by, completion_reason, started_at, ended_at,
            created_at, updated_at)
        values (?, ?, 'FREE_TALK', ?, 'EN', 'KR', 'MIXED', 'COMPLETED', ?, ?,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        baseId,
        USER_ID,
        aiTutorId(),
        endedBy,
        completionReason);
    jdbcTemplate.update(
        """
        insert into free_talk_session (
            id, learning_session_id, start_mode, character_id, conversation_status,
            accumulated_speaking_duration_ms, memory_generation_status,
            memory_generation_started_at, created_at, updated_at)
        values (?, ?, 'USER_FIRST', 'chloe', 'COMPLETED', 0, 'PREPARING',
            NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        baseId + 1,
        baseId);
    jdbcTemplate.update(
        """
        insert into session_history (
            id, learning_session_id, user_profile_id, session_type, target_locale,
            base_locale, started_at, ended_at, duration_seconds, user_message_count, created_at)
        values (?, ?, ?, 'FREE_TALK', 'EN', 'KR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
            0, 1, CURRENT_TIMESTAMP)
        """,
        baseId + 2,
        baseId,
        USER_ID);
    jdbcTemplate.update(
        """
        insert into session_history_message (
            id, session_history_id, message_sequence, turn_number, role, content,
            input_type, created_at, updated_at)
        values (?, ?, 1, 1, 'USER', 'I have an interview next week', 'TEXT',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        baseId + 3,
        baseId + 2);
  }

  private Long aiTutorId() {
    return jdbcTemplate.queryForObject("select min(id) from ai_tutor", Long.class);
  }
}
