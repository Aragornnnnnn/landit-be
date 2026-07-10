// 시나리오 목록 API의 정렬, 진행도, 잠금, 시작 미리보기를 검증한다.
package com.landit.landitbe.content;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class ScenarioListApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM user_writing_expression_completion");
        jdbcTemplate.update("DELETE FROM writing_expression");
        jdbcTemplate.update("DELETE FROM user_scenario_progress");
        jdbcTemplate.update("DELETE FROM scenario_language_variant");
        jdbcTemplate.update("DELETE FROM scenario");
        jdbcTemplate.update("DELETE FROM category_language_variant");
        jdbcTemplate.update("DELETE FROM category");
    }

    @Test
    void scenariosRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void scenariosRejectInvalidAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void scenariosReturnOrderedProgressLockAndOpeningPreview() throws Exception {
        JsonNode loginResponseBody = login();
        long userId = loginResponseBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginResponseBody.get("data").get("accessToken").asText();
        seedScenarioListData(userId);

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].categoryId").value(101))
                .andExpect(jsonPath("$.data.categories[0].categoryName").value("첫 번째 카테고리"))
                .andExpect(jsonPath("$.data.categories[0].displayOrder").value(1))
                .andExpect(jsonPath("$.data.categories[0].categoryLocked").value(false))
                .andExpect(jsonPath("$.data.categories[0].categoryLockReason").value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].scenarioId").value(202))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].starRating").value(2.5))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].displayOrder").value(1))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].scenarioTitle").value("AI 먼저 말하기"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].briefing").value("AI가 먼저 질문합니다."))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].conversationGoal").value("좋아하는 음식을 설명한다."))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].difficulty").value("EASY"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].firstSpeaker").value("AI"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].thumbnailUrl").value("https://cdn.landit.com/ai.png"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].completed").value(true))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].locked").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].lockReason").value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.aiOpeningMessage")
                        .value("What is your favorite food?"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.aiOpeningMessageTranslation")
                        .value("가장 좋아하는 음식이 뭐예요?"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.userOpeningInstruction")
                        .value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.innerThought")
                        .value("음식 이야기는 대화를 열기 좋다."))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.innerThoughtType").value("GOOD"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.ttsVoice.provider")
                        .value("OPENROUTER"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.ttsVoice.model")
                        .value("microsoft/mai-voice-2"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.ttsVoice.providerVoiceId")
                        .value("en-US-Harper:MAI-Voice-2"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.ttsVoice.gender")
                        .value("MALE"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].scenarioId").value(201))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].starRating").value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].completed").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].firstSpeaker").value("USER"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].openingPreview.aiOpeningMessage")
                        .value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].openingPreview.aiOpeningMessageTranslation")
                        .value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].openingPreview.userOpeningInstruction")
                        .value("점원에게 먼저 주문하고 싶은 음료를 말해보세요."))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].openingPreview.innerThought")
                        .value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].openingPreview.innerThoughtType")
                        .value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].openingPreview.ttsVoice.providerVoiceId")
                        .value("en-US-Ethan:MAI-Voice-2"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].openingPreview.ttsVoice.gender")
                        .value("FEMALE"))
                .andExpect(jsonPath("$.data.categories[1].categoryId").value(100))
                .andExpect(jsonPath("$.data.categories[1].scenarios[0].locked").value(true))
                .andExpect(jsonPath("$.data.categories[1].scenarios[0].lockReason").isNotEmpty())
                .andExpect(jsonPath("$.data.categories[1].scenarios[0].openingPreview").value(nullValue()))
                .andExpect(jsonPath("$.data.categories[2].categoryId").value(102))
                .andExpect(jsonPath("$.data.categories[2].categoryLocked").value(true))
                .andExpect(jsonPath("$.data.categories[2].categoryLockReason").isNotEmpty())
                .andExpect(jsonPath("$.data.categories[2].scenarios[0].locked").value(true))
                .andExpect(jsonPath("$.data.categories[2].scenarios[0].openingPreview").value(nullValue()));
    }

    @Test
    void scenariosLockNextScenarioUntilPreviousScenarioIsCleared() throws Exception {
        JsonNode loginResponseBody = login();
        seedScenarioListData(null);

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginResponseBody.get("data").get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].completed").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].locked").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].completed").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].locked").value(true))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].lockReason")
                        .value("PREVIOUS_SCENARIO_NOT_COMPLETED"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].openingPreview").value(nullValue()));
    }

    @Test
    void scenariosReturnNullTtsVoiceWhenVoiceIsInactiveOrMissing() throws Exception {
        JsonNode loginResponseBody = login();
        long inactiveVoiceId = insertTtsVoice(
                990200,
                "test-inactive-voice",
                "INACTIVE"
        );
        insertCategory(110, 1, "ACTIVE", "비활성 음성");
        insertScenario(210, 110, 1, "AI", "EASY", "ACTIVE", null);
        insertScenarioVariant(
                210,
                "비활성 음성 시나리오",
                "비활성 음성을 사용합니다.",
                "비활성 음성 응답을 확인한다.",
                null,
                "Hello",
                "안녕하세요",
                null,
                null,
                inactiveVoiceId,
                "ACTIVE"
        );
        insertCategory(111, 2, "ACTIVE", "미설정 음성");
        insertScenario(211, 111, 1, "USER", "EASY", "ACTIVE", null);
        insertScenarioVariant(
                211,
                "미설정 음성 시나리오",
                "음성을 설정하지 않았습니다.",
                "미설정 음성 응답을 확인한다.",
                "먼저 말해보세요.",
                null,
                null,
                null,
                null,
                null,
                "ACTIVE"
        );

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginResponseBody.get("data").get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.ttsVoice")
                        .value(nullValue()))
                .andExpect(jsonPath("$.data.categories[1].scenarios[0].openingPreview.ttsVoice")
                        .value(nullValue()));
    }

    private JsonNode login() throws Exception {
        String nonce = UUID.randomUUID().toString();
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"%s|scenario@example.com|Scenario User|%s",
                                  "nonce":"%s"
                                }
                                """.formatted(UUID.randomUUID(), nonce, nonce)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsByteArray());
    }

    private void seedScenarioListData(Long clearedUserId) {
        long maleVoiceId = ttsVoiceId("en-US-Harper:MAI-Voice-2");
        long femaleVoiceId = ttsVoiceId("en-US-Ethan:MAI-Voice-2");
        insertCategory(100, 2, "ACTIVE", "두 번째 카테고리");
        insertCategory(101, 1, "ACTIVE", "첫 번째 카테고리");
        insertCategory(102, 3, "INACTIVE", "잠긴 카테고리");

        insertScenario(201, 101, 2, "USER", "NORMAL", "ACTIVE", null);
        insertScenarioVariant(
                201,
                "USER 먼저 말하기",
                "사용자가 먼저 말을 겁니다.",
                "직원에게 음료를 주문한다.",
                "점원에게 먼저 주문하고 싶은 음료를 말해보세요.",
                null,
                null,
                null,
                null,
                femaleVoiceId,
                "ACTIVE"
        );

        insertScenario(202, 101, 1, "AI", "EASY", "ACTIVE", "https://cdn.landit.com/ai.png");
        insertScenarioVariant(
                202,
                "AI 먼저 말하기",
                "AI가 먼저 질문합니다.",
                "좋아하는 음식을 설명한다.",
                null,
                "What is your favorite food?",
                "가장 좋아하는 음식이 뭐예요?",
                "음식 이야기는 대화를 열기 좋다.",
                "GOOD",
                maleVoiceId,
                "ACTIVE"
        );

        insertScenario(203, 100, 1, "AI", "HARD", "INACTIVE", null);
        insertScenarioVariant(
                203,
                "잠긴 시나리오",
                "비활성 시나리오입니다.",
                "잠긴 시나리오를 확인한다.",
                null,
                "Locked opening",
                "잠긴 시작 문구",
                "잠겼다.",
                "BAD",
                null,
                "ACTIVE"
        );

        insertScenario(204, 102, 1, "AI", "EASY", "ACTIVE", null);
        insertScenarioVariant(
                204,
                "카테고리가 잠긴 시나리오",
                "카테고리가 비활성입니다.",
                "카테고리 잠금을 확인한다.",
                null,
                "Category locked opening",
                "카테고리 잠금 시작 문구",
                "카테고리가 잠겼다.",
                "NORMAL",
                null,
                "ACTIVE"
        );

        if (clearedUserId != null) {
            jdbcTemplate.update("""
                            INSERT INTO user_scenario_progress (
                                user_profile_id,
                                scenario_id,
                                target_locale,
                                status,
                                best_star_rating,
                                best_native_score,
                                completed_count,
                                first_cleared_at,
                                last_played_at,
                                created_at,
                                updated_at
                            )
                            VALUES (?, 202, 'EN', 'CLEARED', 2.5, 90, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            """,
                    clearedUserId
            );
        }
    }

    private void insertCategory(long categoryId, int displayOrder, String categoryStatus, String categoryName) {
        jdbcTemplate.update("""
                        INSERT INTO category (id, display_order, status, created_at, updated_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                categoryId,
                displayOrder,
                categoryStatus
        );
        jdbcTemplate.update("""
                        INSERT INTO category_language_variant (
                            category_id,
                            base_locale,
                            name,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'KR', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                categoryId,
                categoryName
        );
    }

    private void insertScenario(
            long scenarioId,
            long categoryId,
            int displayOrder,
            String firstSpeaker,
            String difficulty,
            String scenarioStatus,
            String thumbnailUrl
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
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, 'tutor', ?, ?, 3, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                scenarioId,
                categoryId,
                difficulty,
                firstSpeaker,
                thumbnailUrl,
                displayOrder,
                scenarioStatus
        );
    }

    private void insertScenarioVariant(
            long scenarioId,
            String title,
            String briefing,
            String conversationGoal,
            String userOpeningInstruction,
            String aiOpeningMessage,
            String aiOpeningMessageTranslation,
            String aiOpeningInnerThought,
            String aiOpeningInnerThoughtType,
            Long ttsVoiceId,
            String variantStatus
    ) {
        jdbcTemplate.update("""
                        INSERT INTO scenario_language_variant (
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
                            tts_voice_id,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'EN', 'KR', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                scenarioId,
                title,
                briefing,
                userOpeningInstruction,
                conversationGoal,
                aiOpeningMessage,
                aiOpeningMessageTranslation,
                aiOpeningInnerThought,
                aiOpeningInnerThoughtType,
                ttsVoiceId,
                variantStatus
        );
    }

    private long ttsVoiceId(String providerVoiceId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tts_voice WHERE provider_voice_id = ?",
                Long.class,
                providerVoiceId
        );
    }

    private long insertTtsVoice(long id, String providerVoiceId, String status) {
        jdbcTemplate.update("""
                        INSERT INTO tts_voice (
                            id,
                            provider,
                            model,
                            provider_voice_id,
                            gender,
                            description,
                            accent_locale,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'OPENROUTER', 'test-model', ?, 'MALE', '테스트 음성',
                                'en-US', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                id,
                providerVoiceId,
                status
        );
        return id;
    }
}
