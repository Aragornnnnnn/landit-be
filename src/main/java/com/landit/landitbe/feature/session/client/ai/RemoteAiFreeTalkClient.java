// 원격 AI 서버의 프리톡 생성 API를 호출한다.

package com.landit.landitbe.feature.session.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.session.domain.CharacterEmotion;
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
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 원격 AI 서버의 프리톡 생성 API를 호출한다. */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "landit.ai", name = "client-mode", havingValue = "remote")
public class RemoteAiFreeTalkClient implements AiFreeTalkClient {

  private static final String OPENING_PATH = "/api/v1/free-talk/opening";
  private static final String TURN_PATH = "/api/v1/free-talk/turn";
  private static final String INNER_THOUGHT_PATH = "/api/v1/free-talk/inner-thought";
  private static final String CLOSING_PATH = "/api/v1/free-talk/closing";
  private static final String EXPRESSION_RECOMMENDATIONS_PATH =
      "/api/v1/free-talk/expression-recommendations";
  private static final String CONVERSATION_EMBEDDINGS_PATH =
      "/api/v1/free-talk/conversation-embeddings";
  private static final int MAX_CONVERSATION_EXCERPTS = 4;
  private static final String AI_CALL_ELAPSED_LOG = "AI 호출 소요 시간. path={}, elapsedMs={}";

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

  /**
   * 프리톡 대화에서 학습할 표현을 추천한다.
   *
   * @param request 표현 추천 요청
   * @return 검증된 표현 추천 결과
   * @throws ApiException 원격 AI 호출 또는 응답 검증에 실패했을 때
   */
  @Override
  public AiFreeTalkExpressionRecommendationsResult recommendExpressions(
      AiFreeTalkExpressionRecommendationsRequest request) {
    return post(
            EXPRESSION_RECOMMENDATIONS_PATH, request, RemoteExpressionRecommendationsResponse.class)
        .toResult(request);
  }

  /**
   * 완료된 프리톡 대화에서 핵심 사용자 발화를 추출하고 임베딩한다.
   *
   * @param request 대화 임베딩 요청
   * @return 검증된 핵심 발화와 임베딩 목록
   * @throws ApiException 원격 AI 호출 또는 응답 검증에 실패했을 때
   */
  @Override
  public AiConversationEmbeddingsResult extractConversationEmbeddings(
      AiConversationEmbeddingsRequest request) {
    return post(CONVERSATION_EMBEDDINGS_PATH, request, RemoteConversationEmbeddingsResponse.class)
        .toResult();
  }

  private <T> T post(String path, Object payload, Class<T> responseType) {
    long startNanos = System.nanoTime();
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
    } finally {
      // 성공과 실패를 가리지 않고 왕복 시간을 남겨 지연 구간을 특정한다.
      log.info(
          AI_CALL_ELAPSED_LOG, path, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
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

    // 원격 첫 발화 응답을 검증해 애플리케이션 결과로 변환한다.
    private AiFreeTalkOpeningResult toResult() {
      if (blank(aiMessage) || blank(translatedMessage)) {
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

    // 원격 후속 발화 응답을 검증해 애플리케이션 결과로 변환한다.
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
      return blank(aiMessage) || blank(translatedMessage);
    }

    private boolean hasGeneratedField() {
      return aiMessage != null || translatedMessage != null || emotion != null;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteInnerThoughtResponse(
      String innerThought, InnerThoughtType innerThoughtType) {

    // 원격 속마음 응답을 검증해 애플리케이션 결과로 변환한다.
    private AiFreeTalkInnerThoughtResult toResult() {
      if (blank(innerThought) || innerThoughtType == null) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiFreeTalkInnerThoughtResult(innerThought, innerThoughtType);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteClosingResponse(
      String inferredTitle, String aiMessage, String translatedMessage, CharacterEmotion emotion) {

    // 원격 마무리 응답을 검증해 애플리케이션 결과로 변환한다.
    private AiFreeTalkClosingResult toResult() {
      if (blank(aiMessage) || blank(translatedMessage)) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      String normalizedTitle = blank(inferredTitle) ? null : inferredTitle;
      return new AiFreeTalkClosingResult(normalizedTitle, aiMessage, translatedMessage, emotion);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteExpressionRecommendationsResponse(
      List<AiFreeTalkExpressionRecommendation> recommendations) {

    // 원격 표현 추천 응답을 검증해 애플리케이션 결과로 변환한다.
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

  // 추천 목록에 순서나 출처가 잘못된 표현이 있는지 확인한다.
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

  // 개별 추천 표현의 필수 값과 기존 표현 참조를 검증한다.
  private static boolean invalidRecommendation(
      AiFreeTalkExpressionRecommendation recommendation,
      List<AiFreeTalkExistingExpression> existingExpressions,
      int expectedDisplayOrder) {
    if (recommendation == null
        || recommendation.displayOrder() != expectedDisplayOrder
        || recommendation.existingExpressionId() == null) {
      return true;
    }
    return existingExpressions.stream()
        .noneMatch(
            expression -> expression.expressionId().equals(recommendation.existingExpressionId()));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record RemoteConversationEmbeddingsResponse(List<AiConversationExcerpt> excerpts) {

    // 원격 대화 임베딩 응답을 검증해 애플리케이션 결과로 변환한다.
    private AiConversationEmbeddingsResult toResult() {
      if (excerpts == null
          || excerpts.isEmpty()
          || excerpts.size() > MAX_CONVERSATION_EXCERPTS
          || excerpts.stream().anyMatch(RemoteConversationEmbeddingsResponse::invalidExcerpt)) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return new AiConversationEmbeddingsResult(excerpts);
    }

    // 추출 발화의 필수 값과 임베딩 차원을 검증한다.
    private static boolean invalidExcerpt(AiConversationExcerpt excerpt) {
      return excerpt == null
          || blank(excerpt.excerptText())
          || excerpt.embedding() == null
          || excerpt.embedding().size() != AiConversationExcerpt.EMBEDDING_DIMENSION
          || excerpt.embedding().contains(null);
    }
  }

  // 선택 질문의 원문과 번역이 함께 제공됐는지 검증한다.
  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
