// 문장 발화 발음 평가 API의 통합 동작을 검증한다.

package com.landit.landitbe.feature.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.content.domain.ExpressionPronunciationAsset;
import com.landit.landitbe.shared.domain.AccentLocale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 문장 발화 발음 평가 API의 통합 동작을 검증한다.
 *
 * <p>AI 판정은 로컬 스텁(2번째 단어 음소 오류·4번째 단어 강세 오류·나머지 정상)을 사용한다. 검증 대상은 AI가 아니라 BE의 점수 계산·코칭 조립·자산 병합
 * 로직이다.
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class ExpressionPronunciationApiIntegrationTests {

  private static final long EXPRESSION_ID = 990401L; // 8단어 문장 (점수 75 케이스)
  private static final long SHORT_EXPRESSION_ID = 990402L; // 1단어 문장 (통과 케이스)
  private static final long NO_ASSET_EXPRESSION_ID = 990403L; // 자산 미구축 케이스

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired
  private com.landit.landitbe.feature.content.repository.ExpressionPronunciationAssetRepository
      assetRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("delete from expression_pronunciation_asset");
    jdbcTemplate.update(
        "delete from writing_expression where id in (?, ?, ?)",
        EXPRESSION_ID,
        SHORT_EXPRESSION_ID,
        NO_ASSET_EXPRESSION_ID);

    insertExpression(
        EXPRESSION_ID,
        "There's nothing like hiking to clear my head.",
        "ARRAY['There''s','nothing','like','hiking','to','clear','my','head']");
    insertExpression(SHORT_EXPRESSION_ID, "Hello.", "ARRAY['Hello']");
    insertExpression(
        NO_ASSET_EXPRESSION_ID, "No asset sentence.", "ARRAY['No','asset','sentence']");

    insertAsset(EXPRESSION_ID, 8);
    insertAsset(SHORT_EXPRESSION_ID, 1);
    // NO_ASSET_EXPRESSION_ID는 의도적으로 자산을 넣지 않는다.
  }

  // 시드한 표현 3건을 남기지 않는다 (전역 카운트를 세는 다른 테스트 보호). 자산은 CASCADE로 함께 삭제된다.
  @AfterEach
  void tearDown() {
    jdbcTemplate.update(
        "delete from writing_expression where id in (?, ?, ?)",
        EXPRESSION_ID,
        SHORT_EXPRESSION_ID,
        NO_ASSET_EXPRESSION_ID);
  }

  @Test
  void analyzeReturnsScoreCoachingAndMergedAssetData() throws Exception {
    String accessToken = loginWithUsTutor("pron-analyze-success");

    MvcResult result =
        mockMvc
            .perform(
                multipart(analysisUrl(EXPRESSION_ID))
                    .file(sampleAudio())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            // 8단어 중 2개 오류(로컬 스텁 고정 판정) → 6/8 = 75점, 미통과.
            .andExpect(jsonPath("$.data.score").value(75))
            .andExpect(jsonPath("$.data.passed").value(false))
            .andReturn();

    JsonNode words =
        objectMapper
            .readTree(result.getResponse().getContentAsByteArray())
            .path("data")
            .path("words");
    assertThat(words).hasSize(8);

    // 정상 단어(1번): 판정·타임스탬프만 있고 오류 관련 필드는 비어 있어야 한다.
    JsonNode correct = words.get(0);
    assertThat(correct.path("status").asText()).isEqualTo("CORRECT");
    assertThat(correct.path("startTimeMs").isNumber()).isTrue();
    assertThat(correct.hasNonNull("coachingText")).isFalse();
    assertThat(correct.hasNonNull("nativeWordAudioUrl")).isFalse();

    // 음소 오류(2번): AI 판정(userDisplay·span) + 자산 데이터(nativeDisplay·단어 TTS) + 코칭이 병합돼야 한다.
    JsonNode phoneme = words.get(1);
    assertThat(phoneme.path("status").asText()).isEqualTo("PHONEME_ERROR");
    assertThat(phoneme.path("userDisplay").asText()).isEqualTo("nuh·ssing");
    assertThat(phoneme.path("errorTargetSpan").asText()).isEqualTo("th");
    assertThat(phoneme.path("nativeDisplay").asText()).isEqualTo("nuh·thing");
    assertThat(phoneme.path("nativeWordAudioUrl").asText())
        .isEqualTo("https://cdn.example.com/words/2.mp3");
    assertThat(phoneme.path("coachingText").asText())
        .isEqualTo("'th'가 'ss'처럼 들렸어요. 혀끝을 윗니와 아랫니 사이에 살짝 내밀어 대고 바람을 내보내세요.");

    // 강세 오류(4번): 자산의 음절·정답 강세 + AI의 사용자 강세 + 코칭이 병합돼야 한다.
    JsonNode stress = words.get(3);
    assertThat(stress.path("status").asText()).isEqualTo("STRESS_ERROR");
    assertThat(stress.path("syllables").get(0).asText()).isEqualTo("hik");
    assertThat(stress.path("stressIndex").asInt()).isZero();
    assertThat(stress.path("userStressIndex").asInt()).isEqualTo(1);
    assertThat(stress.path("coachingText").asText()).contains("'hik' 음절에 힘을 줘보세요");
  }

  @Test
  void analyzePassesWhenEveryWordIsCorrect() throws Exception {
    String accessToken = loginWithUsTutor("pron-analyze-pass");

    // 1단어 문장은 로컬 스텁이 전부 정상 판정 → 100점 통과.
    mockMvc
        .perform(
            multipart(analysisUrl(SHORT_EXPRESSION_ID))
                .file(sampleAudio())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.score").value(100))
        .andExpect(jsonPath("$.data.passed").value(true))
        .andExpect(jsonPath("$.data.words.length()").value(1));
  }

  @Test
  void analyzeReturnsNotFoundWhenAssetIsMissing() throws Exception {
    String accessToken = loginWithUsTutor("pron-analyze-no-asset");

    mockMvc
        .perform(
            multipart(analysisUrl(NO_ASSET_EXPRESSION_ID))
                .file(sampleAudio())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("PRONUNCIATION_DATA_NOT_FOUND"));
  }

  @Test
  void analyzeRejectsUnsupportedAudioFormat() throws Exception {
    String accessToken = loginWithUsTutor("pron-analyze-bad-format");

    MockMultipartFile textFile =
        new MockMultipartFile("audio", "notes.txt", "text/plain", "hello".getBytes());
    mockMvc
        .perform(
            multipart(analysisUrl(EXPRESSION_ID))
                .file(textFile)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_AUDIO"));
  }

  @Test
  void analyzeAcceptsWebmRecording() throws Exception {
    String accessToken = loginWithUsTutor("pron-analyze-webm");

    // 크롬·안드로이드 웹뷰의 MediaRecorder는 webm으로만 녹음된다 (웹 버전 지원).
    MockMultipartFile webmFile =
        new MockMultipartFile("audio", "recording.webm", "audio/webm", "fake-audio".getBytes());
    mockMvc
        .perform(
            multipart(analysisUrl(EXPRESSION_ID))
                .file(webmFile)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.score").isNumber());
  }

  @Test
  void analyzeRequiresAuthentication() throws Exception {
    mockMvc
        .perform(multipart(analysisUrl(EXPRESSION_ID)).file(sampleAudio()))
        .andExpect(status().isUnauthorized());
  }

  private String analysisUrl(long expressionId) {
    return "/api/v1/expressions/" + expressionId + "/pronunciation/sentence-analysis";
  }

  private MockMultipartFile sampleAudio() {
    return new MockMultipartFile("audio", "recording.m4a", "audio/mp4", "fake-audio".getBytes());
  }

  // 표현 1건을 시드한다. words 배열 SQL은 호출자가 넘긴다.
  private void insertExpression(long id, String sentenceText, String wordsArraySql) {
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
        values (?, 'FREE_TALK', 'CONVERSATION_SKILL', 'BASIC', 'EN', 'KR', ?,
            'sample expression', '샘플', 'summary', 'description', ?, '해석',
            %s, ARRAY['choice'],
            CAST('[]' AS jsonb), 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """
            .formatted(wordsArraySql),
        id,
        id,
        sentenceText);
  }

  // 표현의 EN_US 발음 자산을 시드한다 (TTS까지 완성된 상태).
  // JDBC CAST(? AS jsonb) 시드는 H2에서 Hibernate JSON 타입 읽기와 호환되지 않아
  // (words가 빈 값으로 읽힘) 실제 저장 경로와 같은 엔티티 저장을 사용한다.
  // words 목록이 AI 요청의 단어 목록이 되므로 문장 단어 수와 개수를 맞춘다.
  private void insertAsset(long expressionId, int wordCount) {
    StringBuilder wordsJson = new StringBuilder("[");
    for (int order = 1; order <= wordCount; order++) {
      if (order > 1) {
        wordsJson.append(",");
      }
      if (order == 2) {
        // 로컬 스텁이 음소 오류로 판정하는 단어 — 발음 카드 병합 검증에 쓰는 상세 데이터.
        wordsJson.append(
            """
            {"order": 2, "word": "nothing", "syllables": ["nuh", "thing"], "stressIndex": 0,
             "pronunciationDisplay": "nuh·thing",
             "accentContrast": {"expected": "sounds like 「nuh·thing」",
                                "other": "sounds like 「nah·ssing」", "errorType": "PHONEME"},
             "audioUrl": "https://cdn.example.com/words/2.mp3"}
            """);
      } else if (order == 4) {
        // 로컬 스텁이 강세 오류로 판정하는 단어.
        wordsJson.append(
            """
            {"order": 4, "word": "hiking", "syllables": ["hik", "ing"], "stressIndex": 0,
             "pronunciationDisplay": "hai·king",
             "audioUrl": "https://cdn.example.com/words/4.mp3"}
            """);
      } else {
        wordsJson.append(
            """
            {"order": %d, "word": "w%d", "syllables": ["w%d"], "stressIndex": 0,
             "pronunciationDisplay": "w%d", "audioUrl": "https://cdn.example.com/words/%d.mp3"}
            """
                .formatted(order, order, order, order, order));
      }
    }
    wordsJson.append("]");
    try {
      ExpressionPronunciationAsset asset =
          new ExpressionPronunciationAsset(
              expressionId, AccentLocale.EN_US, objectMapper.readTree(wordsJson.toString()));
      asset.attachTts(
          "https://cdn.example.com/expression.mp3",
          "https://cdn.example.com/sentence.mp3",
          objectMapper.readTree(wordsJson.toString()));
      assetRepository.save(asset);
    } catch (Exception exception) {
      throw new IllegalStateException("발음 자산 시드 JSON 생성에 실패했습니다.", exception);
    }
  }

  // 로그인 후 유저의 튜터를 EN_US 억양의 튜터로 고정한다 (시드 자산이 EN_US라서).
  private String loginWithUsTutor(String userKey) throws Exception {
    LoginResult loginResult = login(userKey);
    Long usTutorId =
        jdbcTemplate.queryForObject(
            "select id from ai_tutor where accent_locale = 'EN_US' order by id limit 1",
            Long.class);
    jdbcTemplate.update(
        "update user_profile set ai_tutor_id = ? where id = ?",
        usTutorId,
        loginResult.userProfileId());
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
                          "idToken":"%s|%s@example.com|Pron User|%s",
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
