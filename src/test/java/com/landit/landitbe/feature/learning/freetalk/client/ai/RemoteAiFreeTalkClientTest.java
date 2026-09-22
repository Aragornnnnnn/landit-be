// 원격 프리톡 AI 클라이언트의 HTTP 계약과 오류 변환을 검증한다.

package com.landit.landitbe.feature.learning.freetalk.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.config.ai.AiClientProperties;
import com.landit.landitbe.feature.learning.conversation.client.ai.AiConversationHistoryMessage;
import com.landit.landitbe.feature.learning.conversation.domain.CharacterEmotion;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExistingExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsRequest;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkExpressionRecommendationsResult;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkLearnedExpression;
import com.landit.landitbe.feature.learning.freetalk.expression.client.ai.AiFreeTalkUsedExpression;
import com.landit.landitbe.feature.learning.freetalk.feedback.domain.FreeTalkMistakePattern;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkPatternUsageDraft;
import com.landit.landitbe.feature.learning.freetalk.feedback.dto.FreeTalkTurnCorrection;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtRequest;
import com.landit.landitbe.feature.learning.freetalk.innerthought.client.ai.AiFreeTalkInnerThoughtResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingReason;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingRequest;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkClosingResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkOpeningRequest;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkOpeningResult;
import com.landit.landitbe.feature.learning.freetalk.message.client.ai.AiFreeTalkTurnRequest;
import com.landit.landitbe.feature.learning.freetalk.topic.client.ai.AiFreeTalkTopic;
import com.landit.landitbe.feature.memory.client.ai.AiFreeTalkMemoryContext;
import com.landit.landitbe.feature.memory.domain.ConversationMemoryType;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 원격 프리톡 AI 클라이언트의 HTTP 계약과 오류 변환을 검증한다. */
class RemoteAiFreeTalkClientTest {

  private static final String EXPRESSION_TEXT = "I'm up for that";
  private static final String EXPRESSION_MEANING = "좋아, 그거 하자";
  private static final String EXPRESSION_USAGE = "제안에 동의할 때 사용";

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

  @DisplayName("프리톡 시작 요청의 주제와 이름 제외 계약을 전송하고 감정을 변환한다.")
  @Test
  void postsOpeningContractAndMapsResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        requests,
        """
            {
              "success": true,
              "data": {
                "aiMessage": "How was your weekend?",
                "translatedMessage": "주말 어땠어?",
                "emotion": "HAPPY"
              },
              "error": null
            }
        """);

    AiFreeTalkOpeningResult opening = remoteClient().generateOpening(openingRequest());

    assertThat(requests.get("/api/v1/free-talk/opening").get("topic").get("topicId").asLong())
        .isEqualTo(2L);
    assertThat(requests.get("/api/v1/free-talk/opening").has("partnerDisplayName")).isFalse();
    assertThat(opening.emotion()).isEqualTo(CharacterEmotion.HAPPY);
  }

  @DisplayName("프리톡 발화 요청은 응답 모드를 포함하고 파트너 이름을 제외한다.")
  @Test
  void postsTurnContractAndMapsResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/turn",
        requests,
        """
            {
              "success": true,
              "data": {
                "userExitIntentDetected": false,
                "inferredTitle": "주말 이야기",
                "aiMessage": "That sounds fun.",
                "translatedMessage": "재밌겠다.",
                "emotion": "HAPPY",
                "innerThought": "즐거웠나 보다.",
                "innerThoughtType": "GOOD"
              },
              "error": null
            }
        """);

    remoteClient().generateTurn(turnRequest());

    assertThat(requests.get("/api/v1/free-talk/turn").get("responseMode").asString())
        .isEqualTo("NORMAL");
    assertThat(requests.get("/api/v1/free-talk/turn").has("partnerDisplayName")).isFalse();
  }

  @DisplayName("프리톡 속마음 응답의 평가 유형을 변환한다.")
  @Test
  void postsInnerThoughtContractAndMapsResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        requests,
        """
            {"success":true,"data":{"innerThought":"즐거웠나 보다.","innerThoughtType":"GOOD"},"error":null}
        """);

    AiFreeTalkInnerThoughtResult innerThought =
        remoteClient().generateInnerThought(innerThoughtRequest());

    assertThat(innerThought.innerThoughtType().name()).isEqualTo("GOOD");
  }

  @DisplayName("지켜볼 패턴을 요청에 싣고, 응답의 강조 구절과 사용례를 원문과 맞춰 본 뒤 통과한 것만 교정에 붙인다.")
  @Test
  void sendsWatchPatternsAndMapsSpansAndUsages() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        requests,
        """
            {"success":true,"data":{"innerThought":"운동 열심히 하네.","innerThoughtType":"GOOD",
             "reactedToPartner":true,
             "correction":{"originalSentence":"I go to a gym yesterday.",
               "betterSentence":"I went to the gym yesterday.","reason":"어제 일이에요.",
               "mistakePattern":"TENSE","wrongSpan":" go ","betterSpan":"Went"},
             "patternUsages":[
               {"pattern":"TENSE","sentence":"I go to a gym yesterday.","span":"go","correct":false},
               {"pattern":"ARTICLE","sentence":"I go to a gym yesterday.","span":"a gym","correct":false},
               {"pattern":"PLURAL","sentence":"I go to a gym yesterday.","span":"gym","correct":true},
               {"pattern":"TENSE","sentence":"I went there.","span":"went","correct":true},
               {"pattern":"TENSE","sentence":"I go to a gym yesterday.","span":"Go","correct":false},
               {"pattern":"TENSE","sentence":"I go to a gym yesterday.","span":" ","correct":true},
               {"pattern":"TENSE","sentence":"I go to a gym yesterday.","span":"go","correct":null},
               {"pattern":"NOPE","sentence":"I go to a gym yesterday.","span":"go","correct":true},
               null
             ]},"error":null}
        """);
    AiFreeTalkInnerThoughtRequest request =
        new AiFreeTalkInnerThoughtRequest(
            300L,
            "chloe",
            3002L,
            1,
            "EN",
            "KR",
            null,
            List.of(
                new AiConversationHistoryMessage(
                    3002L, 1, "USER", "Hi. I go to a gym yesterday. It was fun.", null)),
            List.of(),
            List.of(FreeTalkMistakePattern.TENSE, FreeTalkMistakePattern.ARTICLE));

    FreeTalkTurnCorrection correction = remoteClient().generateInnerThought(request).correction();

    JsonNode sent = requests.get("/api/v1/free-talk/inner-thought").get("watchPatterns");
    assertThat(sent).hasSize(2);
    assertThat(sent.get(0).asString()).isEqualTo("TENSE");
    // 구절은 앞뒤 공백을 떼어 저장하고, 문장에 대소문자까지 그대로 없으면 그 구절만 버린다.
    assertThat(correction.sentence().wrongSpan()).isEqualTo("go");
    assertThat(correction.sentence().betterSpan()).isNull();
    // 보낸 패턴 밖(PLURAL)·원문에 없는 문장·대소문자가 다른 구절·빈 구절·맞음 여부 없음·모르는 패턴·null 항목은 버린다.
    assertThat(correction.patternUsages())
        .containsExactly(
            new FreeTalkPatternUsageDraft(
                FreeTalkMistakePattern.TENSE, "I go to a gym yesterday.", "go", false),
            new FreeTalkPatternUsageDraft(
                FreeTalkMistakePattern.ARTICLE, "I go to a gym yesterday.", "a gym", false));
  }

  @DisplayName("고칠 것이 없는 턴에도 사용례는 붙고, 지켜볼 패턴이 없으면 요청에 필드를 싣지 않는다.")
  @Test
  void keepsUsagesWithoutCorrectionAndOmitsEmptyWatchPatterns() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        requests,
        """
            {"success":true,"data":{"innerThought":"좋았나 보다.","innerThoughtType":"GOOD",
             "reactedToPartner":true,"correction":null,
             "patternUsages":[{"pattern":"TENSE","sentence":"I'm going hiking with friends.",
               "span":"going","correct":true}]},"error":null}
        """);
    AiFreeTalkInnerThoughtRequest request =
        new AiFreeTalkInnerThoughtRequest(
            300L,
            "chloe",
            3002L,
            1,
            "EN",
            "KR",
            null,
            history(),
            List.of(),
            List.of(FreeTalkMistakePattern.TENSE));

    FreeTalkTurnCorrection correction = remoteClient().generateInnerThought(request).correction();

    assertThat(correction.sentence()).isNull();
    assertThat(correction.patternUsages()).hasSize(1);
    assertThat(correction.patternUsages().getFirst().correct()).isTrue();

    remoteClient().generateInnerThought(innerThoughtRequest());
    assertThat(requests.get("/api/v1/free-talk/inner-thought").has("watchPatterns")).isFalse();
  }

  @DisplayName("AI가 교정 판정을 돌려주지 못해 교정 필드가 비어 온 응답은 속마음을 살리고 교정은 다시 해 볼 실패로 본다.")
  @Test
  void treatsNullCorrectionJudgmentAsRetryableFailure() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        new ConcurrentHashMap<>(),
        """
            {"success":true,"data":{"innerThought":"즐거웠나 보다.","innerThoughtType":"GOOD",
             "reactedToPartner":null,"correction":null},"error":null}
        """);

    AiFreeTalkInnerThoughtResult result =
        remoteClient().generateInnerThought(innerThoughtRequest());

    assertThat(result.innerThought()).isEqualTo("즐거웠나 보다.");
    assertThat(result.correction()).isEqualTo(FreeTalkTurnCorrection.unavailable());
    assertThat(result.correction().retryable()).isTrue();
  }

  @DisplayName("교정 필드가 아예 없는 구버전 속마음 응답도 속마음을 살리고 교정은 다시 해 볼 실패로 본다.")
  @Test
  void treatsMissingCorrectionFieldsAsRetryableFailure() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        new ConcurrentHashMap<>(),
        """
            {"success":true,"data":{"innerThought":"즐거웠나 보다.","innerThoughtType":"GOOD"},"error":null}
        """);

    AiFreeTalkInnerThoughtResult result =
        remoteClient().generateInnerThought(innerThoughtRequest());

    assertThat(result.innerThought()).isEqualTo("즐거웠나 보다.");
    assertThat(result.correction()).isEqualTo(FreeTalkTurnCorrection.unavailable());
  }

  @DisplayName("속마음 응답의 턴 교정과 상대 반응 여부를 변환한다.")
  @Test
  void mapsTurnCorrectionFromInnerThoughtResponse() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        new ConcurrentHashMap<>(),
        """
            {"success":true,"data":{"innerThought":"즐거웠나 보다.","innerThoughtType":"GOOD",
             "reactedToPartner":false,
             "correction":{"originalSentence":"I go hiking yesterday.",
                           "betterSentence":"I went hiking yesterday.",
                           "reason":"어제 일이라 went를 써요.","mistakePattern":"TENSE"}},"error":null}
        """);

    AiFreeTalkInnerThoughtResult result =
        remoteClient().generateInnerThought(innerThoughtRequest());

    assertThat(result.correction())
        .isEqualTo(
            FreeTalkTurnCorrection.completed(
                new FreeTalkTurnCorrection.Sentence(
                    "I go hiking yesterday.",
                    "I went hiking yesterday.",
                    "어제 일이라 went를 써요.",
                    FreeTalkMistakePattern.TENSE),
                false));
  }

  @DisplayName("속마음 요청에 기억 문맥을 보내고 교정의 근거 기억 ID와 라벨을 변환한다.")
  @Test
  void sendsMemoryContextAndMapsCorrectionMemory() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        requests,
        correctionResponse("\"usedMemoryId\":42,\"memoryLabel\":\" 헬스장 \""));

    AiFreeTalkInnerThoughtResult result =
        remoteClient().generateInnerThought(innerThoughtRequest(gymMemoryContext()));

    JsonNode memoryContext = requests.get("/api/v1/free-talk/inner-thought").get("memoryContext");
    assertThat(memoryContext).hasSize(1);
    assertThat(memoryContext.get(0).get("memoryId").asLong()).isEqualTo(42L);
    assertThat(result.correction().status().name()).isEqualTo("COMPLETED");
    assertThat(result.correction().sentence().usedMemoryId()).isEqualTo(42L);
    // 지난 기록이 바뀌지 않도록 기억을 말한 날짜를 보낸 문맥에서 꺼내 교정과 함께 남긴다.
    assertThat(result.correction().sentence().memoryObservedOn())
        .isEqualTo(LocalDate.of(2026, 9, 13));
    assertThat(result.correction().sentence().memoryLabel()).isEqualTo("헬스장");
  }

  @DisplayName("보낸 기억에 말한 날짜가 없으면 태그를 만들 수 없으므로 교정은 살리고 근거 기억만 버린다.")
  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void dropsCorrectionMemoryWhenProvidedMemoryHasNoObservedDate(CapturedOutput output)
      throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        new ConcurrentHashMap<>(),
        correctionResponse("\"usedMemoryId\":42,\"memoryLabel\":\"헬스장\""));

    AiFreeTalkInnerThoughtResult result =
        remoteClient()
            .generateInnerThought(
                innerThoughtRequest(
                    List.of(
                        new AiFreeTalkMemoryContext(
                            42L, ConversationMemoryType.PROFILE, "사용자는 집 앞 헬스장에 다닌다."))));

    assertThat(result.correction().status().name()).isEqualTo("COMPLETED");
    assertThat(result.correction().sentence().betterSentence()).isEqualTo("at the gym");
    assertThat(result.correction().sentence().usedMemoryId()).isNull();
    assertThat(result.correction().sentence().memoryObservedOn()).isNull();
    assertThat(result.correction().sentence().memoryLabel()).isNull();
    assertThat(output.getOut())
        .contains("workflow=free_talk_turn_correction_memory reason=memory_without_observed_at");
  }

  @DisplayName("기억 문맥이 없는 속마음 요청은 기억 필드를 싣지 않고 근거 기억 없는 교정을 변환한다.")
  @Test
  void omitsMemoryContextFieldWhenNoMemoryIsAvailable() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        requests,
        correctionResponse("\"usedMemoryId\":null,\"memoryLabel\":null"));

    AiFreeTalkInnerThoughtResult result =
        remoteClient().generateInnerThought(innerThoughtRequest());

    // 이 필드를 모르는 구버전 AI 서버가 요청 전체를 거부하지 않도록, 보낼 기억이 없으면 필드를 싣지 않는다.
    assertThat(requests.get("/api/v1/free-talk/inner-thought").has("memoryContext")).isFalse();
    assertThat(result.correction().sentence().usedMemoryId()).isNull();
    assertThat(result.correction().sentence().memoryLabel()).isNull();
  }

  @DisplayName("근거 기억 값이 계약과 다르면 교정 문장은 살리고 해당 값만 버린 뒤 이유를 로그로 남긴다.")
  @ParameterizedTest(name = "{0}")
  @CsvSource(
      delimiter = '|',
      nullValues = "NULL",
      value = {
        "unknown_memory_id    | \"usedMemoryId\":99,\"memoryLabel\":\"비밀라벨\"   | NULL",
        "label_missing        | \"usedMemoryId\":42,\"memoryLabel\":\"  \"        | 42",
        "label_missing        | \"usedMemoryId\":42                                | 42",
        "label_invalid        | \"usedMemoryId\":42,\"memoryLabel\":\"비밀\\n라벨\" | 42",
        "label_without_memory | \"usedMemoryId\":null,\"memoryLabel\":\"비밀라벨\" | NULL"
      })
  @ExtendWith(OutputCaptureExtension.class)
  void dropsOnlyInvalidCorrectionMemoryValues(
      String expectedReason, String memoryFields, Long expectedMemoryId, CapturedOutput output)
      throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        new ConcurrentHashMap<>(),
        correctionResponse(memoryFields));

    AiFreeTalkInnerThoughtResult result =
        remoteClient().generateInnerThought(innerThoughtRequest(gymMemoryContext()));

    assertThat(result.correction().status().name()).isEqualTo("COMPLETED");
    assertThat(result.correction().sentence().betterSentence()).isEqualTo("at the gym");
    assertThat(result.correction().sentence().usedMemoryId()).isEqualTo(expectedMemoryId);
    assertThat(result.correction().sentence().memoryLabel()).isNull();
    assertThat(output.getOut())
        .contains("workflow=free_talk_turn_correction_memory reason=" + expectedReason)
        .contains("messageId=3002")
        .doesNotContain("비밀");
  }

  @DisplayName("40자를 넘는 라벨은 버리고 40자 라벨은 그대로 둔다.")
  @Test
  void keepsMemoryLabelUpToColumnLength() throws Exception {
    String fortyCharacters = "가".repeat(40);
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        new ConcurrentHashMap<>(),
        correctionResponse("\"usedMemoryId\":42,\"memoryLabel\":\"" + fortyCharacters + "\""));
    assertThat(
            remoteClient()
                .generateInnerThought(innerThoughtRequest(gymMemoryContext()))
                .correction()
                .sentence()
                .memoryLabel())
        .isEqualTo(fortyCharacters);
    server.removeContext("/api/v1/free-talk/inner-thought");
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        new ConcurrentHashMap<>(),
        correctionResponse("\"usedMemoryId\":42,\"memoryLabel\":\"" + fortyCharacters + "나\""));

    FreeTalkTurnCorrection.Sentence sentence =
        remoteClient()
            .generateInnerThought(innerThoughtRequest(gymMemoryContext()))
            .correction()
            .sentence();

    assertThat(sentence.usedMemoryId()).isEqualTo(42L);
    assertThat(sentence.memoryLabel()).isNull();
  }

  @DisplayName("고칠 것이 없는 턴은 교정 없이 완료로 변환한다.")
  @Test
  void mapsNullCorrectionWithReactionAsCompleted() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        new ConcurrentHashMap<>(),
        """
            {"success":true,"data":{"innerThought":"즐거웠나 보다.","innerThoughtType":"GOOD",
             "reactedToPartner":true,"correction":null},"error":null}
        """);

    AiFreeTalkInnerThoughtResult result =
        remoteClient().generateInnerThought(innerThoughtRequest());

    assertThat(result.correction()).isEqualTo(FreeTalkTurnCorrection.completed(null, true));
  }

  @DisplayName("모르는 실수 패턴은 임의 값으로 바꾸지 않고 교정만 실패로 본다.")
  @Test
  void treatsUnknownMistakePatternAsFailedCorrection() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/inner-thought",
        new ConcurrentHashMap<>(),
        """
            {"success":true,"data":{"innerThought":"즐거웠나 보다.","innerThoughtType":"GOOD",
             "reactedToPartner":true,
             "correction":{"originalSentence":"I go.","betterSentence":"I went.",
                           "reason":"과거예요.","mistakePattern":"BRAND_NEW_PATTERN"}},"error":null}
        """);

    AiFreeTalkInnerThoughtResult result =
        remoteClient().generateInnerThought(innerThoughtRequest());

    assertThat(result.innerThoughtType().name()).isEqualTo("GOOD");
    assertThat(result.correction()).isEqualTo(FreeTalkTurnCorrection.failed());
    assertThat(result.correction().retryable()).isFalse();
  }

  @DisplayName("프리톡 종료 요청에 종료 사유와 제목 생성 조건을 보내고 응답을 변환한다.")
  @Test
  void postsClosingContractAndMapsResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/closing",
        requests,
        """
            {
              "success": true,
              "data": {
                "inferredTitle": "Weekend Hiking",
                "aiMessage": "It was nice talking with you.",
                "translatedMessage": "이야기해서 좋았어.",
                "emotion": "NEUTRAL",
                "innerThought": "대화를 잘 마무리했다.",
                "innerThoughtType": "NORMAL"
              },
              "error": null
            }
        """);

    AiFreeTalkClosingResult closing = remoteClient().generateClosing(closingRequest());

    assertThat(requests.get("/api/v1/free-talk/closing").has("partnerDisplayName")).isFalse();
    assertThat(requests.get("/api/v1/free-talk/closing").get("closingReason").asString())
        .isEqualTo("USER_CONFIRMED");
    assertThat(requests.get("/api/v1/free-talk/closing").get("titleGenerationRequired").asBoolean())
        .isTrue();
    assertThat(closing.inferredTitle()).isEqualTo("Weekend Hiking");
    assertThat(closing.translatedMessage()).isEqualTo("이야기해서 좋았어.");
  }

  @DisplayName("기존 표현 추천 계약을 전송하고 성공 응답을 변환한다.")
  @Test
  void postsExistingExpressionContractAndMapsSuccessfulResponse() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/expression-recommendations",
        requests,
        """
            {
              "success": true,
              "data": {
                "recommendations": [{
                  "displayOrder": 1,
                  "existingExpressionId": 7
                }]
              },
              "error": null
            }
        """);
    RemoteAiFreeTalkClient client = remoteClient();
    AiFreeTalkExpressionRecommendationsResult recommendations =
        client.recommendExpressions(recommendationsRequest());

    assertThat(
            requests
                .get("/api/v1/free-talk/expression-recommendations")
                .get("existingExpressions")
                .get(0)
                .get("expressionId")
                .asLong())
        .isEqualTo(7L);
    assertThat(recommendations.recommendations()).hasSize(1);
    // 보낼 배운 표현이 없으면 필드 자체를 싣지 않아 이 필드를 모르는 AI 서버도 요청을 받는다.
    assertThat(
            requests.get("/api/v1/free-talk/expression-recommendations").has("learnedExpressions"))
        .isFalse();
    assertThat(recommendations.usedExpressions()).isEmpty();
  }

  @DisplayName("배운 표현 후보를 요청에 싣고, 응답의 다시 쓴 표현을 빈 항목만 빼고 그대로 읽는다.")
  @Test
  void sendsLearnedExpressionsAndMapsUsedExpressions() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/expression-recommendations",
        requests,
        """
            {
              "success": true,
              "data": {
                "recommendations": [{
                  "displayOrder": 1,
                  "existingExpressionId": 7
                }],
                "usedExpressions": [
                  {
                    "expressionId": 812,
                    "messageId": 3002,
                    "matchedText": "going hiking"
                  },
                  null
                ]
              },
              "error": null
            }
        """);
    AiFreeTalkExpressionRecommendationsRequest base = recommendationsRequest();

    AiFreeTalkExpressionRecommendationsResult result =
        remoteClient()
            .recommendExpressions(
                new AiFreeTalkExpressionRecommendationsRequest(
                    base.sessionId(),
                    base.targetLocale(),
                    base.baseLocale(),
                    base.conversationHistory(),
                    base.existingExpressions(),
                    List.of(new AiFreeTalkLearnedExpression(812L, "go hiking", "등산하러 가다"))));

    JsonNode learned =
        requests.get("/api/v1/free-talk/expression-recommendations").get("learnedExpressions");
    assertThat(learned).hasSize(1);
    assertThat(learned.get(0).propertyNames())
        .containsExactlyInAnyOrder(
            "expressionId", "targetExpressionText", "baseExpressionMeaningText");
    assertThat(learned.get(0).get("expressionId").asLong()).isEqualTo(812L);
    assertThat(learned.get(0).get("targetExpressionText").asString()).isEqualTo("go hiking");
    assertThat(learned.get(0).get("baseExpressionMeaningText").asString()).isEqualTo("등산하러 가다");
    assertThat(result.usedExpressions())
        .containsExactly(new AiFreeTalkUsedExpression(812L, 3002L, "going hiking"));
  }

  @DisplayName("일반 첫 대화 요청에는 기억 조회보다 긴 대기 시간을 적용한다.")
  @Test
  void normalOpeningWaitsBeyondMemoryTimeout() {
    registerDelayedResponse(
        "/api/v1/free-talk/opening",
        2_300,
        successResponse("{\"aiMessage\":\"Hello!\",\"translatedMessage\":\"안녕!\"}"));
    assertThat(remoteClient(Duration.ofSeconds(5)).generateOpening(openingRequest()).aiMessage())
        .isEqualTo("Hello!");
  }

  private void registerDelayedResponse(String path, long delayMillis, String response) {
    server.createContext(
        path,
        exchange -> {
          try {
            Thread.sleep(delayMillis);
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
  }

  @DisplayName("AI가 먼저 배포되어 시작·발화·표현 추천 응답에 모르는 필드가 실려 와도 기존 값을 그대로 변환한다.")
  @Test
  void ignoresUnknownFieldsAddedByNewerAiServer() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        new ConcurrentHashMap<>(),
        """
            {
              "success": true,
              "data": {
                "aiMessage": "How was your weekend?",
                "translatedMessage": "주말 어땠어?",
                "emotion": null,
                "usedMemoryIds": [],
                "followUpAsked": true,
                "followUpId": 9
              },
              "error": null
            }
        """);
    registerJsonResponse(
        "/api/v1/free-talk/turn",
        new ConcurrentHashMap<>(),
        """
            {
              "success": true,
              "data": {
                "userExitIntentDetected": false,
                "inferredTitle": "주말 이야기",
                "aiMessage": "That sounds fun.",
                "translatedMessage": "재밌겠다.",
                "emotion": null,
                "usedMemoryIds": [],
                "followUpAsked": false,
                "followUpId": null
              },
              "error": null
            }
        """);
    registerJsonResponse(
        "/api/v1/free-talk/expression-recommendations",
        new ConcurrentHashMap<>(),
        """
            {
              "success": true,
              "data": {
                "recommendations": [{
                  "displayOrder": 1,
                  "existingExpressionId": 7
                }],
                "usedExpressions": [{
                  "expressionId": 7,
                  "messageId": 10,
                  "matchedText": "grab a coffee"
                }]
              },
              "error": null
            }
        """);
    RemoteAiFreeTalkClient client = remoteClient();

    assertThat(client.generateOpening(openingRequest()).aiMessage())
        .isEqualTo("How was your weekend?");
    assertThat(client.generateTurn(turnRequest()).aiMessage()).isEqualTo("That sounds fun.");
    assertThat(client.recommendExpressions(recommendationsRequest()).recommendations()).hasSize(1);
  }

  @DisplayName("첫 대화 요청에 기억 문맥을 보내고 사용된 기억 ID를 응답에서 읽는다.")
  @Test
  void mapsUsedMemoryIdsAndSendsMemoryContextForOpening() throws Exception {
    Map<String, JsonNode> requests = new ConcurrentHashMap<>();
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        requests,
        successResponse(
            "{\"aiMessage\":\"How is the interview going?\","
                + "\"translatedMessage\":\"면접은 잘 되어가?\","
                + "\"usedMemoryIds\":[77]}"));

    AiFreeTalkOpeningResult result =
        remoteClient()
            .generateOpening(
                new AiFreeTalkOpeningRequest(
                    300L,
                    "chloe",
                    "EN",
                    "KR",
                    new AiFreeTalkTopic(2L, "주말 계획", "Ask about weekend plans."),
                    List.of(
                        new AiFreeTalkMemoryContext(
                            77L,
                            ConversationMemoryType.EVENT,
                            "면접 계획",
                            LocalDateTime.of(2026, 7, 1, 9, 0),
                            LocalDateTime.of(2026, 7, 31, 23, 59),
                            LocalDateTime.of(2026, 8, 1, 10, 30)))));

    assertThat(
            requests
                .get("/api/v1/free-talk/opening")
                .get("memoryContext")
                .get(0)
                .get("memoryId")
                .asLong())
        .isEqualTo(77L);
    JsonNode memoryContext = requests.get("/api/v1/free-talk/opening").get("memoryContext").get(0);
    assertThat(memoryContext.get("validFrom").asText()).isEqualTo("2026-07-01T09:00:00");
    assertThat(memoryContext.get("validTo").asText()).isEqualTo("2026-07-31T23:59:00");
    assertThat(memoryContext.get("observedAt").asText()).isEqualTo("2026-08-01T10:30:00");
    assertThat(result.usedMemoryIds()).containsExactly(77L);
  }

  @DisplayName("문맥에 없는 기억 ID는 정리하되 대화 응답 자체는 거부하지 않는다.")
  @Test
  void normalizesUsedMemoryIdOutsideContextWithoutRejectingConversation() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        new ConcurrentHashMap<>(),
        successResponse(
            "{\"aiMessage\":\"How is the interview going?\","
                + "\"translatedMessage\":\"면접은 잘 되어가?\","
                + "\"usedMemoryIds\":[88]}"));

    AiFreeTalkOpeningResult result =
        remoteClient()
            .generateOpening(
                new AiFreeTalkOpeningRequest(
                    300L,
                    "chloe",
                    "EN",
                    "KR",
                    new AiFreeTalkTopic(2L, "주말 계획", "Ask about weekend plans."),
                    List.of(
                        new AiFreeTalkMemoryContext(77L, ConversationMemoryType.EVENT, "면접 계획"))));

    assertThat(result.usedMemoryIds()).isEmpty();
  }

  @DisplayName("필수 필드가 빠진 프리톡 AI 응답을 거부한다.")
  @Test
  void rejectsResponsesMissingRequiredFields() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        new ConcurrentHashMap<>(),
        """
            {"success":true,"data":{"translatedMessage":"주말 어땠어?","emotion":"HAPPY"},"error":null}
        """);

    assertThatThrownBy(() -> remoteClient().generateOpening(openingRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));
  }

  @DisplayName("프리톡 시작 응답은 감정 값이 null이어도 허용한다.")
  @Test
  void acceptsNullEmotionForOpening() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/opening",
        new ConcurrentHashMap<>(),
        """
            {
              "success": true,
              "data": {
                "aiMessage": "How was your weekend?",
                "translatedMessage": "주말 어땠어?",
                "emotion": null
              },
              "error": null
            }
        """);

    assertThat(remoteClient().generateOpening(openingRequest()).emotion()).isNull();
  }

  @DisplayName("프리톡 발화 응답은 감정 값이 null이어도 허용한다.")
  @Test
  void acceptsNullEmotionForTurn() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/turn",
        new ConcurrentHashMap<>(),
        """
            {
              "success": true,
              "data": {
                "userExitIntentDetected": false,
                "inferredTitle": "주말 이야기",
                "aiMessage": "That sounds fun.",
                "translatedMessage": "재밌겠다.",
                "emotion": null
              },
              "error": null
            }
        """);

    assertThat(remoteClient().generateTurn(turnRequest()).emotion()).isNull();
  }

  @DisplayName("프리톡 종료 응답은 감정 값이 null이어도 허용한다.")
  @Test
  void acceptsNullEmotionForClosing() throws Exception {
    registerJsonResponse(
        "/api/v1/free-talk/closing",
        new ConcurrentHashMap<>(),
        """
            {
              "success": true,
              "data": {
                "aiMessage": "It was nice talking with you.",
                "translatedMessage": "이야기해서 좋았어.",
                "emotion": null
              },
              "error": null
            }
        """);

    assertThat(remoteClient().generateClosing(closingRequest()).emotion()).isNull();
  }

  @DisplayName("빈 종료 제목은 없는 것으로 처리하고 종료 메시지는 보존한다.")
  @Test
  void treatsBlankClosingTitleAsMissingWhilePreservingClosingMessage() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/closing",
        200,
        successResponse(
            "{\"inferredTitle\":\"   \","
                + "\"aiMessage\":\"It was nice talking with you.\","
                + "\"translatedMessage\":\"이야기해서 좋았어.\",\"emotion\":null}"));

    AiFreeTalkClosingResult result = remoteClient().generateClosing(closingRequest());

    assertThat(result.inferredTitle()).isNull();
    assertThat(result.aiMessage()).isEqualTo("It was nice talking with you.");
    assertThat(result.translatedMessage()).isEqualTo("이야기해서 좋았어.");
  }

  @DisplayName("AI의 502 응답 형식 오류를 유지하고 503은 생성 실패로 변환한다.")
  @Test
  void preservesResponseInvalidForUpstream502AndMaps503ToGenerationFailure() throws Exception {
    server.createContext(
        "/api/v1/free-talk/opening",
        exchange -> writeErrorResponse(exchange, 502, "AI_RESPONSE_INVALID"));

    assertThatThrownBy(() -> remoteClient().generateOpening(openingRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));

    server.removeContext("/api/v1/free-talk/opening");
    server.createContext(
        "/api/v1/free-talk/opening",
        exchange -> writeErrorResponse(exchange, 503, "AI_RESPONSE_INVALID"));

    assertThatThrownBy(() -> remoteClient().generateOpening(openingRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
  }

  @DisplayName("프리톡 AI 네트워크 오류를 생성 실패로 변환한다.")
  @Test
  void mapsNetworkFailureToGenerationFailure() {
    RemoteAiFreeTalkClient client = remoteClient();
    server.stop(0);

    assertThatThrownBy(() -> client.generateOpening(openingRequest()))
        .isInstanceOfSatisfying(
            ApiException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
  }

  @DisplayName("성공 응답의 data가 null이면 프리톡 AI 응답을 거부한다.")
  @Test
  void rejectsNullDataOnSuccessfulResponse() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/opening", 200, "{\"success\":true,\"data\":null,\"error\":null}");

    assertGenerationError(
        () -> remoteClient().generateOpening(openingRequest()), ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("발화와 종료 및 표현 추천의 잘못된 AI 응답을 거부한다.")
  @Test
  void rejectsRepresentativeInvalidResponsesForTurnClosingAndRecommendations() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/turn",
        200,
        successResponse(
            String.join(
                "",
                "{\"userExitIntentDetected\":true,\"inferredTitle\":null,",
                "\"aiMessage\":\"See you.\",\"translatedMessage\":null,\"emotion\":null,",
                "\"innerThought\":null,\"innerThoughtType\":null}")));
    assertGenerationError(
        () -> remoteClient().generateTurn(turnRequest()), ErrorCode.AI_RESPONSE_INVALID);

    registerRawResponse(
        "/api/v1/free-talk/closing",
        200,
        successResponse(
            String.join(
                "",
                "{\"aiMessage\":null,\"translatedMessage\":\"또 봐.\",",
                "\"emotion\":null,\"innerThought\":\"잘 마무리했다.\",",
                "\"innerThoughtType\":\"GOOD\"}")));
    assertGenerationError(
        () -> remoteClient().generateClosing(closingRequest()), ErrorCode.AI_RESPONSE_INVALID);

    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse("{\"recommendations\":[{\"displayOrder\":1}]}"));
    assertGenerationError(
        () -> remoteClient().recommendExpressions(recommendationsRequest()),
        ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("후보에 없는 기존 표현 ID를 추천한 AI 응답을 거부한다.")
  @Test
  void rejectsRecommendationWithUnknownExistingExpressionId() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse(
            recommendationsData(8L, "I'm up for that", "좋아, 그거 하자", "제안에 동의할 때 사용", 1)));

    assertGenerationError(
        () -> remoteClient().recommendExpressions(recommendationsRequest()),
        ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("다시 쓴 표현의 형식이 깨진 응답은 추천까지 통째로 거부한다.")
  @Test
  void rejectsRecommendationWhenUsedExpressionsIsMalformed() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse(
            "{\"recommendations\":[{\"displayOrder\":1,\"existingExpressionId\":7}],"
                + "\"usedExpressions\":\"oops\"}"));

    assertGenerationError(
        () -> remoteClient().recommendExpressions(recommendationsRequest()),
        ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("기존 표현 ID가 빠진 추천 응답을 거부한다.")
  @Test
  void rejectsRecommendationMissingExistingExpressionId() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse("{\"recommendations\":[{\"displayOrder\":1}]}"));

    assertGenerationError(
        () -> remoteClient().recommendExpressions(recommendationsRequest()),
        ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("AI 서버가 검증한 기존 표현 추천 메타데이터를 허용한다.")
  @Test
  void acceptsExistingRecommendationMetadataValidatedByAiServer() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse(
            recommendationsData(7L, "I am up for that", "좋아, 그거 하자", "제안에 동의할 때 사용", 1)));

    AiFreeTalkExpressionRecommendationsResult result =
        remoteClient().recommendExpressions(recommendationsRequest());

    assertThat(result.recommendations().getFirst().existingExpressionId()).isEqualTo(7L);
  }

  @DisplayName("노출 순서가 중복된 표현 추천을 거부한다.")
  @Test
  void rejectsRecommendationWithDuplicateDisplayOrder() throws Exception {
    registerRawResponse(
        "/api/v1/free-talk/expression-recommendations",
        200,
        successResponse(
            "{\"recommendations\":["
                + recommendationData(7L, EXPRESSION_TEXT, EXPRESSION_MEANING, EXPRESSION_USAGE, 1)
                + ","
                + recommendationData(7L, EXPRESSION_TEXT, EXPRESSION_MEANING, EXPRESSION_USAGE, 1)
                + "]}"));

    assertGenerationError(
        () -> remoteClient().recommendExpressions(recommendationsRequest()),
        ErrorCode.AI_RESPONSE_INVALID);
  }

  @DisplayName("프리톡 AI 요청의 시간 초과와 인터럽트를 생성 실패로 변환한다.")
  @Test
  void mapsTimeoutAndInterruptedRequestToGenerationFailure() throws Exception {
    server.createContext(
        "/api/v1/free-talk/opening",
        exchange -> {
          try {
            Thread.sleep(200);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
          }
          exchange.close();
        });

    assertGenerationError(
        () -> remoteClient(Duration.ofMillis(50)).generateOpening(openingRequest()),
        ErrorCode.AI_GENERATION_FAILED);

    Thread.currentThread().interrupt();
    try {
      assertGenerationError(
          () -> remoteClient().generateOpening(openingRequest()), ErrorCode.AI_GENERATION_FAILED);
    } finally {
      Thread.interrupted();
    }
  }

  private void assertGenerationError(Runnable invocation, ErrorCode expectedErrorCode) {
    assertThatThrownBy(invocation::run)
        .isInstanceOfSatisfying(
            ApiException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
  }

  private void registerJsonResponse(String path, Map<String, JsonNode> requests, String response)
      throws Exception {
    server.createContext(
        path,
        exchange -> {
          requests.put(
              path,
              jsonMapper.readTree(
                  new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
          byte[] responseBody = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
  }

  private void registerRawResponse(String path, int status, String response) throws Exception {
    server.createContext(
        path,
        exchange -> {
          byte[] responseBody = response.getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(status, responseBody.length);
          exchange.getResponseBody().write(responseBody);
          exchange.close();
        });
  }

  private void writeErrorResponse(
      com.sun.net.httpserver.HttpExchange exchange, int status, String code)
      throws java.io.IOException {
    byte[] responseBody =
        ("{\"success\":false,\"data\":null,\"error\":{\"code\":\"" + code + "\"}}")
            .getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, responseBody.length);
    exchange.getResponseBody().write(responseBody);
    exchange.close();
  }

  private RemoteAiFreeTalkClient remoteClient() {
    return remoteClient(Duration.ofSeconds(1));
  }

  private RemoteAiFreeTalkClient remoteClient(Duration requestTimeout) {
    return new RemoteAiFreeTalkClient(
        jsonMapper,
        new AiClientProperties(
            "http://localhost:" + server.getAddress().getPort(),
            "remote",
            "KOREAN_LEARNER",
            Duration.ofSeconds(1),
            requestTimeout,
            Duration.ofSeconds(1),
            Duration.ofSeconds(20)));
  }

  private String successResponse(String data) {
    return "{\"success\":true,\"data\":" + data + ",\"error\":null}";
  }

  private String recommendationsData(
      long expressionId,
      String targetExpressionText,
      String meaning,
      String usageSummary,
      int displayOrder) {
    return "{\"recommendations\":["
        + recommendationData(
            expressionId, targetExpressionText, meaning, usageSummary, displayOrder)
        + "]}";
  }

  private String recommendationData(
      long expressionId,
      String targetExpressionText,
      String meaning,
      String usageSummary,
      int displayOrder) {
    return String.join(
        "\n",
        "{",
        "  \"displayOrder\": %d,".formatted(displayOrder),
        "  \"sourceType\": \"EXISTING\",",
        "  \"existingExpressionId\": %d,".formatted(expressionId),
        "  \"targetExpressionText\": \"%s\",".formatted(targetExpressionText),
        "  \"baseExpressionMeaningText\": \"%s\",".formatted(meaning),
        "  \"usageSummary\": \"%s\"".formatted(usageSummary),
        "}");
  }

  private AiFreeTalkOpeningRequest openingRequest() {
    return new AiFreeTalkOpeningRequest(
        300L,
        "chloe",
        "EN",
        "KR",
        new AiFreeTalkTopic(2L, "주말 계획", "Ask about the user's weekend plans."),
        List.of());
  }

  private AiFreeTalkTurnRequest turnRequest() {
    return new AiFreeTalkTurnRequest(
        300L,
        "chloe",
        3002L,
        1,
        "EN",
        "KR",
        AiFreeTalkResponseMode.NORMAL,
        true,
        null,
        history(),
        List.of());
  }

  private AiFreeTalkClosingRequest closingRequest() {
    return new AiFreeTalkClosingRequest(
        300L,
        "chloe",
        3002L,
        1,
        "EN",
        "KR",
        AiFreeTalkClosingReason.USER_CONFIRMED,
        true,
        new AiFreeTalkTopic(null, "주말 이야기", null),
        history());
  }

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest() {
    return innerThoughtRequest(List.of());
  }

  private AiFreeTalkInnerThoughtRequest innerThoughtRequest(
      List<AiFreeTalkMemoryContext> memoryContext) {
    return new AiFreeTalkInnerThoughtRequest(
        300L, "chloe", 3002L, 1, "EN", "KR", null, history(), memoryContext);
  }

  private List<AiFreeTalkMemoryContext> gymMemoryContext() {
    return List.of(
        new AiFreeTalkMemoryContext(
            42L,
            ConversationMemoryType.PROFILE,
            "사용자는 집 앞 헬스장에 다닌다.",
            null,
            null,
            // 자정 직전에 말한 기억도 그날 날짜로 남아야 한다(시각은 서비스 시간대 기준으로 저장되어 있다).
            LocalDateTime.of(2026, 9, 13, 23, 30)));
  }

  // 교정 문장은 고정하고 근거 기억 필드만 바꿔 끼운 속마음 응답을 만든다.
  private String correctionResponse(String memoryFields) {
    return "{\"success\":true,\"data\":{\"innerThought\":\"운동 열심히 하네.\","
        + "\"innerThoughtType\":\"GOOD\",\"reactedToPartner\":true,"
        + "\"correction\":{\"originalSentence\":\"at a gym\",\"betterSentence\":\"at the gym\","
        + "\"reason\":\"둘 다 아는 곳이에요.\",\"mistakePattern\":\"ARTICLE\","
        + memoryFields
        + "}},\"error\":null}";
  }

  private AiFreeTalkExpressionRecommendationsRequest recommendationsRequest() {
    return new AiFreeTalkExpressionRecommendationsRequest(
        300L,
        "EN",
        "KR",
        history(),
        List.of(
            new AiFreeTalkExistingExpression(
                7L, EXPRESSION_TEXT, EXPRESSION_MEANING, EXPRESSION_USAGE)));
  }

  private List<AiConversationHistoryMessage> history() {
    return List.of(
        new AiConversationHistoryMessage(3002L, 1, "USER", "I'm going hiking with friends.", null));
  }
}
