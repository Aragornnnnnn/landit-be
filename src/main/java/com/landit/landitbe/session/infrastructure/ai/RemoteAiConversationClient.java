// 원격 AI 서버의 대화 생성 API를 호출한다.
package com.landit.landitbe.session.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.common.domain.InnerThoughtType;
import com.landit.landitbe.common.exception.ApiException;
import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.session.application.port.AiClosingMessageRequest;
import com.landit.landitbe.session.application.port.AiClosingMessageResult;
import com.landit.landitbe.session.application.port.AiConversationClient;
import com.landit.landitbe.session.application.port.AiNextMessageRequest;
import com.landit.landitbe.session.application.port.AiNextMessageResult;
import com.landit.landitbe.session.domain.GoalCompletionStatus;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "landit.ai", name = "client-mode", havingValue = "remote")
@RequiredArgsConstructor
public class RemoteAiConversationClient implements AiConversationClient {

    private static final String NEXT_MESSAGE_PATH = "/api/v1/conversation/next-message";
    private static final String CLOSING_MESSAGE_PATH = "/api/v1/conversation/closing-message";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final AiClientProperties properties;

    @Override
    public AiNextMessageResult generateNextMessage(AiNextMessageRequest request) {
        return post(nextMessageUri(), request, RemoteNextMessageResponse.class).toResponse();
    }

    @Override
    public AiClosingMessageResult generateClosingMessage(AiClosingMessageRequest request) {
        return post(closingMessageUri(), request, RemoteClosingMessageResponse.class).toResponse();
    }

    private <T> T post(URI uri, Object payload, Class<T> responseType) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload),
                            StandardCharsets.UTF_8
                    ))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
            }
            return readData(response.body(), responseType);
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private <T> T readData(String responseBody, Class<T> responseType) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (!root.path("success").asBoolean(false) || root.get("data") == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
            }
            return objectMapper.treeToValue(root.get("data"), responseType);
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
    }

    private URI nextMessageUri() {
        return aiBaseUri().resolve(NEXT_MESSAGE_PATH);
    }

    private URI closingMessageUri() {
        return aiBaseUri().resolve(CLOSING_MESSAGE_PATH);
    }

    private URI aiBaseUri() {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
        }
        return URI.create(properties.baseUrl());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteNextMessageResponse(
            String aiMessage,
            String translatedMessage,
            String innerThought,
            InnerThoughtType innerThoughtType,
            GoalCompletionStatus goalCompletionStatus
    ) {

        private AiNextMessageResult toResponse() {
            if (blank(aiMessage)
                    || blank(translatedMessage)
                    || blank(innerThought)
                    || innerThoughtType == null
                    || goalCompletionStatus == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
            }
            return new AiNextMessageResult(
                    aiMessage,
                    translatedMessage,
                    innerThought,
                    innerThoughtType,
                    goalCompletionStatus
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteClosingMessageResponse(
            String aiMessage,
            String translatedMessage,
            String innerThought,
            InnerThoughtType innerThoughtType
    ) {

        private AiClosingMessageResult toResponse() {
            if (blank(aiMessage)
                    || blank(translatedMessage)
                    || blank(innerThought)
                    || innerThoughtType == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
            }
            return new AiClosingMessageResult(
                    aiMessage,
                    translatedMessage,
                    innerThought,
                    innerThoughtType
            );
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
