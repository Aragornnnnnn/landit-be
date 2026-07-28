// 프리톡 주제 조회, 세션 시작, 발화 제출과 종료 결정 API의 외부 계약과 저장 경계를 검증한다.

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
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnRequest;
import com.landit.landitbe.feature.session.client.ai.AiFreeTalkTurnResult;
import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
    jdbcTemplate.update("DELETE FROM free_talk_session");
    jdbcTemplate.update("DELETE FROM session_history_message");
    jdbcTemplate.update("DELETE FROM session_history");
    jdbcTemplate.update("DELETE FROM learning_session");
    jdbcTemplate.update("DELETE FROM free_talk_topic");
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
        .andExpect(jsonPath(sessionsPath + ".responses['503'].description").value("AI 생성 실패"));
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
  void endDecisionAndTimeLimitCompleteTheSession() throws Exception {
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

  @Test
  void completesWhenServerCalculatedSpeakingTimeReachesLimit() throws Exception {
    String accessToken =
        login("free-talk-server-time-limit@example.com").get("data").get("accessToken").asText();
    long sessionId = startUserFirstSession(accessToken);

    mockMvc
        .perform(
            post(messagePath(sessionId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    messageRequest(UUID.randomUUID().toString(), "One last thing.", 180000, false)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.turnStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.data.progress.accumulatedSpeakingDurationMs").value(180000));
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
