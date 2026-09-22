// 추가 예문 payload 1건을 응답 항목과 작문용 단어 배열로 파싱한 결과를 표현한다.

package com.landit.landitbe.feature.content.expression.practice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 추가 예문 payload 1건을 응답 항목과 작문용 단어 배열로 파싱한 결과를 표현한다.
 *
 * <p>단어 배열은 작문 문제(writingSentence)에만 노출하고 예문 목록(practiceSentence) 계약에는 노출하지 않으므로, 응답 DTO와 분리해 함께
 * 나른다. 영어 배열과 한국어 배열을 모두 담아 두고, 어느 쪽을 쓸지는 문제의 출제 언어가 정한다.
 *
 * @param sentence 예문 응답 항목
 * @param sentenceWords 정답 예문을 단어 단위로 나눈 배열(정답 순서 유지)
 * @param sentenceWordChoices 정답 단어와 오답 단어를 섞은 선택지 배열(저장된 섞인 순서 그대로)
 * @param sentenceTranslateWords 정답 해석을 단어 단위로 나눈 배열(정답 순서 유지)
 * @param sentenceTranslateWordChoices 정답 해석 단어와 오답 단어를 섞은 선택지 배열(저장된 섞인 순서 그대로)
 * @param sentenceTranslateAcceptedAnswers 원래 한국어 정답을 첫 번째로 포함하는 허용 정답 배열 목록
 */
public record ParsedPracticeSentence(
    PracticeSentenceResponse sentence,
    // 예: ["The", "special", "effects", "blew", "my", "mind"]
    List<String> sentenceWords,
    // 예: ["special", "blew", "The", "mind", "amazing", "have", "get", "effects", "my"]
    List<String> sentenceWordChoices,
    // 예: ["특수효과가", "끝내줬어"]
    List<String> sentenceTranslateWords,
    // 예: ["별로였어", "특수효과가", "무서웠어", "끝내줬어", "자막이"]
    List<String> sentenceTranslateWordChoices,
    List<List<String>> sentenceTranslateAcceptedAnswers) {

  /**
   * 추가 예문 JSON 객체를 파싱 결과로 변환한다. 필수 단어 배열 검증은 호출부(ExpressionPracticeService)가 마친 상태를 전제한다.
   *
   * <p>추가 허용 정답은 원래 정답과 토큰별 개수가 같은 배열만 유지하고 중복을 제거한다. 데이터가 없거나 올바른 추가 정답이 없으면 원래 정답 하나를 사용한다.
   *
   * @param node 추가 예문 JSON 객체
   * @return 파싱 결과
   */
  public static ParsedPracticeSentence from(JsonNode node) {
    List<String> koreanWords = toStringList(node.get("sentenceTranslateWords"));
    return new ParsedPracticeSentence(
        PracticeSentenceResponse.from(node),
        toStringList(node.get("sentenceWords")),
        toStringList(node.get("sentenceWordChoices")),
        koreanWords,
        toStringList(node.get("sentenceTranslateWordChoices")),
        toAcceptedAnswers(node.get("sentenceTranslateAcceptedAnswers"), koreanWords));
  }

  /** 원래 정답은 항상 첫 번째로 두고 검토된 추가 배열의 순서와 토큰 중복 횟수를 보존한다. */
  private static List<List<String>> toAcceptedAnswers(JsonNode arrayNode, List<String> canonical) {
    Set<List<String>> answers = new LinkedHashSet<>();
    answers.add(canonical);
    if (arrayNode != null && arrayNode.isArray()) {
      Map<String, Integer> canonicalCounts = tokenCounts(canonical);
      for (JsonNode answer : arrayNode) {
        if (isValidAnswer(answer, canonicalCounts)) {
          answers.add(toStringList(answer));
        }
      }
    }
    return List.copyOf(answers);
  }

  private static boolean isValidAnswer(JsonNode answer, Map<String, Integer> canonicalCounts) {
    if (!answer.isArray() || answer.isEmpty()) {
      return false;
    }
    for (JsonNode token : answer) {
      if (!token.isTextual() || token.asText().isBlank()) {
        return false;
      }
    }
    return tokenCounts(toStringList(answer)).equals(canonicalCounts);
  }

  private static Map<String, Integer> tokenCounts(List<String> words) {
    Map<String, Integer> counts = new HashMap<>();
    words.forEach(word -> counts.merge(word, 1, Integer::sum));
    return counts;
  }

  /** JSON 배열 노드를 문자열 리스트로 변환한다. */
  private static List<String> toStringList(JsonNode arrayNode) {
    List<String> values = new ArrayList<>();
    arrayNode.forEach(element -> values.add(element.asText()));
    return List.copyOf(values);
  }
}
