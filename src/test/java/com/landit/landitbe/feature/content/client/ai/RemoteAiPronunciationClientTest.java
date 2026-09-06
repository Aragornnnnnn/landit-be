// 원격 AI 발음 분석 클라이언트의 요청·응답 계약을 검증한다.

package com.landit.landitbe.feature.content.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.content.client.ai.dto.AiPronunciationAnalysisRequest;
import com.landit.landitbe.feature.content.client.ai.dto.AiPronunciationJudgedWord;
import com.landit.landitbe.feature.content.client.ai.dto.AiPronunciationWordStatus;
import com.landit.landitbe.feature.content.exception.AiPronunciationResponseInvalidException;
import com.landit.landitbe.feature.content.exception.PronunciationAnalysisFailedException;
import com.landit.landitbe.shared.domain.AccentLocale;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 원격 AI 발음 분석 클라이언트의 요청·응답 계약을 검증한다. */
class RemoteAiPronunciationClientTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
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
  void analyzePostsRequestAndMapsWordJudgements() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    server.createContext(
        "/api/v1/pronunciation/analyze",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] responseBody =
              """
                    {
                      "success": true,
                      "data": {
                        "words": [
                          {"order": 1, "word": "There's", "status": "CORRECT",
                           "startMs": 120, "endMs": 480},
                          {"order": 2, "word": "nothing", "status": "PHONEME_ERROR",
                           "startMs": 500, "endMs": 940, "userDisplay": "nuh·ssing",
                           "errorTargetSpan": "th", "errorUserSpan": "ss"},
                          {"order": 3, "word": "hiking", "status": "STRESS_ERROR",
                           "startMs": 1000, "endMs": 1400, "userStressIndex": 1}
                        ]
                      },
                      "error": null
                    }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    List<AiPronunciationJudgedWord> judgedWordList = remoteClient().analyze(analysisRequest());

    // 요청 본문에 오디오·정답 문장·억양·단어 목록이 실려야 한다.
    JsonNode sentRequest = jsonMapper.readTree(requestBody.get());
    assertThat(sentRequest.path("userAudio").asString()).isEqualTo("YXVkaW8=");
    assertThat(sentRequest.path("userAudioFormat").asString()).isEqualTo("m4a");
    assertThat(sentRequest.path("accentLocale").asString()).isEqualTo("EN_US");
    assertThat(sentRequest.path("words")).hasSize(3);
    // 억양 대조 힌트가 있는 단어는 그대로 실리고, 없는 단어는 null로 나간다 (AI 서버는 생략과 동일 취급).
    assertThat(sentRequest.path("words").get(1).path("accentContrast").path("expected").asString())
        .isEqualTo("sounds like 「nuh·thing」");
    assertThat(sentRequest.path("words").get(0).path("accentContrast").isNull()).isTrue();

    // 응답의 단어별 판정이 그대로 매핑돼야 한다.
    assertThat(judgedWordList).hasSize(3);
    assertThat(judgedWordList.get(0).status()).isEqualTo(AiPronunciationWordStatus.CORRECT);
    assertThat(judgedWordList.get(1).status()).isEqualTo(AiPronunciationWordStatus.PHONEME_ERROR);
    assertThat(judgedWordList.get(1).userDisplay()).isEqualTo("nuh·ssing");
    assertThat(judgedWordList.get(1).errorTargetSpan()).isEqualTo("th");
    assertThat(judgedWordList.get(2).status()).isEqualTo(AiPronunciationWordStatus.STRESS_ERROR);
    assertThat(judgedWordList.get(2).userStressIndex()).isEqualTo(1);
  }

  @Test
  void analyzeMapsServerErrorToPronunciationAnalysisFailed() {
    server.createContext(
        "/api/v1/pronunciation/analyze",
        exchange -> {
          byte[] responseBody =
              "{\"success\":false,\"data\":null,\"error\":{\"code\":\"AI_GENERATION_FAILED\"}}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(503, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    // 도메인 예외 타입과 오류 코드가 함께 맞아야 한다 (예외 파일럿 검증).
    assertThatThrownBy(() -> remoteClient().analyze(analysisRequest()))
        .isInstanceOf(PronunciationAnalysisFailedException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PRONUNCIATION_ANALYSIS_FAILED);
  }

  @Test
  void analyzePropagatesUpstreamInvalidResponseCode() {
    server.createContext(
        "/api/v1/pronunciation/analyze",
        exchange -> {
          byte[] responseBody =
              "{\"success\":false,\"data\":null,\"error\":{\"code\":\"AI_RESPONSE_INVALID\"}}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(502, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    assertThatThrownBy(() -> remoteClient().analyze(analysisRequest()))
        .isInstanceOf(AiPronunciationResponseInvalidException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
  }

  @Test
  void analyzeMapsMalformedSuccessBodyToInvalidResponse() {
    server.createContext(
        "/api/v1/pronunciation/analyze",
        exchange -> {
          byte[] responseBody = "{\"success\":true}".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });

    assertThatThrownBy(() -> remoteClient().analyze(analysisRequest()))
        .isInstanceOf(AiPronunciationResponseInvalidException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
  }

  @Test
  void requestToStringDoesNotContainAudioBase64() {
    // base64 오디오가 로그에 통째로 찍히는 사고를 toString 레벨에서 막는다.
    assertThat(analysisRequest().toString()).doesNotContain("YXVkaW8=").contains("8 bytes base64");
  }

  private RemoteAiPronunciationClient remoteClient() {
    return new RemoteAiPronunciationClient(
        jsonMapper,
        new AiClientProperties(
            "http://localhost:" + server.getAddress().getPort(),
            "remote",
            "KOREAN_LEARNER",
            Duration.ofSeconds(5),
            Duration.ofSeconds(10),
            Duration.ofSeconds(10),
            Duration.ofSeconds(10)));
  }

  private AiPronunciationAnalysisRequest analysisRequest() {
    return new AiPronunciationAnalysisRequest(
        "YXVkaW8=",
        "m4a",
        "There's nothing hiking.",
        "https://cdn.example.com/sentence.mp3",
        AccentLocale.EN_US,
        List.of(
            new AiPronunciationAnalysisRequest.Word(1, "There's", null),
            new AiPronunciationAnalysisRequest.Word(
                2,
                "nothing",
                new AiPronunciationAnalysisRequest.AccentContrast(
                    "sounds like 「nuh·thing」", "sounds like 「nah·ssing」", "PHONEME")),
            new AiPronunciationAnalysisRequest.Word(3, "hiking", null)));
  }
}
