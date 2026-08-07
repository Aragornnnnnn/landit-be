// 원어민 표현 학습 완료 API의 인증, 완료 기록 생성/멱등 갱신, 잠금/미존재 예외를 검증한다.

package com.landit.landitbe.feature.content;

import static org.assertj.core.api.Assertions.assertThat;
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

/** 원어민 표현 학습 완료 API의 인증, 완료 기록 생성/멱등 갱신, 잠금/미존재 예외를 검증한다. */
@ActiveProfiles("test") // test 프로파일 → H2 인메모리 DB 사용
@AutoConfigureMockMvc // 서버 포트 없이 HTTP 요청을 흉내 내는 MockMvc 활성화
@SpringBootTest // 스프링 앱 전체(컨트롤러~DB)를 실제로 띄우는 통합 테스트
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true", // 가짜 소셜 로그인 활성화 (실제 구글/애플 호출 없음)
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough" // 테스트 전용 더미 시크릿
    })
class ExpressionLearningFinishApiIntegrationTests {

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  /** 토큰 없이 호출하면 401(INVALID_TOKEN)로 거절되는지 검증한다. */
  @Test
  void learningFinishRejectsMissingAccessToken() throws Exception {
    // given: 완료 대상 표현이 DB에 존재
    Long scenarioId = seedScenarioWithExpressions();
    Long expressionId = findExpressionIdByDisplayOrder(scenarioId, 1);

    // when: Authorization 헤더 없이 호출하면
    // then: 401 + INVALID_TOKEN
    mockMvc
        .perform(post("/api/v1/expressions/{expressionId}/learning-finish", expressionId))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
  }

  /** 해금된 표현을 완료하면 200 + 빈 객체 응답이 오고, DB에 완료 기록이 1건 생성되는지 검증한다. */
  @Test
  void learningFinishCreatesCompletionForUnlockedExpression() throws Exception {
    // given: 표현 5개짜리 시나리오 + 로그인 (아무것도 완료 안 함 → 1번 표현이 해금 상태)
    Long scenarioId = seedScenarioWithExpressions();
    Long firstExpressionId = findExpressionIdByDisplayOrder(scenarioId, 1);
    String accessToken =
        login("google-finish-1", "finish@example.com", "Finish User", "finish-nonce");
    Long userProfileId = findUserProfileIdByEmail("finish@example.com");

    // when: 해금된 1번 표현을 완료하면
    // then: 200 + data는 빈 객체({}), error는 null
    mockMvc
        .perform(
            post("/api/v1/expressions/{expressionId}/learning-finish", firstExpressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isMap())
        .andExpect(jsonPath("$.data").isEmpty())
        .andExpect(jsonPath("$.error").doesNotExist());

    // then: DB에 완료 기록이 정확히 1건 생성된다
    assertThat(countCompletions(userProfileId, firstExpressionId)).isEqualTo(1);
  }

  /**
   * 이미 완료한 표현을 다시 완료하면 새 기록 없이(1건 유지), 최초 완료 시각(completed_at)은 보존하고 마지막 완료 시각(last_completed_at)만
   * 갱신하는지 검증한다.
   */
  @Test
  void learningFinishRenewsLastCompletedAtForAlreadyCompletedExpression() throws Exception {
    // given: 표현 시나리오 + 로그인 + 1번 표현을 과거 시각으로 이미 완료해 둔 상태
    Long scenarioId = seedScenarioWithExpressions();
    Long firstExpressionId = findExpressionIdByDisplayOrder(scenarioId, 1);
    String accessToken =
        login("google-finish-2", "finish2@example.com", "Finish User2", "finish-nonce-2");
    Long userProfileId = findUserProfileIdByEmail("finish2@example.com");

    LocalDateTime pastTime = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
    insertCompletion(userProfileId, scenarioId, firstExpressionId, pastTime, pastTime);

    // when: 같은 표현을 다시 완료하면
    // then: 200 성공
    mockMvc
        .perform(
            post("/api/v1/expressions/{expressionId}/learning-finish", firstExpressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // then: 기록은 여전히 1건 (새 INSERT 없음)
    assertThat(countCompletions(userProfileId, firstExpressionId)).isEqualTo(1);
    // then: 최초 완료 시각은 과거 그대로, 마지막 완료 시각은 과거보다 뒤로 갱신됨
    assertThat(queryTime(userProfileId, firstExpressionId, "completed_at")).isEqualTo(pastTime);
    assertThat(queryTime(userProfileId, firstExpressionId, "last_completed_at")).isAfter(pastTime);
  }

  /** 아직 잠긴 표현을 완료하려 하면 403(EXPRESSION_LOCKED)로 막고, 완료 기록도 생기지 않는지 검증한다. */
  @Test
  void learningFinishRejectsLockedExpression() throws Exception {
    // given: 아무것도 완료 안 한 사용자 (1번만 해금, 3번은 잠김)
    Long scenarioId = seedScenarioWithExpressions();
    Long thirdExpressionId = findExpressionIdByDisplayOrder(scenarioId, 3);
    String accessToken =
        login("google-finish-3", "finish3@example.com", "Finish User3", "finish-nonce-3");
    Long userProfileId = findUserProfileIdByEmail("finish3@example.com");

    // when: 잠긴 3번 표현을 완료하려 하면
    // then: 403 + EXPRESSION_LOCKED
    mockMvc
        .perform(
            post("/api/v1/expressions/{expressionId}/learning-finish", thirdExpressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("EXPRESSION_LOCKED"));

    // then: 완료 기록은 생기지 않는다
    assertThat(countCompletions(userProfileId, thirdExpressionId)).isZero();
  }

  /** 존재하지 않는 표현 ID로 완료하려 하면 404(RESOURCE_NOT_FOUND)로 거절되는지 검증한다. */
  @Test
  void learningFinishRejectsUnknownExpression() throws Exception {
    // given: 로그인만 하고, 표현은 심지 않은 상태
    String accessToken =
        login("google-finish-4", "finish4@example.com", "Finish User4", "finish-nonce-4");

    // when: DB에 없는 ID(999999)로 완료하려 하면
    // then: 404 + RESOURCE_NOT_FOUND
    mockMvc
        .perform(
            post("/api/v1/expressions/{expressionId}/learning-finish", 999_999L)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
  }

  // ===== 헬퍼 =====

  /**
   * 시나리오 1개 + EN/KR 표현 5개(displayOrder 1~5)를 심고 시나리오 PK를 반환한다. (부모 FK 때문에 category → scenario →
   * writing_expression 순)
   */
  private Long seedScenarioWithExpressions() {
    LocalDateTime now = LocalDateTime.now();
    Long categoryId =
        insertAndGetId(
            "INSERT INTO category (display_order, status, created_at, updated_at) "
                + "VALUES (?, 'ACTIVE', ?, ?)",
            nextDisplayOrder("category"),
            now,
            now);
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

    // 만들어진 scenario에 writingExpression 5개를 심는다. (displayOrder 1~5)
    for (int displayOrder = 1; displayOrder <= 5; displayOrder++) {
      insertWritingExpression(scenarioId, displayOrder, now);
    }
    return scenarioId;
  }

  /** EN/KR 표현 1건을 심는다. */
  private void insertWritingExpression(Long scenarioId, int displayOrder, LocalDateTime now) {
    jdbcTemplate.update(
        "INSERT INTO writing_expression "
            + "(scenario_id, expression_type, usage_frequency_level, target_locale, base_locale, "
            + "display_order, target_expression_text, base_expression_meaning_text, usage_summary, "
            + "usage_description, representative_sentence_text, "
            + "representative_sentence_translation, "
            + "representative_sentence_words, representative_sentence_word_choices, "
            + "practice_examples_payload, status, "
            + "created_at, updated_at) "
            + "VALUES (?, 'DAILY_ROUTINE', 'BASIC', 'EN', 'KR', ?, ?, ?, 'usage summary', "
            + "'usage description', 'sample sentence', '샘플 문장', ARRAY['sample'], "
            + "ARRAY['sample','choice'], CAST(? AS jsonb), 'ACTIVE', ?, ?)",
        scenarioId,
        displayOrder,
        "expression-" + displayOrder,
        "표현-" + displayOrder,
        "[]",
        now,
        now);
  }

  /** Completed_at / last_completed_at을 명시적으로 지정해 완료 기록을 심는다. (재완료 갱신 검증용) */
  private void insertCompletion(
      Long userProfileId,
      Long scenarioId,
      Long writingExpressionId,
      LocalDateTime completedAt,
      LocalDateTime lastCompletedAt) {
    jdbcTemplate.update(
        "INSERT INTO user_writing_expression_completion "
            + "(user_profile_id, scenario_id, writing_expression_id, completed_at, "
            + "last_completed_at) "
            + "VALUES (?, ?, ?, ?, ?)",
        userProfileId,
        scenarioId,
        writingExpressionId,
        completedAt,
        lastCompletedAt);
  }

  /** 특정 사용자-표현의 완료 기록 개수를 센다. */
  private int countCompletions(Long userProfileId, Long writingExpressionId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM user_writing_expression_completion "
            + "WHERE user_profile_id = ? AND writing_expression_id = ?",
        Integer.class,
        userProfileId,
        writingExpressionId);
  }

  /** 완료 기록의 특정 시각 컬럼(completed_at / last_completed_at)을 조회한다. */
  private LocalDateTime queryTime(Long userProfileId, Long writingExpressionId, String column) {
    return jdbcTemplate.queryForObject(
        "SELECT "
            + column
            + " FROM user_writing_expression_completion "
            + "WHERE user_profile_id = ? AND writing_expression_id = ?",
        LocalDateTime.class,
        userProfileId,
        writingExpressionId);
  }

  private Long findExpressionIdByDisplayOrder(Long scenarioId, int displayOrder) {
    return jdbcTemplate.queryForObject(
        "SELECT id FROM writing_expression WHERE scenario_id = ? AND display_order = ?",
        Long.class,
        scenarioId,
        displayOrder);
  }

  private int nextDisplayOrder(String tableName) {
    Integer maxOrder =
        jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(display_order), 0) FROM " + tableName, Integer.class);
    return maxOrder + 1;
  }

  /** INSERT 후 자동 생성된 PK를 돌려주는 유틸. (H2가 RETURNING을 지원하지 않아 GeneratedKeyHolder 사용) */
  private Long insertAndGetId(String sql, Object... args) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < args.length; i++) {
            statement.setObject(
                i + 1, args[i]); // statement = DB에 보낼 SQL 명령서 (SQL + 파라미터를 모두 가지고 있는 객체)
          }
          return statement;
        },
        keyHolder);
    return keyHolder.getKey().longValue();
  }

  /** 가짜 소셜 로그인으로 accessToken을 발급받는다. */
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

  /** 로그인으로 생성된 사용자의 PK를 이메일로 찾아온다. */
  private Long findUserProfileIdByEmail(String email) {
    return jdbcTemplate.queryForObject(
        "SELECT id FROM user_profile WHERE email = ?", Long.class, email);
  }
}
