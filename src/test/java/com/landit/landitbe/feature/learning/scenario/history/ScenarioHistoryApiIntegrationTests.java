// 시나리오 히스토리의 소유권·회차 정렬·저장 데이터 복원과 피드백 잠금을 검증한다.

package com.landit.landitbe.feature.learning.scenario.history;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.landit.landitbe.feature.auth.service.LanditTokenService;
import com.landit.landitbe.feature.learning.scenario.session.client.ai.AiConversationClient;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 실제 DB와 인증 필터를 거쳐 시나리오 히스토리 조회 계약을 검증한다. */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "landit.subscription.launched-at=2020-01-01T00:00:00+09:00")
class ScenarioHistoryApiIntegrationTests {
  private static final long USER = 5550001L;
  private static final long OTHER_USER = 5550002L;
  private static final long SCENARIO = 5550001L;
  private static final LocalDateTime START = LocalDateTime.of(2026, 9, 20, 10, 0);

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private LanditTokenService tokens;
  @MockitoBean private AiConversationClient ai;

  @BeforeEach
  void seed() {
    jdbc.update(
        """
        INSERT INTO ai_tutor (id, accent_locale, target_locale, status, created_at, updated_at)
        VALUES (5550001, 'EN_US', 'EN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);
    for (long userId : new long[] {USER, OTHER_USER}) {
      jdbc.update(
          """
          INSERT INTO user_profile (id, nickname, target_locale, base_locale, current_level,
              push_permission_status, status, created_at, updated_at)
          VALUES (?, '히스토리 사용자', 'EN', 'KR', 1, 'NOT_DETERMINED', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          """,
          userId);
    }
    jdbc.update(
        """
        INSERT INTO category (id, display_order, status, created_at, updated_at)
        VALUES (5550001, 5550001, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);
    for (long scenarioId : new long[] {SCENARIO, SCENARIO + 1}) {
      jdbc.update(
          """
          INSERT INTO scenario (id, category_id, ai_role, difficulty, first_speaker,
              total_question_count, display_order, status, created_at, updated_at)
          VALUES (?, 5550001, 'tutor', 'EASY', 'AI', 3, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          """,
          scenarioId,
          scenarioId);
      jdbc.update(
          """
          INSERT INTO scenario_language_variant (id, scenario_id, target_locale, base_locale,
              title, briefing, conversation_goal, status, created_at, updated_at)
          VALUES (?, ?, 'EN', 'KR', '현재 제목', '현재 설명', '현재 목표', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          """,
          scenarioId,
          scenarioId);
    }
  }

  @AfterEach
  void neverGeneratesAiResults() {
    verifyNoInteractions(ai);
  }

  @Test
  @DisplayName("히스토리는 인증이 필요하며 잘못된 시나리오 ID 형식은 거부한다.")
  void requiresAuthenticationAndNumericScenarioId() throws Exception {
    mvc.perform(get("/api/v1/scenarios/{id}/history", SCENARIO))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            get("/api/v1/scenarios/invalid/history")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.createAccessToken(USER)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("본인 기록이 없거나 시나리오가 없으면 빈 회차 목록을 반환한다.")
  void returnsEmptyWithoutOwnedHistory() throws Exception {
    session(5550101L, OTHER_USER, SCENARIO, "COMPLETED", START.plusMinutes(1));
    history(USER, SCENARIO).andExpect(jsonPath("$.data.sessions", hasSize(0)));
    history(USER, 99999999L).andExpect(jsonPath("$.data.sessions", hasSize(0)));
  }

  @Test
  @DisplayName("전체 완료 회차는 완료 시각과 ID 역순이며 타인·다른 시나리오·미완료 회차를 제외한다.")
  void returnsAllOwnedCompletedRoundsInStableOrder() throws Exception {
    session(5550101L, USER, SCENARIO, "COMPLETED", START.plusMinutes(1));
    session(5550102L, USER, SCENARIO, "COMPLETED", START.plusMinutes(3));
    session(5550103L, USER, SCENARIO, "COMPLETED", START.plusMinutes(3));
    session(5550104L, USER, SCENARIO, "IN_PROGRESS", null);
    session(5550105L, USER, SCENARIO, "INTERRUPTED", START.plusMinutes(4));
    session(5550106L, OTHER_USER, SCENARIO, "COMPLETED", START.plusMinutes(5));
    session(5550107L, USER, SCENARIO + 1, "COMPLETED", START.plusMinutes(6));
    // 콘텐츠가 비활성화되거나 사용자 언어가 변경돼도 과거 기록은 유지된다.
    jdbc.update("UPDATE scenario SET status = 'INACTIVE' WHERE id = ?", SCENARIO);
    jdbc.update("UPDATE scenario_language_variant SET status = 'INACTIVE' WHERE id = ?", SCENARIO);
    jdbc.update("UPDATE user_profile SET target_locale = 'KR' WHERE id = ?", USER);
    history(USER, SCENARIO)
        .andExpect(jsonPath("$.data.scenarioId").value(SCENARIO))
        .andExpect(jsonPath("$.data.sessions", hasSize(3)))
        .andExpect(jsonPath("$.data.sessions[0].sessionId").value(5550103L))
        .andExpect(jsonPath("$.data.sessions[1].sessionId").value(5550102L))
        .andExpect(jsonPath("$.data.sessions[2].sessionId").value(5550101L))
        .andExpect(jsonPath("$.data.sessions[0].startedAt").value("2026-09-20T10:00:00"))
        .andExpect(jsonPath("$.data.sessions[0].endedAt").value("2026-09-20T10:03:00"))
        .andExpect(jsonPath("$.data.sessions[0].feedback").value(nullValue()));
  }

  @Test
  @DisplayName("메시지 ID와 무관하게 실제 대화 순서로 속마음·번역·회차별 피드백을 연결한다.")
  void restoresOrderedMessagesAndStoredFeedback() throws Exception {
    session(5550101L, USER, SCENARIO, "COMPLETED", START.plusMinutes(1));
    session(5550102L, USER, SCENARIO, "COMPLETED", START.plusMinutes(2));
    message(5550203L, 5550101L, 1, "AI", "Old question?");
    message(5550201L, 5550101L, 2, "USER", "My answer.");
    message(5550202L, 5550101L, 3, "AI", "Goodbye.");
    summary(5550101L, "COMPLETED");
    feedback(5550101L, 5550201L);
    history(USER, SCENARIO)
        .andExpect(jsonPath("$.data.sessions[0].feedback").value(nullValue()))
        .andExpect(jsonPath("$.data.sessions[1].messages", hasSize(3)))
        .andExpect(jsonPath("$.data.sessions[1].messages[0].messageId").value(5550203L))
        .andExpect(jsonPath("$.data.sessions[1].messages[0].content").value("Old question?"))
        .andExpect(jsonPath("$.data.sessions[1].messages[1].role").value("USER"))
        .andExpect(jsonPath("$.data.sessions[1].messages[1].messageSequence").value(2))
        .andExpect(jsonPath("$.data.sessions[1].messages[1].turnNumber").value(1))
        .andExpect(jsonPath("$.data.sessions[1].messages[1].innerThought").value("잘 전달됐네."))
        .andExpect(jsonPath("$.data.sessions[1].messages[1].innerThoughtType").value("GOOD"))
        .andExpect(
            jsonPath("$.data.sessions[1].messages[1].innerThoughtProcessingStatus")
                .value("COMPLETED"))
        .andExpect(jsonPath("$.data.sessions[1].messages[2].content").value("Goodbye."))
        .andExpect(jsonPath("$.data.sessions[1].feedback.sessionId").value(5550101L))
        .andExpect(jsonPath("$.data.sessions[1].feedback.nativeScore").value(80))
        .andExpect(jsonPath("$.data.sessions[1].feedback.starRating").value(2.5))
        .andExpect(jsonPath("$.data.sessions[1].feedback.summaryMessage").value("저장된 요약"))
        .andExpect(
            jsonPath("$.data.sessions[1].feedback.messageFeedbacks[0].messageId").value(5550201L))
        .andExpect(
            jsonPath("$.data.sessions[1].feedback.messageFeedbacks[0].correctionExpression")
                .value("Better answer."))
        .andExpect(
            jsonPath("$.data.sessions[1].feedback.messageFeedbacks[0].evaluationContext.content")
                .value("Old question?"))
        .andExpect(
            jsonPath(
                    "$.data.sessions[1].feedback.messageFeedbacks[0]"
                        + ".evaluationContext.translatedContent")
                .value("저장된 번역"));
  }

  @Test
  @DisplayName("사용자 선톡은 시작 안내 스냅샷을 복원하고 미완료 피드백은 null로 반환한다.")
  void restoresUserFirstSnapshotAndDoesNotExposeUnfinishedSummary() throws Exception {
    session(5550101L, USER, SCENARIO, "COMPLETED", START.plusMinutes(1));
    message(5550201L, 5550101L, 1, "USER", "Hello.");
    summary(5550101L, "COMPLETED");
    feedback(5550101L, 5550201L);
    session(5550102L, USER, SCENARIO, "COMPLETED", START.plusMinutes(2));
    summary(5550102L, "PREPARING");
    history(USER, SCENARIO)
        .andExpect(jsonPath("$.data.sessions[0].feedback").value(nullValue()))
        .andExpect(
            jsonPath("$.data.sessions[1].feedback.messageFeedbacks[0].evaluationContext.type")
                .value("SCENARIO_OPENING_INSTRUCTION"))
        .andExpect(
            jsonPath("$.data.sessions[1].feedback.messageFeedbacks[0].evaluationContext.content")
                .value("당시의 시작 안내"));
  }

  @Test
  @DisplayName("무료 첫 완료 회차만 상세 피드백을 공개하고 재완료 회차는 대화·요약을 유지하며 잠근다.")
  void appliesExistingFeedbackPolicyPerRound() throws Exception {
    session(5550101L, USER, SCENARIO, "COMPLETED", START.plusMinutes(1));
    session(5550102L, USER, SCENARIO, "COMPLETED", START.plusMinutes(2));
    for (long id : new long[] {5550101L, 5550102L}) {
      message(id, id, 1, "USER", "Hello.");
      summary(id, "COMPLETED");
      feedback(id, id);
    }
    jdbc.update(
        """
        INSERT INTO free_scenario_reservation (user_id, session_id, scenario_id, reserved_at)
        VALUES (?, 5550101, ?, ?)
        """,
        USER,
        SCENARIO,
        START);
    history(USER, SCENARIO)
        .andExpect(jsonPath("$.data.sessions[0].feedback.detailFeedbackLocked").value(true))
        .andExpect(jsonPath("$.data.sessions[0].feedback.messageFeedbacks", hasSize(0)))
        .andExpect(jsonPath("$.data.sessions[0].feedback.summaryMessage").value("저장된 요약"))
        .andExpect(jsonPath("$.data.sessions[0].messages", hasSize(1)))
        .andExpect(jsonPath("$.data.sessions[1].feedback.detailFeedbackLocked").value(false))
        .andExpect(jsonPath("$.data.sessions[1].feedback.messageFeedbacks", hasSize(1)));
  }

  @Test
  @DisplayName("OpenAPI는 인증과 시나리오 전용 메시지 스키마를 제공한다.")
  void documentsHistoryWithoutCollidingWithFreeTalkMessages() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/scenarios/{scenarioId}/history']"
                        + ".get.security[0].bearerAuth")
                .isArray())
        .andExpect(
            jsonPath(
                    "$.components.schemas.ScenarioHistorySession"
                        + ".properties.messages.items['$ref']")
                .value("#/components/schemas/ScenarioHistoryMessage"))
        .andExpect(
            jsonPath(
                    "$.components.schemas.ScenarioHistoryMessage"
                        + ".properties.innerThoughtProcessingStatus")
                .exists());
  }

  private ResultActions history(long userId, long scenarioId) throws Exception {
    return mvc.perform(
            get("/api/v1/scenarios/{id}/history", scenarioId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.createAccessToken(userId)))
        .andExpect(status().isOk());
  }

  private void session(long id, long userId, long scenarioId, String state, LocalDateTime endedAt) {
    jdbc.update(
        """
        INSERT INTO learning_session (id, user_profile_id, session_type, ai_tutor_id,
            target_locale, base_locale, input_mode, status, started_at, ended_at, ended_by,
            completion_reason, created_at, updated_at)
        VALUES (?, ?, 'SCENARIO', 5550001, 'EN', 'KR', 'MIXED', ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        id,
        userId,
        state,
        START,
        endedAt,
        endedAt == null ? null : "SYSTEM",
        endedAt == null ? null : "GOAL_COMPLETED");
    jdbc.update(
        """
        INSERT INTO scenario_session (learning_session_id, scenario_language_variant_id,
            question_level_group, user_opening_instruction_snapshot, created_at, updated_at)
        VALUES (?, ?, 'LEVEL_1', '당시의 시작 안내', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        id,
        scenarioId);
    jdbc.update(
        """
        INSERT INTO session_history (id, learning_session_id, user_profile_id, session_type,
            target_locale, base_locale, started_at, ended_at, duration_seconds, user_message_count, created_at)
        VALUES (?, ?, ?, 'SCENARIO', 'EN', 'KR', ?, ?, 60, 1, CURRENT_TIMESTAMP)
        """,
        id,
        id,
        userId,
        START,
        endedAt == null ? START : endedAt);
  }

  private void message(long id, long historyId, int sequence, String role, String content) {
    jdbc.update(
        """
        INSERT INTO session_history_message (id, session_history_id, message_sequence, turn_number,
            role, content, translated_content, input_type, inner_thought, inner_thought_type,
            inner_thought_processing_status, created_at, updated_at)
        VALUES (?, ?, ?, 1, ?, ?, '저장된 번역', 'TEXT', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        id,
        historyId,
        sequence,
        role,
        content,
        role.equals("USER") ? "잘 전달됐네." : null,
        role.equals("USER") ? "GOOD" : null,
        role.equals("USER") ? "COMPLETED" : null);
  }

  private void summary(long historyId, String processingStatus) {
    jdbc.update(
        """
        INSERT INTO session_history_summary_feedback (id, session_history_id, processing_status,
            native_score, star_rating, total_message_count, native_like_message_count,
            highlight_message, summary_message, created_at, updated_at)
        VALUES (?, ?, ?, 80, 2.5, 1, 1, '저장된 강조', '저장된 요약', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        historyId,
        historyId,
        processingStatus);
  }

  private void feedback(long summaryId, long messageId) {
    jdbc.update(
        """
        INSERT INTO session_history_message_feedback (session_history_summary_feedback_id,
            session_history_message_id, target_locale, base_locale, processing_status,
            feedback_type, correction_expression, correction_reason, created_at, updated_at)
        VALUES (?, ?, 'EN', 'KR', 'COMPLETED', 'NEEDS_IMPROVEMENT', 'Better answer.', '저장된 이유', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        summaryId,
        messageId);
  }
}
