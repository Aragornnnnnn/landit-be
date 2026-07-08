// 원어민 표현 추가 예문 조회 API의 인증, 예문 목록/작문 문제 응답, 예외 처리를 검증한다.
package com.landit.landitbe.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
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
        "landit.auth.oidc.fake-enabled=true",   // 가짜 소셜 로그인 활성화 (실제 구글/애플 호출 없음)
        "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"  // 테스트 전용 더미 시크릿
})
class ExpressionPracticeApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 토큰 없이 호출하면 401(AUTH_REQUIRED)로 거절되는지 검증한다. */
    @Test
    void practiceRejectsMissingAccessToken() throws Exception {
        // given: 조회 대상 표현이 DB에 존재
        Long expressionId = seedExpressionWithPracticeExamples();

        // when: Authorization 헤더 없이 호출하면
        // then: 401 + AUTH_REQUIRED
        mockMvc.perform(get("/api/v1/expressions/{expressionId}/practice", expressionId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    /** 정상 호출 시 표현 정보 + 예문 4개 + 작문 문제(예문 중 하나)가 응답에 담기는지 검증한다. */
    @Test
    void practiceReturnsExamplesAndWritingSentence() throws Exception {
        // given: payload에 예문 4개를 가진 표현이 DB에 존재하고, 로그인한 상태
        Long expressionId = seedExpressionWithPracticeExamples();
        String accessToken = login("google-practice-1", "practice@example.com", "Practice User", "practice-nonce");

        // when: 토큰을 붙여 추가 예문 조회 API를 호출하면
        MvcResult result = mockMvc.perform(get("/api/v1/expressions/{expressionId}/practice", expressionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                // then: 200 + 표현 정보와 예문 목록이 시딩한 값 그대로 나온다
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetExpressionText").value("blow my mind"))
                .andExpect(jsonPath("$.data.baseExpressionMeaningText").value("끝내주게 놀랍다"))
                .andExpect(jsonPath("$.data.usageDescription").value("강렬한 인상을 받았을 때 최고의 리액션이에요."))
                .andExpect(jsonPath("$.data.practiceSentence.length()").value(4))
                // 첫 번째 예문으로 전 필드 매핑 검증 (payload에 넣은 값 그대로인지)
                .andExpect(jsonPath("$.data.practiceSentence[0].sentenceText").value("practice-sentence-0"))
                .andExpect(jsonPath("$.data.practiceSentence[0].highlightingPart").value("highlight-0"))
                .andExpect(jsonPath("$.data.practiceSentence[0].sentenceTranslation").value("예문해석-0"))
                .andExpect(jsonPath("$.data.practiceSentence[0].practiceQuestion").value("question-0"))
                .andExpect(jsonPath("$.data.practiceSentence[0].practiceQuestionTranslation").value("질문해석-0"))
                .andExpect(jsonPath("$.data.practiceSentence[0].imageUrl").value("https://cdn.example.com/practice/0.png"))
                .andReturn();

        // then: writingSentence는 랜덤이라 특정 값 고정 검증이 불가능하므로,
        //       "예문 4개 중 하나에서 만들어졌는지"(문장/해석/질문 세트가 일치하는지)를 검증한다
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
        JsonNode writingSentence = data.get("writingSentence");
        List<String> seededSentenceTexts = List.of(
                "practice-sentence-0", "practice-sentence-1", "practice-sentence-2", "practice-sentence-3");

        String pickedText = writingSentence.get("writingSentenceText").asText();
        assertThat(seededSentenceTexts).contains(pickedText);

        // 뽑힌 예문의 인덱스(끝자리 숫자)를 알아내서, 해석/질문도 같은 예문에서 왔는지 확인
        String index = pickedText.substring(pickedText.length() - 1);
        assertThat(writingSentence.get("writingSentenceTranslation").asText()).isEqualTo("예문해석-" + index);
        assertThat(writingSentence.get("writingQuestion").asText()).isEqualTo("question-" + index);
        assertThat(writingSentence.get("writingQuestionTranslation").asText()).isEqualTo("질문해석-" + index);
    }

    /** 존재하지 않는 표현 ID로 호출하면 404(RESOURCE_NOT_FOUND)로 거절되는지 검증한다. */
    @Test
    void practiceRejectsUnknownExpression() throws Exception {
        // given: 로그인만 하고, 표현은 심지 않은 상태
        String accessToken = login("google-practice-2", "practice2@example.com", "Practice User2", "practice-nonce-2");

        // when: DB에 없는 ID(999999)로 호출하면
        // then: 404 + RESOURCE_NOT_FOUND
        mockMvc.perform(get("/api/v1/expressions/{expressionId}/practice", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    /** INACTIVE(내려간) 표현은 존재하지 않는 것처럼 404(RESOURCE_NOT_FOUND)로 거절되는지 검증한다. */
    @Test
    void practiceRejectsInactiveExpression() throws Exception {
        // given: INACTIVE 상태로 심어진 표현 (payload에 예문 4개가 있어도 노출되면 안 됨)
        Long expressionId = seedExpressionWithPracticeExamples("INACTIVE");
        String accessToken = login("google-practice-3", "practice3@example.com", "Practice User3", "practice-nonce-3");

        // when: 그 표현 ID로 호출하면
        // then: 404 + RESOURCE_NOT_FOUND
        mockMvc.perform(get("/api/v1/expressions/{expressionId}/practice", expressionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    /**
     * 테스트용 Writing 표현 1건을 "추가 예문 4개가 담긴 payload"와 함께 DB에 심고 그 표현의 PK를 반환한다.
     *
     * writing_expression은 scenario_id가 NOT NULL + 외래키(FK)라서 표현만 단독으로 넣을 수 없다.
     * 그래서 부모 테이블부터 순서대로 심는다: category → scenario → writing_expression.
     * practice_examples_payload에는 콘텐츠 시딩 계약대로 camelCase 키의 JSON 배열을 넣는다.
     */
    private Long seedExpressionWithPracticeExamples() {
        return seedExpressionWithPracticeExamples("ACTIVE");
    }

    /** status를 지정해 표현을 심는 버전. INACTIVE(내려간 콘텐츠) 케이스 검증에 사용한다. */
    private Long seedExpressionWithPracticeExamples(String status) {
        LocalDateTime now = LocalDateTime.now();

        // 1) 최상위 부모: category
        Long categoryId = insertAndGetId(
                "INSERT INTO category (display_order, status, created_at, updated_at) VALUES (?, 'ACTIVE', ?, ?)",
                nextDisplayOrder("category"), now, now
        );

        // 2) 중간 부모: scenario (category FK 필요)
        Long scenarioId = insertAndGetId(
                "INSERT INTO scenario "
                        + "(category_id, ai_role, difficulty, first_speaker, total_question_count, "
                        + "display_order, status, created_at, updated_at) "
                        + "VALUES (?, 'barista', 'NORMAL', 'AI', 5, ?, 'ACTIVE', ?, ?)",
                categoryId, nextDisplayOrder("scenario"), now, now
        );

        // 3) 표현 + 추가 예문 payload (인덱스 0~3으로 구분되는 예문 4개)
        return insertAndGetId(
                "INSERT INTO writing_expression "
                        + "(scenario_id, expression_type, usage_frequency_level, target_locale, base_locale, "
                        + "display_order, target_expression_text, base_expression_meaning_text, usage_summary, "
                        + "usage_description, representative_sentence_text, representative_sentence_translation, "
                        + "representative_sentence_translation_highlight_text, "
                        + "practice_examples_payload, status, created_at, updated_at) "
                        // H2에서 CAST(? AS jsonb)는 문자열을 "JSON 문자열 값"으로 저장해버려서(배열로 파싱 안 됨)
                        // 진짜 JSON으로 파싱해 저장하는 H2 문법인 "? FORMAT JSON"을 쓴다.
                        + "VALUES (?, 'DAILY_ROUTINE', 'BASIC', 'en', 'ko', 1, 'blow my mind', '끝내주게 놀랍다', "
                        + "'usage summary', '강렬한 인상을 받았을 때 최고의 리액션이에요.', "
                        + "'representative sentence', '대표 예문 해석', '대표 강조', "
                        + "? FORMAT JSON, ?, ?, ?)",
                scenarioId, practiceExamplesPayloadJson(), status, now, now
        );
    }

    /** 추가 예문 4개짜리 payload JSON 문자열을 만든다. (= practice_examples_payload) */
    private String practiceExamplesPayloadJson() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("""
                    {
                      "sentenceText": "practice-sentence-%d",
                      "highlightingPart": "highlight-%d",
                      "sentenceTranslation": "예문해석-%d",
                      "practiceQuestion": "question-%d",
                      "practiceQuestionTranslation": "질문해석-%d",
                      "imageUrl": "https://cdn.example.com/practice/%d.png"
                    }
                    """.formatted(i, i, i, i, i, i));
        }
        return json.append("]").toString();
    }

    /**
     * INSERT를 실행하고 DB가 자동 생성한 PK(id)를 돌려주는 유틸.
     * H2가 PostgreSQL의 "RETURNING id" 문법을 지원하지 않아서
     * 스프링의 GeneratedKeyHolder로 생성된 키를 받는 방식을 쓴다.
     */
    private Long insertAndGetId(String sql, Object... args) {
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
}
