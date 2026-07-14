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

@ActiveProfiles("test")           // test 프로파일 → H2 인메모리 DB 사용
@AutoConfigureMockMvc             // 서버 포트 없이 HTTP 요청을 흉내 내는 MockMvc 활성화
@SpringBootTest                   // 스프링 앱 전체(컨트롤러~DB)를 실제로 띄우는 통합 테스트
@TestPropertySource(properties = {
        "landit.auth.oidc.fake-enabled=true",   // 가짜 소셜 로그인 활성화 (실제 구글/애플 호출 없음)
        "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"  // 테스트 전용 더미 시크릿
})
class ExpressionApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;          // 가짜 HTTP 요청을 보내는 도구

    @Autowired
    private JdbcTemplate jdbcTemplate; // 테스트 데이터를 SQL로 직접 심기 위한 도구
                                       // (엔티티 생성자가 protected라 객체로는 못 만들어서 SQL 사용)

    private final ObjectMapper objectMapper = new ObjectMapper(); // 응답 JSON 파싱용

    /** 토큰 없이 호출하면 401(INVALID_TOKEN)로 거절되는지 검증한다. */
    @Test
    void getExpressionsRejectsMissingAccessToken() throws Exception {
        // given: 조회 대상 시나리오와 표현들이 DB에 존재
        Long scenarioId = seedScenarioWithExpressions();

        // when: Authorization 헤더 없이 호출하면
        // then: 401 + INVALID_TOKEN
        mockMvc.perform(get("/api/v1/expressions/{scenarioId}", scenarioId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    /** 일부 완료 상태에서 표현이 학습 순서대로 반환되고, 완료/해금/잠김 3가지 상태가 규칙대로 계산되는지 검증한다. */
    @Test
    void getExpressionsReturnsOrderedListWithCompletionAndLockStatus() throws Exception {
        // given: 표현 5개가 있는 시나리오에서 1번 표현만 완료한 사용자
        Long scenarioId = seedScenarioWithExpressions();
        Long firstExpressionId = findExpressionIdByDisplayOrder(scenarioId, 1);
        Long secondExpressionId = findExpressionIdByDisplayOrder(scenarioId, 2);
        Long thirdExpressionId = findExpressionIdByDisplayOrder(scenarioId, 3);

        String accessToken = login("google-expr-1", "expr@example.com", "Expr User", "expr-nonce");
        Long userProfileId = findUserProfileIdByEmail("expr@example.com");
        markExpressionCompleted(userProfileId, scenarioId, firstExpressionId);

        // when: 표현 목록을 조회하면
        // then: displayOrder 오름차순 + 완료(1번)/해금(2번=첫 미완료)/잠김(3~5번) 상태가 정확하다
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

    /** 모든 표현을 완료한 사용자는 전부 completed=true, locked=false로 받는지 검증한다. (해금 대상이 없는 엣지 케이스) */
    @Test
    void getExpressionsUnlocksEveryExpressionWhenAllCompleted() throws Exception {
        // given: 표현 5개를 전부 완료한 사용자
        Long scenarioId = seedScenarioWithExpressions();

        String accessToken = login("google-expr-3", "expr3@example.com", "Expr User3", "expr-nonce-3");
        Long userProfileId = findUserProfileIdByEmail("expr3@example.com");
        for (int displayOrder = 1; displayOrder <= 5; displayOrder++) {
            markExpressionCompleted(userProfileId, scenarioId, findExpressionIdByDisplayOrder(scenarioId, displayOrder));
        }

        // when: 표현 목록을 조회하면
        // then: 전부 완료 상태이고 잠긴 표현이 없다
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

    /** 아무것도 완료하지 않은 사용자는 학습 순서 첫 번째 표현만 해금되는지 검증한다. (최초 진입 상태) */
    @Test
    void getExpressionsUnlocksOnlyFirstWhenNoneCompleted() throws Exception {
        // given: 아무 표현도 완료하지 않은 사용자
        Long scenarioId = seedScenarioWithExpressions();

        String accessToken = login("google-expr-4", "expr4@example.com", "Expr User4", "expr-nonce-4");

        // when: 표현 목록을 조회하면
        // then: displayOrder 1번만 해금(locked=false)이고 나머지는 잠긴다
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

    /** 존재하지 않는 시나리오 ID로 호출하면 404(SCENARIO_NOT_FOUND)로 거절되는지 검증한다. */
    @Test
    void getExpressionsRejectsUnknownScenario() throws Exception {
        // given: 로그인만 하고, 시나리오는 심지 않은 상태
        String accessToken = login("google-expr-2", "expr2@example.com", "Expr User2", "expr-nonce-2");

        // when: DB에 없는 ID(999999)로 호출하면
        // then: 404 + SCENARIO_NOT_FOUND
        mockMvc.perform(get("/api/v1/expressions/{scenarioId}", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SCENARIO_NOT_FOUND"));
    }

    /**
     * 다국어 데이터가 섞여 있어도 사용자 locale(en/ko)의 표현만 반환되는지 검증한다.
     * locale이 다른 표현은 displayOrder가 겹칠 수 있어서(UNIQUE가 locale 조합별), 필터가 없으면
     * 응답에 섞여 들어가고 순차 해금 순서도 깨진다.
     */
    @Test
    void getExpressionsReturnsOnlyUserLocaleExpressions() throws Exception {
        // given: en-ko 표현 5개가 있는 시나리오에 en-ja 표현 1개(displayOrder 1 중복)를 추가로 심는다
        Long scenarioId = seedScenarioWithExpressions();
        insertWritingExpression(scenarioId, "EN", "JA", 1, "ja-only expression", "日本語訳", LocalDateTime.now());

        String accessToken = login("google-expr-5", "expr5@example.com", "Expr User5", "expr-nonce-5");

        // when & then: 사용자(기본 en/ko)에게는 en-ko 5개만 보이고 en-ja 표현은 제외된다
        mockMvc.perform(get("/api/v1/expressions/{scenarioId}", scenarioId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[*].targetExpressionText").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("ja-only expression"))))
                // 해금 판정도 en-ko 시퀀스 기준으로 유지된다 (displayOrder 1이 해금)
                .andExpect(jsonPath("$.data[0].displayOrder").value(1))
                .andExpect(jsonPath("$.data[0].locked").value(false));
    }

    /**
     * 테스트용 시나리오 1개와 en-ko 표현 5개(displayOrder 1~5)를 DB에 심고 시나리오 PK를 반환한다.
     *
     * writing_expression은 scenario_id가 NOT NULL + 외래키(FK)라서 표현만 단독으로 넣을 수 없다.
     * 그래서 부모 테이블부터 순서대로 심는다: category → scenario → writing_expression.
     * 표현을 일부러 displayOrder 역순(5→1)으로 INSERT해서, API가 정렬을 제대로 하는지도 함께 검증된다.
     */
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

    /**
     * INSERT를 실행하고 DB가 자동 생성한 PK(id)를 돌려주는 유틸. (범용 메서드)
     * H2가 PostgreSQL의 "RETURNING id" 문법을 지원하지 않아서
     * 스프링의 GeneratedKeyHolder로 생성된 키를 받는 방식을 쓴다.
     */
    private Long insertAndGetId(String sql, Object... args) { // sql을 통째로 받음
        KeyHolder keyHolder = new GeneratedKeyHolder(); // 생성된 PK가 담길 그릇
        jdbcTemplate.update(connection -> {
            // RETURN_GENERATED_KEYS: 실행 후 자동 생성 키를 돌려달라는 옵션
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            // SQL의 ? 자리(1번부터 시작)에 가변인자로 받은 값들을 순서대로 채운다
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue(); // 그릇에서 생성된 PK를 꺼낸다
    }

    private void insertWritingExpression(
            Long scenarioId,
            int displayOrder,
            String targetExpressionText,
            String baseExpressionMeaningText,
            LocalDateTime now
    ) {
        // 기본 시딩은 사용자 기본 locale(en/ko)과 동일하게 심는다.
        insertWritingExpression(scenarioId, "EN", "KR", displayOrder, targetExpressionText, baseExpressionMeaningText, now);
    }

    /** locale까지 지정해 표현을 심는 버전. 다국어 데이터 필터 검증에 사용한다. */
    private void insertWritingExpression(
            Long scenarioId,
            String targetLocale,
            String baseLocale,
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
                        + "representative_sentence_words, representative_sentence_word_choices, practice_examples_payload, status, "
                        + "created_at, updated_at) "
                        + "VALUES (?, 'DAILY_ROUTINE', 'BASIC', ?, ?, ?, ?, ?, 'usage summary', "
                        + "'usage description', 'sample sentence', '샘플 문장', ARRAY['sample'], ARRAY['sample','choice'], CAST(? AS jsonb), 'ACTIVE', ?, ?)",
                scenarioId, targetLocale, baseLocale, displayOrder,
                targetExpressionText, baseExpressionMeaningText, "[]", now, now
        );
    }

    /** 심어둔 표현의 실제 PK를 학습 순서로 찾아온다. (PK는 DB가 자동 생성하므로 미리 알 수 없음) */
    private Long findExpressionIdByDisplayOrder(Long scenarioId, int displayOrder) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM writing_expression WHERE scenario_id = ? AND display_order = ?",
                Long.class, scenarioId, displayOrder
        );
    }

    /** "사용자가 이 표현을 완료했다"는 기록을 DB에 심는다. */
    private void markExpressionCompleted(Long userProfileId, Long scenarioId, Long writingExpressionId) {
        jdbcTemplate.update(
                "INSERT INTO user_writing_expression_completion "
                        + "(user_profile_id, scenario_id, writing_expression_id, completed_at, last_completed_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                userProfileId, scenarioId, writingExpressionId, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    /** display_order에 UNIQUE 제약이 있어, 다른 테스트와 겹치지 않게 현재 최댓값+1을 반환한다. */
    private int nextDisplayOrder(String tableName) {
        Integer maxOrder = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(display_order), 0) FROM " + tableName, Integer.class
        );
        return maxOrder + 1;
    }

    /**
     * 가짜 소셜 로그인으로 진짜 accessToken을 발급받는 헬퍼.
     * fake-enabled=true 설정 덕분에 idToken에 "sub|이메일|닉네임|nonce" 문자열만 넣으면
     * 실제 구글 호출 없이 로그인이 성공하고, 응답에서 accessToken을 꺼내 반환한다.
     */
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

    /** 로그인으로 생성된 사용자의 PK를 이메일로 찾아온다. (완료 기록 시딩에 필요) */
    private Long findUserProfileIdByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM user_profile WHERE email = ?", Long.class, email
        );
    }
}
