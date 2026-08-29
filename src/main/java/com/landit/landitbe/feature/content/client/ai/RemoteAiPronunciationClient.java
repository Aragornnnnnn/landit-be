// 원격 AI 서버의 발음 분석 API를 호출한다.

package com.landit.landitbe.feature.content.client.ai;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.content.client.ai.dto.AiPronunciationAnalysisRequest;
import com.landit.landitbe.feature.content.client.ai.dto.AiPronunciationJudgedWord;
import com.landit.landitbe.feature.content.exception.AiPronunciationResponseInvalidException;
import com.landit.landitbe.feature.content.exception.InvalidAudioException;
import com.landit.landitbe.feature.content.exception.PronunciationAnalysisFailedException;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
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
  public List<AiPronunciationJudgedWord> analyze(AiPronunciationAnalysisRequest request) {
    // base-url 미설정은 다른 Remote 클라이언트들처럼 호출 실패(502)로 처리한다.
    if (StringUtils.isBlank(properties.baseUrl())) {
      throw new PronunciationAnalysisFailedException("AI 서버 주소가 설정되지 않았습니다.");
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
      if (!HttpStatusCode.valueOf(response.statusCode()).is2xxSuccessful()) {
        throw toUpstreamException(response.body());
      }
      return readJudgedWords(response.body());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new PronunciationAnalysisFailedException("AI 서버 응답 대기가 중단됐습니다.");
    } catch (IOException exception) {
      throw new PronunciationAnalysisFailedException("AI 서버 호출에 실패했습니다.");
    }
  }

  /**
   * AI 서버 오류 응답에서 공개할 수 있는 오류 코드만 선별해 도메인 예외로 변환한다.
   *
   * @param responseBody 오류 응답 본문
   * @return 통과 대상 코드는 그대로, 그 외는 분석 실패 예외
   */
  private ApiException toUpstreamException(String responseBody) {
    try {
      JsonNode root = jsonMapper.readTree(responseBody);
      if (root != null) {
        String upstreamErrorCode = root.path("error").path("code").asString();
        if (ErrorCode.AI_RESPONSE_INVALID.name().equals(upstreamErrorCode)) {
          return new AiPronunciationResponseInvalidException();
        }
        // AI 서버의 오디오 검증(길이 30초 등)에 걸린 경우는 사용자 입력 문제로 그대로 전달한다.
        if (ErrorCode.INVALID_AUDIO.name().equals(upstreamErrorCode)) {
          return new InvalidAudioException();
        }
      }
    } catch (JacksonException ignored) {
      // 오류 본문을 해석할 수 없으면 PRONUNCIATION_ANALYSIS_FAILED로 처리한다.
    }
    return new PronunciationAnalysisFailedException();
  }

  /**
   * 공통 응답 형식(success/data)에서 단어별 판정 목록을 꺼낸다.
   *
   * @param responseBody 성공 응답 본문
   * @return 단어별 판정 목록
   */
  private List<AiPronunciationJudgedWord> readJudgedWords(String responseBody) {
    try {
      JsonNode root = jsonMapper.readTree(responseBody);
      if (!root.path("success").asBoolean(false) || root.get("data") == null) {
        throw new AiPronunciationResponseInvalidException();
      }
      WordsPayload payload = jsonMapper.treeToValue(root.get("data"), WordsPayload.class);
      if (payload.words() == null) {
        throw new AiPronunciationResponseInvalidException();
      }
      return payload.words();
    } catch (ApiException exception) {
      throw exception;
    } catch (JacksonException exception) {
      throw new AiPronunciationResponseInvalidException();
    }
  }

  /**
   * 응답 {@code data} 노드의 역직렬화 전용 틀이다. HTTP JSON 계약({@code data.words})은 그대로 두고, 코드에서는 목록만 쓰기 위해
   * 클라이언트 내부에서만 사용한다.
   *
   * @param words 단어별 판정 목록
   */
  private record WordsPayload(List<AiPronunciationJudgedWord> words) {}
}
