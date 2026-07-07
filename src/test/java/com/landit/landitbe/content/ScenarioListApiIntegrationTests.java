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
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
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
        JsonNode loginBody = login();
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedScenarios(userId);

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
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].openingPreview.ttsVoiceSetId")
                        .value("voice-ai"))
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
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].openingPreview.ttsVoiceSetId")
                        .value(nullValue()))
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

    private JsonNode login() throws Exception {
        String nonce = UUID.randomUUID().toString();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/social-login")
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
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private void seedScenarios(long userId) {
        insertCategory(100, 2, "ACTIVE", "두 번째 카테고리");
        insertCategory(101, 1, "ACTIVE", "첫 번째 카테고리");
        insertCategory(102, 3, "INACTIVE", "잠긴 카테고리");

        insertScenario(201, 101, 2, "USER", "NORMAL", "ACTIVE", null, null);
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
                "ACTIVE"
        );

        insertScenario(202, 101, 1, "AI", "EASY", "ACTIVE", "https://cdn.landit.com/ai.png", "voice-ai");
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
                "ACTIVE"
        );

        insertScenario(203, 100, 1, "AI", "HARD", "INACTIVE", null, null);
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
                "ACTIVE"
        );

        insertScenario(204, 102, 1, "AI", "EASY", "ACTIVE", null, null);
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
                "ACTIVE"
        );

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
                        VALUES (?, 202, 'en', 'CLEARED', 4, 90, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                userId
        );
    }

    private void insertCategory(long categoryId, int displayOrder, String status, String name) {
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

    private void insertScenario(
            long scenarioId,
            long categoryId,
            int displayOrder,
            String firstSpeaker,
            String difficulty,
            String status,
            String thumbnailUrl,
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
                        VALUES (?, ?, 'tutor', ?, ?, 3, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                scenarioId,
                categoryId,
                difficulty,
                firstSpeaker,
                thumbnailUrl,
                displayOrder,
                status,
                ttsVoiceSetId
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
            String status
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
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'en', 'ko', ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
                status
        );
    }
}
