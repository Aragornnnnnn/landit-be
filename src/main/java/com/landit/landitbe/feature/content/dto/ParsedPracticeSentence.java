// 추가 예문 payload 1건을 응답 항목과 작문용 단어 배열로 파싱한 결과를 표현한다.

package com.landit.landitbe.feature.content.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * 추가 예문 payload 1건을 응답 항목과 작문용 단어 배열로 파싱한 결과를 표현한다.
 *
 * <p>단어 배열은 작문 문제(writingSentence)에만 노출하고 예문 목록(practiceSentence) 계약에는 노출하지 않으므로, 응답 DTO와 분리해 함께
 * 나른다.
 *
 * @param sentence 예문 응답 항목
 * @param sentenceWords 정답 예문을 단어 단위로 나눈 배열(정답 순서 유지)
 * @param sentenceWordChoices 정답 단어와 오답 단어를 섞은 선택지 배열(저장된 섞인 순서 그대로)
 */
public record ParsedPracticeSentence(
    PracticeSentenceResponse sentence,
    // 예: ["The", "special", "effects", "blew", "my", "mind"]
    List<String> sentenceWords,
    // 예: ["special", "blew", "The", "mind", "amazing", "have", "get", "effects", "my"]
    List<String> sentenceWordChoices) {

  /**
   * 추가 예문 JSON 객체를 파싱 결과로 변환한다. 단어 배열 키 검증은 호출부(ExpressionQueryService)가 마친 상태를 전제한다.
   *
   * @param node 추가 예문 JSON 객체
   * @return 파싱 결과
   */
  public static ParsedPracticeSentence from(JsonNode node) {
    return new ParsedPracticeSentence(
        PracticeSentenceResponse.from(node),
        toStringList(node.get("sentenceWords")),
        toStringList(node.get("sentenceWordChoices")));
  }

  /** JSON 배열 노드를 문자열 리스트로 변환한다. */
  private static List<String> toStringList(JsonNode arrayNode) {
    List<String> values = new ArrayList<>();
    arrayNode.forEach(element -> values.add(element.asText()));
    return List.copyOf(values);
  }
}
