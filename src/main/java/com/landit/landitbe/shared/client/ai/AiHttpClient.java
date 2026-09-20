// AI 서버의 공통 HTTP 전송·응답 봉투와 오류 변환을 처리한다.

package com.landit.landitbe.shared.client.ai;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** AI 서버의 공통 HTTP 전송·응답 봉투와 오류 변환을 처리한다. */
@Slf4j
public class AiHttpClient {

  private static final String AI_CALL_ELAPSED_LOG = "AI 호출 소요 시간. path={}, elapsedMs={}";

  private final HttpClient httpClient;
  private final JsonMapper jsonMapper;
  private final AiClientProperties properties;

  /**
   * JSON 변환기와 AI 서버 설정으로 공통 AI HTTP 클라이언트를 구성한다.
   *
   * @param jsonMapper AI 요청과 응답 JSON 변환기
   * @param properties AI 서버 연결 설정
   */
  public AiHttpClient(JsonMapper jsonMapper, AiClientProperties properties) {
    this.jsonMapper = jsonMapper;
    this.properties = properties;
    this.httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
  }

  /**
   * 공통 AI JSON 계약으로 요청하고 응답 data를 변환한다.
   *
   * @param <T> 응답 data 타입
   * @param path API 경로
   * @param payload 요청 값
   * @param responseType 응답 클래스
   * @return 변환한 응답 data
   * @throws ApiException 외부 호출 또는 응답 형식이 잘못됐을 때
   */
  public <T> T post(String path, Object payload, Class<T> responseType) {
    return post(path, payload, responseType, properties.requestTimeout());
  }

  /**
   * 지정한 응답 제한 시간으로 AI 요청을 전송한다.
   *
   * @param <T> 응답 data 타입
   * @param path API 경로
   * @param payload 요청 값
   * @param responseType 응답 클래스
   * @param requestTimeout 응답 제한 시간
   * @return 변환한 응답 data
   * @throws ApiException 외부 호출 또는 응답 형식이 잘못됐을 때
   */
  public <T> T post(String path, Object payload, Class<T> responseType, Duration requestTimeout) {
    long startNanos = System.nanoTime();
    try {
      HttpRequest request =
          properties
              .authorize(HttpRequest.newBuilder(aiUri(path)))
              .version(HttpClient.Version.HTTP_1_1)
              .header("Accept", "application/json")
              .header("Content-Type", "application/json")
              .timeout(requestTimeout)
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
      if (statusCode == 400 && root != null) {
        String code = root.path("error").path("code").asString();
        if (ErrorCode.FREE_TALK_CONTEXT_TOO_LARGE.name().equals(code)) {
          return new ApiException(ErrorCode.FREE_TALK_CONTEXT_TOO_LARGE);
        }
        if (ErrorCode.FREE_TALK_SUMMARY_INPUT_TOO_LARGE.name().equals(code)) {
          return new ApiException(ErrorCode.FREE_TALK_SUMMARY_INPUT_TOO_LARGE);
        }
      }
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
}
