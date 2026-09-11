// 결제 도입 전에도 시나리오와 피드백은 정상 동작하고 수준 평가만 실행되지 않는지 검증한다.

package com.landit.landitbe.feature.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.content.domain.ContentLearningLevel;
import com.landit.landitbe.feature.content.service.ScenarioLearningLevelService;
import com.landit.landitbe.feature.session.client.ai.AiConversationClient;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ScenarioSessionApiIntegrationTests.FakeAiClientConfiguration.class)
@TestPropertySource(
    properties = {
      "landit.subscription.launched-at=",
      "spring.datasource.url=jdbc:h2:mem:lan483;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    })
class SessionLevelAssessmentLaunchUnsetIntegrationTests {
  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ScenarioLearningLevelService contentLevelService;

  @MockitoSpyBean(name = "fakeAiConversationClient")
  private AiConversationClient ai;

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void completesScenarioAndFeedbackWithoutAssessmentAndKeepsOwnershipChecks() throws Exception {
    seedDiagnosticScenario();
    JsonNode login = login();
    String token = login.path("accessToken").asText();
    long userId = login.path("user").path("userId").asLong();
    assertThat(
            jdbc.queryForObject(
                "SELECT learning_level FROM user_profile WHERE id=?", Integer.class, userId))
        .isEqualTo(3);
    var start =
        mockMvc
            .perform(
                post("/api/v1/scenarios/1/sessions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long sessionId =
        mapper
            .readTree(start.getResponse().getContentAsByteArray())
            .path("data")
            .path("sessionId")
            .asLong();
    for (int turn = 0; turn < 4; turn++) {
      mockMvc
          .perform(
              post("/api/v1/sessions/{id}/messages", sessionId)
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"content\":\"I like traveling because I can meet people.\","
                          + "\"inputType\":\"VOICE\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.progress.completed").value(turn == 3));
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT level_assessment_processing_status FROM learning_session WHERE id=?",
                String.class,
                sessionId))
        .isNull();
    mockMvc
        .perform(
            post("/api/v1/sessions/{id}/feedback", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk());
    assertNullAssessment(token, sessionId);
    // 과거 배포가 남긴 만료 예약도 비활성 기간에는 fallback으로 변경하지 않는다.
    jdbc.update(
        "UPDATE learning_session SET level_assessment_processing_status='PREPARING', "
            + "level_assessment_requested_at='2020-01-01 00:00:00' WHERE id=?",
        sessionId);
    assertNullAssessment(token, sessionId);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_level_assessment WHERE learning_session_id=?",
                Integer.class,
                sessionId))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT level_assessment_processing_status FROM learning_session WHERE id=?",
                String.class,
                sessionId))
        .isEqualTo("PREPARING");
    verify(ai, never()).generateSessionLevelAssessment(any());
    assertThat(contentLevelService.expressionLevel(userId, 1L))
        .isEqualTo(ContentLearningLevel.LEVEL_2_TO_3);
    jdbc.update("UPDATE learning_session SET ended_at='2020-01-01 00:00:00' WHERE id=?", sessionId);
    jdbc.update("UPDATE user_profile SET learning_level=5 WHERE id=?", userId);
    assertThat(contentLevelService.expressionLevel(userId, 1L))
        .isEqualTo(ContentLearningLevel.LEVEL_2_TO_3);
    assertThat(
            jdbc.queryForObject(
                "SELECT learning_level_at_completion FROM scenario_session "
                    + "WHERE learning_session_id=?",
                Integer.class,
                sessionId))
        .isEqualTo(3);
    jdbc.update("UPDATE user_profile SET learning_level=3 WHERE id=?", userId);
    assertThat(
            jdbc.queryForObject(
                "SELECT learning_level FROM user_profile WHERE id=?", Integer.class, userId))
        .isEqualTo(3);
    String otherToken = login().path("accessToken").asText();
    mockMvc
        .perform(
            get("/api/v1/sessions/{id}/level-assessment", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/v1/sessions/{id}/level-assessment", sessionId))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            get("/api/v1/sessions/{id}/level-assessment", Long.MAX_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void documentsNullableAssessmentData() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.components.schemas.SessionLevelAssessmentResponse.type")
                .value(org.hamcrest.Matchers.hasItem("null")));
  }

  private void assertNullAssessment(String token, long sessionId) throws Exception {
    mockMvc
        .perform(
            get("/api/v1/sessions/{id}/level-assessment", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").hasJsonPath())
        .andExpect(jsonPath("$.data").value(nullValue()));
  }

  private JsonNode login() throws Exception {
    String key = UUID.randomUUID().toString();
    var result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"provider":"GOOGLE","idToken":"%s|%s@example.com|Tester|%s","nonce":"%s"}
                        """
                            .formatted(key, key, key, key)))
            .andExpect(status().isOk())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
  }

  private void seedDiagnosticScenario() {
    jdbc.update(
        "INSERT INTO category (id, display_order, status, created_at, updated_at) "
            + "VALUES (1, 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    jdbc.update(
        "INSERT INTO category_language_variant "
            + "(category_id, base_locale, name, created_at, updated_at) "
            + "VALUES (1, 'KR', '진단', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    jdbc.update(
        "INSERT INTO scenario (id, category_id, ai_role, difficulty, first_speaker, "
            + "total_question_count, character_id, display_order, status, created_at, updated_at) "
            + "VALUES (1, 1, 'friend', 'EASY', 'AI', 4, 'chloe', 1, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    jdbc.update(
        "INSERT INTO scenario_language_variant (scenario_id, target_locale, base_locale, "
            + "title, briefing, conversation_goal, status, created_at, updated_at) "
            + "VALUES (1, 'EN', 'KR', '첫 대화', '자기소개', '나를 소개한다', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    jdbc.execute(
        (ConnectionCallback<Void>)
            connection -> {
              ScriptUtils.executeSqlScript(
                  connection,
                  new ClassPathResource(
                      "db/migration/V90__insert_common_diagnostic_questions.sql"));
              return null;
            });
  }
}
