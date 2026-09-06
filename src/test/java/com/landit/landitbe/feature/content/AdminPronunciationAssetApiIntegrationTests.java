// 관리자 발음 평가 자산 2단계 임포트 API의 통합 동작을 검증한다.

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
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 관리자 발음 평가 자산 2단계 임포트 API의 통합 동작을 검증한다.
 *
 * <p>1단계(기준 데이터) → 2단계(TTS) 순서와 각 단계의 실패 처리, 커버리지의 단계별 결석 표시를 확인한다. 테스트 프로필은 매니페스트를 클래스패스({@code
 * src/test/resources/pronunciation-manifests/})에서 읽는 로컬 리더를 사용한다.
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

  private static final String BASE_URL = "/api/v1/admin/expressions/pronunciation-assets";
  private static final String IMPORT_REFERENCE_URL = BASE_URL + "/import-reference-from-s3";
  private static final String IMPORT_TTS_URL = BASE_URL + "/import-tts-from-s3";
  private static final String COVERAGE_URL = BASE_URL + "/coverage";
  private static final long EXPRESSION_ID = 990301L;
  // 패턴형 표현(타겟 텍스트에 ~ 포함) 시드용. 표현 음성이 없는 것이 정상인 케이스를 검증한다.
  private static final long PATTERN_EXPRESSION_ID = 990304L;

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("delete from expression_pronunciation_asset");
    jdbcTemplate.update("delete from writing_expression where id = ?", PATTERN_EXPRESSION_ID);
    jdbcTemplate.update("delete from writing_expression where id = ?", EXPRESSION_ID);
    jdbcTemplate.update(
        """
        insert into writing_expression (
            id, expression_source, expression_type, usage_frequency_level, difficulty_level, target_locale,
            base_locale, display_order, target_expression_text, base_expression_meaning_text,
            usage_summary, usage_description, representative_sentence_text,
            representative_sentence_translation, representative_sentence_words,
            representative_sentence_word_choices, practice_examples_payload, status,
            created_at, updated_at
        )
        values (?, 'FREE_TALK', 'CONVERSATION_SKILL', 'BASIC', 3, 'EN', 'KR', 990301,
            'There is nothing like', '~만 한 게 없다', 'summary', 'description',
            'There''s nothing like hiking.', '하이킹만 한 게 없어.',
            ARRAY['There''s','nothing','like','hiking.'], ARRAY['There''s','nothing'],
            CAST('[]' AS jsonb), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        EXPRESSION_ID);
  }

  /**
   * 시드한 표현을 남기지 않는다.
   *
   * <p>다른 테스트(예: 스키마 테스트의 FREE_TALK 표현 카운트)가 전역 상태를 세기 때문에, 마지막 테스트 후에도 세상을 원래대로 돌려놓아야 한다. 자산 행은 FK
   * CASCADE로 함께 삭제된다.
   */
  @AfterEach
  void tearDown() {
    jdbcTemplate.update("delete from writing_expression where id = ?", EXPRESSION_ID);
    jdbcTemplate.update("delete from writing_expression where id = ?", PATTERN_EXPRESSION_ID);
  }

  @Test
  void referenceImportInsertsThenUpdatesWords() throws Exception {
    String accessToken = loginAsAdmin("pron-ref-upsert");

    // 1차 임포트는 삽입, 같은 (표현, 억양)의 2차 임포트는 갱신이어야 한다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(1))
        .andExpect(jsonPath("$.data.updated").value(0))
        .andExpect(jsonPath("$.data.failures").isEmpty());

    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us_v2.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(0))
        .andExpect(jsonPath("$.data.updated").value(1));

    // words가 v2 내용으로 교체됐고, 음성 URL은 아직 비어 있어야 한다 (TTS 임포트 전).
    String words =
        jdbcTemplate.queryForObject(
            "select cast(words as varchar) from expression_pronunciation_asset"
                + " where writing_expression_id = ? and accent_locale = 'EN_US'",
            String.class,
            EXPRESSION_ID);
    String sentenceAudioUrl =
        jdbcTemplate.queryForObject(
            "select sentence_audio_url from expression_pronunciation_asset"
                + " where writing_expression_id = ? and accent_locale = 'EN_US'",
            String.class,
            EXPRESSION_ID);
    assertThat(words).contains("thairz-v2");
    assertThat(sentenceAudioUrl).isNull();
  }

  @Test
  void referenceImportRejectsStaleSentenceText() throws Exception {
    String accessToken = loginAsAdmin("pron-ref-stale");

    // 기준 데이터를 만든 문장이 DB의 대표 예문과 다르면 낡은 데이터로 보고 실패 처리해야 한다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_stale.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(0))
        .andExpect(
            jsonPath("$.data.failures[0].reason")
                .value("기준 데이터의 문장이 DB의 대표 예문과 다릅니다. 최신 문장으로 재생성이 필요합니다."));
  }

  @Test
  void referenceImportRejectsMissingSentenceText() throws Exception {
    String accessToken = loginAsAdmin("pron-ref-no-sentence");

    // sentenceText를 생략하면 낡은 데이터 검증이 우회되므로, 생략 자체를 실패 처리해야 한다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_missing_sentence.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(0))
        .andExpect(jsonPath("$.data.failures[0].reason").value("필수 값이 누락됐습니다."));
  }

  @Test
  void referenceImportRejectsDuplicatedWordOrder() throws Exception {
    String accessToken = loginAsAdmin("pron-ref-dup-order");

    // words 배열 안에서 order가 중복되면 런타임 조인이 깨지므로 임포트에서 거른다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_bad_word_items.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(0))
        .andExpect(jsonPath("$.data.failures[0].reason").value("words 항목의 order가 중복됩니다."));
  }

  @Test
  void referenceImportRejectsBlankWord() throws Exception {
    String accessToken = loginAsAdmin("pron-ref-blank-word");

    // 단어 텍스트가 빈 항목은 런타임 단어 대조를 무력화하므로 임포트에서 거른다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_blank_word.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.inserted").value(0))
        .andExpect(jsonPath("$.data.failures[0].reason").value("words 항목에 word가 없습니다."));
  }

  @Test
  void referenceReimportResetsTtsStateSoCoverageCatchesIt() throws Exception {
    String accessToken = loginAsAdmin("pron-ref-reimport-reset");

    // TTS까지 완성한 뒤 기준 데이터를 재임포트하면, 단어 audioUrl이 사라진 반쪽 상태가 된다.
    // 이때 문장 URL이 남아 있으면 커버리지가 "완성"이라고 거짓말하므로, URL이 초기화되고
    // audioMissing 결석 목록에 다시 잡혀야 한다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us.json", accessToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us_v2.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(1));

    String sentenceAudioUrl =
        jdbcTemplate.queryForObject(
            "select sentence_audio_url from expression_pronunciation_asset"
                + " where writing_expression_id = ? and accent_locale = 'EN_US'",
            String.class,
            EXPRESSION_ID);
    assertThat(sentenceAudioUrl).isNull();
    assertThat(missingOf(coverageData(accessToken), "EN_US", "audioMissing"))
        .contains(EXPRESSION_ID);
  }

  @Test
  void referenceImportReportsUnknownExpressionAsFailure() throws Exception {
    String accessToken = loginAsAdmin("pron-ref-unknown");

    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_unknown_expression.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.failures[0].expressionId").value(999999))
        .andExpect(jsonPath("$.data.failures[0].reason").value("존재하지 않는 표현입니다."));
  }

  @Test
  void ttsImportFailsWithoutReferenceData() throws Exception {
    String accessToken = loginAsAdmin("pron-tts-no-ref");

    // 기준 데이터(1단계) 없이 TTS(2단계)부터 임포트하면 실패 목록에 담겨야 한다.
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(0))
        .andExpect(
            jsonPath("$.data.failures[0].reason").value("기준 데이터가 없습니다. 기준 데이터 임포트를 먼저 실행하세요."));
  }

  @Test
  void ttsImportAttachesUrlsAndJoinsWordAudio() throws Exception {
    String accessToken = loginAsAdmin("pron-tts-join");

    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(1))
        .andExpect(jsonPath("$.data.failures").isEmpty());

    // 문장 URL 컬럼이 채워지고, words의 각 항목에 audioUrl이 order로 조인돼야 한다.
    String sentenceAudioUrl =
        jdbcTemplate.queryForObject(
            "select sentence_audio_url from expression_pronunciation_asset"
                + " where writing_expression_id = ? and accent_locale = 'EN_US'",
            String.class,
            EXPRESSION_ID);
    String words =
        jdbcTemplate.queryForObject(
            "select cast(words as varchar) from expression_pronunciation_asset"
                + " where writing_expression_id = ? and accent_locale = 'EN_US'",
            String.class,
            EXPRESSION_ID);
    assertThat(sentenceAudioUrl).isEqualTo("https://cdn.example.com/sentence.mp3");
    assertThat(words)
        .contains("https://cdn.example.com/words/2.mp3")
        .contains("accentContrast"); // 기준 데이터의 기존 필드가 조인 후에도 보존돼야 한다.
  }

  @Test
  void ttsImportFailsWhenWordOrderIsMissing() throws Exception {
    String accessToken = loginAsAdmin("pron-tts-missing-word");

    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us_missing_word.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(0))
        .andExpect(
            jsonPath("$.data.failures[0].reason").value("TTS 매니페스트의 단어 order가 기준 데이터와 맞지 않습니다."));
  }

  @Test
  void coverageSeparatesReferenceAndAudioMissing() throws Exception {
    String accessToken = loginAsAdmin("pron-coverage");

    // 기준 데이터만 임포트한 상태: EN_US는 기준 데이터 출석 + 음성 결석, EN_GB는 기준 데이터부터 결석.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isOk());

    JsonNode afterReference = coverageData(accessToken);
    assertThat(missingOf(afterReference, "EN_US", "referenceMissing"))
        .doesNotContain(EXPRESSION_ID);
    assertThat(missingOf(afterReference, "EN_US", "audioMissing")).contains(EXPRESSION_ID);
    assertThat(missingOf(afterReference, "EN_GB", "referenceMissing")).contains(EXPRESSION_ID);

    // TTS까지 임포트하면 EN_US의 음성 결석도 사라진다.
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us.json", accessToken))
        .andExpect(status().isOk());

    JsonNode afterTts = coverageData(accessToken);
    assertThat(missingOf(afterTts, "EN_US", "audioMissing")).doesNotContain(EXPRESSION_ID);
  }

  @Test
  void importReturnsNotFoundForMissingManifest() throws Exception {
    String accessToken = loginAsAdmin("pron-missing-manifest");

    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "no-such-manifest.json", accessToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void importReturnsBadRequestForMalformedManifest() throws Exception {
    String accessToken = loginAsAdmin("pron-malformed");

    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "malformed.json", accessToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void importIsForbiddenForNonAdminUser() throws Exception {
    String accessToken = login("pron-normal-user").accessToken();

    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void importRequiresAuthentication() throws Exception {
    mockMvc
        .perform(post(IMPORT_REFERENCE_URL).param("manifestKey", "reference_us.json"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void ttsImportAllowsNullExpressionAudioForTemplatedExpression() throws Exception {
    String accessToken = loginAsAdmin("pron-pattern-null-ok");
    seedPatternExpression();

    // 패턴형 표현("be busy ~ing")은 표현 음성을 아예 만들지 않으므로 expressionAudioUrl이
    // null이어도 정상 임포트돼야 한다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us_pattern.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.failures").isEmpty());
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us_pattern.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(1))
        .andExpect(jsonPath("$.data.failures").isEmpty());

    // 표현 음성 칸은 null 그대로, 문장 음성은 채워져 자산이 완성 상태여야 한다.
    String expressionAudioUrl =
        jdbcTemplate.queryForObject(
            "select expression_audio_url from expression_pronunciation_asset"
                + " where writing_expression_id = ? and accent_locale = 'EN_US'",
            String.class,
            PATTERN_EXPRESSION_ID);
    String sentenceAudioUrl =
        jdbcTemplate.queryForObject(
            "select sentence_audio_url from expression_pronunciation_asset"
                + " where writing_expression_id = ? and accent_locale = 'EN_US'",
            String.class,
            PATTERN_EXPRESSION_ID);
    assertThat(expressionAudioUrl).isNull();
    assertThat(sentenceAudioUrl).isEqualTo("https://cdn.example.com/pattern/sentence.mp3");
  }

  @Test
  void ttsImportRejectsNullExpressionAudioForSpeakableExpression() throws Exception {
    String accessToken = loginAsAdmin("pron-speakable-null");

    // 발화 가능한 일반 표현("There is nothing like")인데 표현 음성이 없으면 생성 누락 사고다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us_null_expression_audio.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(0))
        .andExpect(jsonPath("$.data.failures[0].expressionId").value(EXPRESSION_ID))
        .andExpect(
            jsonPath("$.data.failures[0].reason")
                .value("발화 가능한 표현인데 표현 음성(expressionAudioUrl)이 없습니다."));
  }

  @Test
  void ttsImportRejectsNullWordAudioUrl() throws Exception {
    String accessToken = loginAsAdmin("pron-word-audio-null");

    // 단어 음성 URL이 비면 조인에서 터지는 대신 실패 목록행으로 잡혀야 한다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us_null_word_audio.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(0))
        .andExpect(
            jsonPath("$.data.failures[0].reason").value("TTS 매니페스트 words 항목에 audioUrl이 없습니다."));
  }

  @Test
  void ttsImportRejectsDuplicateWordOrder() throws Exception {
    String accessToken = loginAsAdmin("pron-tts-dup-order");

    // order가 중복되면 조인이 한쪽 URL을 조용히 버리는 대신 실패 목록행으로 잡혀야 한다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us_duplicate_order.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(0))
        .andExpect(jsonPath("$.data.failures[0].reason").value("TTS 매니페스트 words의 order가 중복됩니다."));
  }

  @Test
  void ttsImportRejectsExtraWordOrder() throws Exception {
    String accessToken = loginAsAdmin("pron-tts-extra-order");

    // 기준 데이터에 없는 order가 섞이면 조용히 버리는 대신 실패 목록행으로 잡혀야 한다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us_extra_order.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(0))
        .andExpect(
            jsonPath("$.data.failures[0].reason").value("TTS 매니페스트의 단어 order가 기준 데이터와 맞지 않습니다."));
  }

  @Test
  void ttsImportRejectsNullWordEntry() throws Exception {
    String accessToken = loginAsAdmin("pron-tts-null-word");

    // words 배열의 null 항목이 NPE로 임포트 전체를 죽이는 대신 실패 목록행으로 잡혀야 한다.
    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "reference_us.json", accessToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(importFrom(IMPORT_TTS_URL, "tts_us_null_word_entry.json", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.updated").value(0))
        .andExpect(jsonPath("$.data.failures[0].reason").value("TTS 매니페스트 words에 빈 항목이 있습니다."));
  }

  @Test
  void importRejectsBlankManifestKey() throws Exception {
    String accessToken = loginAsAdmin("pron-blank-key");

    mockMvc
        .perform(importFrom(IMPORT_REFERENCE_URL, "", accessToken))
        .andExpect(status().isBadRequest());
    mockMvc.perform(importFrom(IMPORT_TTS_URL, "", accessToken)).andExpect(status().isBadRequest());
  }

  /** 패턴형 표현(타겟 텍스트에 ~ 포함, 표현 음성 없음)을 시드한다. */
  private void seedPatternExpression() {
    jdbcTemplate.update(
        """
        insert into writing_expression (
            id, expression_source, expression_type, usage_frequency_level, difficulty_level, target_locale,
            base_locale, display_order, target_expression_text, base_expression_meaning_text,
            usage_summary, usage_description, representative_sentence_text,
            representative_sentence_translation, representative_sentence_words,
            representative_sentence_word_choices, practice_examples_payload, status,
            created_at, updated_at
        )
        values (?, 'FREE_TALK', 'CONVERSATION_SKILL', 'BASIC', 3, 'EN', 'KR', 990304,
            'be busy ~ing', '~하느라 바쁘다', 'summary', 'description',
            'I''m busy studying now.', '지금 공부하느라 바빠.',
            ARRAY['I''m','busy','studying','now.'], ARRAY['I''m','busy'],
            CAST('[]' AS jsonb), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        PATTERN_EXPRESSION_ID);
  }

  /** 매니페스트 키로 임포트 요청을 만든다. */
  private MockHttpServletRequestBuilder importFrom(
      String url, String manifestKey, String accessToken) {
    return post(url)
        .param("manifestKey", manifestKey)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
  }

  /** 커버리지 응답의 data 노드를 조회한다. */
  private JsonNode coverageData(String accessToken) throws Exception {
    MvcResult result =
        mockMvc
            .perform(get(COVERAGE_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsByteArray()).get("data");
  }

  /** 커버리지 응답에서 특정 억양의 결석 목록(referenceMissing 또는 audioMissing)을 꺼낸다. */
  private List<Long> missingOf(JsonNode data, String accentLocale, String fieldName) {
    for (JsonNode locale : data.get("locales")) {
      if (accentLocale.equals(locale.get("accentLocale").asText())) {
        List<Long> ids = new ArrayList<>();
        locale.get(fieldName).forEach(id -> ids.add(id.asLong()));
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
