// 원어민 표현 학습 시작 API의 인증, 표현 상세 응답, 예외 처리를 검증한다.
package com.landit.landitbe.content;

import static org.hamcrest.Matchers.contains;
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
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true", // 가짜 소셜 로그인 활성화 (실제 구글/애플 호출 없음)
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough" // 테스트 전용 더미 시크릿
    })
class ExpressionLearningApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate; // 테스트 데이터를 SQL로 직접 심기 위한 도구
  // (엔티티 생성자가 protected라 객체로는 못 만들어서 SQL 사용)

  private final ObjectMapper objectMapper = new ObjectMapper(); // 응답 JSON 파싱용

  /** 토큰 없이 호출하면 401(INVALID_TOKEN)로 거절되는지 검증한다. */
  @Test
  void learningStartRejectsMissingAccessToken() throws Exception {
    // given: 조회 대상 표현이 DB에 존재
    Long expressionId = seedExpression();

    // when: Authorization 헤더 없이 호출하면
    // then: 401 + INVALID_TOKEN
    mockMvc
        .perform(get("/api/v1/expressions/{expressionId}/learning-start", expressionId))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
  }

  /** 정상 호출 시 DB에 심어둔 표현 상세가 응답 필드에 그대로 담기는지 검증한다. */
  @Test
  void learningStartReturnsExpressionDetail() throws Exception {
    // given: 조회 대상 표현이 DB에 존재하고, 로그인해서 토큰을 발급받은 상태
    Long expressionId = seedExpression();
    String accessToken = login("google-learn-1", "learn@example.com", "Learn User", "learn-nonce");

    // when: 토큰을 붙여 학습 시작 API를 호출하면
    // then: 200 + 심어둔 값들이 응답에 그대로 나온다
    mockMvc
        .perform(
            get("/api/v1/expressions/{expressionId}/learning-start", expressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.expressionId").value(expressionId))
        .andExpect(jsonPath("$.data.targetExpressionText").value("blow my mind"))
        .andExpect(jsonPath("$.data.baseExpressionMeaningText").value("끝내주게 놀랍다"))
        .andExpect(jsonPath("$.data.usageDescription").value("강렬한 인상을 받았을 때 최고의 리액션이에요."))
        .andExpect(
            jsonPath("$.data.representativeQuestionText")
                .value("What should I definitely see in Korea?"))
        .andExpect(jsonPath("$.data.representativeQuestionTranslation").value("한국에서 뭘 꼭 봐야 해?"))
        .andExpect(
            jsonPath("$.data.representativeSentenceText")
                .value("Gyeongbokgung Palace will blow your mind."))
        .andExpect(
            jsonPath("$.data.representativeSentenceTranslation").value("경복궁은 널 완전 놀라게 할 거야."))
        // 정답 단어 배열: 정답 순서 그대로 유지되어야 한다.
        .andExpect(
            jsonPath("$.data.representativeSentenceWords")
                .value(contains("Gyeongbokgung", "Palace", "will", "blow", "your", "mind")))
        // 선택지 배열: DB 저장(섞인) 순서 그대로, 백엔드가 재정렬하지 않는다.
        .andExpect(
            jsonPath("$.data.representativeSentenceWordChoices")
                .value(
                    contains(
                        "Gyeongbokgung",
                        "blow",
                        "will",
                        "Palace",
                        "amazing",
                        "have",
                        "get",
                        "your",
                        "mind")))
        .andExpect(
            jsonPath("$.data.representativeImageUrl")
                .value("https://cdn.example.com/images/101.png"));
  }

  /** 존재하지 않는 표현 ID로 호출하면 404(RESOURCE_NOT_FOUND)로 거절되는지 검증한다. */
  @Test
  void learningStartRejectsUnknownExpression() throws Exception {
    // given: 로그인만 하고, 표현은 심지 않은 상태
    String accessToken =
        login("google-learn-2", "learn2@example.com", "Learn User2", "learn-nonce-2");

    // when: DB에 없는 ID(999999)로 호출하면
    // then: 404 + RESOURCE_NOT_FOUND
    mockMvc
        .perform(
            get("/api/v1/expressions/{expressionId}/learning-start", 999_999L)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
  }

  /** INACTIVE(내려간) 표현은 존재하지 않는 것처럼 404(RESOURCE_NOT_FOUND)로 거절되는지 검증한다. */
  @Test
  void learningStartRejectsInactiveExpression() throws Exception {
    // given: INACTIVE 상태로 심어진 표현
    Long expressionId = seedExpression("INACTIVE");
    String accessToken =
        login("google-learn-3", "learn3@example.com", "Learn User3", "learn-nonce-3");

    // when: 그 표현 ID로 호출하면
    // then: 404 + RESOURCE_NOT_FOUND (내려간 콘텐츠는 노출되면 안 됨)
    mockMvc
        .perform(
            get("/api/v1/expressions/{expressionId}/learning-start", expressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
  }

  /**
   * 테스트용 Writing 표현 1건을 DB에 심고 그 표현의 PK를 반환한다.
   *
   * <p>writing_expression은 scenario_id가 NOT NULL + 외래키(FK)라서 표현만 단독으로 넣을 수 없다. 그래서 부모 테이블부터 순서대로
   * 심는다: category → scenario → writing_expression. INSERT가 3번인 이유가 이것이고, 컬럼이 많은 건
   * writing_expression에 NOT NULL 컬럼이 많아서다.
   */
  private Long seedExpression() {
    return seedExpression("ACTIVE");
  }

  /** status를 지정해 표현을 심는 버전. INACTIVE(내려간 콘텐츠) 케이스 검증에 사용한다. */
  private Long seedExpression(String status) {
    LocalDateTime now = LocalDateTime.now();

    // 1) 최상위 부모: category
    Long categoryId =
        insertAndGetId(
            "INSERT INTO category (display_order, status, created_at, updated_at) VALUES (?, 'ACTIVE', ?, ?)",
            nextDisplayOrder("category"),
            now,
            now);

    // 2) 중간 부모: scenario (category FK 필요)
    Long scenarioId =
        insertAndGetId(
            "INSERT INTO scenario "
                + "(category_id, ai_role, difficulty, first_speaker, total_question_count, "
                + "display_order, status, created_at, updated_at) "
                + "VALUES (?, 'barista', 'NORMAL', 'AI', 5, ?, 'ACTIVE', ?, ?)",
            categoryId,
            nextDisplayOrder("scenario"),
            now,
            now);

    // 3) 우리가 진짜 원하는 것: writing_expression (scenario FK 필요)
    //    테스트가 검증할 값들(blow my mind 등)을 여기서 심는다.
    return insertAndGetId(
        "INSERT INTO writing_expression "
            + "(scenario_id, expression_type, usage_frequency_level, target_locale, base_locale, "
            + "display_order, target_expression_text, base_expression_meaning_text, usage_summary, "
            + "usage_description, representative_question_text, representative_question_translation, "
            + "representative_sentence_text, representative_sentence_translation, "
            + "representative_sentence_words, representative_sentence_word_choices, representative_image_url, "
            + "practice_examples_payload, status, created_at, updated_at) "
            + "VALUES (?, 'DAILY_ROUTINE', 'BASIC', 'EN', 'KR', 1, 'blow my mind', '끝내주게 놀랍다', "
            + "'usage summary', '강렬한 인상을 받았을 때 최고의 리액션이에요.', "
            + "'What should I definitely see in Korea?', '한국에서 뭘 꼭 봐야 해?', "
            + "'Gyeongbokgung Palace will blow your mind.', '경복궁은 널 완전 놀라게 할 거야.', "
            + "ARRAY['Gyeongbokgung','Palace','will','blow','your','mind'], "
            + "ARRAY['Gyeongbokgung','blow','will','Palace','amazing','have','get','your','mind'], "
            + "'https://cdn.example.com/images/101.png', "
            + "CAST(? AS jsonb), ?, ?, ?)",
        scenarioId,
        "[]",
        status,
        now,
        now);
  }

  /**
   * INSERT를 실행하고 DB가 자동 생성한 PK(id)를 돌려주는 유틸.
   *
   * <p>심은 데이터의 id를 알아야 그 id로 API를 호출할 수 있는데, id는 DB가 자동 생성하므로 INSERT 후에 받아와야 한다. H2가 PostgreSQL의
   * "RETURNING id" 문법을 지원하지 않아서 스프링의 GeneratedKeyHolder로 생성된 키를 받는 방식을 쓴다.
   */
  private Long insertAndGetId(String sql, Object... args) {
    KeyHolder keyHolder = new GeneratedKeyHolder(); // 생성된 PK가 담길 그릇

    // jdbcTemplate.update(문장 만드는 방법, 키 그릇) 형태.
    // 람다는 "DB 커넥션을 받아 PreparedStatement를 만들어 돌려주는 방법"을 정의한 것이다.
    jdbcTemplate.update(
        connection -> {
          // RETURN_GENERATED_KEYS: 실행 후 자동 생성 키를 돌려달라는 옵션
          PreparedStatement statement =
              connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          // SQL의 ? 자리(1번부터 시작)에 가변인자로 받은 값들을 순서대로 채운다
          for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
          }
          return statement;
        },
        keyHolder);

    return keyHolder.getKey().longValue(); // 그릇에서 생성된 PK를 꺼낸다
  }

  /** display_order에 UNIQUE 제약이 있어, 다른 테스트와 겹치지 않게 현재 최댓값+1을 반환한다. */
  private int nextDisplayOrder(String tableName) {
    Integer maxOrder =
        jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(display_order), 0) FROM " + tableName, Integer.class);
    return maxOrder + 1;
  }

  /**
   * 가짜 소셜 로그인으로 진짜 accessToken을 발급받는 헬퍼.
   *
   * <p>fake-enabled=true 설정 덕분에 idToken에 "sub|이메일|닉네임|nonce" 문자열만 넣으면 실제 구글 호출 없이 로그인이 성공하고, 응답에서
   * accessToken을 꺼내 반환한다.
   */
  private String login(String sub, String email, String nickname, String nonce) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"%s|%s|%s|%s",
                                  "nonce":"%s"
                                }
                                """
                            .formatted(sub, email, nickname, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    return body.get("data").get("accessToken").asText();
  }
}
