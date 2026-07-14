// 시나리오 세션 시작과 중도 종료 API의 응답과 저장 상태를 검증한다.
package com.landit.landitbe.session;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.common.domain.InnerThoughtType;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.application.port.AiClosingMessageRequest;
import com.landit.landitbe.session.application.port.AiClosingMessageResult;
import com.landit.landitbe.session.application.port.AiConversationClient;
import com.landit.landitbe.session.application.port.AiMessageFeedbackRequest;
import com.landit.landitbe.session.application.port.AiMessageFeedbackResult;
import com.landit.landitbe.session.application.port.AiNextMessageRequest;
import com.landit.landitbe.session.application.port.AiNextMessageResult;
import com.landit.landitbe.session.application.port.AiSessionFeedbackRequest;
import com.landit.landitbe.session.application.port.AiSessionFeedbackResult;
import com.landit.landitbe.session.application.port.AiSessionMessageFeedbackResult;
import com.landit.landitbe.session.domain.FeedbackType;
import com.landit.landitbe.session.domain.GoalCompletionStatus;
import com.landit.landitbe.session.domain.ProcessingStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
@TestPropertySource(properties = {
        "landit.auth.oidc.fake-enabled=true",
        "landit.auth.token.secret=landit-test-token-secret-that-is-long-enough"
})
class ScenarioSessionApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeAiConversationClient fakeAiConversationClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        fakeAiConversationClient.reset();
        jdbcTemplate.update("DELETE FROM session_history_message_feedback");
        jdbcTemplate.update("DELETE FROM session_history_summary_feedback");
        jdbcTemplate.update("DELETE FROM session_history_artifact");
        jdbcTemplate.update("DELETE FROM session_history_message");
        jdbcTemplate.update("DELETE FROM scenario_session");
        jdbcTemplate.update("DELETE FROM session_history");
        jdbcTemplate.update("DELETE FROM learning_session");
        jdbcTemplate.update("DELETE FROM user_scenario_progress");
        jdbcTemplate.update("DELETE FROM scenario_question_language_variant");
        jdbcTemplate.update("DELETE FROM scenario_question");
        jdbcTemplate.update("DELETE FROM scenario_language_variant");
        jdbcTemplate.update("DELETE FROM scenario");
        jdbcTemplate.update("DELETE FROM category_language_variant");
        jdbcTemplate.update("DELETE FROM category");
    }

    @Test
    void startAiFirstScenarioCreatesSessionProgressOpeningMessageAndResponse() throws Exception {
        JsonNode loginBody = login("ai-first@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1001, 1, "ACTIVE", "음식");
        seedScenario(2001, 1001, 1, "AI", "ACTIVE", 4);
        seedScenarioQuestion(
                4001,
                2001,
                1,
                "What food do you like? Why do you like it?",
                "좋아하는 음식이 있어? 왜 좋아해?",
                "질문 1번의 속마음",
                "GOOD"
        );
        seedScenarioVariant(
                3001,
                2001,
                "좋아하는 음식",
                "음식 취향을 말합니다.",
                "좋아하는 음식을 이유와 함께 말한다.",
                null,
                "Legacy opening message",
                "기존 시작 메시지",
                "음식 이야기는 처음 대화를 열기 좋다.",
                "GOOD",
                ttsVoiceId("en-US-Harper:MAI-Voice-2"),
                "ACTIVE"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/2001/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.sessionId").value(notNullValue()))
                .andExpect(jsonPath("$.data.scenarioId").value(2001))
                .andExpect(jsonPath("$.data.sessionType").value("SCENARIO"))
                .andExpect(jsonPath("$.data.firstSpeaker").value("AI"))
                .andExpect(jsonPath("$.data.userOpeningInstruction").value(nullValue()))
                .andExpect(jsonPath("$.data.ttsVoice.provider").value("OPENROUTER"))
                .andExpect(jsonPath("$.data.ttsVoice.model").value("microsoft/mai-voice-2"))
                .andExpect(jsonPath("$.data.ttsVoice.providerVoiceId")
                        .value("en-US-Harper:MAI-Voice-2"))
                .andExpect(jsonPath("$.data.ttsVoice.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.currentMessage.messageId").value(notNullValue()))
                .andExpect(jsonPath("$.data.currentMessage.turnNumber").value(1))
                .andExpect(jsonPath("$.data.currentMessage.messageSequence").value(1))
                .andExpect(jsonPath("$.data.currentMessage.role").value("AI"))
                .andExpect(jsonPath("$.data.currentMessage.content")
                        .value("What food do you like? Why do you like it?"))
                .andExpect(jsonPath("$.data.currentMessage.translatedContent")
                        .value("좋아하는 음식이 있어? 왜 좋아해?"))
                .andExpect(jsonPath("$.data.currentMessage.innerThought")
                        .value("질문 1번의 속마음"))
                .andExpect(jsonPath("$.data.currentMessage.innerThoughtType").value("GOOD"))
                .andExpect(jsonPath("$.data.progress.currentTurnNumber").value(1))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(4))
                .andExpect(jsonPath("$.data.progress.completed").value(false))
                .andReturn();

        long sessionId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("data")
                .get("sessionId")
                .asLong();
        assertLearningSession(sessionId, userId, "IN_PROGRESS", null, null);
        assertScenarioSession(sessionId, 3001);
        assertProgress(userId, 2001, "IN_PROGRESS");
        assertHistoryMessage(sessionId, "AI", "What food do you like? Why do you like it?");
    }

    @Test
    void startAiFirstScenarioWithoutFirstFixedQuestionReturnsInternalServerError() throws Exception {
        JsonNode loginBody = login("ai-first-without-question@example.com");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1002, 1, "ACTIVE", "음식");
        seedScenario(2002, 1002, 1, "AI", "ACTIVE", 3);
        seedScenarioVariant(
                3002,
                2002,
                "좋아하는 음식",
                "음식 취향을 말합니다.",
                "좋아하는 음식을 이유와 함께 말한다.",
                null,
                "Legacy opening message",
                "기존 시작 메시지",
                null,
                null,
                null,
                "ACTIVE"
        );
        jdbcTemplate.update("DELETE FROM scenario_question_language_variant WHERE scenario_question_id = ?", 102002L);
        jdbcTemplate.update("DELETE FROM scenario_question WHERE id = ?", 102002L);

        mockMvc.perform(post("/api/v1/scenarios/2002/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void submitMessageSavesUserMessageGeneratesNextAiMessageAndReturnsProgress() throws Exception {
        JsonNode loginBody = login("message-submit@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1101, 1, "ACTIVE", "음식");
        seedScenario(2101, 1101, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3101,
                2101,
                "음식에 대한 대화하기",
                "좋아하는 음식과 최근에 먹은 음식에 대해 이야기합니다.",
                "내 취향과 경험을 영어로 설명해봅니다.",
                null,
                "What food do you like? Why do you like it?",
                "좋아하는 음식이 있어? 왜 좋아해?",
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(
                4102,
                2101,
                2,
                "What food did you eat recently?",
                "최근에는 어떤 음식을 먹었어?"
        );
        long sessionId = startScenario(accessToken, 2101);

        MvcResult result = mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"I like pizza because it is spicy.",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.submittedMessage.messageId").value(notNullValue()))
                .andExpect(jsonPath("$.data.submittedMessage.turnNumber").value(1))
                .andExpect(jsonPath("$.data.submittedMessage.messageSequence").value(2))
                .andExpect(jsonPath("$.data.submittedMessage.role").value("USER"))
                .andExpect(jsonPath("$.data.submittedMessage.feedbackProcessingStatus").value("PREPARING"))
                .andExpect(jsonPath("$.data.submittedMessage.innerThoughtProcessingStatus")
                        .value("PREPARING"))
                .andExpect(jsonPath("$.data.submittedMessage.innerThought").value(nullValue()))
                .andExpect(jsonPath("$.data.submittedMessage.innerThoughtType").value(nullValue()))
                .andExpect(jsonPath("$.data.nextMessage.messageId").value(notNullValue()))
                .andExpect(jsonPath("$.data.nextMessage.turnNumber").value(2))
                .andExpect(jsonPath("$.data.nextMessage.messageSequence").value(3))
                .andExpect(jsonPath("$.data.nextMessage.role").value("AI"))
                .andExpect(jsonPath("$.data.nextMessage.content")
                        .value("Oh, you like spicy pizza. What food did you eat recently?"))
                .andExpect(jsonPath("$.data.nextMessage.translatedContent")
                        .value("아, 매콤한 피자를 좋아하는구나. 최근에는 어떤 음식을 먹었어?"))
                .andExpect(jsonPath("$.data.progress.currentTurnNumber").value(2))
                .andExpect(jsonPath("$.data.progress.currentMessageSequenceNumber").value(2))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(2))
                .andExpect(jsonPath("$.data.progress.completed").value(false))
                .andReturn();

        long submittedMessageId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("data")
                .get("submittedMessage")
                .get("messageId")
                .asLong();
        assertThat(fakeAiConversationClient.lastNextMessageRequest()).isNotNull();
        assertThat(fakeAiConversationClient.lastNextMessageRequest().submittedMessageId())
                .isEqualTo(submittedMessageId);
        assertThat(fakeAiConversationClient.lastNextMessageRequest().submittedTurnNumber())
                .isEqualTo(1);
        assertThat(fakeAiConversationClient.lastNextMessageRequest().scenario().scenarioId())
                .isEqualTo(2101);
        assertThat(fakeAiConversationClient.lastNextMessageRequest().scenario().counterpartRole())
                .isEqualTo("tutor");
        assertThat(fakeAiConversationClient.lastNextMessageRequest().conversationHistory())
                .extracting("content")
                .containsExactly(
                        "What food do you like? Why do you like it?",
                        "I like pizza because it is spicy."
                );
        assertThat(fakeAiConversationClient.lastNextMessageRequest().nextQuestion().questionId())
                .isEqualTo(4102);
        assertThat(fakeAiConversationClient.nextMessageTransactionActive()).containsOnly(false);
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest()).isNotNull();
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().sessionId()).isEqualTo(sessionId);
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().messageId())
                .isEqualTo(submittedMessageId);
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().turnNumber()).isEqualTo(1);
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().messageSequence()).isEqualTo(2);
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().scenario().scenarioId())
                .isEqualTo(2101);
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().evaluationContext().type().name())
                .isEqualTo("AI_MESSAGE");
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().evaluationContext().content())
                .isEqualTo("What food do you like? Why do you like it?");
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().evaluationContext()
                .translatedContent()).isEqualTo("좋아하는 음식이 있어? 왜 좋아해?");
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().userMessage())
                .isEqualTo("I like pizza because it is spicy.");
        assertThat(fakeAiConversationClient.messageFeedbackTransactionActive()).containsOnly(false);

        List<Map<String, Object>> messages = jdbcTemplate.queryForList(
                """
                        SELECT shm.role,
                               shm.content,
                               shm.translated_content,
                               shm.input_type,
                               shm.inner_thought,
                               shm.inner_thought_type,
                               shm.inner_thought_processing_status,
                               shm.message_sequence,
                               shm.turn_number
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        ORDER BY shm.message_sequence ASC
                        """,
                sessionId
        );
        assertThat(messages).hasSize(3);
        assertThat(messages.get(1).get("ROLE")).isEqualTo("USER");
        assertThat(messages.get(1).get("INPUT_TYPE")).isEqualTo("VOICE");
        assertThat(messages.get(1).get("INNER_THOUGHT")).isNull();
        assertThat(messages.get(1).get("INNER_THOUGHT_PROCESSING_STATUS"))
                .isEqualTo("PREPARING");
        assertThat(messages.get(2).get("ROLE")).isEqualTo("AI");
        assertThat(messages.get(2).get("CONTENT"))
                .isEqualTo("Oh, you like spicy pizza. What food did you eat recently?");
    }

    @Test
    void submitUserFirstMessagesUseOpeningInstructionThenPrecedingAiMessage() throws Exception {
        JsonNode loginBody = login("user-first-submit@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1108, 1, "ACTIVE", "카페");
        seedScenario(2108, 1108, 1, "USER", "ACTIVE", 1);
        seedScenarioVariant(
                3108,
                2108,
                "카페 주문",
                "카페에서 음료를 주문합니다.",
                "원하는 음료를 주문합니다.",
                "점원에게 먼저 주문하고 싶은 음료를 말해보세요.",
                null,
                null,
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(
                4108,
                2108,
                1,
                "What size would you like?",
                "어떤 사이즈로 드릴까요?"
        );
        long sessionId = startScenario(accessToken, 2108);

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"I would like an iced americano.",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedMessage.turnNumber").value(1))
                .andExpect(jsonPath("$.data.submittedMessage.messageSequence").value(1))
                .andExpect(jsonPath("$.data.submittedMessage.feedbackProcessingStatus")
                        .value("PREPARING"))
                .andExpect(jsonPath("$.data.nextMessage.turnNumber").value(2))
                .andExpect(jsonPath("$.data.nextMessage.messageSequence").value(2))
                .andExpect(jsonPath("$.data.progress.currentTurnNumber").value(2))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(1))
                .andExpect(jsonPath("$.data.progress.completed").value(false));

        assertThat(fakeAiConversationClient.lastNextMessageRequest()).isNotNull();
        assertThat(fakeAiConversationClient.lastNextMessageRequest().conversationHistory())
                .extracting("content")
                .containsExactly("I would like an iced americano.");
        assertThat(fakeAiConversationClient.lastNextMessageRequest().nextQuestion().sequence())
                .isEqualTo(1);
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest()).isNotNull();
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().messageSequence()).isEqualTo(1);
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().evaluationContext().type().name())
                .isEqualTo("SCENARIO_OPENING_INSTRUCTION");
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().evaluationContext().content())
                .isEqualTo("점원에게 먼저 주문하고 싶은 음료를 말해보세요.");
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().evaluationContext()
                .translatedContent()).isNull();
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().userMessage())
                .isEqualTo("I would like an iced americano.");

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"A medium size, please.",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedMessage.turnNumber").value(2))
                .andExpect(jsonPath("$.data.submittedMessage.messageSequence").value(3))
                .andExpect(jsonPath("$.data.submittedMessage.feedbackProcessingStatus")
                        .value("PREPARING"));

        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().evaluationContext().type().name())
                .isEqualTo("AI_MESSAGE");
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().evaluationContext().content())
                .isEqualTo("Oh, you like spicy pizza. What food did you eat recently?");
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().userMessage())
                .isEqualTo("A medium size, please.");
    }

    @Test
    void userFirstScenarioReadsAllFixedQuestionsInContinuousOrder() throws Exception {
        JsonNode loginBody = login("user-first-continuous-order@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1130, 1, "ACTIVE", "카페");
        seedScenario(2130, 1130, 1, "USER", "ACTIVE", 4);
        seedScenarioVariant(
                3130,
                2130,
                "카페 주문",
                "카페에서 음료를 주문합니다.",
                "원하는 음료를 자연스럽게 주문합니다.",
                "점원에게 먼저 주문하고 싶은 음료를 말해보세요.",
                null,
                null,
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(4130, 2130, 1, "What would you like to order?", "무엇을 주문할까요?");
        seedScenarioQuestion(4131, 2130, 2, "What size would you like?", "어떤 사이즈로 드릴까요?");
        seedScenarioQuestion(4132, 2130, 3, "Would you like anything else?", "더 필요한 것은 없나요?");
        long sessionId = startScenario(accessToken, 2130);

        submitMessage(accessToken, sessionId, "I would like an iced americano.");
        assertThat(fakeAiConversationClient.lastNextMessageRequest().nextQuestion().sequence()).isEqualTo(1);

        submitMessage(accessToken, sessionId, "A medium size, please.");
        assertThat(fakeAiConversationClient.lastNextMessageRequest().nextQuestion().sequence()).isEqualTo(2);

        submitMessage(accessToken, sessionId, "That is all, thank you.");
        assertThat(fakeAiConversationClient.lastNextMessageRequest().nextQuestion().sequence()).isEqualTo(3);

        submitMessage(accessToken, sessionId, "No, thank you.");
        assertThat(fakeAiConversationClient.lastClosingMessageRequest()).isNotNull();
        assertLearningSession(sessionId, userId, "COMPLETED", "SYSTEM", "MAX_TURNS_REACHED");
    }

    @Test
    void userFirstMessageUsesOpeningInstructionSnapshot() throws Exception {
        JsonNode loginBody = login("user-first-opening-snapshot@example.com");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1120, 1, "ACTIVE", "카페");
        seedScenario(2120, 1120, 1, "USER", "ACTIVE", 1);
        seedScenarioVariant(
                3120,
                2120,
                "카페 주문",
                "카페에서 음료를 주문합니다.",
                "원하는 음료를 주문합니다.",
                "수정 전 시작 안내입니다.",
                null,
                null,
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(4120, 2120, 1, "What would you like to order?", "무엇을 주문할까요?");
        long sessionId = startScenario(accessToken, 2120);
        jdbcTemplate.update(
                "UPDATE scenario_language_variant SET user_opening_instruction = ? WHERE id = ?",
                "수정 후 시작 안내입니다.",
                3120
        );

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"I would like an americano.",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().evaluationContext().content())
                .isEqualTo("수정 전 시작 안내입니다.");
    }

    @Test
    void submitUserFirstMessageWithoutOpeningInstructionReturnsInternalServerError() throws Exception {
        JsonNode loginBody = login("user-first-missing-instruction@example.com");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1118, 1, "ACTIVE", "카페");
        seedScenario(2118, 1118, 1, "USER", "ACTIVE", 1);
        seedScenarioVariant(
                3118,
                2118,
                "카페 주문",
                "카페에서 음료를 주문합니다.",
                "원하는 음료를 주문합니다.",
                null,
                null,
                null,
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(
                4118,
                2118,
                1,
                "What size would you like?",
                "어떤 사이즈로 드릴까요?"
        );
        long sessionId = startScenario(accessToken, 2118);

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Can I get an iced americano?",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void submitAiFirstMessageWithoutPrecedingAiMessageReturnsInternalServerError() throws Exception {
        JsonNode loginBody = login("ai-first-missing-message@example.com");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1119, 1, "ACTIVE", "음식");
        seedScenario(2119, 1119, 1, "AI", "ACTIVE", 1);
        seedScenarioVariant(
                3119,
                2119,
                "음식 대화",
                "좋아하는 음식을 이야기합니다.",
                "좋아하는 음식을 영어로 설명합니다.",
                null,
                "What food do you like?",
                "어떤 음식을 좋아해?",
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(
                4119,
                2119,
                2,
                "Why do you like it?",
                "왜 좋아해?"
        );
        long sessionId = startScenario(accessToken, 2119);
        jdbcTemplate.update(
                """
                        DELETE FROM session_history_message
                        WHERE session_history_id = (
                            SELECT id
                            FROM session_history
                            WHERE learning_session_id = ?
                        )
                        """,
                sessionId
        );

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"I like pizza.",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));

        Integer messageCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        """,
                Integer.class,
                sessionId
        );
        assertThat(messageCount).isZero();
    }

    @Test
    void submitMessageCompletesSessionWithClosingMessageWhenNextQuestionDoesNotExist() throws Exception {
        JsonNode loginBody = login("max-turn-submit@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1102, 1, "ACTIVE", "음식");
        seedScenario(2102, 1102, 1, "AI", "ACTIVE", 1);
        seedScenarioVariant(
                3102,
                2102,
                "짧은 음식 대화",
                "좋아하는 음식을 이야기합니다.",
                "좋아하는 음식을 영어로 설명합니다.",
                null,
                "What food do you like?",
                "어떤 음식을 좋아해?",
                null,
                null,
                null,
                "ACTIVE"
        );
        long sessionId = startScenario(accessToken, 2102);

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"I like pizza.",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedMessage.feedbackProcessingStatus")
                        .value("PREPARING"))
                .andExpect(jsonPath("$.data.nextMessage.content")
                        .value("Thanks for sharing. That was a good conversation."))
                .andExpect(jsonPath("$.data.progress.currentTurnNumber").value(2))
                .andExpect(jsonPath("$.data.progress.currentMessageSequenceNumber").value(2))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(1))
                .andExpect(jsonPath("$.data.progress.completed").value(true));

        assertThat(fakeAiConversationClient.lastNextMessageRequest()).isNull();
        assertThat(fakeAiConversationClient.lastClosingMessageRequest()).isNotNull();
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest()).isNotNull();
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().evaluationContext().content())
                .isEqualTo("What food do you like?");
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest().userMessage())
                .isEqualTo("I like pizza.");
        assertThat(fakeAiConversationClient.closingMessageTransactionActive()).containsOnly(false);
        assertThat(fakeAiConversationClient.messageFeedbackTransactionActive()).containsOnly(false);
        assertThat(fakeAiConversationClient.lastClosingMessageRequest().closingReason().name())
                .isEqualTo("MAX_TURNS_REACHED");
        assertThat(fakeAiConversationClient.lastClosingMessageRequest().goalCompletionStatus())
                .isEqualTo(GoalCompletionStatus.COMPLETED);
        assertLearningSession(
                sessionId,
                userId,
                "COMPLETED",
                "SYSTEM",
                "MAX_TURNS_REACHED"
        );
        assertScenarioSessionGoalStatus(sessionId, "COMPLETED");
        assertSessionHistoryPlaceholder(sessionId);
    }

    @Test
    void getSessionFeedbackCreatesResultAndReturnsExistingResultWithoutRegeneration() throws Exception {
        StartedSession startedSession = startCompletedAiFirstSession("session-feedback-api@example.com");
        long userId = startedSession.userId();
        String accessToken = startedSession.accessToken();
        long sessionId = startedSession.sessionId();

        mockMvc.perform(post("/api/v1/sessions/%d/feedback".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.nativeScore").value(90))
                .andExpect(jsonPath("$.data.starRating").value(3.0))
                .andExpect(jsonPath("$.data.messageFeedbacks[0].messageId").isNumber())
                .andExpect(jsonPath("$.data.messageFeedbacks[0].messageFeedbackId").isNumber())
                .andExpect(jsonPath("$.data.messageFeedbacks[0].evaluationContext.type")
                        .value("AI_MESSAGE"))
                .andExpect(jsonPath("$.data.messageFeedbacks[0].evaluationContext.content")
                        .value("What food do you like?"))
                .andExpect(jsonPath("$.data.messageFeedbacks[0].feedbackType").value("GOOD"));

        Map<String, Object> historyBeforeSecondRequest = jdbcTemplate.queryForMap(
                """
                        SELECT ended_at, duration_seconds, user_message_count
                        FROM session_history
                        WHERE learning_session_id = ?
                        """,
                sessionId
        );
        Map<String, Object> progressBeforeSecondRequest = jdbcTemplate.queryForMap(
                """
                        SELECT status, completed_count, first_cleared_at, last_played_at,
                               best_star_rating, best_native_score
                        FROM user_scenario_progress
                        WHERE user_profile_id = ?
                          AND scenario_id = 2120
                          AND target_locale = 'EN'
                        """,
                userId
        );

        mockMvc.perform(post("/api/v1/sessions/%d/feedback".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nativeScore").value(90));

        assertThat(fakeAiConversationClient.sessionFeedbackCallCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_history_summary_feedback",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM session_history_message_feedback",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForMap(
                """
                        SELECT ended_at, duration_seconds, user_message_count
                        FROM session_history
                        WHERE learning_session_id = ?
                        """,
                sessionId
        )).isEqualTo(historyBeforeSecondRequest);
        assertThat(jdbcTemplate.queryForMap(
                """
                        SELECT status, completed_count, first_cleared_at, last_played_at,
                               best_star_rating, best_native_score
                        FROM user_scenario_progress
                        WHERE user_profile_id = ?
                          AND scenario_id = 2120
                          AND target_locale = 'EN'
                        """,
                userId
        )).isEqualTo(progressBeforeSecondRequest);
        Map<String, Object> progress = jdbcTemplate.queryForMap(
                """
                        SELECT status, completed_count
                        FROM user_scenario_progress
                        WHERE user_profile_id = ?
                          AND scenario_id = 2120
                          AND target_locale = 'EN'
                        """,
                userId
        );
        assertThat(progress.get("STATUS")).isEqualTo("CLEARED");
        assertThat(progress.get("COMPLETED_COUNT")).isEqualTo(1);
    }

    @Test
    void getSessionFeedbackUsesNativeScoreStarRatingWhenAiStarRatingDiffers() throws Exception {
        StartedSession startedSession = startCompletedAiFirstSession(
                "session-feedback-invalid-result@example.com"
        );
        fakeAiConversationClient.returnMismatchedSessionFeedbackStarRating();

        mockMvc.perform(post("/api/v1/sessions/%d/feedback".formatted(startedSession.sessionId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + startedSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nativeScore").value(90))
                .andExpect(jsonPath("$.data.starRating").value(3.0));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT star_rating FROM session_history_summary_feedback",
                BigDecimal.class
        )).isEqualByComparingTo("3.0");
    }

    @Test
    void getSessionFeedbackIncludesUserFirstOpeningInstructionAndAllUserMessages() throws Exception {
        JsonNode loginBody = login("session-feedback-user-first@example.com");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1121, 1, "ACTIVE", "카페");
        seedScenario(2121, 1121, 1, "USER", "ACTIVE", 1);
        seedScenarioVariant(
                3121,
                2121,
                "카페 주문",
                "카페에서 음료를 주문합니다.",
                "원하는 음료를 자연스럽게 주문합니다.",
                "점원에게 먼저 주문하고 싶은 음료를 말해보세요.",
                null,
                null,
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(
                4121,
                2121,
                1,
                "Would you like anything else?",
                "더 필요한 것은 없나요?"
        );
        long sessionId = startScenario(accessToken, 2121);

        submitMessage(accessToken, sessionId, "Can I get an iced americano?");
        submitMessage(accessToken, sessionId, "That is all, thank you.");

        mockMvc.perform(post("/api/v1/sessions/%d/feedback".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageFeedbacks.length()").value(2))
                .andExpect(jsonPath("$.data.messageFeedbacks[0].evaluationContext.type")
                        .value("SCENARIO_OPENING_INSTRUCTION"))
                .andExpect(jsonPath("$.data.messageFeedbacks[0].evaluationContext.content")
                        .value("점원에게 먼저 주문하고 싶은 음료를 말해보세요."))
                .andExpect(jsonPath("$.data.messageFeedbacks[0].evaluationContext.translatedContent")
                        .value(nullValue()))
                .andExpect(jsonPath("$.data.messageFeedbacks[1].evaluationContext.type")
                        .value("AI_MESSAGE"))
                .andExpect(jsonPath("$.data.messageFeedbacks[1].evaluationContext.content")
                        .value("Oh, you like spicy pizza. What food did you eat recently?"));

        assertThat(fakeAiConversationClient.lastSessionFeedbackRequest().expectedMessageIds())
                .containsExactlyElementsOf(userMessageIds(sessionId));
        assertThat(fakeAiConversationClient.sessionFeedbackTransactionActive()).containsOnly(false);
    }

    @Test
    void getSessionFeedbackRejectsInProgressSession() throws Exception {
        JsonNode loginBody = login("session-feedback-in-progress@example.com");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1122, 1, "ACTIVE", "음식");
        seedScenario(2122, 1122, 1, "AI", "ACTIVE", 1);
        seedScenarioVariant(
                3122,
                2122,
                "음식 대화",
                "음식에 대해 이야기합니다.",
                "음식 취향을 설명합니다.",
                null,
                "What food do you like?",
                "어떤 음식을 좋아해?",
                null,
                null,
                null,
                "ACTIVE"
        );
        long sessionId = startScenario(accessToken, 2122);

        mockMvc.perform(post("/api/v1/sessions/%d/feedback".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_NOT_COMPLETED"));

        assertThat(fakeAiConversationClient.sessionFeedbackCallCount()).isZero();
    }

    @Test
    void submitMessageContinuesWhenAiReportsGoalCompletedButNextQuestionExists() throws Exception {
        fakeAiConversationClient.completeGoalOnNextMessage();
        JsonNode loginBody = login("goal-completed-submit@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1103, 1, "ACTIVE", "기숙사");
        seedScenario(2103, 1103, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3103,
                2103,
                "조용히 해달라고 말하기",
                "룸메이트에게 밤에 조용히 해달라고 말합니다.",
                "불편함을 공격적이지 않게 전달합니다.",
                null,
                "What do you want me to do?",
                "내가 어떻게 해주면 좋겠어?",
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(
                4103,
                2103,
                2,
                "Do you want me to stop now?",
                "지금 그만하면 될까?"
        );
        long sessionId = startScenario(accessToken, 2103);

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Could you keep it down at night?",
                                  "inputType":"VOICE"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedMessage.innerThoughtProcessingStatus")
                        .value("PREPARING"))
                .andExpect(jsonPath("$.data.submittedMessage.innerThought").value(nullValue()))
                .andExpect(jsonPath("$.data.nextMessage.content")
                        .value("Oh, you like spicy pizza. What food did you eat recently?"))
                .andExpect(jsonPath("$.data.progress.completed").value(false));

        assertThat(fakeAiConversationClient.lastNextMessageRequest()).isNotNull();
        assertThat(fakeAiConversationClient.lastClosingMessageRequest()).isNull();
        assertThat(fakeAiConversationClient.nextMessageTransactionActive()).containsOnly(false);
        assertThat(fakeAiConversationClient.closingMessageTransactionActive()).isEmpty();
        assertLearningSession(
                sessionId,
                userId,
                "IN_PROGRESS",
                null,
                null
        );
        assertScenarioSessionGoalStatus(sessionId, "COMPLETED");
        List<String> contents = jdbcTemplate.queryForList(
                """
                        SELECT shm.content
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        ORDER BY shm.message_sequence ASC
                        """,
                String.class,
                sessionId
        );
        assertThat(contents).containsExactly(
                "What do you want me to do?",
                "Could you keep it down at night?",
                "Oh, you like spicy pizza. What food did you eat recently?"
        );
    }

    @Test
    void submitMessageRejectsOtherUserSession() throws Exception {
        JsonNode ownerLoginBody = login("message-owner@example.com");
        long ownerId = ownerLoginBody.get("data").get("user").get("userId").asLong();
        String ownerAccessToken = ownerLoginBody.get("data").get("accessToken").asText();
        seedCategory(1104, 1, "ACTIVE", "권한");
        seedScenario(2104, 1104, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3104,
                2104,
                "권한 테스트",
                "권한 테스트",
                "권한 테스트",
                null,
                "Hello",
                "안녕",
                null,
                null,
                null,
                "ACTIVE"
        );
        long sessionId = startScenario(ownerAccessToken, 2104);
        JsonNode otherLoginBody = login("message-other@example.com");

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + otherLoginBody.get("data").get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void submitMessageRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void submitMessageRejectsMissingSession() throws Exception {
        JsonNode loginBody = login("message-missing@example.com");

        mockMvc.perform(post("/api/v1/sessions/999999/messages")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginBody.get("data").get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void submitMessageRejectsCompletedSession() throws Exception {
        StartedSession startedSession = startUserFirstSession(
                "message-completed@example.com",
                1105,
                2105,
                3105
        );
        jdbcTemplate.update("""
                        UPDATE learning_session
                        SET status = 'COMPLETED',
                            ended_by = 'SYSTEM',
                            completion_reason = 'GOAL_COMPLETED',
                            ended_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                startedSession.sessionId()
        );

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(startedSession.sessionId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + startedSession.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_ALREADY_COMPLETED"));
    }

    @Test
    void submitMessageRejectsBlankMessage() throws Exception {
        StartedSession startedSession = startUserFirstSession(
                "message-blank@example.com",
                1106,
                2106,
                3106
        );

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(startedSession.sessionId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + startedSession.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"   ",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void submitMessageRollsBackUserMessageWhenAiGenerationFails() throws Exception {
        fakeAiConversationClient.failNextMessageGeneration();
        JsonNode loginBody = login("message-ai-fail@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1107, 1, "ACTIVE", "AI 실패");
        seedScenario(2107, 1107, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3107,
                2107,
                "AI 실패 테스트",
                "AI 실패 테스트",
                "AI 실패 테스트",
                null,
                "Hello",
                "안녕",
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(4107, 2107, 2, "Next question", "다음 질문");
        long sessionId = startScenario(accessToken, 2107);

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("AI_GENERATION_FAILED"));

        assertThat(fakeAiConversationClient.nextMessageTransactionActive()).containsOnly(false);
        Integer messageCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        """,
                Integer.class,
                sessionId
        );
        assertThat(messageCount).isEqualTo(1);
    }

    @Test
    void submitMessageRollsBackUserMessageWhenMessageFeedbackRequestFails() throws Exception {
        fakeAiConversationClient.failMessageFeedbackRequest();
        JsonNode loginBody = login("message-feedback-fail@example.com");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1109, 1, "ACTIVE", "피드백 실패");
        seedScenario(2109, 1109, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3109,
                2109,
                "피드백 실패 테스트",
                "피드백 실패 테스트",
                "피드백 실패 테스트",
                null,
                "Hello",
                "안녕",
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(4109, 2109, 2, "Next question", "다음 질문");
        long sessionId = startScenario(accessToken, 2109);

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("AI_GENERATION_FAILED"));

        assertThat(fakeAiConversationClient.lastNextMessageRequest()).isNotNull();
        assertThat(fakeAiConversationClient.lastMessageFeedbackRequest()).isNotNull();
        assertThat(fakeAiConversationClient.messageFeedbackTransactionActive()).containsOnly(false);
        Integer messageCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        """,
                Integer.class,
                sessionId
        );
        assertThat(messageCount).isEqualTo(1);
    }

    @Test
    void submitMessageRollsBackUserMessageWhenMessageFeedbackStatusIsNotPreparing() throws Exception {
        fakeAiConversationClient.returnMessageFeedbackStatus(ProcessingStatus.COMPLETED);
        JsonNode loginBody = login("message-feedback-invalid@example.com");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1110, 1, "ACTIVE", "피드백 응답 오류");
        seedScenario(2110, 1110, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3110,
                2110,
                "피드백 응답 오류 테스트",
                "피드백 응답 오류 테스트",
                "피드백 응답 오류 테스트",
                null,
                "Hello",
                "안녕",
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(4110, 2110, 2, "Next question", "다음 질문");
        long sessionId = startScenario(accessToken, 2110);

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("AI_RESPONSE_INVALID"));

        Integer messageCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        """,
                Integer.class,
                sessionId
        );
        assertThat(messageCount).isEqualTo(1);
    }

    @Test
    void submitMessageRollsBackUserMessageWhenMessageFeedbackResponseMessageIdDiffers() throws Exception {
        fakeAiConversationClient.returnMessageFeedbackForMessageId(9999L);
        JsonNode loginBody = login("message-feedback-id-invalid@example.com");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1111, 1, "ACTIVE", "피드백 식별자 오류");
        seedScenario(2111, 1111, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3111,
                2111,
                "피드백 식별자 오류 테스트",
                "피드백 식별자 오류 테스트",
                "피드백 식별자 오류 테스트",
                null,
                "Hello",
                "안녕",
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(4111, 2111, 2, "Next question", "다음 질문");
        long sessionId = startScenario(accessToken, 2111);

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("AI_RESPONSE_INVALID"));

        Integer messageCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        """,
                Integer.class,
                sessionId
        );
        assertThat(messageCount).isEqualTo(1);
    }

    @Test
    void submitMessageRollsBackUserMessageWhenMessageFeedbackResponseSessionIdDiffers() throws Exception {
        fakeAiConversationClient.returnMessageFeedbackForSessionId(9999L);
        JsonNode loginBody = login("message-feedback-session-id-invalid@example.com");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1112, 1, "ACTIVE", "피드백 세션 식별자 오류");
        seedScenario(2112, 1112, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3112,
                2112,
                "피드백 세션 식별자 오류 테스트",
                "피드백 세션 식별자 오류 테스트",
                "피드백 세션 식별자 오류 테스트",
                null,
                "Hello",
                "안녕",
                null,
                null,
                null,
                "ACTIVE"
        );
        seedScenarioQuestion(4112, 2112, 2, "Next question", "다음 질문");
        long sessionId = startScenario(accessToken, 2112);

        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Hello",
                                  "inputType":"VOICE"
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("AI_RESPONSE_INVALID"));

        Integer messageCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        """,
                Integer.class,
                sessionId
        );
        assertThat(messageCount).isEqualTo(1);
    }

    @Test
    void startUserFirstScenarioReturnsInstructionWithoutOpeningMessage() throws Exception {
        JsonNode loginBody = login("user-first@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1002, 1, "ACTIVE", "카페");
        seedScenario(2002, 1002, 1, "USER", "ACTIVE", 3);
        seedScenarioVariant(
                3002,
                2002,
                "카페 주문",
                "카페에서 음료를 주문합니다.",
                "원하는 음료를 주문한다.",
                "점원에게 먼저 주문하고 싶은 음료를 말해보세요.",
                null,
                null,
                null,
                null,
                ttsVoiceId("en-US-Ethan:MAI-Voice-2"),
                "ACTIVE"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/2002/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sessionId").value(notNullValue()))
                .andExpect(jsonPath("$.data.scenarioId").value(2002))
                .andExpect(jsonPath("$.data.sessionType").value("SCENARIO"))
                .andExpect(jsonPath("$.data.firstSpeaker").value("USER"))
                .andExpect(jsonPath("$.data.userOpeningInstruction")
                        .value("점원에게 먼저 주문하고 싶은 음료를 말해보세요."))
                .andExpect(jsonPath("$.data.ttsVoice.providerVoiceId")
                        .value("en-US-Ethan:MAI-Voice-2"))
                .andExpect(jsonPath("$.data.ttsVoice.gender").value("MALE"))
                .andExpect(jsonPath("$.data.currentMessage").value(nullValue()))
                .andExpect(jsonPath("$.data.progress.currentTurnNumber").value(1))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(3))
                .andExpect(jsonPath("$.data.progress.completed").value(false))
                .andReturn();

        long sessionId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("data")
                .get("sessionId")
                .asLong();
        assertLearningSession(sessionId, userId, "IN_PROGRESS", null, null);
        assertScenarioSession(sessionId, 3002);
        assertProgress(userId, 2002, "IN_PROGRESS");
        Integer messageCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        """,
                Integer.class,
                sessionId
        );
        assertThat(messageCount).isZero();
    }

    @Test
    void startScenarioReturnsNullTtsVoiceWhenVoiceIsInactive() throws Exception {
        JsonNode loginBody = login("inactive-tts@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1011, 1, "ACTIVE", "비활성 음성");
        seedScenario(2011, 1011, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3011,
                2011,
                "비활성 음성",
                "비활성 음성을 사용합니다.",
                "비활성 음성 응답을 확인한다.",
                null,
                "Hello",
                "안녕하세요",
                null,
                null,
                insertTtsVoice(990201, "test-session-inactive-voice", "INACTIVE"),
                "ACTIVE"
        );

        mockMvc.perform(post("/api/v1/scenarios/2011/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ttsVoice").value(nullValue()));
    }

    @Test
    void startScenarioHandlesConcurrentProgressCreationForSameUser() throws Exception {
        JsonNode loginBody = login("concurrent-start@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1010, 1, "ACTIVE", "동시 시작");
        seedScenario(2010, 1010, 1, "USER", "ACTIVE", 2);
        seedScenarioVariant(
                3010,
                2010,
                "동시 시작",
                "동시 시작",
                "동시 시작",
                "먼저 말해보세요.",
                null,
                null,
                null,
                null,
                null,
                "ACTIVE"
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Callable<Integer> startRequest = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return mockMvc.perform(post("/api/v1/scenarios/2010/sessions")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        try {
            Future<Integer> first = executorService.submit(startRequest);
            Future<Integer> second = executorService.submit(startRequest);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            )).containsExactlyInAnyOrder(201, 201);
        } finally {
            executorService.shutdownNow();
        }

        Integer progressCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM user_scenario_progress
                        WHERE user_profile_id = ?
                          AND scenario_id = ?
                          AND target_locale = 'EN'
                        """,
                Integer.class,
                userId,
                2010
        );
        assertThat(progressCount).isEqualTo(1);
    }

    @Test
    void startScenarioRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/scenarios/2001/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
    }

    @Test
    void startScenarioRejectsMissingScenario() throws Exception {
        JsonNode loginBody = login("missing-scenario@example.com");

        mockMvc.perform(post("/api/v1/scenarios/999999/sessions")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginBody.get("data").get("accessToken").asText()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SCENARIO_NOT_FOUND"));
    }

    @Test
    void startScenarioRejectsInactiveCategory() throws Exception {
        JsonNode loginBody = login("locked-category@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        seedCategory(1003, 1, "INACTIVE", "잠긴 카테고리");
        seedScenario(2003, 1003, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3003, 2003, "잠김", "잠김", "잠김", null, "Hello", "안녕", null, null, null, "ACTIVE"
        );

        mockMvc.perform(post("/api/v1/scenarios/2003/sessions")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginBody.get("data").get("accessToken").asText()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_LOCKED"));
    }

    @Test
    void startScenarioRejectsPreviousScenarioNotCleared() throws Exception {
        JsonNode loginBody = login("locked-scenario@example.com");
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        seedCategory(1004, 1, "ACTIVE", "순차 카테고리");
        seedScenario(2004, 1004, 1, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3004, 2004, "첫번째", "첫번째", "첫번째", null, "First", "첫번째", null, null, null, "ACTIVE"
        );
        seedScenario(2005, 1004, 2, "AI", "ACTIVE", 2);
        seedScenarioVariant(
                3005, 2005, "두번째", "두번째", "두번째", null, "Second", "두번째", null, null, null, "ACTIVE"
        );

        mockMvc.perform(post("/api/v1/scenarios/2005/sessions")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginBody.get("data").get("accessToken").asText()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SCENARIO_LOCKED"))
                .andExpect(jsonPath("$.error.message").value("PREVIOUS_SCENARIO_NOT_COMPLETED"));
    }

    @Test
    void endSessionInterruptsOwnedInProgressSession() throws Exception {
        StartedSession startedSession = startUserFirstSession("end-owned@example.com", 1005, 2006, 3006);

        mockMvc.perform(patch("/api/v1/sessions/%d/end".formatted(startedSession.sessionId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + startedSession.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        assertLearningSession(
                startedSession.sessionId(),
                startedSession.userId(),
                "INTERRUPTED",
                "USER",
                "USER_ENDED"
        );
        Integer endedAtCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM learning_session WHERE id = ? AND ended_at IS NOT NULL",
                Integer.class,
                startedSession.sessionId()
        );
        assertThat(endedAtCount).isEqualTo(1);
    }

    @Test
    void endSessionRejectsOtherUserSession() throws Exception {
        StartedSession ownerSession = startUserFirstSession("owner@example.com", 1006, 2007, 3007);
        JsonNode otherLoginBody = login("other@example.com");

        mockMvc.perform(patch("/api/v1/sessions/%d/end".formatted(ownerSession.sessionId()))
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + otherLoginBody.get("data").get("accessToken").asText()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void endSessionRejectsMissingSession() throws Exception {
        JsonNode loginBody = login("missing-session@example.com");

        mockMvc.perform(patch("/api/v1/sessions/999999/end")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + loginBody.get("data").get("accessToken").asText()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void endSessionRejectsAlreadyEndedSession() throws Exception {
        StartedSession startedSession = startUserFirstSession("already-ended@example.com", 1007, 2008, 3008);
        jdbcTemplate.update("""
                        UPDATE learning_session
                        SET status = 'COMPLETED',
                            ended_by = 'SYSTEM',
                            completion_reason = 'GOAL_COMPLETED',
                            ended_at = CURRENT_TIMESTAMP,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                startedSession.sessionId()
        );

        mockMvc.perform(patch("/api/v1/sessions/%d/end".formatted(startedSession.sessionId()))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + startedSession.accessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_ALREADY_COMPLETED"));
    }

    private StartedSession startUserFirstSession(
            String email,
            long categoryId,
            long scenarioId,
            long variantId
    ) throws Exception {
        JsonNode loginBody = login(email);
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(categoryId, 1, "ACTIVE", "종료 테스트");
        seedScenario(scenarioId, categoryId, 1, "USER", "ACTIVE", 2);
        seedScenarioVariant(
                variantId,
                scenarioId,
                "종료",
                "종료",
                "종료",
                "먼저 말해보세요.",
                null,
                null,
                null,
                null,
                null,
                "ACTIVE"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/%d/sessions".formatted(scenarioId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();
        long sessionId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("data")
                .get("sessionId")
                .asLong();
        return new StartedSession(userId, accessToken, sessionId);
    }

    private JsonNode login(String email) throws Exception {
        String nonce = UUID.randomUUID().toString();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"%s|%s|Scenario Session User|%s",
                                  "nonce":"%s"
                                }
                                """.formatted(UUID.randomUUID(), email, nonce, nonce)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private long startScenario(String accessToken, long scenarioId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/%d/sessions".formatted(scenarioId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("data")
                .get("sessionId")
                .asLong();
    }

    private void seedCategory(long categoryId, int displayOrder, String status, String name) {
        jdbcTemplate.update("""
                        INSERT INTO category (id, display_order, status, created_at, updated_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                categoryId,
                displayOrder,
                status
        );
        jdbcTemplate.update("""
                        INSERT INTO category_language_variant (
                            category_id,
                            base_locale,
                            name,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'KR', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                categoryId,
                name
        );
    }

    private void seedScenario(
            long scenarioId,
            long categoryId,
            int displayOrder,
            String firstSpeaker,
            String status,
            int totalQuestionCount
    ) {
        jdbcTemplate.update("""
                        INSERT INTO scenario (
                            id,
                            category_id,
                            ai_role,
                            difficulty,
                            first_speaker,
                            total_question_count,
                            thumbnail_url,
                            display_order,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, 'tutor', 'EASY', ?, ?, NULL, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                scenarioId,
                categoryId,
                firstSpeaker,
                totalQuestionCount,
                displayOrder,
                status
        );
    }

    private void seedScenarioVariant(
            long variantId,
            long scenarioId,
            String title,
            String briefing,
            String conversationGoal,
            String userOpeningInstruction,
            String openingQuestionText,
            String openingQuestionTranslation,
            String openingInnerThought,
            String openingInnerThoughtType,
            Long ttsVoiceId,
            String status
    ) {
        jdbcTemplate.update("""
                        INSERT INTO scenario_language_variant (
                            id,
                            scenario_id,
                            target_locale,
                            base_locale,
                            title,
                            briefing,
                            user_opening_instruction,
                            conversation_goal,
                            tts_voice_id,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, 'EN', 'KR', ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                variantId,
                scenarioId,
                title,
                briefing,
                userOpeningInstruction,
                conversationGoal,
                ttsVoiceId,
                status
        );
        if (openingQuestionText != null && !hasScenarioQuestion(scenarioId, 1)) {
            seedScenarioQuestion(
                    100_000L + scenarioId,
                    scenarioId,
                    1,
                    openingQuestionText,
                    openingQuestionTranslation,
                    openingInnerThought,
                    openingInnerThoughtType
            );
        }
    }

    private boolean hasScenarioQuestion(long scenarioId, int displayOrder) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM scenario_question WHERE scenario_id = ? AND display_order = ?",
                Integer.class,
                scenarioId,
                displayOrder
        );
        return count != null && count > 0;
    }

    private void seedScenarioQuestion(
            long questionId,
            long scenarioId,
            int displayOrder,
            String questionText,
            String questionTranslation
    ) {
        seedScenarioQuestion(
                questionId,
                scenarioId,
                displayOrder,
                questionText,
                questionTranslation,
                null,
                null
        );
    }

    private void seedScenarioQuestion(
            long questionId,
            long scenarioId,
            int displayOrder,
            String questionText,
            String questionTranslation,
            String innerThought,
            String innerThoughtType
    ) {
        jdbcTemplate.update("""
                        INSERT INTO scenario_question (
                            id,
                            scenario_id,
                            display_order,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                questionId,
                scenarioId,
                displayOrder
        );
        jdbcTemplate.update("""
                        INSERT INTO scenario_question_language_variant (
                            scenario_question_id,
                            target_locale,
                            base_locale,
                            question_text,
                            question_translation,
                            inner_thought,
                            inner_thought_type,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'EN', 'KR', ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                questionId,
                questionText,
                questionTranslation,
                innerThought,
                innerThoughtType
        );
    }

    private long ttsVoiceId(String providerVoiceId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tts_voice WHERE provider_voice_id = ?",
                Long.class,
                providerVoiceId
        );
    }

    private long defaultAiTutorId() {
        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM ai_tutor
                WHERE accent_locale = 'EN_US'
                  AND target_locale = 'EN'
                  AND status = 'ACTIVE'
                """, Long.class);
    }

    private long insertTtsVoice(long id, String providerVoiceId, String status) {
        jdbcTemplate.update("""
                        INSERT INTO tts_voice (
                            id,
                            provider,
                            model,
                            provider_voice_id,
                            gender,
                            description,
                            accent_locale,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, 'OPENROUTER', 'test-model', ?, 'MALE', '테스트 음성',
                                'EN_US', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                id,
                providerVoiceId,
                status
        );
        return id;
    }
    private void assertLearningSession(
            long sessionId,
            long userId,
            String status,
            String endedBy,
            String completionReason
    ) {
        Map<String, Object> session = jdbcTemplate.queryForMap(
                """
                        SELECT user_profile_id,
                               session_type,
                               ai_tutor_id,
                               target_locale,
                               base_locale,
                               input_mode,
                               status,
                               ended_by,
                               completion_reason
                        FROM learning_session
                        WHERE id = ?
                        """,
                sessionId
        );
        assertThat(session.get("USER_PROFILE_ID")).isEqualTo(userId);
        assertThat(session.get("SESSION_TYPE")).isEqualTo("SCENARIO");
        assertThat(session.get("AI_TUTOR_ID")).isEqualTo(defaultAiTutorId());
        assertThat(session.get("TARGET_LOCALE")).isEqualTo("EN");
        assertThat(session.get("BASE_LOCALE")).isEqualTo("KR");
        assertThat(session.get("INPUT_MODE")).isEqualTo("MIXED");
        assertThat(session.get("STATUS")).isEqualTo(status);
        assertThat(session.get("ENDED_BY")).isEqualTo(endedBy);
        assertThat(session.get("COMPLETION_REASON")).isEqualTo(completionReason);
    }

    private void assertScenarioSession(long sessionId, long variantId) {
        Map<String, Object> scenarioSession = jdbcTemplate.queryForMap(
                """
                        SELECT scenario_language_variant_id, goal_completion_status
                        FROM scenario_session
                        WHERE learning_session_id = ?
                        """,
                sessionId
        );
        assertThat(scenarioSession.get("SCENARIO_LANGUAGE_VARIANT_ID")).isEqualTo(variantId);
        assertThat(scenarioSession.get("GOAL_COMPLETION_STATUS")).isEqualTo("NOT_STARTED");
    }

    private void assertScenarioSessionGoalStatus(long sessionId, String goalCompletionStatus) {
        String actualStatus = jdbcTemplate.queryForObject(
                """
                        SELECT goal_completion_status
                        FROM scenario_session
                        WHERE learning_session_id = ?
                        """,
                String.class,
                sessionId
        );
        assertThat(actualStatus).isEqualTo(goalCompletionStatus);
    }

    private void assertSessionHistoryPlaceholder(long sessionId) {
        Map<String, Object> history = jdbcTemplate.queryForMap(
                """
                        SELECT ended_at, duration_seconds, user_message_count
                        FROM session_history
                        WHERE learning_session_id = ?
                        """,
                sessionId
        );
        assertThat(history.get("ENDED_AT")).isNotNull();
        assertThat(history.get("DURATION_SECONDS")).isEqualTo(0);
        assertThat(history.get("USER_MESSAGE_COUNT")).isEqualTo(0);
    }

    private void assertProgress(long userId, long scenarioId, String status) {
        Map<String, Object> progress = jdbcTemplate.queryForMap(
                """
                        SELECT status, completed_count, last_played_at
                        FROM user_scenario_progress
                        WHERE user_profile_id = ?
                          AND scenario_id = ?
                          AND target_locale = 'EN'
                        """,
                userId,
                scenarioId
        );
        assertThat(progress.get("STATUS")).isEqualTo(status);
        assertThat(progress.get("COMPLETED_COUNT")).isEqualTo(0);
        assertThat(progress.get("LAST_PLAYED_AT")).isNotNull();
    }

    private void assertHistoryMessage(long sessionId, String role, String content) {
        Map<String, Object> message = jdbcTemplate.queryForMap(
                """
                        SELECT shm.role, shm.content, shm.input_type, shm.message_sequence, shm.turn_number
                        FROM session_history_message shm
                        JOIN session_history sh ON sh.id = shm.session_history_id
                        WHERE sh.learning_session_id = ?
                        """,
                sessionId
        );
        assertThat(message.get("ROLE")).isEqualTo(role);
        assertThat(message.get("CONTENT")).isEqualTo(content);
        assertThat(message.get("INPUT_TYPE")).isEqualTo("GENERATED");
        assertThat(message.get("MESSAGE_SEQUENCE")).isEqualTo(1);
        assertThat(message.get("TURN_NUMBER")).isEqualTo(1);
    }

    private void submitMessage(String accessToken, long sessionId, String content) throws Exception {
        mockMvc.perform(post("/api/v1/sessions/%d/messages".formatted(sessionId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"%s",
                                  "inputType":"VOICE"
                                }
                                """.formatted(content)))
                .andExpect(status().isOk());
    }

    private List<Long> userMessageIds(long sessionId) {
        return jdbcTemplate.queryForList(
                """
                        SELECT message.id
                        FROM session_history_message message
                        JOIN session_history history ON history.id = message.session_history_id
                        WHERE history.learning_session_id = ?
                          AND message.role = 'USER'
                        ORDER BY message.message_sequence
                        """,
                Long.class,
                sessionId
        );
    }

    private StartedSession startCompletedAiFirstSession(String email) throws Exception {
        JsonNode loginBody = login(email);
        long userId = loginBody.get("data").get("user").get("userId").asLong();
        String accessToken = loginBody.get("data").get("accessToken").asText();
        seedCategory(1120, 1, "ACTIVE", "음식");
        seedScenario(2120, 1120, 1, "AI", "ACTIVE", 1);
        seedScenarioVariant(
                3120,
                2120,
                "음식에 대한 대화",
                "좋아하는 음식에 대해 이야기합니다.",
                "음식 취향과 이유를 자연스럽게 설명합니다.",
                null,
                "What food do you like?",
                "어떤 음식을 좋아해?",
                null,
                null,
                null,
                "ACTIVE"
        );
        long sessionId = startScenario(accessToken, 2120);
        submitMessage(accessToken, sessionId, "I like pizza.");
        return new StartedSession(userId, accessToken, sessionId);
    }

    private record StartedSession(long userId, String accessToken, long sessionId) {
    }

    @TestConfiguration
    static class FakeAiClientConfiguration {

        @Bean
        @Primary
        FakeAiConversationClient fakeAiConversationClient() {
            return new FakeAiConversationClient();
        }
    }

    private static class FakeAiConversationClient implements AiConversationClient {

        private AiNextMessageRequest lastNextMessageRequest;

        private AiClosingMessageRequest lastClosingMessageRequest;

        private AiMessageFeedbackRequest lastMessageFeedbackRequest;

        private AiSessionFeedbackRequest lastSessionFeedbackRequest;

        private final List<Boolean> nextMessageTransactionActive = new ArrayList<>();

        private final List<Boolean> closingMessageTransactionActive = new ArrayList<>();

        private final List<Boolean> messageFeedbackTransactionActive = new ArrayList<>();

        private final List<Boolean> sessionFeedbackTransactionActive = new ArrayList<>();

        private GoalCompletionStatus nextGoalCompletionStatus = GoalCompletionStatus.PARTIAL;

        private boolean failNextMessageGeneration;

        private boolean failMessageFeedbackRequest;

        private BigDecimal sessionFeedbackStarRating = new BigDecimal("3.0");

        private int sessionFeedbackCallCount;

        private ProcessingStatus messageFeedbackStatus = ProcessingStatus.PREPARING;

        private Long messageFeedbackResponseMessageId;

        private Long messageFeedbackResponseSessionId;

        @Override
        public AiNextMessageResult generateNextMessage(AiNextMessageRequest request) {
            lastNextMessageRequest = request;
            nextMessageTransactionActive.add(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            if (failNextMessageGeneration) {
                throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
            }
            return new AiNextMessageResult(
                    "Oh, you like spicy pizza. What food did you eat recently?",
                    "아, 매콤한 피자를 좋아하는구나. 최근에는 어떤 음식을 먹었어?",
                    "매운 피자를 좋아한다고 이유까지 말해주네.",
                    InnerThoughtType.GOOD,
                    nextGoalCompletionStatus
            );
        }

        @Override
        public AiClosingMessageResult generateClosingMessage(AiClosingMessageRequest request) {
            lastClosingMessageRequest = request;
            closingMessageTransactionActive.add(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            return new AiClosingMessageResult(
                    "Thanks for sharing. That was a good conversation.",
                    "이야기해줘서 고마워. 좋은 대화였어.",
                    "마지막까지 답해줘서 대화를 자연스럽게 마무리하면 좋겠다.",
                    InnerThoughtType.NORMAL
            );
        }

        @Override
        public AiMessageFeedbackResult requestMessageFeedback(AiMessageFeedbackRequest request) {
            lastMessageFeedbackRequest = request;
            messageFeedbackTransactionActive.add(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            if (failMessageFeedbackRequest) {
                throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
            }
            return new AiMessageFeedbackResult(
                    messageFeedbackResponseSessionId == null
                            ? request.sessionId()
                            : messageFeedbackResponseSessionId,
                    messageFeedbackResponseMessageId == null
                            ? request.messageId()
                            : messageFeedbackResponseMessageId,
                    messageFeedbackStatus
            );
        }

        @Override
        public AiSessionFeedbackResult generateSessionFeedback(AiSessionFeedbackRequest request) {
            lastSessionFeedbackRequest = request;
            sessionFeedbackTransactionActive.add(
                    TransactionSynchronizationManager.isActualTransactionActive()
            );
            sessionFeedbackCallCount++;
            return new AiSessionFeedbackResult(
                    request.sessionId(),
                    90,
                    sessionFeedbackStarRating,
                    "You clearly communicated your main idea.",
                    "Keep practicing complete sentences with clear reasons.",
                    request.expectedMessageIds().stream()
                            .map(messageId -> new AiSessionMessageFeedbackResult(
                                    messageId,
                                    FeedbackType.GOOD,
                                    "한국어로 이유를 덧붙여 자연스럽게 말한 것과 비슷해요.",
                                    null,
                                    "Your message clearly communicates the main idea.",
                                    null,
                                    null,
                                    "Your message clearly communicates the main idea."
                            ))
                            .toList()
            );
        }

        private void reset() {
            lastNextMessageRequest = null;
            lastClosingMessageRequest = null;
            lastMessageFeedbackRequest = null;
            lastSessionFeedbackRequest = null;
            nextMessageTransactionActive.clear();
            closingMessageTransactionActive.clear();
            messageFeedbackTransactionActive.clear();
            sessionFeedbackTransactionActive.clear();
            nextGoalCompletionStatus = GoalCompletionStatus.PARTIAL;
            failNextMessageGeneration = false;
            failMessageFeedbackRequest = false;
            sessionFeedbackStarRating = new BigDecimal("3.0");
            sessionFeedbackCallCount = 0;
            messageFeedbackStatus = ProcessingStatus.PREPARING;
            messageFeedbackResponseMessageId = null;
            messageFeedbackResponseSessionId = null;
        }

        private void completeGoalOnNextMessage() {
            nextGoalCompletionStatus = GoalCompletionStatus.COMPLETED;
        }

        private void failNextMessageGeneration() {
            failNextMessageGeneration = true;
        }

        private void failMessageFeedbackRequest() {
            failMessageFeedbackRequest = true;
        }

        private void returnMismatchedSessionFeedbackStarRating() {
            sessionFeedbackStarRating = new BigDecimal("2.5");
        }

        private void returnMessageFeedbackStatus(ProcessingStatus status) {
            messageFeedbackStatus = status;
        }

        private void returnMessageFeedbackForMessageId(long messageId) {
            messageFeedbackResponseMessageId = messageId;
        }

        private void returnMessageFeedbackForSessionId(long sessionId) {
            messageFeedbackResponseSessionId = sessionId;
        }

        private int sessionFeedbackCallCount() {
            return sessionFeedbackCallCount;
        }

        private AiSessionFeedbackRequest lastSessionFeedbackRequest() {
            return lastSessionFeedbackRequest;
        }

        private List<Boolean> sessionFeedbackTransactionActive() {
            return sessionFeedbackTransactionActive;
        }

        private AiNextMessageRequest lastNextMessageRequest() {
            return lastNextMessageRequest;
        }

        private AiClosingMessageRequest lastClosingMessageRequest() {
            return lastClosingMessageRequest;
        }

        private AiMessageFeedbackRequest lastMessageFeedbackRequest() {
            return lastMessageFeedbackRequest;
        }

        private List<Boolean> nextMessageTransactionActive() {
            return nextMessageTransactionActive;
        }

        private List<Boolean> closingMessageTransactionActive() {
            return closingMessageTransactionActive;
        }

        private List<Boolean> messageFeedbackTransactionActive() {
            return messageFeedbackTransactionActive;
        }
    }
}
