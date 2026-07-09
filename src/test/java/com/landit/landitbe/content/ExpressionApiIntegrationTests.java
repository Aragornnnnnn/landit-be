// 시나리오별 원어민 표현 조회 API의 정렬, 완료 여부, 인증, 예외 처리를 검증한다.
package com.landit.landitbe.content;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
class ExpressionApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getExpressionsRejectsMissingAccessToken() throws Exception {
        Long scenarioId = seedScenarioWithExpressions();

        mockMvc.perform(get("/api/v1/expressions/{scenarioId}", scenarioId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    @Test
    void getExpressionsReturnsOrderedListWithCompletionAndLockStatus() throws Exception {
        Long scenarioId = seedScenarioWithExpressions();
        Long firstExpressionId = findExpressionIdByDisplayOrder(scenarioId, 1);
        Long secondExpressionId = findExpressionIdByDisplayOrder(scenarioId, 2);
        Long thirdExpressionId = findExpressionIdByDisplayOrder(scenarioId, 3);

        String accessToken = login("google-expr-1", "expr@example.com", "Expr User", "expr-nonce");
        Long userProfileId = findUserProfileIdByEmail("expr@example.com");
        markExpressionCompleted(userProfileId, scenarioId, firstExpressionId);

        mockMvc.perform(get("/api/v1/expressions/{scenarioId}", scenarioId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(5))
                // 완료한 표현: completed=true, locked=false
                .andExpect(jsonPath("$.data[0].expressionId").value(firstExpressionId))
                .andExpect(jsonPath("$.data[0].displayOrder").value(1))
                .andExpect(jsonPath("$.data[0].targetExpressionText").value("There is nothing like"))
                .andExpect(jsonPath("$.data[0].baseExpressionMeaningText").value("~만 한 게 없다"))
                .andExpect(jsonPath("$.data[0].completed").value(true))
                .andExpect(jsonPath("$.data[0].locked").value(false))
                // 미완료 표현 중 학습 순서가 가장 앞선 표현: completed=false, locked=false
                .andExpect(jsonPath("$.data[1].expressionId").value(secondExpressionId))
                .andExpect(jsonPath("$.data[1].displayOrder").value(2))
                .andExpect(jsonPath("$.data[1].completed").value(false))
                .andExpect(jsonPath("$.data[1].locked").value(false))
                // 그 뒤의 미완료 표현들: completed=false, locked=true
                .andExpect(jsonPath("$.data[2].expressionId").value(thirdExpressionId))
                .andExpect(jsonPath("$.data[2].displayOrder").value(3))
                .andExpect(jsonPath("$.data[2].completed").value(false))
                .andExpect(jsonPath("$.data[2].locked").value(true))
                .andExpect(jsonPath("$.data[3].displayOrder").value(4))
                .andExpect(jsonPath("$.data[3].completed").value(false))
                .andExpect(jsonPath("$.data[3].locked").value(true))
                .andExpect(jsonPath("$.data[4].displayOrder").value(5))
                .andExpect(jsonPath("$.data[4].completed").value(false))
                .andExpect(jsonPath("$.data[4].locked").value(true));
    }

    @Test
    void getExpressionsUnlocksEveryExpressionWhenAllCompleted() throws Exception {
        Long scenarioId = seedScenarioWithExpressions();

        String accessToken = login("google-expr-3", "expr3@example.com", "Expr User3", "expr-nonce-3");
        Long userProfileId = findUserProfileIdByEmail("expr3@example.com");
        for (int displayOrder = 1; displayOrder <= 5; displayOrder++) {
            markExpressionCompleted(userProfileId, scenarioId, findExpressionIdByDisplayOrder(scenarioId, displayOrder));
        }

        mockMvc.perform(get("/api/v1/expressions/{scenarioId}", scenarioId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                // 모두 완료했으므로 전부 completed=true, locked=false
                .andExpect(jsonPath("$.data[0].completed").value(true))
                .andExpect(jsonPath("$.data[0].locked").value(false))
                .andExpect(jsonPath("$.data[1].completed").value(true))
                .andExpect(jsonPath("$.data[1].locked").value(false))
                .andExpect(jsonPath("$.data[2].completed").value(true))
                .andExpect(jsonPath("$.data[2].locked").value(false))
                .andExpect(jsonPath("$.data[3].completed").value(true))
                .andExpect(jsonPath("$.data[3].locked").value(false))
                .andExpect(jsonPath("$.data[4].completed").value(true))
                .andExpect(jsonPath("$.data[4].locked").value(false));
    }

    @Test
    void getExpressionsUnlocksOnlyFirstWhenNoneCompleted() throws Exception {
        Long scenarioId = seedScenarioWithExpressions();

        String accessToken = login("google-expr-4", "expr4@example.com", "Expr User4", "expr-nonce-4");

        mockMvc.perform(get("/api/v1/expressions/{scenarioId}", scenarioId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                // 모두 미완료이므로 학습 순서가 가장 앞선 표현만 locked=false, 나머지는 locked=true
                .andExpect(jsonPath("$.data[0].displayOrder").value(1))
                .andExpect(jsonPath("$.data[0].completed").value(false))
                .andExpect(jsonPath("$.data[0].locked").value(false))
                .andExpect(jsonPath("$.data[1].completed").value(false))
                .andExpect(jsonPath("$.data[1].locked").value(true))
                .andExpect(jsonPath("$.data[2].completed").value(false))
                .andExpect(jsonPath("$.data[2].locked").value(true))
                .andExpect(jsonPath("$.data[3].completed").value(false))
                .andExpect(jsonPath("$.data[3].locked").value(true))
                .andExpect(jsonPath("$.data[4].completed").value(false))
                .andExpect(jsonPath("$.data[4].locked").value(true));
    }

    @Test
    void getExpressionsRejectsUnknownScenario() throws Exception {
        String accessToken = login("google-expr-2", "expr2@example.com", "Expr User2", "expr-nonce-2");

        mockMvc.perform(get("/api/v1/expressions/{scenarioId}", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SCENARIO_NOT_FOUND"));
    }

    private Long seedScenarioWithExpressions() {
        LocalDateTime now = LocalDateTime.now();
        Long categoryId = insertAndGetId(
                "INSERT INTO category (display_order, status, created_at, updated_at) VALUES (?, 'ACTIVE', ?, ?)",
                nextDisplayOrder("category"), now, now
        );
        Long scenarioId = insertAndGetId(
                "INSERT INTO scenario "
                        + "(category_id, ai_role, difficulty, first_speaker, total_question_count, "
                        + "display_order, status, created_at, updated_at) "
                        + "VALUES (?, 'barista', 'NORMAL', 'AI', 5, ?, 'ACTIVE', ?, ?)",
                categoryId, nextDisplayOrder("scenario"), now, now
        );
        insertWritingExpression(scenarioId, 5, "for here or to go", "여기서 드세요, 가져가세요?", now);
        insertWritingExpression(scenarioId, 4, "no big deal", "별거 아니야.", now);
        insertWritingExpression(scenarioId, 3, "hang in there", "조금만 버텨.", now);
        insertWritingExpression(scenarioId, 2, "blow my mind", "끝내주게 놀랍다.", now);
        insertWritingExpression(scenarioId, 1, "There is nothing like", "~만 한 게 없다", now);
        return scenarioId;
    }

    private Long insertAndGetId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private void insertWritingExpression(
            Long scenarioId,
            int displayOrder,
            String targetExpressionText,
            String baseExpressionMeaningText,
            LocalDateTime now
    ) {
        jdbcTemplate.update(
                "INSERT INTO writing_expression "
                        + "(scenario_id, expression_type, usage_frequency_level, target_locale, base_locale, "
                        + "display_order, target_expression_text, base_expression_meaning_text, usage_summary, "
                        + "usage_description, representative_sentence_text, representative_sentence_translation, "
                        + "representative_sentence_translation_highlight_text, practice_examples_payload, status, "
                        + "created_at, updated_at) "
                        + "VALUES (?, 'DAILY_ROUTINE', 'BASIC', 'en', 'ko', ?, ?, ?, 'usage summary', "
                        + "'usage description', 'sample sentence', '샘플 문장', '샘플', CAST(? AS jsonb), 'ACTIVE', ?, ?)",
                scenarioId, displayOrder, targetExpressionText, baseExpressionMeaningText, "[]", now, now
        );
    }

    private Long findExpressionIdByDisplayOrder(Long scenarioId, int displayOrder) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM writing_expression WHERE scenario_id = ? AND display_order = ?",
                Long.class, scenarioId, displayOrder
        );
    }

    private void markExpressionCompleted(Long userProfileId, Long scenarioId, Long writingExpressionId) {
        jdbcTemplate.update(
                "INSERT INTO user_writing_expression_completion "
                        + "(user_profile_id, scenario_id, writing_expression_id, completed_at) "
                        + "VALUES (?, ?, ?, ?)",
                userProfileId, scenarioId, writingExpressionId, LocalDateTime.now()
        );
    }

    private int nextDisplayOrder(String tableName) {
        Integer maxOrder = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(display_order), 0) FROM " + tableName, Integer.class
        );
        return maxOrder + 1;
    }

    private String login(String sub, String email, String nickname, String nonce) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"%s|%s|%s|%s",
                                  "nonce":"%s"
                                }
                                """.formatted(sub, email, nickname, nonce, nonce)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("accessToken").asText();
    }

    private Long findUserProfileIdByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM user_profile WHERE email = ?", Long.class, email
        );
    }
}
