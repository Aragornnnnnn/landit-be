// 평가 응답의 숫자 척도와 잘못된 혼합 계약을 검증한다.

package com.landit.landitbe.feature.learning.scenario.assessment.client.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.landit.landitbe.feature.learning.scenario.assessment.domain.AssessmentScale;
import com.landit.landitbe.shared.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class AiAssessmentResponseParserTest {
  private final JsonMapper mapper = new JsonMapper();

  @DisplayName("점수의 크기로 척도를 추정하지 않으며 원본 JSON을 변경하지 않는다.")
  @Test
  void preservesSmallScoresAndLegacyValues() {
    for (String field : new String[] {"level", "score"}) {
      var payload = payload(field, "4");
      String original = payload.toString();
      var result = AiAssessmentResponseParser.parse(payload, null, mapper);
      assertThat(result.scale())
          .isEqualTo(field.equals("level") ? AssessmentScale.LEGACY : AssessmentScale.SCORE);
      assertThat(result.core().messages().getFirst().domains().grammar().score()).isEqualTo(4);
      assertThat(payload.toString()).isEqualTo(original);
    }
  }

  @DisplayName("미관찰 null도 필드명으로 척도를 유지한다.")
  @Test
  void preservesScaleWithoutObservedValues() {
    for (String field : new String[] {"level", "score"}) {
      var payload = payload(field, "null");
      payload
          .path("core")
          .path("messages")
          .get(0)
          .path("domains")
          .forEach(
              domain -> {
                ((ObjectNode) domain).put("evidenceStatus", "NOT_OBSERVED");
                ((ObjectNode) domain).putNull("evidenceExcerpt");
              });
      var result = AiAssessmentResponseParser.parse(payload, null, mapper);
      assertThat(result.scale().maximum()).isEqualTo(field.equals("level") ? 5 : 100);
    }
  }

  @DisplayName("구형 값의 범위와 정수 타입을 엄격히 검증한다.")
  @ParameterizedTest
  @ValueSource(strings = {"0", "6", "100", "4.0", "true", "\"4\"", "2147483648"})
  void rejectsInvalidLegacyValues(String value) {
    assertThatThrownBy(
            () -> AiAssessmentResponseParser.parse(payload("level", value), null, mapper))
        .isInstanceOf(ApiException.class);
  }

  @DisplayName("척도 혼합이나 버전 불일치를 정상 평가로 받아들이지 않는다.")
  @Test
  void rejectsMixedAndMislabeledContracts() {
    var mixed = payload("level", "4");
    var grammar =
        (ObjectNode) mixed.path("core").path("messages").get(0).path("domains").path("grammar");
    grammar.put("score", 80);
    assertThatThrownBy(() -> AiAssessmentResponseParser.parse(mixed, null, mapper))
        .isInstanceOf(ApiException.class);
    grammar.remove("level");
    assertThatThrownBy(() -> AiAssessmentResponseParser.parse(mixed, null, mapper))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () ->
                AiAssessmentResponseParser.parse(payload("score", "4"), "text-level-v1.3", mapper))
        .isInstanceOf(ApiException.class);
    assertThatThrownBy(
            () -> AiAssessmentResponseParser.parse(payload("score", "101"), null, mapper))
        .isInstanceOf(ApiException.class);
  }

  private ObjectNode payload(String field, String value) {
    var domains = mapper.createObjectNode();
    for (String name :
        new String[] {
          "situationPerformance", "grammar", "vocabulary", "discourse", "interactionPragmatics"
        }) {
      domains.set(
          name,
          mapper.readTree(
              "{\""
                  + field
                  + "\":"
                  + value
                  + ",\"evidenceStatus\":\"OBSERVED\",\"evidenceExcerpt\":\"I like coffee.\"}"));
    }
    var message = mapper.createObjectNode().put("messageId", 1).put("taskPerformance", "ACHIEVED");
    message.set("domains", domains);
    var core = mapper.createObjectNode();
    core.set("messages", mapper.createArrayNode().add(message));
    var payload = mapper.createObjectNode();
    payload.set("core", core);
    return payload;
  }
}
