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
import com.landit.landitbe.session.application.port.AiMessageFeedbackRequest;
import com.landit.landitbe.session.application.port.AiMessageFeedbackResult;
import com.landit.landitbe.session.application.port.AiNextMessageRequest;
import com.landit.landitbe.session.application.port.AiNextMessageResult;
import com.landit.landitbe.session.application.port.AiSessionFeedbackRequest;
import com.landit.landitbe.session.application.port.AiSessionFeedbackResult;
import com.landit.landitbe.session.application.port.AiSessionMessageFeedbackResult;
import com.landit.landitbe.session.domain.GoalCompletionStatus;
import com.landit.landitbe.session.domain.ProcessingStatus;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "landit.ai", name = "client-mode", havingValue = "remote")
@RequiredArgsConstructor
public class RemoteAiConversationClient implements AiConversationClient {

    private static final String NEXT_MESSAGE_PATH = "/api/v1/conversation/next-message";
    private static final String CLOSING_MESSAGE_PATH = "/api/v1/conversation/closing-message";
    private static final String MESSAGE_FEEDBACK_PATH = "/api/v1/conversation/message-feedback";
    private static final String SESSION_FEEDBACK_PATH = "/api/v1/conversation/session-feedback";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final AiClientProperties properties;

    @Override
    public AiNextMessageResult generateNextMessage(AiNextMessageRequest request) {
        return post(
                nextMessageUri(),
                request,
                RemoteNextMessageResponse.class,
                ErrorCode.AI_GENERATION_FAILED
        ).toResponse();
    }

    @Override
    public AiClosingMessageResult generateClosingMessage(AiClosingMessageRequest request) {
        return post(
                closingMessageUri(),
                request,
                RemoteClosingMessageResponse.class,
                ErrorCode.AI_GENERATION_FAILED
        ).toResponse();
    }

    @Override
    public AiMessageFeedbackResult requestMessageFeedback(AiMessageFeedbackRequest request) {
        return post(
                messageFeedbackUri(),
                request,
                RemoteMessageFeedbackResponse.class,
                ErrorCode.AI_GENERATION_FAILED
        ).toResult();
    }

    /** AI 서버에 세션 단위 최종 피드백 생성을 요청하고 FE 저장용 결과로 변환한다. */
    @Override
    public AiSessionFeedbackResult generateSessionFeedback(AiSessionFeedbackRequest request) {
        return post(
                sessionFeedbackUri(),
                request,
                RemoteSessionFeedbackResponse.class,
                ErrorCode.FEEDBACK_GENERATION_FAILED
        ).toResult();
    }

    private <T> T post(URI uri, Object payload, Class<T> responseType, ErrorCode fallbackErrorCode) {
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
                throw toApiException(response.body(), fallbackErrorCode);
            }
            return readData(response.body(), responseType);
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(fallbackErrorCode);
        } catch (IOException exception) {
            throw new ApiException(fallbackErrorCode);
        }
    }

    /** AI 서버 오류 응답에서 공개할 수 있는 오류 코드만 선별해 변환한다. */
    private ApiException toApiException(String responseBody, ErrorCode fallbackErrorCode) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root != null) {
                String upstreamErrorCode = root.path("error").path("code").asText();
                if (ErrorCode.AI_RESPONSE_INVALID.name().equals(upstreamErrorCode)) {
                    return new ApiException(ErrorCode.AI_RESPONSE_INVALID);
                }
                if (fallbackErrorCode == ErrorCode.FEEDBACK_GENERATION_FAILED
                        && "MESSAGE_FEEDBACK_NOT_READY".equals(upstreamErrorCode)) {
                    return new ApiException(ErrorCode.FEEDBACK_NOT_READY);
                }
            }
        } catch (IOException ignored) {
            // 오류 본문을 해석할 수 없으면 외부 AI 호출 실패로 처리한다.
        }
        return new ApiException(fallbackErrorCode);
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
        return aiBaseUri(ErrorCode.AI_GENERATION_FAILED).resolve(NEXT_MESSAGE_PATH);
    }

    private URI closingMessageUri() {
        return aiBaseUri(ErrorCode.AI_GENERATION_FAILED).resolve(CLOSING_MESSAGE_PATH);
    }

    private URI messageFeedbackUri() {
        return aiBaseUri(ErrorCode.AI_GENERATION_FAILED).resolve(MESSAGE_FEEDBACK_PATH);
    }

    private URI sessionFeedbackUri() {
        return aiBaseUri(ErrorCode.FEEDBACK_GENERATION_FAILED).resolve(SESSION_FEEDBACK_PATH);
    }

    private URI aiBaseUri(ErrorCode fallbackErrorCode) {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new ApiException(fallbackErrorCode);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteMessageFeedbackResponse(
            Long sessionId,
            Long messageId,
            ProcessingStatus feedbackStatus
    ) {

        private AiMessageFeedbackResult toResult() {
            if (sessionId == null || messageId == null || feedbackStatus == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
            }
            return new AiMessageFeedbackResult(sessionId, messageId, feedbackStatus);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteSessionFeedbackResponse(
            Long sessionId,
            Integer nativeScore,
            BigDecimal starRating,
            String highlightMessage,
            String summaryMessage,
            List<AiSessionMessageFeedbackResult> messageFeedbacks
    ) {

        /** 응답의 최상위 필수 필드를 확인한 뒤 애플리케이션 포트 결과로 변환한다. */
        private AiSessionFeedbackResult toResult() {
            if (sessionId == null
                    || nativeScore == null
                    || starRating == null
                    || blank(highlightMessage)
                    || blank(summaryMessage)
                    || messageFeedbacks == null) {
                throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
            }
            return new AiSessionFeedbackResult(
                    sessionId,
                    nativeScore,
                    starRating,
                    highlightMessage,
                    summaryMessage,
                    messageFeedbacks
            );
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
