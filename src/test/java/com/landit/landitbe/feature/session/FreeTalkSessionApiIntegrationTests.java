// 프리톡 주제 조회와 세션 시작 API의 외부 계약과 저장 경계를 검증한다.

package com.landit.landitbe.feature.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClient;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionLearningContentRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionLearningContentResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionRecommendationsRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkExpressionRecommendationsResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnResult;
import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.feature.session.domain.FreeTalkExpression;
import com.landit.landitbe.feature.session.domain.FreeTalkSessionExpression;
import com.landit.landitbe.feature.session.repository.FreeTalkExpressionRepository;
import com.landit.landitbe.feature.session.repository.FreeTalkSessionExpressionRepository;
import com.landit.landitbe.shared.domain.Locale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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

  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private FakeAiFreeTalkClient fakeAiFreeTalkClient;

  @Autowired private FreeTalkExpressionRepository freeTalkExpressionRepository;

  @Autowired private FreeTalkSessionExpressionRepository freeTalkSessionExpressionRepository;

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
    jdbcTemplate.update("DELETE FROM user_free_talk_expression_completion");
    jdbcTemplate.update("DELETE FROM free_talk_session_expression");
    jdbcTemplate.update("DELETE FROM free_talk_expression");
    jdbcTemplate.update("DELETE FROM free_talk_session");
    jdbcTemplate.update("DELETE FROM session_history_message");
    jdbcTemplate.update("DELETE FROM session_history");
    jdbcTemplate.update("DELETE FROM learning_session");
    jdbcTemplate.update("DELETE FROM free_talk_topic");
    jdbcTemplate.update("DELETE FROM writing_expression WHERE id = 994103");
    jdbcTemplate.update("DELETE FROM scenario WHERE id = 994102");
    jdbcTemplate.update("DELETE FROM category WHERE id = 994101");
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
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].topicId").value(1001))
        .andExpect(jsonPath("$.data[0].displayName").value("첫 번째"))
        .andExpect(jsonPath("$.data[0].displayOrder").value(1))
        .andExpect(jsonPath("$.data[1].topicId").value(1002));
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
  void startAiFirstSessionPersistsOpeningAndUsesHarperContextWithoutTransaction() throws Exception {
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
                    .content("{\"startMode\":\"AI_FIRST\",\"topicId\":1101}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.sessionType").value("FREE_TALK"))
            .andExpect(jsonPath("$.data.startMode").value("AI_FIRST"))
            .andExpect(jsonPath("$.data.title").value("주말 계획"))
            .andExpect(jsonPath("$.data.speakingTimeLimitMs").value(180000))
            .andExpect(jsonPath("$.data.ttsVoice.provider").value("OPENROUTER"))
            .andExpect(
                jsonPath("$.data.ttsVoice.providerVoiceId").value("en-US-Harper:MAI-Voice-2"))
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
    assertThat(fakeAiFreeTalkClient.lastOpeningRequest().partnerDisplayName()).isEqualTo("Harper");
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
                    .content("{\"startMode\":\"USER_FIRST\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.startMode").value("USER_FIRST"))
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
  void rejectsInvalidStartModeAndTopicCombinations() throws Exception {
    seedTopic(1201, "오늘", "오늘의 일을 묻는다.", 1, "ACTIVE");
    String accessToken =
        login("free-talk-invalid-request@example.com").get("data").get("accessToken").asText();

    mockMvc
        .perform(
            post("/api/v1/free-talk/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startMode\":\"AI_FIRST\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    mockMvc
        .perform(
            post("/api/v1/free-talk/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startMode\":\"USER_FIRST\",\"topicId\":1201}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
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
                .content("{\"startMode\":\"AI_FIRST\",\"topicId\":1251}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    mockMvc
        .perform(
            post("/api/v1/free-talk/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startMode\":\"AI_FIRST\",\"topicId\":1252}"))
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
                .content("{\"startMode\":\"AI_FIRST\",\"topicId\":1301}"))
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
  void openApiDocumentsTopicAndSessionStartContracts() throws Exception {
    String topicsPath = "$.paths['/api/v1/free-talk/topics'].get";
    String sessionsPath = "$.paths['/api/v1/free-talk/sessions'].post";
    String expressionPath =
        "$.paths['/api/v1/free-talk/expressions/{sessionExpressionId}/learning-start'].get";

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
        .andExpect(jsonPath(expressionPath + ".security[0].bearerAuth").exists())
        .andExpect(
            jsonPath("$.components.schemas.FreeTalkExpressionLearningResponse.properties.completed")
                .exists());
  }

  @Test
  void submitsNormalUserFirstTurnAndRejectsDuplicateClientMessageId() throws Exception {
    String accessToken =
        login("free-talk-message@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    String clientMessageId = UUID.randomUUID().toString();
    String request = messageRequest(clientMessageId, "I went hiking with friends.", 4200, false);

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Hiking with friends"))
        .andExpect(jsonPath("$.data.turnStatus").value("CONTINUE"))
        .andExpect(jsonPath("$.data.submittedMessage.role").value("USER"))
        .andExpect(jsonPath("$.data.submittedMessage.innerThought").value("즐거운 시간을 보냈나 봐."))
        .andExpect(jsonPath("$.data.nextMessage.role").value("AI"))
        .andExpect(jsonPath("$.data.progress.accumulatedSpeakingDurationMs").value(4200))
        .andExpect(jsonPath("$.data.progress.sessionStatus").value("IN_PROGRESS"));

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("CONFLICT"));

    assertThat(fakeAiFreeTalkClient.turnTransactionActive()).isFalse();
    assertThat(fakeAiFreeTalkClient.turnCallCount()).isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_history_message", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void exitDetectionStoresOnlyTheUserMessageAndRejectsRepeatedDecision() throws Exception {
    String accessToken =
        login("free-talk-exit@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    fakeAiFreeTalkClient.detectExitIntent();
    MvcResult detected =
        mockMvc
            .perform(
                post(messagePath(sessionId))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        messageRequest(
                            UUID.randomUUID().toString(), "I should go now.", 1200, false)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.turnStatus").value("EXIT_CONFIRMATION_REQUIRED"))
            .andExpect(jsonPath("$.data.nextMessage").value(nullValue()))
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
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("CONFLICT"));

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
  void endDecisionAndTimeLimitCompleteTheSessionWithPreparingStatus() throws Exception {
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
        .andExpect(jsonPath("$.data.progress.sessionStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.data.progress.expressionGenerationStatus").value("PREPARING"));

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
        .andExpect(jsonPath("$.data.progress.accumulatedSpeakingDurationMs").value(180000))
        .andExpect(jsonPath("$.data.progress.expressionGenerationStatus").value("PREPARING"));
  }

  @Test
  void learnsAndCompletesNewFreeTalkExpressionWithoutImages() throws Exception {
    JsonNode loginBody = login("free-talk-new-expression@example.com");
    final long userId = loginBody.at("/data/user/userId").asLong();
    String accessToken = loginBody.at("/data/accessToken").asText();
    long learningSessionId = startUserFirstSession(accessToken);
    long sessionExpressionId = seedNewExpressionForCompletedSession(learningSessionId);
    String otherToken =
        login("free-talk-expression-other@example.com").at("/data/accessToken").asText();

    mockMvc
        .perform(
            get(
                    "/api/v1/free-talk/expressions/{sessionExpressionId}/learning-start",
                    sessionExpressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

    mockMvc
        .perform(
            get(
                    "/api/v1/free-talk/expressions/{sessionExpressionId}/learning-start",
                    sessionExpressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessionExpressionId").value(sessionExpressionId))
        .andExpect(jsonPath("$.data.targetExpressionText").value("hit it off"))
        .andExpect(jsonPath("$.data.representativeSentenceWords.length()").value(6))
        .andExpect(jsonPath("$.data.representativeImageUrl").value(nullValue()))
        .andExpect(jsonPath("$.data.completed").value(false));

    mockMvc
        .perform(
            get("/api/v1/free-talk/expressions/{sessionExpressionId}/practice", sessionExpressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.practiceSentence.length()").value(4))
        .andExpect(jsonPath("$.data.practiceSentence[0].imageUrl").value(nullValue()))
        .andExpect(jsonPath("$.data.writingSentence.writingSentenceWords").isArray());

    for (int attempt = 0; attempt < 2; attempt++) {
      mockMvc
          .perform(
              post(
                      "/api/v1/free-talk/expressions/{sessionExpressionId}/learning-finish",
                      sessionExpressionId)
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data").isEmpty());
    }

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM user_free_talk_expression_completion
                WHERE user_profile_id = ?
                """,
                Integer.class,
                userId))
        .isEqualTo(1);

    mockMvc
        .perform(
            get(
                    "/api/v1/free-talk/expressions/{sessionExpressionId}/learning-start",
                    sessionExpressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.completed").value(true));
  }

  @Test
  void newFreeTalkPracticeExcludesInvalidExamples() throws Exception {
    JsonNode loginBody = login("free-talk-invalid-practice@example.com");
    String accessToken = loginBody.at("/data/accessToken").asText();
    long learningSessionId = startUserFirstSession(accessToken);
    long sessionExpressionId =
        seedNewExpressionForCompletedSession(learningSessionId, practiceExamplesWithInvalidEntry());

    mockMvc
        .perform(
            get("/api/v1/free-talk/expressions/{sessionExpressionId}/practice", sessionExpressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.practiceSentence.length()").value(1))
        .andExpect(jsonPath("$.data.practiceSentence[0].sentenceText").value("Valid sentence."));
  }

  @Test
  void reusesWritingExpressionLearningContentForExistingExpression() throws Exception {
    JsonNode loginBody = login("free-talk-existing-expression@example.com");
    String accessToken = loginBody.at("/data/accessToken").asText();
    long learningSessionId = startUserFirstSession(accessToken);
    long sessionExpressionId = seedExistingExpressionForCompletedSession(learningSessionId);

    mockMvc
        .perform(
            get(
                    "/api/v1/free-talk/expressions/{sessionExpressionId}/learning-start",
                    sessionExpressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.targetExpressionText").value("make up for"))
        .andExpect(
            jsonPath("$.data.representativeImageUrl").value("https://cdn/representative.png"))
        .andExpect(jsonPath("$.data.completed").value(false));

    mockMvc
        .perform(
            get("/api/v1/free-talk/expressions/{sessionExpressionId}/practice", sessionExpressionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.practiceSentence.length()").value(4))
        .andExpect(
            jsonPath("$.data.practiceSentence[0].imageUrl").value("https://cdn/practice.png"));
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
  void compensatesUserMessageAndMarkerWhenAiTurnFails() throws Exception {
    String accessToken =
        login("free-talk-compensation@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);
    fakeAiFreeTalkClient.failTurn();

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    messageRequest(UUID.randomUUID().toString(), "This should fail.", 700, false)))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error.code").value("AI_GENERATION_FAILED"));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_history_message", Integer.class))
        .isZero();
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

  private long startUserFirstSession(String accessToken) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/free-talk/sessions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"startMode\":\"USER_FIRST\"}"))
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

  private String messageRequest(
      String clientMessageId, String content, long utteranceDurationMs, boolean timeLimitReached) {
    return ("{\"clientMessageId\":\"%s\",\"content\":\"%s\",\"inputType\":\"VOICE\","
            + "\"utteranceDurationMs\":%d,\"timeLimitReached\":%s}")
        .formatted(clientMessageId, content, utteranceDurationMs, timeLimitReached);
  }

  private long seedNewExpressionForCompletedSession(long learningSessionId) throws Exception {
    return seedNewExpressionForCompletedSession(learningSessionId, practiceExamples(null));
  }

  private long seedNewExpressionForCompletedSession(
      long learningSessionId, JsonNode practiceExamples) throws Exception {
    FreeTalkExpression expression =
        freeTalkExpressionRepository.saveAndFlush(
            FreeTalkExpression.newExpression(
                Locale.EN,
                Locale.KR,
                "hit it off",
                "죽이 잘 맞다",
                "처음 만난 사람과 잘 통할 때 사용한다.",
                "서로 대화가 잘 통하고 금방 친해졌을 때 사용하는 표현이다.",
                "How was meeting your new teammate?",
                "새 팀원을 만나 보니 어땠어?",
                "We really hit it off.",
                "우리는 정말 죽이 잘 맞았어.",
                objectMapper.readTree("[\"We\", \"really\", \"hit\", \"it\", \"off\", \".\"]"),
                objectMapper.readTree(
                    "[\"hit\", \"We\", \"miss\", \"off\", \"it\", \"really\", \".\"]"),
                null,
                practiceExamples));
    return connectExpressionToCompletedSession(learningSessionId, expression.getId());
  }

  private long seedExistingExpressionForCompletedSession(long learningSessionId) throws Exception {
    long writingExpressionId = seedWritingExpression();
    FreeTalkExpression expression =
        freeTalkExpressionRepository.saveAndFlush(
            FreeTalkExpression.existingExpression(
                writingExpressionId,
                Locale.EN,
                Locale.KR,
                "make up for",
                "만회하다",
                "부족했던 부분을 보완할 때 사용한다."));
    return connectExpressionToCompletedSession(learningSessionId, expression.getId());
  }

  private long connectExpressionToCompletedSession(
      long learningSessionId, long freeTalkExpressionId) {
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
    return freeTalkSessionExpressionRepository
        .saveAndFlush(
            FreeTalkSessionExpression.create(
                freeTalkSessionId,
                freeTalkExpressionId,
                1,
                "We could have really hit it off.",
                "우리는 정말 죽이 잘 맞을 수도 있었어요."))
        .getId();
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
            id, scenario_id, expression_type, usage_frequency_level, target_locale, base_locale,
            display_order, target_expression_text, base_expression_meaning_text, usage_summary,
            usage_description, representative_question_text, representative_question_translation,
            representative_sentence_text, representative_sentence_translation,
            representative_sentence_words, representative_sentence_word_choices,
            representative_image_url, practice_examples_payload, status, created_at, updated_at
        )
        VALUES (
            994103, 994102, 'DAILY_ROUTINE', 'BASIC', 'EN', 'KR', 1,
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
    private boolean turnTransactionActive;
    private boolean failTurn;
    private boolean exitIntentDetected;
    private int turnCallCount;
    private CountDownLatch turnStarted = new CountDownLatch(1);
    private CountDownLatch turnRelease = new CountDownLatch(0);

    @Override
    public AiFreeTalkOpeningResult generateOpening(AiFreeTalkOpeningRequest request) {
      lastOpeningRequest = request;
      openingTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
      if (failOpening) {
        throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
      }
      return new AiFreeTalkOpeningResult(
          "What are your weekend plans?", "이번 주말 계획은 뭐야?", CharacterEmotion.HAPPY);
    }

    @Override
    public AiFreeTalkTurnResult generateTurn(AiFreeTalkTurnRequest request) {
      turnTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
      turnCallCount++;
      turnStarted.countDown();
      try {
        turnRelease.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(exception);
      }
      if (failTurn) {
        throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
      }
      if (exitIntentDetected && request.responseMode().name().equals("NORMAL")) {
        return new AiFreeTalkTurnResult(true, null, null, null, null, null, null);
      }
      return new AiFreeTalkTurnResult(
          false,
          request.isFirstUserTurn() ? "Hiking with friends" : null,
          "That sounds fun! Where are you going next?",
          "재밌겠다! 다음에는 어디로 갈 거야?",
          CharacterEmotion.HAPPY,
          "즐거운 시간을 보냈나 봐.",
          com.landit.landitbe.shared.domain.InnerThoughtType.GOOD);
    }

    @Override
    public AiFreeTalkClosingResult generateClosing(AiFreeTalkClosingRequest request) {
      return new AiFreeTalkClosingResult(
          "It was great talking with you!",
          "이야기해서 즐거웠어!",
          CharacterEmotion.HAPPY,
          "즐거운 시간을 보냈나 봐.",
          com.landit.landitbe.shared.domain.InnerThoughtType.GOOD);
    }

    @Override
    public AiFreeTalkExpressionRecommendationsResult recommendExpressions(
        AiFreeTalkExpressionRecommendationsRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AiFreeTalkExpressionLearningContentResult generateExpressionLearningContent(
        AiFreeTalkExpressionLearningContentRequest request) {
      throw new UnsupportedOperationException();
    }

    void reset() {
      lastOpeningRequest = null;
      openingTransactionActive = false;
      failOpening = false;
      turnTransactionActive = false;
      failTurn = false;
      exitIntentDetected = false;
      turnCallCount = 0;
      turnStarted = new CountDownLatch(1);
      turnRelease = new CountDownLatch(0);
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
      return turnCallCount;
    }
  }
}
