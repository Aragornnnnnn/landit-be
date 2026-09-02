// 프리톡 API의 외부 계약과 저장 경계를 검증한다.

package com.landit.landitbe.feature.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.session.client.ai.AiConversationEmbeddingsRequest;
import com.landit.landitbe.feature.session.client.ai.AiConversationEmbeddingsResult;
import com.landit.landitbe.feature.session.client.ai.AiConversationExcerpt;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionRecommendation;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionRecommendationsRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionRecommendationsResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkInnerThoughtResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnResult;
import com.landit.landitbe.feature.session.client.ai.AiMemoryCandidatesRequest;
import com.landit.landitbe.feature.session.client.ai.AiMemoryCandidatesResult;
import com.landit.landitbe.feature.session.client.ai.AiMemoryOperation;
import com.landit.landitbe.feature.session.client.ai.AiMemoryQueryEmbeddingRequest;
import com.landit.landitbe.feature.session.client.ai.AiMemoryQueryEmbeddingResult;
import com.landit.landitbe.feature.session.client.ai.AiMemoryResolutionRequest;
import com.landit.landitbe.feature.session.client.ai.AiMemoryResolutionResult;
import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.feature.session.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@TestPropertySource(
    properties = {
      "landit.auth.oidc.fake-enabled=true",
      "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
    })
class FreeTalkSessionApiIntegrationTests {

  private static final String EXPRESSION_GENERATION_STATUS_QUERY =
      "SELECT expression_generation_status "
          + "FROM free_talk_session "
          + "WHERE learning_session_id = ?";

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FreeTalkSessionExpressionRepository freeTalkSessionExpressionRepository;

  @Autowired private FakeAiFreeTalkClient fakeAiFreeTalkClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    fakeAiFreeTalkClient.reset();
    cleanUpDatabase();
  }

  @AfterEach
  void tearDown() {
    cleanUpDatabase();
  }

  private void cleanUpDatabase() {
    awaitPendingExpressionGeneration();
    jdbcTemplate.update("DELETE FROM user_daily_activity");
    jdbcTemplate.update("DELETE FROM user_learning_activity_summary");
    jdbcTemplate.update("DELETE FROM free_talk_daily_speaking_usage");
    jdbcTemplate.update("DELETE FROM free_talk_session_expression");
    jdbcTemplate.update("DELETE FROM user_writing_expression_completion");
    jdbcTemplate.update("DELETE FROM free_talk_session");
    jdbcTemplate.update("DELETE FROM session_history_message");
    jdbcTemplate.update("DELETE FROM session_history");
    jdbcTemplate.update("DELETE FROM learning_session");
    jdbcTemplate.update("DELETE FROM free_talk_topic");
    jdbcTemplate.update("DELETE FROM writing_expression WHERE id = 994104");
    jdbcTemplate.update("DELETE FROM writing_expression WHERE id = 994103");
    jdbcTemplate.update("DELETE FROM writing_expression WHERE id = 994201");
    jdbcTemplate.update("DELETE FROM scenario WHERE id = 994102");
    jdbcTemplate.update("DELETE FROM category WHERE id = 994101");
  }

  private void awaitPendingExpressionGeneration() {
    for (int attempt = 0; attempt < 100; attempt++) {
      Integer pendingCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM free_talk_session "
                  + "WHERE expression_generation_status = 'PREPARING'",
              Integer.class);
      if (pendingCount == null || pendingCount == 0) {
        return;
      }
      try {
        Thread.sleep(50L);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        String message = "프리톡 표현 생성 종료를 기다리는 중 인터럽트되었습니다.";
        throw new IllegalStateException(message, exception);
      }
    }
    throw new IllegalStateException("프리톡 표현 생성이 제한 시간 안에 종료되지 않았습니다.");
  }

  @Test
  void listTopicsReturnsOnlyActiveTopicsInDisplayOrder() throws Exception {
    seedTopic(1002, "두 번째", "두 번째 설명", 2, "ACTIVE");
    seedTopic(1001, "첫 번째", "첫 번째 설명", 1, "ACTIVE");
    seedTopic(1003, "숨김", "숨김 설명", 3, "INACTIVE");
    String accessToken =
        login("free-talk-topics@example.com").get("data").get("accessToken").asText();

    mockMvc
        .perform(
            get("/api/v1/free-talk/topics")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.dailySpeakingTimeLimitMs").value(60000))
        .andExpect(jsonPath("$.data.remainingSpeakingTimeMs").value(60000))
        .andExpect(jsonPath("$.data.topics.length()").value(2))
        .andExpect(jsonPath("$.data.topics[0].topicId").value(1001))
        .andExpect(jsonPath("$.data.topics[0].displayName").value("첫 번째"))
        .andExpect(jsonPath("$.data.topics[0].displayOrder").value(1))
        .andExpect(jsonPath("$.data.topics[1].topicId").value(1002));
  }

  @Test
  void rejectsUnauthenticatedTopicAndSessionRequestsWithStandardError() throws Exception {
    mockMvc
        .perform(get("/api/v1/free-talk/topics"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    mockMvc
        .perform(
            post("/api/v1/free-talk/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startMode\":\"USER_FIRST\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
  }

  @Test
  void startAiFirstSessionPersistsOpeningWithoutTutorCharacterContext() throws Exception {
    seedTopic(1101, "주말 계획", "다가오는 주말의 계획을 묻는다.", 1, "ACTIVE");
    JsonNode loginBody = login("free-talk-ai-first@example.com");
    long userId = loginBody.get("data").get("user").get("userId").asLong();
    String accessToken = loginBody.get("data").get("accessToken").asText();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/free-talk/sessions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"startMode\":\"AI_FIRST\",\"topicId\":1101,\"characterId\":\"chloe\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.sessionType").value("FREE_TALK"))
            .andExpect(jsonPath("$.data.startMode").value("AI_FIRST"))
            .andExpect(jsonPath("$.data.character.characterId").value("chloe"))
            .andExpect(jsonPath("$.data.title").value("주말 계획"))
            .andExpect(jsonPath("$.data.speakingTimeLimitMs").value(60000))
            .andExpect(jsonPath("$.data.character.ttsVoice.provider").value("OPENROUTER"))
            .andExpect(
                jsonPath("$.data.character.ttsVoice.providerVoiceId").value("aura-2-luna-en"))
            .andExpect(
                jsonPath("$.data.currentMessage.content").value("What are your weekend plans?"))
            .andExpect(jsonPath("$.data.currentMessage.translatedContent").value("이번 주말 계획은 뭐야?"))
            .andExpect(jsonPath("$.data.currentMessage.emotion").value("HAPPY"))
            .andReturn();

    long sessionId =
        objectMapper
            .readTree(result.getResponse().getContentAsByteArray())
            .get("data")
            .get("sessionId")
            .asLong();
    assertThat(fakeAiFreeTalkClient.lastOpeningRequest().sessionId()).isEqualTo(sessionId);
    assertThat(fakeAiFreeTalkClient.lastOpeningRequest().characterId()).isEqualTo("chloe");
    assertThat(fakeAiFreeTalkClient.lastOpeningRequest().topic().topicId()).isEqualTo(1101);
    assertThat(fakeAiFreeTalkClient.openingTransactionActive()).isFalse();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT session_type FROM learning_session WHERE id = ?", String.class, sessionId))
        .isEqualTo("FREE_TALK");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT user_profile_id FROM learning_session WHERE id = ?", Long.class, sessionId))
        .isEqualTo(userId);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT content
                FROM session_history_message shm
                JOIN session_history sh ON sh.id = shm.session_history_id
                WHERE sh.learning_session_id = ?
                """,
                String.class,
                sessionId))
        .isEqualTo("What are your weekend plans?");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT character_id FROM free_talk_session WHERE learning_session_id = ?",
                String.class,
                sessionId))
        .isEqualTo("chloe");
  }

  @Test
  void startUserFirstSessionDoesNotCallAiAndCreatesEmptyHistory() throws Exception {
    JsonNode loginBody = login("free-talk-user-first@example.com");
    String accessToken = loginBody.get("data").get("accessToken").asText();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/free-talk/sessions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"startMode\":\"USER_FIRST\",\"characterId\":\"marco\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.startMode").value("USER_FIRST"))
            .andExpect(jsonPath("$.data.character.characterId").value("marco"))
            .andExpect(jsonPath("$.data.character.ttsVoice.provider").value("OPENROUTER"))
            .andExpect(
                jsonPath("$.data.character.ttsVoice.providerVoiceId").value("aura-2-hyperion-en"))
            .andExpect(jsonPath("$.data.title").value(nullValue()))
            .andExpect(jsonPath("$.data.currentMessage").value(nullValue()))
            .andReturn();

    long sessionId =
        objectMapper
            .readTree(result.getResponse().getContentAsByteArray())
            .get("data")
            .get("sessionId")
            .asLong();
    assertThat(fakeAiFreeTalkClient.lastOpeningRequest()).isNull();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM session_history_message shm
                JOIN session_history sh ON sh.id = shm.session_history_id
                WHERE sh.learning_session_id = ?
                """,
                Integer.class,
                sessionId))
        .isZero();
  }

  @Test
  void completedSessionListAndDetailIncludeStoredCharacterId() throws Exception {
    JsonNode loginBody = login("free-talk-character-history@example.com");
    String accessToken = loginBody.get("data").get("accessToken").asText();
    MvcResult startResult =
        mockMvc
            .perform(
                post("/api/v1/free-talk/sessions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"startMode\":\"USER_FIRST\",\"characterId\":\"teddy\"}"))
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.data.character.ttsVoice.providerVoiceId").value("aura-2-draco-en"))
            .andReturn();
    long sessionId =
        objectMapper
            .readTree(startResult.getResponse().getContentAsByteArray())
            .get("data")
            .get("sessionId")
            .asLong();
    completeSession(sessionId);

    mockMvc
        .perform(
            get("/api/v1/free-talk/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].characterId").value("teddy"));
    mockMvc
        .perform(
            get("/api/v1/free-talk/sessions/{sessionId}", sessionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.characterId").value("teddy"));
  }

  @Test
  void rejectsInvalidStartModeAndTopicCombinations() throws Exception {
    seedTopic(1201, "오늘", "오늘의 일을 묻는다.", 1, "ACTIVE");
    String accessToken =
        login("free-talk-invalid-request@example.com").get("data").get("accessToken").asText();

    mockMvc
        .perform(
            post("/api/v1/free-talk/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startMode\":\"AI_FIRST\",\"characterId\":\"chloe\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    mockMvc
        .perform(
            post("/api/v1/free-talk/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"startMode\":\"USER_FIRST\",\"topicId\":1201,\"characterId\":\"chloe\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
  }

  @Test
  void rejectsMissingAndUnsupportedCharacterId() throws Exception {
    String accessToken =
        login("free-talk-invalid-character@example.com").get("data").get("accessToken").asText();

    for (String content :
        List.of(
            "{\"startMode\":\"USER_FIRST\"}",
            "{\"startMode\":\"USER_FIRST\",\"characterId\":\"unknown\"}")) {
      mockMvc
          .perform(
              post("/api/v1/free-talk/sessions")
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(content))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
  }

  @Test
  void rejectsInvalidCharacterIdBeforeDailySpeakingLimit() throws Exception {
    JsonNode loginBody = login("free-talk-invalid-character-limit@example.com");
    long userId = loginBody.get("data").get("user").get("userId").asLong();
    String accessToken = loginBody.get("data").get("accessToken").asText();
    jdbcTemplate.update(
        """
        INSERT INTO free_talk_daily_speaking_usage (
            user_profile_id, usage_date, used_speaking_duration_ms
        )
        VALUES (?, CURRENT_DATE, 60000)
        """,
        userId);

    for (String content :
        List.of(
            "{\"startMode\":\"USER_FIRST\"}",
            "{\"startMode\":\"USER_FIRST\",\"characterId\":\"\"}",
            "{\"startMode\":\"USER_FIRST\",\"characterId\":\"unknown\"}")) {
      mockMvc
          .perform(
              post("/api/v1/free-talk/sessions")
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(content))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
  }

  @Test
  void rejectsInactiveAndMissingAiFirstTopics() throws Exception {
    seedTopic(1251, "비활성", "노출하지 않는 주제다.", 1, "INACTIVE");
    String accessToken =
        login("free-talk-missing-topic@example.com").get("data").get("accessToken").asText();

    mockMvc
        .perform(
            post("/api/v1/free-talk/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startMode\":\"AI_FIRST\",\"topicId\":1251,\"characterId\":\"chloe\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    mockMvc
        .perform(
            post("/api/v1/free-talk/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startMode\":\"AI_FIRST\",\"topicId\":1252,\"characterId\":\"chloe\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void removesEveryCreatedRecordWhenAiOpeningFails() throws Exception {
    seedTopic(1301, "영화", "최근 본 영화를 묻는다.", 1, "ACTIVE");
    fakeAiFreeTalkClient.failOpening();
    String accessToken =
        login("free-talk-opening-failure@example.com").get("data").get("accessToken").asText();

    mockMvc
        .perform(
            post("/api/v1/free-talk/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startMode\":\"AI_FIRST\",\"topicId\":1301,\"characterId\":\"chloe\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error.code").value("AI_GENERATION_FAILED"));

    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM learning_session", Integer.class))
        .isZero();
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM free_talk_session", Integer.class))
        .isZero();
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM session_history", Integer.class))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_history_message", Integer.class))
        .isZero();
  }

  @Test
  void openApiDocumentsFreeTalkContracts() throws Exception {
    String topicsPath = "$.paths['/api/v1/free-talk/topics'].get";
    String sessionsPath = "$.paths['/api/v1/free-talk/sessions'].post";
    String messagesPath = "$.paths['/api/v1/free-talk/sessions/{sessionId}/messages'].post";
    String exitDecisionPath =
        "$.paths['/api/v1/free-talk/sessions/{sessionId}/exit-decision'].post";
    String pastSessionsPath = "$.paths['/api/v1/free-talk/sessions'].get";
    String pastSessionDetailPath = "$.paths['/api/v1/free-talk/sessions/{sessionId}'].get";
    String innerThoughtProcessingStatusPath =
        "$.components.schemas.SessionInnerThoughtResponse.properties.processingStatus";

    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(topicsPath + ".security[0].bearerAuth").exists())
        .andExpect(jsonPath(topicsPath + ".responses['200'].description").value("조회 성공"))
        .andExpect(jsonPath(topicsPath + ".responses['401'].description").value("인증 실패"))
        .andExpect(jsonPath(sessionsPath + ".security[0].bearerAuth").exists())
        .andExpect(jsonPath(sessionsPath + ".responses['201'].description").value("시작 성공"))
        .andExpect(jsonPath(sessionsPath + ".responses['400'].description").value("요청 오류"))
        .andExpect(jsonPath(sessionsPath + ".responses['401'].description").value("인증 실패"))
        .andExpect(jsonPath(sessionsPath + ".responses['404'].description").value("주제 없음"))
        .andExpect(jsonPath(sessionsPath + ".responses['502'].description").value("AI 응답 오류"))
        .andExpect(jsonPath(sessionsPath + ".responses['503'].description").value("AI 생성 실패"))
        .andExpect(
            jsonPath(
                    "$.components.schemas.FreeTalkSessionStartRequest.required"
                        + "[?(@ == 'characterId')]")
                .exists())
        .andExpect(
            jsonPath(
                    "$.components.schemas.FreeTalkSessionStartRequest.properties"
                        + ".characterId.enum.length()")
                .value(3))
        .andExpect(jsonPath(messagesPath + ".security[0].bearerAuth").exists())
        .andExpect(jsonPath(messagesPath + ".responses['401'].description").value("인증 실패"))
        .andExpect(jsonPath(exitDecisionPath + ".security[0].bearerAuth").exists())
        .andExpect(jsonPath(exitDecisionPath + ".responses['401'].description").value("인증 실패"))
        .andExpect(
            jsonPath(pastSessionsPath + ".responses['400'].description").value("페이지 번호 또는 크기 오류"))
        .andExpect(jsonPath(pastSessionsPath + ".responses['401'].description").value("인증 실패"))
        .andExpect(
            jsonPath(pastSessionDetailPath + ".responses['403'].description").value("세션 소유자 아님"))
        .andExpect(
            jsonPath(pastSessionDetailPath + ".responses['404'].description")
                .value("완료된 프리톡 세션 없음"))
        .andExpect(
            jsonPath(innerThoughtProcessingStatusPath + ".description")
                .value("속마음 처리 상태. 종료 의사 감지 뒤 속마음 생성을 시작하지 않은 경우 null"));
  }

  @Test
  void replaysCompletedTurnForDuplicateClientMessageId() throws Exception {
    String accessToken =
        login("free-talk-message@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    String clientMessageId = UUID.randomUUID().toString();
    String request = messageRequest(clientMessageId, "I went hiking with friends.", 4200, false);

    MvcResult firstResult =
        mockMvc
            .perform(
                post(messagePath(sessionId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value(nullValue()))
            .andExpect(jsonPath("$.data.turnStatus").value("CONTINUE"))
            .andExpect(jsonPath("$.data.submittedMessage.role").value("USER"))
            .andExpect(
                jsonPath("$.data.submittedMessage.innerThoughtProcessingStatus").value("PREPARING"))
            .andExpect(jsonPath("$.data.submittedMessage.innerThought").value(nullValue()))
            .andExpect(jsonPath("$.data.nextMessage.role").value("AI"))
            .andExpect(jsonPath("$.data.progress.accumulatedSpeakingDurationMs").value(4200))
            .andExpect(jsonPath("$.data.progress.usedSpeakingTimeMs").value(4200))
            .andExpect(jsonPath("$.data.progress.remainingSpeakingTimeMs").value(55800))
            .andExpect(jsonPath("$.data.progress.sessionStatus").value("IN_PROGRESS"))
            .andReturn();

    MvcResult replayedResult =
        mockMvc
            .perform(
                post(messagePath(sessionId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.turnStatus").value("CONTINUE"))
            .andReturn();

    assertThat(
            objectMapper
                .readTree(firstResult.getResponse().getContentAsByteArray())
                .at("/data/submittedMessage/messageId"))
        .isEqualTo(
            objectMapper
                .readTree(replayedResult.getResponse().getContentAsByteArray())
                .at("/data/submittedMessage/messageId"));
    assertThat(
            objectMapper
                .readTree(firstResult.getResponse().getContentAsByteArray())
                .at("/data/nextMessage/messageId"))
        .isEqualTo(
            objectMapper
                .readTree(replayedResult.getResponse().getContentAsByteArray())
                .at("/data/nextMessage/messageId"));

    assertThat(fakeAiFreeTalkClient.turnTransactionActive()).isFalse();
    assertThat(fakeAiFreeTalkClient.turnCallCount()).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_history_message", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void exitDetectionStoresOnlyTheUserMessageAndReplaysRepeatedDecision() throws Exception {
    String accessToken =
        login("free-talk-exit@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    fakeAiFreeTalkClient.detectExitIntent();
    String exitRequest =
        messageRequest(UUID.randomUUID().toString(), "I should go now.", 1200, false);
    MvcResult detected =
        mockMvc
            .perform(
                post(messagePath(sessionId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(exitRequest))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.turnStatus").value("EXIT_CONFIRMATION_REQUIRED"))
            .andExpect(jsonPath("$.data.nextMessage").value(nullValue()))
            .andExpect(
                jsonPath("$.data.submittedMessage.innerThoughtProcessingStatus").value(nullValue()))
            .andExpect(jsonPath("$.data.progress.sessionStatus").value("AWAITING_EXIT_DECISION"))
            .andReturn();
    long submittedMessageId =
        objectMapper
            .readTree(detected.getResponse().getContentAsByteArray())
            .at("/data/submittedMessage/messageId")
            .asLong();
    String decisionRequest =
        "{\"submittedMessageId\":%d,\"decision\":\"CONTINUE\"}".formatted(submittedMessageId);

    mockMvc
        .perform(
            post(exitDecisionPath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(decisionRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("CONTINUE"))
        .andExpect(jsonPath("$.data.progress.sessionStatus").value("IN_PROGRESS"));
    mockMvc
        .perform(
            post(exitDecisionPath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(decisionRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("CONTINUE"))
        .andExpect(jsonPath("$.data.progress.sessionStatus").value("IN_PROGRESS"));

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(exitRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("CONTINUE"))
        .andExpect(jsonPath("$.data.nextMessage.role").value("AI"));

    mockMvc
        .perform(
            post(exitDecisionPath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"submittedMessageId\":%d,\"decision\":\"END\"}"
                        .formatted(submittedMessageId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("CONFLICT"));
  }

  @Test
  void assignsCharacterFallbackTitleWhenUserFirstSessionEndsWithoutGeneratedTitle()
      throws Exception {
    String accessToken =
        login("free-talk-title-fallback@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    fakeAiFreeTalkClient.omitClosingTitle();
    fakeAiFreeTalkClient.detectExitIntent();
    long submittedMessageId = submitForExit(accessToken, sessionId);

    mockMvc
        .perform(
            post(exitDecisionPath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"submittedMessageId\":%d,\"decision\":\"END\"}"
                        .formatted(submittedMessageId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Chloe와의 대화"))
        .andExpect(jsonPath("$.data.turnStatus").value("COMPLETED"));
  }

  @Test
  void assignsGeneratedEnglishTitleWhenUserFirstSessionEnds() throws Exception {
    String accessToken =
        login("free-talk-generated-title@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    fakeAiFreeTalkClient.detectExitIntent();
    long submittedMessageId = submitForExit(accessToken, sessionId);

    mockMvc
        .perform(
            post(exitDecisionPath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"submittedMessageId\":%d,\"decision\":\"END\"}"
                        .formatted(submittedMessageId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Weekend Hiking"))
        .andExpect(jsonPath("$.data.turnStatus").value("COMPLETED"));
  }

  @Test
  void preservesRecommendedTopicTitleWhenAiFirstSessionEnds() throws Exception {
    seedTopic(1151, "주말 계획", "다가오는 주말의 계획을 묻는다.", 1, "ACTIVE");
    String accessToken =
        login("free-talk-topic-title@example.com").get("data").get("accessToken").asText();
    MvcResult startResult =
        mockMvc
            .perform(
                post("/api/v1/free-talk/sessions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"startMode\":\"AI_FIRST\",\"topicId\":1151,"
                            + "\"characterId\":\"chloe\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    long sessionId =
        objectMapper
            .readTree(startResult.getResponse().getContentAsByteArray())
            .at("/data/sessionId")
            .asLong();

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    messageRequest(UUID.randomUUID().toString(), "One last thing.", 60000, false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("주말 계획"))
        .andExpect(jsonPath("$.data.turnStatus").value("COMPLETED"));
  }

  @Test
  void resumesExpiredMessageReservationForTheSameClientMessageId() throws Exception {
    String accessToken =
        login("free-talk-stale-reservation@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    long historyId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM session_history WHERE learning_session_id = ?", Long.class, sessionId);
    String clientMessageId = UUID.randomUUID().toString();
    jdbcTemplate.update(
        """
        INSERT INTO session_history_message (
            session_history_id, message_sequence, turn_number, role, content, client_message_id,
            utterance_duration_ms, input_type, created_at, updated_at
        )
        VALUES (?, 1, 1, 'USER', 'I went hiking with friends.', ?, 1200, 'VOICE',
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        historyId,
        clientMessageId);
    jdbcTemplate.update(
        """
        UPDATE free_talk_session
        SET processing_client_message_id = ?,
            updated_at = DATEADD('SECOND', -91, CURRENT_TIMESTAMP)
        WHERE learning_session_id = ?
        """,
        clientMessageId,
        sessionId);

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    messageRequest(clientMessageId, "I went hiking with friends.", 1200, false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("CONTINUE"))
        .andExpect(jsonPath("$.data.submittedMessage.messageSequence").value(1))
        .andExpect(jsonPath("$.data.progress.accumulatedSpeakingDurationMs").value(1200));

    assertThat(fakeAiFreeTalkClient.turnCallCount()).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_history_message", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void endDecisionRecordsStreakWithoutDuplicatingTheSameDayTimeLimit() throws Exception {
    seedEmbeddedCandidateExpression();
    String accessToken =
        login("free-talk-complete@example.com").get("data").get("accessToken").asText();
    long exitSessionId = startUserFirstSession(accessToken);
    fakeAiFreeTalkClient.detectExitIntent();
    long submittedMessageId = submitForExit(accessToken, exitSessionId);

    mockMvc
        .perform(
            post(exitDecisionPath(exitSessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"submittedMessageId\":%d,\"decision\":\"END\"}"
                        .formatted(submittedMessageId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.data.progress.sessionStatus").value("COMPLETED"));
    assertThat(awaitExpressionGenerationStatus(exitSessionId)).isEqualTo("READY");
    assertCurrentStreak(accessToken, 1, true);

    fakeAiFreeTalkClient.reset();
    long timeLimitSessionId = startUserFirstSession(accessToken);
    mockMvc
        .perform(
            post(messagePath(timeLimitSessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    messageRequest(UUID.randomUUID().toString(), "One last thing.", 180000, true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.data.progress.accumulatedSpeakingDurationMs").value(180000));
    assertThat(awaitExpressionGenerationStatus(timeLimitSessionId)).isEqualTo("READY");
    assertCurrentStreak(accessToken, 1, true);
  }

  @Test
  void excludesCandidatesAboveLearnerDifficultyLevel() throws Exception {
    // 난이도 4 표현만 심어두면 학습 수준 2(상한 3) 사용자에게는 후보가 남지 않아 실패로 전환된다.
    seedEmbeddedCandidateExpression(4);
    String accessToken =
        login("free-talk-difficulty-excluded@example.com").get("data").get("accessToken").asText();
    updateLearningLevel(accessToken, 2);
    long sessionId = startUserFirstSession(accessToken);
    fakeAiFreeTalkClient.detectExitIntent();
    long submittedMessageId = submitForExit(accessToken, sessionId);

    mockMvc
        .perform(
            post(exitDecisionPath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"submittedMessageId\":%d,\"decision\":\"END\"}"
                        .formatted(submittedMessageId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("COMPLETED"));

    assertThat(awaitExpressionGenerationStatus(sessionId)).isEqualTo("FAILED");
  }

  @Test
  void keepsCandidatesWithinLearnerDifficultyLevel() throws Exception {
    // 같은 난이도 4 표현이라도 학습 수준 4(상한 5) 사용자에게는 후보로 남는다.
    seedEmbeddedCandidateExpression(4);
    String accessToken =
        login("free-talk-difficulty-kept@example.com").get("data").get("accessToken").asText();
    updateLearningLevel(accessToken, 4);
    long sessionId = startUserFirstSession(accessToken);
    fakeAiFreeTalkClient.detectExitIntent();
    long submittedMessageId = submitForExit(accessToken, sessionId);

    mockMvc
        .perform(
            post(exitDecisionPath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"submittedMessageId\":%d,\"decision\":\"END\"}"
                        .formatted(submittedMessageId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("COMPLETED"));

    assertThat(awaitExpressionGenerationStatus(sessionId)).isEqualTo("READY");
  }

  @Test
  void failsExpressionGenerationWhenNoEmbeddedCandidateExists() throws Exception {
    // 임베딩이 있는 공용 후보를 심지 않으면 유사도 검색이 빈손이 되어 실패로 전환된다.
    String accessToken =
        login("free-talk-no-candidate@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    fakeAiFreeTalkClient.detectExitIntent();
    long submittedMessageId = submitForExit(accessToken, sessionId);

    mockMvc
        .perform(
            post(exitDecisionPath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"submittedMessageId\":%d,\"decision\":\"END\"}"
                        .formatted(submittedMessageId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("COMPLETED"));

    assertThat(awaitExpressionGenerationStatus(sessionId)).isEqualTo("FAILED");
  }

  @Test
  void returnsPublicFreeTalkExpressionLearningContent() throws Exception {
    JsonNode loginBody = login("free-talk-learning-content@example.com");
    String accessToken = loginBody.at("/data/accessToken").asText();
    long learningSessionId = startUserFirstSession(accessToken);
    FreeTalkExpressionLink link = seedNewExpressionForCompletedSession(learningSessionId);

    mockMvc
        .perform(
            get("/api/v1/expressions/{expressionId}/learning-start", link.expressionId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.targetExpressionText").value("hit it off"))
        .andExpect(jsonPath("$.data.representativeSentenceWords.length()").value(6))
        .andExpect(jsonPath("$.data.representativeImageUrl").value(nullValue()));

    mockMvc
        .perform(
            get("/api/v1/expressions/{expressionId}/practice", link.expressionId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.practiceSentence.length()").value(2))
        // imageUrl은 응답 계약에서 빠졌다
        .andExpect(jsonPath("$.data.practiceSentence[0].imageUrl").doesNotExist())
        .andExpect(jsonPath("$.data.writingSentence.length()").value(2))
        .andExpect(jsonPath("$.data.writingSentence[0].writingSentenceWords").isArray())
        .andExpect(jsonPath("$.data.writingSentence[*].quizLanguage").isArray());
  }

  @Test
  void completesPrivateFreeTalkExpressionIdempotently() throws Exception {
    JsonNode loginBody = login("free-talk-learning-finish@example.com");
    String accessToken = loginBody.at("/data/accessToken").asText();
    long learningSessionId = startUserFirstSession(accessToken);
    FreeTalkExpressionLink link = seedNewExpressionForCompletedSession(learningSessionId);

    finishExpression(accessToken, link);

    Object firstSessionCompletedAt =
        jdbcTemplate.queryForObject(
            "SELECT completed_at FROM free_talk_session_expression "
                + "WHERE free_talk_session_id = ? AND writing_expression_id = ?",
            Object.class,
            link.freeTalkSessionId(),
            link.expressionId());
    assertThat(firstSessionCompletedAt).isNotNull();

    Object firstCompletedAt =
        jdbcTemplate.queryForObject(
            "SELECT completed_at FROM user_writing_expression_completion "
                + "WHERE writing_expression_id = ?",
            Object.class,
            link.expressionId());

    finishExpression(accessToken, link);

    Object repeatedSessionCompletedAt =
        jdbcTemplate.queryForObject(
            "SELECT completed_at FROM free_talk_session_expression "
                + "WHERE free_talk_session_id = ? AND writing_expression_id = ?",
            Object.class,
            link.freeTalkSessionId(),
            link.expressionId());

    Object repeatedCompletedAt =
        jdbcTemplate.queryForObject(
            "SELECT completed_at FROM user_writing_expression_completion "
                + "WHERE writing_expression_id = ?",
            Object.class,
            link.expressionId());
    assertThat(repeatedCompletedAt).isEqualTo(firstCompletedAt);
    assertThat(repeatedSessionCompletedAt).isEqualTo(firstSessionCompletedAt);

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_writing_expression_completion
                WHERE writing_expression_id = ?
                """,
                Integer.class,
                link.expressionId()))
        .isEqualTo(1);
  }

  @Test
  void keepsRepeatedExpressionCompletionSeparateForEachFreeTalkSession() throws Exception {
    JsonNode loginBody = login("free-talk-repeated-expression@example.com");
    String accessToken = loginBody.at("/data/accessToken").asText();
    long firstLearningSessionId = startUserFirstSession(accessToken);
    FreeTalkExpressionLink firstLink = seedNewExpressionForCompletedSession(firstLearningSessionId);
    jdbcTemplate.update(
        "UPDATE free_talk_session_expression "
            + "SET created_at = TIMESTAMP '2026-07-27 10:00:00' "
            + "WHERE free_talk_session_id = ? AND writing_expression_id = ?",
        firstLink.freeTalkSessionId(),
        firstLink.expressionId());

    long secondLearningSessionId = startUserFirstSession(accessToken);
    long secondFreeTalkSessionId = completeSession(secondLearningSessionId);
    freeTalkSessionExpressionRepository.saveAndFlush(
        FreeTalkSessionExpression.link(secondFreeTalkSessionId, firstLink.expressionId(), 1));
    jdbcTemplate.update(
        "UPDATE free_talk_session_expression "
            + "SET created_at = TIMESTAMP '2026-07-28 10:00:00' "
            + "WHERE free_talk_session_id = ? AND writing_expression_id = ?",
        secondFreeTalkSessionId,
        firstLink.expressionId());
    FreeTalkExpressionLink secondLink =
        new FreeTalkExpressionLink(
            secondLearningSessionId, secondFreeTalkSessionId, firstLink.expressionId());

    finishExpression(accessToken, secondLink);

    mockMvc
        .perform(
            get("/api/v1/free-talk/sessions/{sessionId}", firstLearningSessionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.expressions[0].completed").value(false))
        .andExpect(jsonPath("$.data.expressions[0].lastRecommendedAt").value(nullValue()));
    mockMvc
        .perform(
            get("/api/v1/free-talk/sessions/{sessionId}", secondLearningSessionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.expressions[0].completed").value(true))
        .andExpect(
            jsonPath("$.data.expressions[0].lastRecommendedAt").value("2026-07-27T10:00:00"));
  }

  @Test
  void rejectsForeignMissingCompletedAndAwaitingSessions() throws Exception {
    JsonNode ownerLogin = login("free-talk-owner@example.com");
    String ownerToken = ownerLogin.get("data").get("accessToken").asText();
    String otherToken =
        login("free-talk-other@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(ownerToken);
    String request = messageRequest(UUID.randomUUID().toString(), "Hello.", 0, false);

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post(messagePath(999999L))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isNotFound());

    fakeAiFreeTalkClient.detectExitIntent();
    submitForExit(ownerToken, sessionId);
    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageRequest(UUID.randomUUID().toString(), "Again.", 0, false)))
        .andExpect(status().isConflict());
  }

  @Test
  void refundsDailyUsageWhenAiTurnFails() throws Exception {
    String accessToken =
        login("free-talk-compensation@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    String clientMessageId = UUID.randomUUID().toString();
    String request = messageRequest(clientMessageId, "This should fail.", 700, false);
    fakeAiFreeTalkClient.failTurn();

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error.code").value("AI_GENERATION_FAILED"));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_history_message", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT accumulated_speaking_duration_ms FROM free_talk_session "
                    + "WHERE learning_session_id = ?",
                Long.class,
                sessionId))
        .isZero();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT processing_client_message_id FROM free_talk_session "
                    + "WHERE learning_session_id = ?",
                String.class,
                sessionId))
        .isNull();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT used_speaking_duration_ms FROM free_talk_daily_speaking_usage", Long.class))
        .isZero();

    fakeAiFreeTalkClient.reset();
    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("CONTINUE"));
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT used_speaking_duration_ms FROM free_talk_daily_speaking_usage", Long.class))
        .isEqualTo(700L);
  }

  @Test
  void rejectsAnotherMessageWhileTheFirstMessageIsCallingAi() throws Exception {
    String accessToken =
        login("free-talk-concurrency@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    fakeAiFreeTalkClient.blockTurn();
    final CompletableFuture<Integer> firstStatus =
        CompletableFuture.supplyAsync(
            () ->
                performMessageStatus(
                    accessToken,
                    sessionId,
                    messageRequest(UUID.randomUUID().toString(), "First.", 0, false)));
    assertThat(fakeAiFreeTalkClient.awaitTurnStarted()).isTrue();

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageRequest(UUID.randomUUID().toString(), "Second.", 0, false)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("CONFLICT"));

    fakeAiFreeTalkClient.releaseTurn();
    assertThat(firstStatus.get(5, TimeUnit.SECONDS)).isEqualTo(200);
  }

  @Test
  void completesAfterLastUtteranceCrossesDailySpeakingLimit() throws Exception {
    seedEmbeddedCandidateExpression();
    String accessToken =
        login("free-talk-server-time-limit@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    messageRequest(UUID.randomUUID().toString(), "Almost done.", 59000, false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("CONTINUE"));

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    messageRequest(UUID.randomUUID().toString(), "One last thing.", 3000, false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.data.title").value("Weekend Hiking"))
        .andExpect(jsonPath("$.data.progress.accumulatedSpeakingDurationMs").value(62000));
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT title FROM free_talk_session WHERE learning_session_id = ?",
                String.class,
                sessionId))
        .isEqualTo("Weekend Hiking");
    assertThat(awaitExpressionGenerationStatus(sessionId)).isEqualTo("READY");
  }

  @Test
  void ignoresClientTimeLimitSignalWhileDailySpeakingTimeRemains() throws Exception {
    String accessToken =
        login("free-talk-client-time-limit@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(messageRequest(UUID.randomUUID().toString(), "Keep talking.", 1000, true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("CONTINUE"))
        .andExpect(jsonPath("$.data.progress.remainingSpeakingTimeMs").value(59000));
  }

  private long startUserFirstSession(String accessToken) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/free-talk/sessions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"startMode\":\"USER_FIRST\",\"characterId\":\"chloe\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsByteArray())
        .at("/data/sessionId")
        .asLong();
  }

  private long submitForExit(String accessToken, long sessionId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(messagePath(sessionId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        messageRequest(
                            UUID.randomUUID().toString(), "I have to leave.", 1200, false)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsByteArray())
        .at("/data/submittedMessage/messageId")
        .asLong();
  }

  private int performMessageStatus(String accessToken, long sessionId, String request) {
    try {
      return mockMvc
          .perform(
              post(messagePath(sessionId))
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(request))
          .andReturn()
          .getResponse()
          .getStatus();
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private String messagePath(long sessionId) {
    return "/api/v1/free-talk/sessions/%d/messages".formatted(sessionId);
  }

  private String exitDecisionPath(long sessionId) {
    return "/api/v1/free-talk/sessions/%d/exit-decision".formatted(sessionId);
  }

  private String awaitExpressionGenerationStatus(long sessionId) throws InterruptedException {
    for (int attempt = 0; attempt < 50; attempt++) {
      String status =
          jdbcTemplate.queryForObject(EXPRESSION_GENERATION_STATUS_QUERY, String.class, sessionId);
      if (!"PREPARING".equals(status)) {
        return status;
      }
      Thread.sleep(20L);
    }
    return jdbcTemplate.queryForObject(EXPRESSION_GENERATION_STATUS_QUERY, String.class, sessionId);
  }

  private String messageRequest(
      String clientMessageId, String content, long utteranceDurationMs, boolean timeLimitReached) {
    return ("{\"clientMessageId\":\"%s\",\"content\":\"%s\",\"inputType\":\"VOICE\","
            + "\"utteranceDurationMs\":%d,\"timeLimitReached\":%s}")
        .formatted(clientMessageId, content, utteranceDurationMs, timeLimitReached);
  }

  // 유사도 검색이 찾을 수 있도록 Fake 임베딩과 같은 방향의 벡터를 가진 공용 후보 표현을 심는다.
  private void seedEmbeddedCandidateExpression() {
    seedEmbeddedCandidateExpression(3);
  }

  private void seedEmbeddedCandidateExpression(int difficultyLevel) {
    jdbcTemplate.update(
        """
        INSERT INTO writing_expression (
            id, scenario_id, expression_source, expression_type,
            usage_frequency_level, difficulty_level, target_locale, base_locale, display_order, target_expression_text,
            base_expression_meaning_text, usage_summary, usage_description,
            representative_sentence_text, representative_sentence_translation,
            representative_sentence_words, representative_sentence_word_choices,
            practice_examples_payload, embedding, status, created_at, updated_at
        )
        VALUES (
            994201, NULL, 'FREE_TALK', 'CONVERSATION_SKILL', 'BASIC', ?, 'EN', 'KR', 1,
            'piece of cake', '식은 죽 먹기', '쉬운 일을 말할 때 사용한다.',
            '아주 쉬운 일이었다고 말할 때 사용하는 표현이다.',
            'It was a piece of cake.', '그건 식은 죽 먹기였어.',
            ARRAY['It', 'was', 'a', 'piece', 'of', 'cake'],
            ARRAY['It', 'was', 'a', 'piece', 'of', 'cake'],
            '[]' FORMAT JSON, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """,
        difficultyLevel,
        firstAxisEmbeddingLiteral());
  }

  // Fake 클라이언트의 쿼리 벡터([1,0,...])와 코사인 거리 0이 되는 1,536차원 벡터 문자열을 만든다.
  private static String firstAxisEmbeddingLiteral() {
    StringBuilder literal = new StringBuilder("[1");
    for (int index = 1; index < 1536; index++) {
      literal.append(",0");
    }
    return literal.append("]").toString();
  }

  private FreeTalkExpressionLink seedNewExpressionForCompletedSession(long learningSessionId)
      throws Exception {
    long freeTalkSessionId = completeSession(learningSessionId);
    long expressionId = 994104L;
    jdbcTemplate.update(
        """
        INSERT INTO writing_expression (
            id, scenario_id, expression_source, expression_type,
            usage_frequency_level, difficulty_level, target_locale, base_locale, display_order, target_expression_text,
            base_expression_meaning_text, usage_summary, usage_description,
            representative_question_text, representative_question_translation,
            representative_sentence_text, representative_sentence_translation,
            representative_sentence_words, representative_sentence_word_choices,
            representative_image_url, practice_examples_payload, status, created_at, updated_at
        )
        VALUES (
            ?, NULL, 'FREE_TALK', 'CONVERSATION_SKILL', 'BASIC', 3, 'EN', 'KR', 1, 'hit it off',
            '죽이 잘 맞다', '처음 만난 사람과 잘 통할 때 사용한다.',
            '서로 대화가 잘 통하고 금방 친해졌을 때 사용하는 표현이다.',
            'How was meeting your new teammate?', '새 팀원을 만나 보니 어땠어?',
            'We really hit it off.', '우리는 정말 죽이 잘 맞았어.',
            ARRAY['We', 'really', 'hit', 'it', 'off', '.'],
            ARRAY['hit', 'We', 'miss', 'off', 'it', 'really', '.'],
            NULL, ? FORMAT JSON, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """,
        expressionId,
        practiceExamples(null).toString());
    freeTalkSessionExpressionRepository.saveAndFlush(
        FreeTalkSessionExpression.link(freeTalkSessionId, expressionId, 1));
    return new FreeTalkExpressionLink(learningSessionId, freeTalkSessionId, expressionId);
  }

  private void finishExpression(String accessToken, FreeTalkExpressionLink link) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/expressions/{expressionId}/learning-finish", link.expressionId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"freeTalkSessionId\":%d}".formatted(link.learningSessionId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  private void assertCurrentStreak(
      String accessToken, int expectedCurrentStreakDays, boolean expectedActiveToday)
      throws Exception {
    mockMvc
        .perform(
            get("/api/v1/me/streak").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.currentStreakDays").value(expectedCurrentStreakDays))
        .andExpect(jsonPath("$.data.activeToday").value(expectedActiveToday));
  }

  private record FreeTalkExpressionLink(
      long learningSessionId, long freeTalkSessionId, long expressionId) {}

  private long completeSession(long learningSessionId) {
    jdbcTemplate.update(
        """
        UPDATE learning_session
        SET status = 'COMPLETED', ended_at = CURRENT_TIMESTAMP,
            ended_by = 'USER', completion_reason = 'USER_ENDED'
        WHERE id = ?
        """,
        learningSessionId);
    jdbcTemplate.update(
        """
        UPDATE free_talk_session
        SET conversation_status = 'COMPLETED', expression_generation_status = 'READY'
        WHERE learning_session_id = ?
        """,
        learningSessionId);
    long freeTalkSessionId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM free_talk_session WHERE learning_session_id = ?",
            Long.class,
            learningSessionId);
    return freeTalkSessionId;
  }

  private JsonNode practiceExamples(String imageUrl) throws Exception {
    String imageProperty = imageUrl == null ? "" : ",\"imageUrl\":\"" + imageUrl + "\"";
    String example =
        """
        {
          "sentenceText": "They hit it off right away.",
          "sentenceWords": ["They", "hit", "it", "off", "right", "away", "."],
          "highlightingPart": "hit it off",
          "practiceQuestion": "How did the introduction go?",
          "sentenceTranslation": "그들은 바로 죽이 잘 맞았어.",
          "sentenceWordChoices": ["hit", "They", "miss", "it", "off", "right", "away", "."],
          "sentenceTranslateWords": ["그들은", "바로", "죽이", "잘", "맞았어"],
          "sentenceTranslateWordChoices": ["맞았어", "그들은", "안", "죽이", "바로", "잘"],
          "practiceQuestionTranslation": "소개는 어땠어?"%s
        }
        """
            .formatted(imageProperty);
    return objectMapper.readTree("[" + String.join(",", Collections.nCopies(4, example)) + "]");
  }

  private JsonNode practiceExamplesWithInvalidEntry() throws Exception {
    return objectMapper.readTree(
        """
        [
          {
            "sentenceText": "Valid sentence.",
            "sentenceWords": ["Valid", "sentence", "."],
            "highlightingPart": "Valid",
            "practiceQuestion": "Is this valid?",
            "sentenceTranslation": "정상 문장.",
            "sentenceWordChoices": ["sentence", "Valid", "."],
            "sentenceTranslateWords": ["정상", "문장"],
            "sentenceTranslateWordChoices": ["문장", "오답", "정상"],
            "practiceQuestionTranslation": "이 문장은 정상이야?"
          },
          {
            "sentenceText": "Missing words.",
            "highlightingPart": "Missing",
            "practiceQuestion": "Are words missing?",
            "sentenceTranslation": "단어 배열이 빠진 문장.",
            "sentenceWordChoices": ["Missing", "words", "."],
            "practiceQuestionTranslation": "단어 배열이 빠졌어?"
          }
        ]
        """);
  }

  private long seedWritingExpression() throws Exception {
    jdbcTemplate.update(
        """
        INSERT INTO category (id, display_order, status, created_at, updated_at)
        VALUES (994101, 994101, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """);
    jdbcTemplate.update(
        """
        INSERT INTO scenario (
            id, category_id, ai_role, difficulty, first_speaker, total_question_count,
            display_order, status, created_at, updated_at
        )
        VALUES (
            994102, 994101, 'friend', 'NORMAL', 'AI', 1,
            994102, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """);
    jdbcTemplate.update(
        """
        INSERT INTO writing_expression (
            id, scenario_id, expression_type, usage_frequency_level, difficulty_level, target_locale, base_locale,
            display_order, target_expression_text, base_expression_meaning_text, usage_summary,
            usage_description, representative_question_text, representative_question_translation,
            representative_sentence_text, representative_sentence_translation,
            representative_sentence_words, representative_sentence_word_choices,
            representative_image_url, practice_examples_payload, status, created_at, updated_at
        )
        VALUES (
            994103, 994102, 'DAILY_ROUTINE', 'BASIC', 3, 'EN', 'KR', 1,
            'make up for', '만회하다', '부족했던 부분을 보완한다.',
            '부족하거나 잘못된 일을 다른 행동으로 보완할 때 쓴다.',
            'How will you fix it?', '어떻게 만회할 거야?',
            'I will make up for it.', '내가 만회할게.',
            ARRAY['I', 'will', 'make', 'up', 'for', 'it', '.'],
            ARRAY['make', 'I', 'it', 'up', 'for', 'will', '.'],
            'https://cdn/representative.png',
            ? FORMAT JSON, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        """,
        practiceExamples("https://cdn/practice.png").toString());
    return 994103L;
  }

  // 온보딩에서 고르는 학습 수준을 실제 API로 설정한다.
  private void updateLearningLevel(String accessToken, int learningLevel) throws Exception {
    mockMvc
        .perform(
            put("/api/v1/me/learning-level")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"learningLevel\":%d}".formatted(learningLevel)))
        .andExpect(status().isOk());
  }

  private JsonNode login(String email) throws Exception {
    String nonce = UUID.randomUUID().toString();
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/social-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"provider":"GOOGLE","idToken":"%s|%s|Free Talk User|%s","nonce":"%s"}
                        """
                            .formatted(UUID.randomUUID(), email, nonce, nonce)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsByteArray());
  }

  private void seedTopic(
      long id, String displayName, String promptDescription, int displayOrder, String status) {
    jdbcTemplate.update(
        """
        INSERT INTO free_talk_topic (
            id, display_name, prompt_description, display_order, status, created_at, updated_at
        )
        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        id,
        displayName,
        promptDescription,
        displayOrder,
        status);
  }

  @TestConfiguration
  static class FakeAiFreeTalkClientConfiguration {

    @Bean
    @Primary
    FakeAiFreeTalkClient fakeAiFreeTalkClient() {
      return new FakeAiFreeTalkClient();
    }
  }

  static class FakeAiFreeTalkClient implements AiFreeTalkClient {

    private AiFreeTalkOpeningRequest lastOpeningRequest;
    private boolean openingTransactionActive;
    private boolean failOpening;
    private volatile boolean turnTransactionActive;
    private volatile boolean failTurn;
    private volatile boolean exitIntentDetected;
    private final AtomicInteger turnCallCount = new AtomicInteger();
    private volatile CountDownLatch turnStarted = new CountDownLatch(1);
    private volatile CountDownLatch turnRelease = new CountDownLatch(0);
    private volatile boolean omitClosingTitle;

    @Override
    public AiFreeTalkOpeningResult generateOpening(AiFreeTalkOpeningRequest request) {
      lastOpeningRequest = request;
      openingTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
      if (failOpening) {
        throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
      }
      return new AiFreeTalkOpeningResult(
          "What are your weekend plans?", "이번 주말 계획은 뭐야?", CharacterEmotion.HAPPY, List.of());
    }

    @Override
    public AiFreeTalkTurnResult generateTurn(AiFreeTalkTurnRequest request) {
      turnTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
      turnCallCount.incrementAndGet();
      turnStarted.countDown();
      try {
        if (!turnRelease.await(5, TimeUnit.SECONDS)) {
          throw new IllegalStateException("AI 턴 대기가 시간 초과되었습니다.");
        }
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(exception);
      }
      if (failTurn) {
        throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
      }
      if (exitIntentDetected && request.responseMode().name().equals("NORMAL")) {
        return new AiFreeTalkTurnResult(true, null, null, null, null, List.of());
      }
      return new AiFreeTalkTurnResult(
          false,
          request.isFirstUserTurn() ? "Hiking with friends" : null,
          "That sounds fun! Where are you going next?",
          "재밌겠다! 다음에는 어디로 갈 거야?",
          CharacterEmotion.HAPPY,
          List.of());
    }

    @Override
    public AiMemoryQueryEmbeddingResult embedMemoryQuery(AiMemoryQueryEmbeddingRequest request) {
      List<Float> embedding = new ArrayList<>(Collections.nCopies(1536, 0.0f));
      embedding.set(0, 1.0f);
      return new AiMemoryQueryEmbeddingResult("openai/text-embedding-3-small", embedding);
    }

    @Override
    public AiFreeTalkInnerThoughtResult generateInnerThought(
        AiFreeTalkInnerThoughtRequest request) {
      return new AiFreeTalkInnerThoughtResult(
          "즐거운 시간을 보냈나 봐.", com.landit.landitbe.shared.domain.InnerThoughtType.GOOD);
    }

    @Override
    public AiFreeTalkClosingResult generateClosing(AiFreeTalkClosingRequest request) {
      return new AiFreeTalkClosingResult(
          request.titleGenerationRequired() && !omitClosingTitle ? "Weekend Hiking" : null,
          "It was great talking with you!",
          "이야기해서 즐거웠어!",
          CharacterEmotion.HAPPY);
    }

    @Override
    public AiFreeTalkExpressionRecommendationsResult recommendExpressions(
        AiFreeTalkExpressionRecommendationsRequest request) {
      if (request.existingExpressions().isEmpty()) {
        return new AiFreeTalkExpressionRecommendationsResult(List.of());
      }
      return new AiFreeTalkExpressionRecommendationsResult(
          List.of(
              new AiFreeTalkExpressionRecommendation(
                  1, request.existingExpressions().getFirst().expressionId())));
    }

    @Override
    public AiConversationEmbeddingsResult extractConversationEmbeddings(
        AiConversationEmbeddingsRequest request) {
      // 첫 성분만 1인 고정 벡터로 유사도 검색 결과를 예측할 수 있게 한다.
      List<Float> embedding =
          new java.util.ArrayList<>(
              Collections.nCopies(AiConversationExcerpt.EMBEDDING_DIMENSION, 0.0f));
      embedding.set(0, 1.0f);
      return new AiConversationEmbeddingsResult(
          List.of(new AiConversationExcerpt("That sounds interesting.", List.copyOf(embedding))));
    }

    @Override
    public AiMemoryCandidatesResult extractMemoryCandidates(AiMemoryCandidatesRequest request) {
      return new AiMemoryCandidatesResult("memory-candidate-v1", List.of());
    }

    @Override
    public AiMemoryResolutionResult resolveMemory(AiMemoryResolutionRequest request) {
      return new AiMemoryResolutionResult(
          request.candidates().stream()
              .map(
                  candidate ->
                      new AiMemoryResolutionResult.Resolution(
                          candidate.candidateIndex(), AiMemoryOperation.ADD, List.of()))
              .toList());
    }

    void reset() {
      lastOpeningRequest = null;
      openingTransactionActive = false;
      failOpening = false;
      turnTransactionActive = false;
      failTurn = false;
      exitIntentDetected = false;
      turnCallCount.set(0);
      turnStarted = new CountDownLatch(1);
      turnRelease = new CountDownLatch(0);
      omitClosingTitle = false;
    }

    void omitClosingTitle() {
      omitClosingTitle = true;
    }

    void failOpening() {
      failOpening = true;
    }

    void failTurn() {
      failTurn = true;
    }

    void detectExitIntent() {
      exitIntentDetected = true;
    }

    void blockTurn() {
      turnRelease = new CountDownLatch(1);
    }

    boolean awaitTurnStarted() throws InterruptedException {
      return turnStarted.await(5, TimeUnit.SECONDS);
    }

    void releaseTurn() {
      turnRelease.countDown();
    }

    AiFreeTalkOpeningRequest lastOpeningRequest() {
      return lastOpeningRequest;
    }

    boolean openingTransactionActive() {
      return openingTransactionActive;
    }

    boolean turnTransactionActive() {
      return turnTransactionActive;
    }

    int turnCallCount() {
      return turnCallCount.get();
    }
  }
}
