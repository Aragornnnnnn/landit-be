// 원격 AI 서버의 프리톡 생성 API를 호출한다.

package com.landit.landitbe.feature.session.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.session.domain.CharacterEmotion;
import com.landit.landitbe.feature.session.domain.FreeTalkExpressionSourceType;
import com.landit.landitbe.shared.domain.InnerThoughtType;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 원격 AI 서버의 프리톡 생성 API를 호출한다. */
@Component
@ConditionalOnProperty(prefix = "landit.ai", name = "client-mode", havingValue = "remote")
public class RemoteAiFreeTalkClient implements AiFreeTalkClient {

  private static final String OPENING_PATH = "/api/v1/free-talk/opening";
  private static final String TURN_PATH = "/api/v1/free-talk/turn";
  private static final String INNER_THOUGHT_PATH = "/api/v1/free-talk/inner-thought";
  private static final String CLOSING_PATH = "/api/v1/free-talk/closing";
  private static final String EXPRESSION_RECOMMENDATIONS_PATH =
      "/api/v1/free-talk/expression-recommendations";
  private static final String EXPRESSION_LEARNING_CONTENT_PATH =
      "/api/v1/free-talk/expression-learning-content";

  private final HttpClient httpClient;
  private final JsonMapper jsonMapper;
  private final AiClientProperties properties;

  /**
   * JSON 변환기와 AI 서버 설정으로 원격 프리톡 클라이언트를 구성한다.
   *
   * @param jsonMapper AI 요청과 응답 JSON 변환기
   * @param properties AI 서버 연결 설정
   */
  public RemoteAiFreeTalkClient(JsonMapper jsonMapper, AiClientProperties properties) {
    this.jsonMapper = jsonMapper;
    this.properties = properties;
    this.httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkOpeningResult generateOpening(AiFreeTalkOpeningRequest request) {
    return post(OPENING_PATH, request, RemoteOpeningResponse.class).toResult();
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkTurnResult generateTurn(AiFreeTalkTurnRequest request) {
    return post(TURN_PATH, request, RemoteTurnResponse.class).toResult();
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkInnerThoughtResult generateInnerThought(AiFreeTalkInnerThoughtRequest request) {
    return post(INNER_THOUGHT_PATH, request, RemoteInnerThoughtResponse.class).toResult();
  }

  /** {@inheritDoc} */
  @Override
  public AiFreeTalkClosingResult generateClosing(AiFreeTalkClosingRequest request) {
    return post(CLOSING_PATH, request, RemoteClosingResponse.class).toResult();
  }

  @Override
  public AiFreeTalkExpressionRecommendationsResult recommendExpressions(
      AiFreeTalkExpressionRecommendationsRequest request) {
    return post(
            EXPRESSION_RECOMMENDATIONS_PATH, request, RemoteExpressionRecommendationsResponse.class)
        .toResult(request);
  }

  @Override
  public AiFreeTalkExpressionLearningContentResult generateExpressionLearningContent(
      AiFreeTalkExpressionLearningContentRequest request) {
    return post(
            EXPRESSION_LEARNING_CONTENT_PATH,
            request,
            RemoteExpressionLearningContentResponse.class)
        .toResult(request);
  }

  private <T> T post(String path, Object payload, Class<T> responseType) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(aiUri(path))
              .version(HttpClient.Version.HTTP_1_1)
              .header("Accept", "application/json")
              .header("Content-Type", "application/json")
              .timeout(properties.requestTimeout())
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      jsonMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw toApiException(response.statusCode(), response.body());
      }
      return readData(response.body(), responseType);
    } catch (ApiException exception) {
      throw exception;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
    }
  }

  private ApiException toApiException(int statusCode, String responseBody) {
    try {
      JsonNode root = jsonMapper.readTree(responseBody);
      if (statusCode == 502
          && root != null
          && ErrorCode.AI_RESPONSE_INVALID
              .name()
              .equals(root.path("error").path("code").asString())) {
        return new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
    } catch (JacksonException ignored) {
      // 오류 본문을 해석할 수 없으면 외부 AI 호출 실패로 처리한다.
    }
    return new ApiException(ErrorCode.AI_GENERATION_FAILED);
  }

  private <T> T readData(String responseBody, Class<T> responseType) {
    try {
      JsonNode root = jsonMapper.readTree(responseBody);
      JsonNode data = root.get("data");
      if (!root.path("success").asBoolean(false) || data == null || data.isNull()) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return jsonMapper.treeToValue(data, responseType);
    } catch (ApiException exception) {
      throw exception;
    } catch (JacksonException exception) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
  }

  private URI aiUri(String path) {
    if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
      throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
    }
    return URI.create(properties.baseUrl()).resolve(path);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteOpeningResponse(
      String aiMessage, String translatedMessage, CharacterEmotion emotion) {

    private AiFreeTalkOpeningResult toResult() {
      if (blank(aiMessage) || blank(translatedMessage) || emotion == null) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkOpeningResult(aiMessage, translatedMessage, emotion);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteTurnResponse(
      Boolean userExitIntentDetected,
      String inferredTitle,
      String aiMessage,
      String translatedMessage,
      CharacterEmotion emotion) {

    private AiFreeTalkTurnResult toResult() {
      if (userExitIntentDetected == null
          || (!userExitIntentDetected && hasMissingGeneratedField())) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      if ((userExitIntentDetected && hasGeneratedField())
          || (inferredTitle != null && blank(inferredTitle))) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkTurnResult(
          userExitIntentDetected, inferredTitle, aiMessage, translatedMessage, emotion);
    }

    private boolean hasMissingGeneratedField() {
      return blank(aiMessage) || blank(translatedMessage) || emotion == null;
    }

    private boolean hasGeneratedField() {
      return aiMessage != null || translatedMessage != null || emotion != null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteInnerThoughtResponse(
      String innerThought, InnerThoughtType innerThoughtType) {

    private AiFreeTalkInnerThoughtResult toResult() {
      if (blank(innerThought) || innerThoughtType == null) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkInnerThoughtResult(innerThought, innerThoughtType);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteClosingResponse(
      String aiMessage, String translatedMessage, CharacterEmotion emotion) {

    private AiFreeTalkClosingResult toResult() {
      if (blank(aiMessage) || blank(translatedMessage) || emotion == null) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkClosingResult(aiMessage, translatedMessage, emotion);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteExpressionRecommendationsResponse(
      List<AiFreeTalkExpressionRecommendation> recommendations) {

    private AiFreeTalkExpressionRecommendationsResult toResult(
        AiFreeTalkExpressionRecommendationsRequest request) {
      if (recommendations == null
          || recommendations.isEmpty()
          || recommendations.size() > 3
          || request.existingExpressions() == null
          || hasInvalidRecommendation(recommendations, request.existingExpressions())) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkExpressionRecommendationsResult(recommendations);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteExpressionLearningContentResponse(
      List<AiFreeTalkExpressionLearningContent> expressions) {

    private AiFreeTalkExpressionLearningContentResult toResult(
        AiFreeTalkExpressionLearningContentRequest request) {
      if (expressions == null
          || expressions.isEmpty()
          || request.expressions() == null
          || expressions.size() != request.expressions().size()
          || hasInvalidLearningContent(expressions, request.expressions())) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkExpressionLearningContentResult(expressions);
    }
  }

  private static boolean hasInvalidRecommendation(
      List<AiFreeTalkExpressionRecommendation> recommendations,
      List<AiFreeTalkExistingExpression> existingExpressions) {
    for (int index = 0; index < recommendations.size(); index++) {
      if (invalidRecommendation(recommendations.get(index), existingExpressions, index + 1)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasInvalidLearningContent(
      List<AiFreeTalkExpressionLearningContent> expressions,
      List<AiFreeTalkLearningExpression> requestedExpressions) {
    for (int index = 0; index < expressions.size(); index++) {
      if (invalidLearningContent(expressions.get(index), requestedExpressions.get(index))) {
        return true;
      }
    }
    return false;
  }

  private static boolean invalidRecommendation(
      AiFreeTalkExpressionRecommendation recommendation,
      List<AiFreeTalkExistingExpression> existingExpressions,
      int expectedDisplayOrder) {
    if (recommendation == null
        || recommendation.displayOrder() != expectedDisplayOrder
        || recommendation.sourceType() == null
        || blank(recommendation.targetExpressionText())
        || blank(recommendation.baseExpressionMeaningText())
        || blank(recommendation.usageSummary())
        || invalidContextualExample(recommendation.contextualExample())) {
      return true;
    }
    if (recommendation.sourceType() == FreeTalkExpressionSourceType.NEW) {
      return recommendation.existingExpressionId() != null;
    }
    return recommendation.existingExpressionId() == null
        || existingExpressions.stream()
            .noneMatch(
                expression ->
                    expression.expressionId().equals(recommendation.existingExpressionId()));
  }

  private static boolean invalidLearningContent(
      AiFreeTalkExpressionLearningContent content,
      AiFreeTalkLearningExpression requestedExpression) {
    return content == null
        || requestedExpression == null
        || blank(content.targetExpressionText())
        || blank(content.baseExpressionMeaningText())
        || blank(content.usageSummary())
        || blank(requestedExpression.targetExpressionText())
        || blank(requestedExpression.baseExpressionMeaningText())
        || blank(requestedExpression.usageSummary())
        || !requestedExpression.targetExpressionText().equals(content.targetExpressionText())
        || !requestedExpression
            .baseExpressionMeaningText()
            .equals(content.baseExpressionMeaningText())
        || !requestedExpression.usageSummary().equals(content.usageSummary());
  }

  private static boolean invalidContextualExample(AiFreeTalkContextualExample contextualExample) {
    return contextualExample == null
        || blank(contextualExample.sentenceText())
        || blank(contextualExample.sentenceTranslation());
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
