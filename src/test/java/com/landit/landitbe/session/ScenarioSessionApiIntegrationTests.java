// 시나리오 세션 시작과 중도 종료 API의 응답과 저장 상태를 검증한다.
package com.landit.landitbe.session;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.common.domain.InnerThoughtType;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.domain.GoalCompletionStatus;
import com.landit.landitbe.session.application.port.AiClosingMessageRequest;
import com.landit.landitbe.session.application.port.AiClosingMessageResult;
import com.landit.landitbe.session.application.port.AiConversationClient;
import com.landit.landitbe.session.application.port.AiNextMessageRequest;
import com.landit.landitbe.session.application.port.AiNextMessageResult;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(properties = {
        "landit.auth.oidc.fake-enabled=true",
        "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
})
class ScenarioSessionApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeAiConversationClient fakeAiConversationClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        fakeAiConversationClient.reset();
        jdbcTemplate.update("DELETE FROM session_history_message_feedback");
        jdbcTemplate.update("DELETE FROM session_history_artifact");
        jdbcTemplate.update("DELETE FROM session_history_message");
        jdbcTemplate.update("DELETE FROM scenario_session");
        jdbcTemplate.update("DELETE FROM session_history");
        jdbcTemplate.update("DELETE FROM learning_session");
        jdbcTemplate.update("DELETE FROM user_scenario_progress");
        jdbcTemplate.update("DELETE FROM scenario_question_language_variant");
        jdbcTemplate.update("DELETE FROM scenario_question");
        jdbcTemplate.update("DELETE FROM scenario_language_variant");
        jdbcTemplate.update("DELETE FROM scenario");
        jdbcTemplate.update("DELETE FROM category_language_variant");
        jdbcTemplate.update("DELETE FROM category");
        jdbcTemplate.update("UPDATE user_profile SET ai_tutor_id = NULL");
        jdbcTemplate.update("DELETE FROM ai_tutor");
    }

    @Test
    void startAiFirstScenarioCreatesSessionProgressOpeningMessageAndResponse() throws Exception {
        JsonNode loginBody = login("ai-first@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedAiTutor(9001);
        assignAiTutor(userId, 9001);
        seedCategory(1001, 1, "ACTIVE", "음식");
        seedScenario(2001, 1001, 1, "AI", "ACTIVE", 4, "voice-food");
        seedScenarioVariant(
                3001,
                2001,
                "좋아하는 음식",
                "음식 취향을 말합니다.",
                "좋아하는 음식을 이유와 함께 말한다.",
                null,
                "What food do you like? Why do you like it?",
                "좋아하는 음식이 있어? 왜 좋아해?",
                "음식 이야기는 처음 대화를 열기 좋다.",
                "GOOD",
                "ACTIVE"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/2001/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.sessionId").value(notNullValue()))
                .andExpect(jsonPath("$.data.scenarioId").value(2001))
                .andExpect(jsonPath("$.data.sessionType").value("SCENARIO"))
                .andExpect(jsonPath("$.data.firstSpeaker").value("AI"))
                .andExpect(jsonPath("$.data.userOpeningInstruction").value(nullValue()))
                .andExpect(jsonPath("$.data.ttsVoiceSetId").value("voice-food"))
                .andExpect(jsonPath("$.data.currentMessage.messageId").value(notNullValue()))
                .andExpect(jsonPath("$.data.currentMessage.turnNumber").value(1))
                .andExpect(jsonPath("$.data.currentMessage.messageSequence").value(1))
                .andExpect(jsonPath("$.data.currentMessage.role").value("AI"))
                .andExpect(jsonPath("$.data.currentMessage.content")
                        .value("What food do you like? Why do you like it?"))
                .andExpect(jsonPath("$.data.currentMessage.translatedContent")
                        .value("좋아하는 음식이 있어? 왜 좋아해?"))
                .andExpect(jsonPath("$.data.currentMessage.innerThought")
                        .value("음식 이야기는 처음 대화를 열기 좋다."))
                .andExpect(jsonPath("$.data.currentMessage.innerThoughtType").value("GOOD"))
                .andExpect(jsonPath("$.data.progress.currentTurnNumber").value(1))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(4))
                .andExpect(jsonPath("$.data.progress.completed").value(false))
                .andReturn();

        long sessionId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("data")
                .get("sessionId")
                .asLong();
        assertLearningSession(sessionId, userId, 9001, "IN_PROGRESS", null, null);
        assertScenarioSession(sessionId, 3001);
        assertProgress(userId, 2001, "IN_PROGRESS");
        assertHistoryMessage(sessionId, "AI", "What food do you like? Why do you like it?");
    }

    @Test
    void startUserFirstScenarioReturnsInstructionWithoutOpeningMessage() throws Exception {
        JsonNode loginBody = login("user-first@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedAiTutor(9002);
        assignAiTutor(userId, 9002);
        seedCategory(1002, 1, "ACTIVE", "카페");
        seedScenario(2002, 1002, 1, "USER", "ACTIVE", 3, null);
        seedScenarioVariant(
                3002,
                2002,
                "카페 주문",
                "카페에서 음료를 주문합니다.",
                "원하는 음료를 주문한다.",
                "점원에게 먼저 주문하고 싶은 음료를 말해보세요.",
                null,
                null,
                null,
                null,
                "ACTIVE"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/2002/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sessionId").value(notNullValue()))
                .andExpect(jsonPath("$.data.scenarioId").value(2002))
                .andExpect(jsonPath("$.data.sessionType").value("SCENARIO"))
                .andExpect(jsonPath("$.data.firstSpeaker").value("USER"))
                .andExpect(jsonPath("$.data.userOpeningInstruction")
                        .value("점원에게 먼저 주문하고 싶은 음료를 말해보세요."))
                .andExpect(jsonPath("$.data.ttsVoiceSetId").value(nullValue()))
                .andExpect(jsonPath("$.data.currentMessage").value(nullValue()))
                .andExpect(jsonPath("$.data.progress.currentTurnNumber").value(1))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(3))
                .andExpect(jsonPath("$.data.progress.completed").value(false))
                .andReturn();

        long sessionId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("data")
                .get("sessionId")
                .asLong();
        assertLearningSession(sessionId, userId, 9002, "IN_PROGRESS", null, null);
        assertScenarioSession(sessionId, 3002);
        assertProgress(userId, 2002, "IN_PROGRESS");
        Integer messageCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        """,
                Integer.class,
                sessionId
        );
        assertThat(messageCount).isZero();
    }

    @Test
    void startScenarioHandlesConcurrentProgressCreationForSameUser() throws Exception {
        JsonNode loginBody = login("concurrent-start@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedAiTutor(9010);
        assignAiTutor(userId, 9010);
        seedCategory(1010, 1, "ACTIVE", "동시 시작");
        seedScenario(2010, 1010, 1, "USER", "ACTIVE", 2, null);
        seedScenarioVariant(
                3010,
                2010,
                "동시 시작",
                "동시 시작",
                "동시 시작",
                "먼저 말해보세요.",
                null,
                null,
                null,
                null,
                "ACTIVE"
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Callable<Integer> startRequest = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return mockMvc.perform(post("/api/v1/scenarios/2010/sessions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        try {
            Future<Integer> first = executorService.submit(startRequest);
            Future<Integer> second = executorService.submit(startRequest);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            )).containsExactlyInAnyOrder(201, 201);
        } finally {
            executorService.shutdownNow();
        }

        Integer progressCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM user_scenario_progress
                        WHERE user_profile_id = ?
                          AND scenario_id = ?
                          AND target_locale = 'en'
                        """,
                Integer.class,
                userId,
                2010
        );
        assertThat(progressCount).isEqualTo(1);
    }

    @Test
    void startScenarioRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/scenarios/2001/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    @Test
    void startScenarioRejectsMissingScenario() throws Exception {
        JsonNode loginBody = login("missing-scenario@example.com");

        mockMvc.perform(post("/api/v1/scenarios/999999/sessions")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginBody.get("data").get("accessToken").asText()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SCENARIO_NOT_FOUND"));
    }

    @Test
    void startScenarioRejectsInactiveCategory() throws Exception {
        JsonNode loginBody = login("locked-category@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        seedAiTutor(9003);
        assignAiTutor(userId, 9003);
        seedCategory(1003, 1, "INACTIVE", "잠긴 카테고리");
        seedScenario(2003, 1003, 1, "AI", "ACTIVE", 2, null);
        seedScenarioVariant(3003, 2003, "잠김", "잠김", "잠김", null, "Hello", "안녕", null, null, "ACTIVE");

        mockMvc.perform(post("/api/v1/scenarios/2003/sessions")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginBody.get("data").get("accessToken").asText()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_LOCKED"));
    }

    @Test
    void startScenarioRejectsPreviousScenarioNotCleared() throws Exception {
        JsonNode loginBody = login("locked-scenario@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        seedAiTutor(9004);
        assignAiTutor(userId, 9004);
        seedCategory(1004, 1, "ACTIVE", "순차 카테고리");
        seedScenario(2004, 1004, 1, "AI", "ACTIVE", 2, null);
        seedScenarioVariant(
                3004, 2004, "첫번째", "첫번째", "첫번째", null, "First", "첫번째", null, null, "ACTIVE"
        );
        seedScenario(2005, 1004, 2, "AI", "ACTIVE", 2, null);
        seedScenarioVariant(
                3005, 2005, "두번째", "두번째", "두번째", null, "Second", "두번째", null, null, "ACTIVE"
        );

        mockMvc.perform(post("/api/v1/scenarios/2005/sessions")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginBody.get("data").get("accessToken").asText()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SCENARIO_LOCKED"))
                .andExpect(jsonPath("$.error.message").value("PREVIOUS_SCENARIO_NOT_COMPLETED"));
    }

    @Test
    void endSessionInterruptsOwnedInProgressSession() throws Exception {
        StartedSession startedSession = startUserFirstSession("end-owned@example.com", 9005, 1005, 2006, 3006);

        mockMvc.perform(patch("/api/v1/sessions/%d/end".formatted(startedSession.sessionId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + startedSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        assertLearningSession(
                startedSession.sessionId(),
                startedSession.userId(),
                9005,
                "INTERRUPTED",
                "USER",
                "USER_ENDED"
        );
        Integer endedAtCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM learning_session WHERE id = ? AND ended_at IS NOT NULL",
                Integer.class,
                startedSession.sessionId()
        );
        assertThat(endedAtCount).isEqualTo(1);
    }

    @Test
    void endSessionRejectsOtherUserSession() throws Exception {
        StartedSession ownerSession = startUserFirstSession("owner@example.com", 9006, 1006, 2007, 3007);
        JsonNode otherLoginBody = login("other@example.com");

        mockMvc.perform(patch("/api/v1/sessions/%d/end".formatted(ownerSession.sessionId()))
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + otherLoginBody.get("data").get("accessToken").asText()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void endSessionRejectsMissingSession() throws Exception {
        JsonNode loginBody = login("missing-session@example.com");

        mockMvc.perform(patch("/api/v1/sessions/999999/end")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginBody.get("data").get("accessToken").asText()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void endSessionRejectsAlreadyEndedSession() throws Exception {
        StartedSession startedSession = startUserFirstSession("already-ended@example.com", 9007, 1007, 2008, 3008);
        jdbcTemplate.update("""
                        UPDATE learning_session
                        SET status = 'COMPLETED',
                            ended_by = 'SYSTEM',
                            completion_reason = 'GOAL_COMPLETED',
                            ended_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                startedSession.sessionId()
        );

        mockMvc.perform(patch("/api/v1/sessions/%d/end".formatted(startedSession.sessionId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + startedSession.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_ALREADY_COMPLETED"));
    }

    private StartedSession startUserFirstSession(
            String email,
            long aiTutorId,
            long categoryId,
            long scenarioId,
            long variantId
    ) throws Exception {
        JsonNode loginBody = login(email);
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedAiTutor(aiTutorId);
        assignAiTutor(userId, aiTutorId);
        seedCategory(categoryId, 1, "ACTIVE", "종료 테스트");
        seedScenario(scenarioId, categoryId, 1, "USER", "ACTIVE", 2, null);
        seedScenarioVariant(
                variantId,
                scenarioId,
                "종료",
                "종료",
                "종료",
                "먼저 말해보세요.",
                null,
                null,
                null,
                null,
                "ACTIVE"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/%d/sessions".formatted(scenarioId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();
        long sessionId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("data")
                .get("sessionId")
                .asLong();
        return new StartedSession(userId, accessToken, sessionId);
    }

    private JsonNode login(String email) throws Exception {
        String nonce = UUID.randomUUID().toString();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"%s|%s|Scenario Session User|%s",
                                  "nonce":"%s"
                                }
                                """.formatted(UUID.randomUUID(), email, nonce, nonce)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private long startScenario(String accessToken, long scenarioId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/%d/sessions".formatted(scenarioId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("data")
                .get("sessionId")
                .asLong();
    }

    private void seedAiTutor(long aiTutorId) {
        jdbcTemplate.update("""
                        INSERT INTO ai_tutor (
                            id,
                            accent_locale,
                            target_locale,
                            voice_provider,
                            voice_id,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'en-US', 'en', 'TEST', 'voice-id', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                aiTutorId
        );
    }

    private void assignAiTutor(long userId, long aiTutorId) {
        jdbcTemplate.update(
                "UPDATE user_profile SET ai_tutor_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                aiTutorId,
                userId
        );
    }

    private void seedCategory(long categoryId, int displayOrder, String status, String name) {
        jdbcTemplate.update("""
                        INSERT INTO category (id, display_order, status, created_at, updated_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                categoryId,
                displayOrder,
                status
        );
        jdbcTemplate.update("""
                        INSERT INTO category_language_variant (
                            category_id,
                            base_locale,
                            name,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'ko', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                categoryId,
                name
        );
    }

    private void seedScenario(
            long scenarioId,
            long categoryId,
            int displayOrder,
            String firstSpeaker,
            String status,
            int totalQuestionCount,
            String ttsVoiceSetId
    ) {
        jdbcTemplate.update("""
                        INSERT INTO scenario (
                            id,
                            category_id,
                            ai_role,
                            difficulty,
                            first_speaker,
                            total_question_count,
                            thumbnail_url,
                            display_order,
                            status,
                            tts_voice_set_id,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, 'tutor', 'EASY', ?, ?, NULL, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                scenarioId,
                categoryId,
                firstSpeaker,
                totalQuestionCount,
                displayOrder,
                status,
                ttsVoiceSetId
        );
    }

    private void seedScenarioVariant(
            long variantId,
            long scenarioId,
            String title,
            String briefing,
            String conversationGoal,
            String userOpeningInstruction,
            String aiOpeningMessage,
            String aiOpeningMessageTranslation,
            String aiOpeningInnerThought,
            String aiOpeningInnerThoughtType,
            String status
    ) {
        jdbcTemplate.update("""
                        INSERT INTO scenario_language_variant (
                            id,
                            scenario_id,
                            target_locale,
                            base_locale,
                            title,
                            briefing,
                            user_opening_instruction,
                            conversation_goal,
                            ai_opening_message,
                            ai_opening_message_translation,
                            ai_opening_inner_thought,
                            ai_opening_inner_thought_type,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, 'en', 'ko', ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                variantId,
                scenarioId,
                title,
                briefing,
                userOpeningInstruction,
                conversationGoal,
                aiOpeningMessage,
                aiOpeningMessageTranslation,
                aiOpeningInnerThought,
                aiOpeningInnerThoughtType,
                status
        );
    }

    private void seedScenarioQuestion(
            long questionId,
            long scenarioId,
            int displayOrder,
            String questionText,
            String questionTranslation
    ) {
        jdbcTemplate.update("""
                        INSERT INTO scenario_question (
                            id,
                            scenario_id,
                            display_order,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                questionId,
                scenarioId,
                displayOrder
        );
        jdbcTemplate.update("""
                        INSERT INTO scenario_question_language_variant (
                            scenario_question_id,
                            target_locale,
                            base_locale,
                            question_text,
                            question_translation,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'en', 'ko', ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                questionId,
                questionText,
                questionTranslation
        );
    }

    private void assertLearningSession(
            long sessionId,
            long userId,
            long aiTutorId,
            String status,
            String endedBy,
            String completionReason
    ) {
        Map<String, Object> session = jdbcTemplate.queryForMap(
                """
                        SELECT user_profile_id,
                               session_type,
                               ai_tutor_id,
                               target_locale,
                               base_locale,
                               input_mode,
                               status,
                               ended_by,
                               completion_reason
                        FROM learning_session
                        WHERE id = ?
                        """,
                sessionId
        );
        assertThat(session.get("USER_PROFILE_ID")).isEqualTo(userId);
        assertThat(session.get("SESSION_TYPE")).isEqualTo("SCENARIO");
        assertThat(session.get("AI_TUTOR_ID")).isEqualTo(aiTutorId);
        assertThat(session.get("TARGET_LOCALE")).isEqualTo("en");
        assertThat(session.get("BASE_LOCALE")).isEqualTo("ko");
        assertThat(session.get("INPUT_MODE")).isEqualTo("MIXED");
        assertThat(session.get("STATUS")).isEqualTo(status);
        assertThat(session.get("ENDED_BY")).isEqualTo(endedBy);
        assertThat(session.get("COMPLETION_REASON")).isEqualTo(completionReason);
    }

    private void assertScenarioSession(long sessionId, long variantId) {
        Map<String, Object> scenarioSession = jdbcTemplate.queryForMap(
                """
                        SELECT scenario_language_variant_id, goal_completion_status
                        FROM scenario_session
                        WHERE learning_session_id = ?
                        """,
                sessionId
        );
        assertThat(scenarioSession.get("SCENARIO_LANGUAGE_VARIANT_ID")).isEqualTo(variantId);
        assertThat(scenarioSession.get("GOAL_COMPLETION_STATUS")).isEqualTo("NOT_STARTED");
    }

    private void assertScenarioSessionGoalStatus(long sessionId, String goalCompletionStatus) {
        String actualStatus = jdbcTemplate.queryForObject(
                """
                        SELECT goal_completion_status
                        FROM scenario_session
                        WHERE learning_session_id = ?
                        """,
                String.class,
                sessionId
        );
        assertThat(actualStatus).isEqualTo(goalCompletionStatus);
    }

    private void assertSessionHistoryCompleted(long sessionId, int userMessageCount) {
        Map<String, Object> history = jdbcTemplate.queryForMap(
                """
                        SELECT ended_at, duration_seconds, user_message_count
                        FROM session_history
                        WHERE learning_session_id = ?
                        """,
                sessionId
        );
        assertThat(history.get("ENDED_AT")).isNotNull();
        assertThat(history.get("DURATION_SECONDS")).isNotNull();
        assertThat(history.get("USER_MESSAGE_COUNT")).isEqualTo(userMessageCount);
    }

    private void assertProgress(long userId, long scenarioId, String status) {
        Map<String, Object> progress = jdbcTemplate.queryForMap(
                """
                        SELECT status, completed_count, last_played_at
                        FROM user_scenario_progress
                        WHERE user_profile_id = ?
                          AND scenario_id = ?
                          AND target_locale = 'en'
                        """,
                userId,
                scenarioId
        );
        assertThat(progress.get("STATUS")).isEqualTo(status);
        assertThat(progress.get("COMPLETED_COUNT")).isEqualTo(0);
        assertThat(progress.get("LAST_PLAYED_AT")).isNotNull();
    }

    private void assertHistoryMessage(long sessionId, String role, String content) {
        Map<String, Object> message = jdbcTemplate.queryForMap(
                """
                        SELECT shm.role, shm.content, shm.input_type, shm.message_sequence, shm.turn_number
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        """,
                sessionId
        );
        assertThat(message.get("ROLE")).isEqualTo(role);
        assertThat(message.get("CONTENT")).isEqualTo(content);
        assertThat(message.get("INPUT_TYPE")).isEqualTo("GENERATED");
        assertThat(message.get("MESSAGE_SEQUENCE")).isEqualTo(1);
        assertThat(message.get("TURN_NUMBER")).isEqualTo(1);
    }

    private record StartedSession(long userId, String accessToken, long sessionId) {
    }

    @TestConfiguration
    static class FakeAiClientConfiguration {

        @Bean
        @Primary
        FakeAiConversationClient fakeAiConversationClient() {
            return new FakeAiConversationClient();
        }
    }

    private static class FakeAiConversationClient implements AiConversationClient {

        private AiNextMessageRequest lastNextMessageRequest;

        private AiClosingMessageRequest lastClosingMessageRequest;

        private GoalCompletionStatus nextGoalCompletionStatus = GoalCompletionStatus.PARTIAL;

        private boolean failNextMessageGeneration;

        @Override
        public AiNextMessageResult generateNextMessage(AiNextMessageRequest request) {
            lastNextMessageRequest = request;
            if (failNextMessageGeneration) {
                throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
            }
            return new AiNextMessageResult(
                    "Oh, you like spicy pizza. What food did you eat recently?",
                    "아, 매콤한 피자를 좋아하는구나. 최근에는 어떤 음식을 먹었어?",
                    "매운 피자를 좋아한다고 이유까지 말해주네.",
                    InnerThoughtType.GOOD,
                    nextGoalCompletionStatus
            );
        }

        @Override
        public AiClosingMessageResult generateClosingMessage(AiClosingMessageRequest request) {
            lastClosingMessageRequest = request;
            return new AiClosingMessageResult(
                    "Thanks for sharing. That was a good conversation.",
                    "이야기해줘서 고마워. 좋은 대화였어.",
                    "마지막까지 답해줘서 대화를 자연스럽게 마무리하면 좋겠다.",
                    InnerThoughtType.NORMAL
            );
        }

        private void reset() {
            lastNextMessageRequest = null;
            lastClosingMessageRequest = null;
            nextGoalCompletionStatus = GoalCompletionStatus.PARTIAL;
            failNextMessageGeneration = false;
        }

        private void completeGoalOnNextMessage() {
            nextGoalCompletionStatus = GoalCompletionStatus.COMPLETED;
        }

        private void failNextMessageGeneration() {
            failNextMessageGeneration = true;
        }

        private AiNextMessageRequest lastNextMessageRequest() {
            return lastNextMessageRequest;
        }

        private AiClosingMessageRequest lastClosingMessageRequest() {
            return lastClosingMessageRequest;
        }
    }
}
