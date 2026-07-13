// 원격 AI 대화 클라이언트의 메시지별 피드백 요청 계약을 검증한다.
package com.landit.landitbe.session.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.application.port.AiMessageFeedbackEvaluationContext;
import com.landit.landitbe.session.application.port.AiMessageFeedbackEvaluationContextType;
import com.landit.landitbe.session.application.port.AiMessageFeedbackRequest;
import com.landit.landitbe.session.application.port.AiMessageFeedbackResult;
import com.landit.landitbe.session.application.port.AiScenarioContext;
import com.landit.landitbe.session.domain.ProcessingStatus;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RemoteAiConversationClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void requestMessageFeedbackPostsContractAndMapsPreparingResponse() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/api/v1/conversation/message-feedback", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] responseBody = """
                    {
                      "success": true,
                      "data": {
                        "sessionId": 100,
                        "messageId": 200,
                        "feedbackStatus": "PREPARING"
                      },
                      "error": null
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(202, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });

        RemoteAiConversationClient client = new RemoteAiConversationClient(
                objectMapper,
                new AiClientProperties(baseUrl(), "remote", "KOREAN_LEARNER")
        );

        AiMessageFeedbackResult result = client.requestMessageFeedback(new AiMessageFeedbackRequest(
                100L,
                200L,
                1,
                2,
                new AiScenarioContext(
                        10L,
                        "음식에 대한 대화하기",
                        "좋아하는 음식과 최근에 먹은 음식에 대해 이야기합니다.",
                        "내 취향과 경험을 영어로 설명해봅니다.",
                        "friend",
                        "KOREAN_LEARNER"
                ),
                new AiMessageFeedbackEvaluationContext(
                        AiMessageFeedbackEvaluationContextType.AI_MESSAGE,
                        "What food do you like? Why do you like it?",
                        "좋아하는 음식이 있어? 왜 좋아해?"
                ),
                "I like pizza because it is spicy."
        ));

        JsonNode request = objectMapper.readTree(requestBody.get());
        assertThat(request.get("sessionId").asLong()).isEqualTo(100L);
        assertThat(request.get("messageId").asLong()).isEqualTo(200L);
        assertThat(request.get("turnNumber").asInt()).isEqualTo(1);
        assertThat(request.get("messageSequence").asInt()).isEqualTo(2);
        assertThat(request.get("scenario").get("counterpartRole").asText()).isEqualTo("friend");
        assertThat(request.get("evaluationContext").get("type").asText()).isEqualTo("AI_MESSAGE");
        assertThat(request.get("evaluationContext").get("content").asText())
                .isEqualTo("What food do you like? Why do you like it?");
        assertThat(request.get("evaluationContext").get("translatedContent").asText())
                .isEqualTo("좋아하는 음식이 있어? 왜 좋아해?");
        assertThat(request.get("userMessage").asText())
                .isEqualTo("I like pizza because it is spicy.");
        assertThat(result).isEqualTo(new AiMessageFeedbackResult(
                100L,
                200L,
                ProcessingStatus.PREPARING
        ));
    }

    @Test
    void requestMessageFeedbackPostsScenarioOpeningInstructionContext() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/api/v1/conversation/message-feedback", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] responseBody = """
                    {
                      "success": true,
                      "data": {
                        "sessionId": 101,
                        "messageId": 201,
                        "feedbackStatus": "PREPARING"
                      },
                      "error": null
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(202, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });

        RemoteAiConversationClient client = new RemoteAiConversationClient(
                objectMapper,
                new AiClientProperties(baseUrl(), "remote", "KOREAN_LEARNER")
        );

        client.requestMessageFeedback(new AiMessageFeedbackRequest(
                101L,
                201L,
                1,
                1,
                new AiScenarioContext(
                        20L,
                        "카페에서 음료 주문하기",
                        "카페 점원에게 원하는 음료를 주문합니다.",
                        "원하는 음료를 자연스럽고 공손하게 주문합니다.",
                        "cafe staff",
                        "KOREAN_LEARNER"
                ),
                new AiMessageFeedbackEvaluationContext(
                        AiMessageFeedbackEvaluationContextType.SCENARIO_OPENING_INSTRUCTION,
                        "점원에게 먼저 주문하고 싶은 음료를 말해보세요.",
                        null
                ),
                "Can I get an iced americano?"
        ));

        JsonNode request = objectMapper.readTree(requestBody.get());
        assertThat(request.get("messageSequence").asInt()).isEqualTo(1);
        assertThat(request.get("evaluationContext").get("type").asText())
                .isEqualTo("SCENARIO_OPENING_INSTRUCTION");
        assertThat(request.get("evaluationContext").get("content").asText())
                .isEqualTo("점원에게 먼저 주문하고 싶은 음료를 말해보세요.");
        assertThat(request.get("evaluationContext").get("translatedContent").isNull()).isTrue();
        assertThat(request.get("userMessage").asText()).isEqualTo("Can I get an iced americano?");
    }

    @Test
    void requestMessageFeedbackPreservesAiResponseInvalidError() {
        server.createContext("/api/v1/conversation/message-feedback", exchange -> {
            byte[] responseBody = """
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": "AI_RESPONSE_INVALID",
                        "message": "AI 응답 형식이 올바르지 않습니다."
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(502, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });

        RemoteAiConversationClient client = new RemoteAiConversationClient(
                objectMapper,
                new AiClientProperties(baseUrl(), "remote", "KOREAN_LEARNER")
        );

        assertThatThrownBy(() -> client.requestMessageFeedback(aiMessageFeedbackRequest()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));
    }

    @Test
    void requestMessageFeedbackMapsOtherErrorResponseToGenerationFailed() {
        server.createContext("/api/v1/conversation/message-feedback", exchange -> {
            byte[] responseBody = """
                    {
                      "success": false,
                      "data": null,
                      "error": {
                        "code": "AI_GENERATION_FAILED",
                        "message": "AI 응답 생성에 실패했습니다."
                      }
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });

        RemoteAiConversationClient client = new RemoteAiConversationClient(
                objectMapper,
                new AiClientProperties(baseUrl(), "remote", "KOREAN_LEARNER")
        );

        assertThatThrownBy(() -> client.requestMessageFeedback(aiMessageFeedbackRequest()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
    }

    private AiMessageFeedbackRequest aiMessageFeedbackRequest() {
        return new AiMessageFeedbackRequest(
                100L,
                200L,
                1,
                2,
                new AiScenarioContext(
                        10L,
                        "음식에 대한 대화하기",
                        "좋아하는 음식과 최근에 먹은 음식에 대해 이야기합니다.",
                        "내 취향과 경험을 영어로 설명해봅니다.",
                        "friend",
                        "KOREAN_LEARNER"
                ),
                new AiMessageFeedbackEvaluationContext(
                        AiMessageFeedbackEvaluationContextType.AI_MESSAGE,
                        "What food do you like?",
                        "어떤 음식을 좋아해?"
                ),
                "I like pizza."
        );
    }

    private String baseUrl() {
        return "http://localhost:%d/".formatted(server.getAddress().getPort());
    }
}
