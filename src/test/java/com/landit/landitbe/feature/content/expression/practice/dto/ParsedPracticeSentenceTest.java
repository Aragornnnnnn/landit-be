// 한국어 복수 정답 파싱의 구버전 호환성과 토큰 중복 횟수 보존을 검증한다.

package com.landit.landitbe.feature.content.expression.practice.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** 선택적 복수 정답 데이터가 없어도 기존 정답을 유지하는 파싱 계약을 검증한다. */
class ParsedPracticeSentenceTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final List<String> canonical = List.of("나는", "오늘", "쉬어");

  @Test
  void missingAcceptedAnswersUsesCanonicalAnswer() throws Exception {
    assertThat(ParsedPracticeSentence.from(example()).sentenceTranslateAcceptedAnswers())
        .containsExactly(canonical);
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "[]", "{}", "\"invalid\"", "[\"나는\",\"오늘\",\"쉬어\"]"})
  void malformedAcceptedAnswersUsesCanonicalAnswer(String json) throws Exception {
    ObjectNode example = example();
    example.set("sentenceTranslateAcceptedAnswers", mapper.readTree(json));
    assertThat(ParsedPracticeSentence.from(example).sentenceTranslateAcceptedAnswers())
        .containsExactly(canonical);
  }

  @Test
  void keepsCanonicalFirstAndRetainsDistinctValidOrders() throws Exception {
    ObjectNode example = example();
    example.set(
        "sentenceTranslateAcceptedAnswers",
        mapper.readTree(
            """
            [["오늘","나는","쉬어"], ["오늘","나는","쉬어"], ["나는","쉬어","오늘"],
             null, [], ["나는","오늘"], ["나는","오늘","오답"], ["나는","오늘",3],
             ["나는","오늘",null], ["나는","오늘"," "]]
            """));
    assertThat(ParsedPracticeSentence.from(example).sentenceTranslateAcceptedAnswers())
        .containsExactly(canonical, List.of("오늘", "나는", "쉬어"), List.of("나는", "쉬어", "오늘"));
  }

  @Test
  void rejectsSameTokenSetWithDifferentMultiplicity() throws Exception {
    ObjectNode example = example();
    example.set("sentenceTranslateWords", mapper.readTree("[\"다시\",\"다시\",\"시도해\"]"));
    example.set(
        "sentenceTranslateAcceptedAnswers",
        mapper.readTree(
            """
            [["다시","시도해","시도해"], ["다시","시도해","다시"]]
            """));
    assertThat(ParsedPracticeSentence.from(example).sentenceTranslateAcceptedAnswers())
        .containsExactly(List.of("다시", "다시", "시도해"), List.of("다시", "시도해", "다시"));
  }

  private ObjectNode example() throws Exception {
    return (ObjectNode)
        mapper.readTree(
            """
            {"sentenceText":"I'm resting today.","highlightingPart":"resting",
             "sentenceTranslation":"나는 오늘 쉬어.","practiceQuestion":"What are you doing today?",
             "practiceQuestionTranslation":"오늘 뭐 해?","sentenceWords":["I'm","resting","today"],
             "sentenceWordChoices":["resting","I'm","today","tomorrow"],
             "sentenceTranslateWords":["나는","오늘","쉬어"],
             "sentenceTranslateWordChoices":["오늘","나는","쉬어","내일"]}
            """);
  }
}
