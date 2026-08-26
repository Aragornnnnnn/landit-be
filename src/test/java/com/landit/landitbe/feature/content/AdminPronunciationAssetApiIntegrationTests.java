// 관리자 발음 평가 자산 S3 임포트 API의 통합 동작을 검증한다.

package com.landit.landitbe.feature.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
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

/**
 * 관리자 발음 평가 자산 S3 임포트 API의 통합 동작을 검증한다.
 *
 * <p>테스트 프로필은 매니페스트를 클래스패스({@code src/test/resources/pronunciation-manifests/})에서 읽는 로컬 리더를 사용한다.
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class AdminPronunciationAssetApiIntegrationTests {

  private static final String IMPORT_URL =
      "/api/v1/admin/expressions/pronunciation-assets/import-from-s3";
  private static final String COVERAGE_URL =
      "/api/v1/admin/expressions/pronunciation-assets/coverage";
  private static final long EXPRESSION_ID = 990301L;

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("delete from expression_pronunciation_asset");
    jdbcTemplate.update("delete from writing_expression where id = ?", EXPRESSION_ID);
    jdbcTemplate.update(
        """
        insert into writing_expression (
            id, expression_source, expression_type, usage_frequency_level, target_locale,
            base_locale, display_order, target_expression_text, base_expression_meaning_text,
            usage_summary, usage_description, representative_sentence_text,
            representative_sentence_translation, representative_sentence_words,
            representative_sentence_word_choices, practice_examples_payload, status,
            created_at, updated_at
        )
        values (?, 'FREE_TALK', 'CONVERSATION_SKILL', 'BASIC', 'EN', 'KR', 990301,
            'There is nothing like', '~만 한 게 없다', 'summary', 'description',
            'There''s nothing like hiking.', '하이킹만 한 게 없어.',
            ARRAY['There''s','nothing','like','hiking.'], ARRAY['There''s','nothing'],
            CAST('[]' AS jsonb), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        EXPRESSION_ID);
  }

  @Test
  void importInsertsNewAssetAndUpdatesExistingAsset() throws Exception {
    String accessToken = loginAsAdmin("pron-asset-upsert");

    // 같은 (표현, 억양)을 두 번 임포트하면 첫 번째는 삽입, 두 번째는 갱신이어야 한다.
    mockMvc
        .perform(importFromS3("first.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(1))
        .andExpect(jsonPath("$.data.updated").value(0))
        .andExpect(jsonPath("$.data.failures").isEmpty());

    mockMvc
        .perform(importFromS3("second.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(0))
        .andExpect(jsonPath("$.data.updated").value(1));

    Integer assetCount =
        jdbcTemplate.queryForObject(
            "select count(*) from expression_pronunciation_asset where writing_expression_id = ?",
            Integer.class,
            EXPRESSION_ID);
    String sentenceAudioUrl =
        jdbcTemplate.queryForObject(
            """
            select sentence_audio_url from expression_pronunciation_asset
            where writing_expression_id = ? and accent_locale = 'EN_US'
            """,
            String.class,
            EXPRESSION_ID);
    assertThat(assetCount).isEqualTo(1);
    assertThat(sentenceAudioUrl).isEqualTo("https://cdn.example.com/second.mp3");
  }

  @Test
  void importStoresSeparateRowsPerAccentLocale() throws Exception {
    String accessToken = loginAsAdmin("pron-asset-accent");

    mockMvc.perform(importFromS3("first.json", accessToken)).andExpect(status().isOk());
    mockMvc
        .perform(importFromS3("gb.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(1));

    Integer assetCount =
        jdbcTemplate.queryForObject(
            "select count(*) from expression_pronunciation_asset where writing_expression_id = ?",
            Integer.class,
            EXPRESSION_ID);
    assertThat(assetCount).isEqualTo(2);
  }

  @Test
  void importReportsUnknownExpressionAsFailureAndImportsValidOnes() throws Exception {
    String accessToken = loginAsAdmin("pron-asset-partial");

    mockMvc
        .perform(importFromS3("partial.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(1))
        .andExpect(jsonPath("$.data.failures[0].expressionId").value(999999))
        .andExpect(jsonPath("$.data.failures[0].reason").value("존재하지 않는 표현입니다."));
  }

  @Test
  void importRejectsEmptyWordsArrayAsFailure() throws Exception {
    String accessToken = loginAsAdmin("pron-asset-empty-words");

    mockMvc
        .perform(importFromS3("empty-words.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(0))
        .andExpect(jsonPath("$.data.failures[0].reason").value("words는 비어 있지 않은 배열이어야 합니다."));
  }

  @Test
  void importReturnsNotFoundForMissingManifest() throws Exception {
    String accessToken = loginAsAdmin("pron-asset-missing-manifest");

    mockMvc
        .perform(importFromS3("no-such-manifest.json", accessToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void importReturnsBadRequestForMalformedManifest() throws Exception {
    String accessToken = loginAsAdmin("pron-asset-malformed");

    mockMvc
        .perform(importFromS3("malformed.json", accessToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void coverageReportsMissingExpressionPerLocale() throws Exception {
    String accessToken = loginAsAdmin("pron-asset-coverage");

    // EN_US 자산만 임포트한 상태에서 커버리지를 조회하면,
    // 시드 표현이 EN_US에서는 빠진 목록에 없고 EN_GB에서는 빠진 목록에 있어야 한다.
    mockMvc.perform(importFromS3("first.json", accessToken)).andExpect(status().isOk());

    MvcResult result =
        mockMvc
            .perform(get(COVERAGE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode data = objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
    assertThat(data.get("totalActiveExpressions").asInt()).isGreaterThanOrEqualTo(1);
    assertThat(missingIdsOf(data, "EN_US")).doesNotContain(EXPRESSION_ID);
    assertThat(missingIdsOf(data, "EN_GB")).contains(EXPRESSION_ID);
  }

  @Test
  void importIsForbiddenForNonAdminUser() throws Exception {
    String accessToken = login("pron-asset-normal-user").accessToken();

    mockMvc.perform(importFromS3("first.json", accessToken)).andExpect(status().isForbidden());
  }

  @Test
  void importRequiresAuthentication() throws Exception {
    mockMvc
        .perform(post(IMPORT_URL).param("manifestKey", "first.json"))
        .andExpect(status().isUnauthorized());
  }

  // 매니페스트 키로 임포트 요청을 만든다.
  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder importFromS3(
      String manifestKey, String accessToken) {
    return post(IMPORT_URL)
        .param("manifestKey", manifestKey)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
  }

  // 커버리지 응답에서 특정 억양의 빠진 표현 ID 목록을 꺼낸다.
  private List<Long> missingIdsOf(JsonNode data, String accentLocale) {
    for (JsonNode locale : data.get("locales")) {
      if (accentLocale.equals(locale.get("accentLocale").asText())) {
        List<Long> ids = new ArrayList<>();
        locale.get("missing").forEach(id -> ids.add(id.asLong()));
        return ids;
      }
    }
    throw new AssertionError("커버리지 응답에 억양이 없습니다: " + accentLocale);
  }

  private String loginAsAdmin(String userKey) throws Exception {
    LoginResult loginResult = login(userKey);
    jdbcTemplate.update(
        "update user_profile set role = 'ADMIN' where id = ?", loginResult.userProfileId());
    return loginResult.accessToken();
  }

  private LoginResult login(String userKey) throws Exception {
    String nonce = userKey + "-nonce";
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "provider":"GOOGLE",
                          "idToken":"%s|%s@example.com|Asset Admin|%s",
                          "nonce":"%s"
                        }
                        """
                            .formatted(userKey, userKey, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    Long userProfileId =
        jdbcTemplate.queryForObject(
            "select id from user_profile where email = ?", Long.class, userKey + "@example.com");
    return new LoginResult(userProfileId, body.get("data").get("accessToken").asText());
  }

  private record LoginResult(Long userProfileId, String accessToken) {}
}
