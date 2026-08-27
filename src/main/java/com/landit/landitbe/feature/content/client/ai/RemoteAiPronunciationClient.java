// 원격 AI 서버의 발음 분석 API를 호출한다.

package com.landit.landitbe.feature.content.client.ai;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 원격 AI 서버의 발음 분석 API를 호출한다. */
@Component
@ConditionalOnProperty(prefix = "landit.ai", name = "client-mode", havingValue = "remote")
public class RemoteAiPronunciationClient implements AiPronunciationClient {

  private static final String ANALYZE_PATH = "/api/v1/pronunciation/analyze";

  private final HttpClient httpClient;
  private final JsonMapper jsonMapper;
  private final AiClientProperties properties;

  /**
   * JSON 변환기와 AI 서버 설정으로 원격 클라이언트를 구성한다.
   *
   * @param jsonMapper AI 요청과 응답 JSON 변환기
   * @param properties AI 서버 연결 설정
   */
  public RemoteAiPronunciationClient(JsonMapper jsonMapper, AiClientProperties properties) {
    this.jsonMapper = jsonMapper;
    this.properties = properties;
    this.httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
  }

  /** {@inheritDoc} */
  @Override
  public AiPronunciationAnalysisResult analyze(AiPronunciationAnalysisRequest request) {
    // base-url 설정이 비면 URI 조립 단계에서 500으로 새므로, 다른 Remote 클라이언트들처럼
    // 여기서 걸러 호출 실패(502)로 응답한다. resolve()는 끝 슬래시 유무도 흡수한다.
    if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
      throw new ApiException(ErrorCode.PRONUNCIATION_ANALYSIS_FAILED);
    }
    URI uri = URI.create(properties.baseUrl()).resolve(ANALYZE_PATH);
    try {
      HttpRequest httpRequest =
          HttpRequest.newBuilder(uri)
              .version(HttpClient.Version.HTTP_1_1)
              .header("Accept", "application/json")
              .header("Content-Type", "application/json")
              .timeout(properties.pronunciationRequestTimeout())
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      jsonMapper.writeValueAsString(request), StandardCharsets.UTF_8))
              .build();
      HttpResponse<String> response =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw toApiException(response.body());
      }
      return readData(response.body());
    } catch (ApiException exception) {
      throw exception;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ApiException(ErrorCode.PRONUNCIATION_ANALYSIS_FAILED);
    } catch (IOException exception) {
      throw new ApiException(ErrorCode.PRONUNCIATION_ANALYSIS_FAILED);
    }
  }

  /** AI 서버 오류 응답에서 공개할 수 있는 오류 코드만 선별해 변환한다. */
  private ApiException toApiException(String responseBody) {
    try {
      JsonNode root = jsonMapper.readTree(responseBody);
      if (root != null) {
        String upstreamErrorCode = root.path("error").path("code").asString();
        if (ErrorCode.AI_RESPONSE_INVALID.name().equals(upstreamErrorCode)) {
          return new ApiException(ErrorCode.AI_RESPONSE_INVALID);
        }
        // AI 서버의 오디오 검증(길이 30초 등)에 걸린 경우는 사용자 입력 문제로 그대로 전달한다.
        if (ErrorCode.INVALID_AUDIO.name().equals(upstreamErrorCode)) {
          return new ApiException(ErrorCode.INVALID_AUDIO);
        }
      }
    } catch (JacksonException ignored) {
      // 오류 본문을 해석할 수 없으면 PRONUNCIATION_ANALYSIS_FAILED로 처리한다.
    }
    return new ApiException(ErrorCode.PRONUNCIATION_ANALYSIS_FAILED);
  }

  // 공통 응답 형식(success/data)에서 판정 결과를 꺼낸다.
  private AiPronunciationAnalysisResult readData(String responseBody) {
    try {
      JsonNode root = jsonMapper.readTree(responseBody);
      if (!root.path("success").asBoolean(false) || root.get("data") == null) {
        throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
      }
      return jsonMapper.treeToValue(root.get("data"), AiPronunciationAnalysisResult.class);
    } catch (ApiException exception) {
      throw exception;
    } catch (JacksonException exception) {
      throw new ApiException(ErrorCode.AI_RESPONSE_INVALID);
    }
  }
}
