// 구형 level과 신형 score 응답의 척도를 확인한 뒤 내부 평가 계약으로 변환한다.

package com.landit.landitbe.feature.learning.scenario.assessment.client.ai;

import com.landit.landitbe.feature.learning.scenario.assessment.domain.AssessmentScale;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** 숫자 크기로 척도를 추정하거나 잘못된 혼합 응답을 임의 환산하지 않는다. */
public final class AiAssessmentResponseParser {
  private AiAssessmentResponseParser() {}

  /**
   * 원본 응답의 척도를 확인하고 관찰값을 환산 없이 내부 계약으로 옮긴다.
   *
   * @param payload AI의 원본 평가 JSON
   * @param version AI가 명시한 버전. 구버전 응답은 null
   * @param mapper 내부 계약 역직렬화에 사용할 변환기
   * @return 실제 척도를 보존한 평가. AI가 평가를 생성하지 못하면 null
   * @throws ApiException 척도가 섞이거나 숫자와 버전 계약이 잘못된 경우
   */
  public static AiSessionLevelAssessment parse(
      JsonNode payload, String version, JsonMapper mapper) {
    if (payload == null || payload.isNull()) {
      return null;
    }
    JsonNode normalized = payload.deepCopy();
    JsonNode messages = normalized.path("core").path("messages");
    if (!messages.isArray() || messages.isEmpty()) {
      throw invalid();
    }
    AssessmentScale scale = null;
    for (JsonNode message : messages) {
      JsonNode domains = message.path("domains");
      if (!domains.isObject() || domains.size() != 5) {
        throw invalid();
      }
      for (JsonNode domain : domains) {
        AssessmentScale current = normalizeDomain(domain);
        if (scale != null && scale != current) {
          throw invalid();
        }
        scale = current;
      }
    }
    if (scale == null || (version != null && !scale.version().equals(version))) {
      throw invalid();
    }
    AiSessionLevelAssessment result =
        mapper.treeToValue(normalized, AiSessionLevelAssessment.class);
    return new AiSessionLevelAssessment(result.core(), result.details(), scale);
  }

  private static AssessmentScale normalizeDomain(JsonNode domain) {
    if (!(domain instanceof ObjectNode object) || domain.has("score") == domain.has("level")) {
      throw invalid();
    }
    boolean legacy = domain.has("level");
    JsonNode value = domain.path(legacy ? "level" : "score");
    AssessmentScale scale = legacy ? AssessmentScale.LEGACY : AssessmentScale.SCORE;
    if (!value.isNull()
        && (!value.isIntegralNumber()
            || !value.canConvertToInt()
            || value.intValue() < 1
            || value.intValue() > scale.maximum())) {
      throw invalid();
    }
    if (legacy) {
      object.remove("level");
      object.set("score", value);
    }
    return scale;
  }

  private static ApiException invalid() {
    return new ApiException(ErrorCode.AI_RESPONSE_INVALID);
  }
}
