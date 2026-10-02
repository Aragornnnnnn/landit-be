// 승인된 예외 12건의 본문 교정과 양방향 퀴즈 데이터 일치를 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.landit.landitbe.feature.content.expression.practice.dto.ParsedPracticeSentence;
import com.landit.landitbe.feature.content.expression.practice.dto.WritingSentenceResponse;
import com.landit.landitbe.shared.domain.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Lan584QuizTextMigrationTests {

  static final String MIGRATION = "V140__correct_reviewed_quiz_texts.sql";

  @DisplayName("추가 승인된 12개 예문의 필요한 필드만 교정한다.")
  @Test
  void pinsApprovedTextChanges() throws Exception {
    assertThat(
            HexFormat.of()
                .formatHex(
                    MessageDigest.getInstance("SHA-256")
                        .digest(manifestText().getBytes(StandardCharsets.UTF_8))))
        .isEqualTo("7718418736e31561dce4ff7849dcd719a4715627a617b6e60e0fa540e3c139ce");
    Set<String> keys = new HashSet<>();
    for (JsonNode patch : readManifest()) {
      keys.add(patch.path("expressionId").asText() + "." + patch.path("exampleNumber").asText());
      patch
          .path("changes")
          .fieldNames()
          .forEachRemaining(
              field ->
                  assertThat(field)
                      .isIn(
                          "sentenceText",
                          "sentenceTranslation",
                          "sentenceWords",
                          "sentenceWordChoices",
                          "sentenceTranslateWords",
                          "sentenceTranslateWordChoices",
                          "sentenceTranslateAcceptedAnswers",
                          "practiceQuestion",
                          "practiceQuestionTranslation",
                          "highlightingPart"));
    }
    assertThat(keys)
        .containsExactlyInAnyOrder(
            "435.2", "910.2", "1254.1", "1269.1", "1462.1", "1467.2", "1497.2", "1508.2", "1712.2",
            "2026.2", "2115.1", "2410.1");
  }

  @DisplayName("영어 및 한국어 문장·정답·보기·복수 정답을 API까지 일치시킨다.")
  @Test
  void keepsBothQuizLanguagesConsistent() throws Exception {
    for (JsonNode patch : readManifest()) {
      ObjectNode original = (ObjectNode) patch.path("expected");
      ObjectNode example = original.deepCopy();
      example.setAll((ObjectNode) patch.path("changes"));
      assertThat(example.path("sentenceText").asText())
          .contains(example.path("highlightingPart").asText());
      assertThat(normalize(String.join(" ", tokens(example.path("sentenceWords")))))
          .isEqualTo(normalize(example.path("sentenceText").asText()));
      assertThat(normalize(String.join(" ", tokens(example.path("sentenceTranslateWords")))))
          .isEqualTo(normalize(example.path("sentenceTranslation").asText()));
      ParsedPracticeSentence parsed = ParsedPracticeSentence.from(example);
      for (Locale locale : List.of(Locale.EN, Locale.KR)) {
        String prefix = locale == Locale.EN ? "sentence" : "sentenceTranslate";
        WritingSentenceResponse response = WritingSentenceResponse.from(parsed, locale);
        List<String> words = tokens(example.path(prefix + "Words"));
        List<String> choices = tokens(example.path(prefix + "WordChoices"));
        assertThat(response.writingSentenceWords()).isEqualTo(words);
        assertThat(response.writingSentenceWordChoices()).isEqualTo(choices);
        assertThat(choices.size() - words.size())
            .isEqualTo(
                original.path(prefix + "WordChoices").size()
                    - original.path(prefix + "Words").size());
        assertThat(response.writingSentenceAcceptedAnswers()).contains(words);
        for (List<String> accepted : response.writingSentenceAcceptedAnswers()) {
          for (String token : accepted) {
            assertThat(Collections.frequency(choices, token))
                .isGreaterThanOrEqualTo(Collections.frequency(accepted, token));
          }
        }
        if (locale == Locale.KR) {
          List<List<String>> expected = new ArrayList<>();
          for (JsonNode answer : example.path("sentenceTranslateAcceptedAnswers")) {
            expected.add(tokens(answer));
          }
          assertThat(response.writingSentenceAcceptedAnswers()).isEqualTo(expected);
        }
      }
    }
  }

  @DisplayName("Maybe의 의미가 번역과 모든 한국어 정답에 포함된다.")
  @Test
  void includesUncertaintyInEveryKoreanAnswer() throws Exception {
    for (JsonNode patch : readManifest()) {
      if (patch.path("expressionId").asInt() == 2026) {
        JsonNode changes = patch.path("changes");
        assertThat(changes.path("sentenceTranslation").asText()).isEqualTo("아마 언젠가는.");
        for (JsonNode answer : changes.path("sentenceTranslateAcceptedAnswers")) {
          assertThat(tokens(answer)).containsExactlyInAnyOrder("아마", "언젠가는");
        }
        assertThat(changes.has("sentenceWords")).isFalse();
      }
    }
  }

  static String readSql() throws Exception {
    try (var input = new ClassPathResource("db/postgresql/" + MIGRATION).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  static JsonNode readManifest() throws Exception {
    return Lan584EnglishQuizMigrationTests.MAPPER.readTree(manifestText());
  }

  private static String manifestText() throws Exception {
    return readSql().split("\\$lan584_text\\$")[1].strip();
  }

  private static List<String> tokens(JsonNode array) {
    List<String> result = new ArrayList<>();
    for (JsonNode token : array) {
      result.add(token.asText());
    }
    return result;
  }

  private static String normalize(String text) {
    return text.replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
  }
}
