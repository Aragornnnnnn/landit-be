// LAN-584 영어 칩 명세의 범위와 원문 보존 및 API 호환성을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.landit.landitbe.feature.content.expression.practice.dto.ParsedPracticeSentence;
import com.landit.landitbe.feature.content.expression.practice.dto.WritingSentenceResponse;
import com.landit.landitbe.shared.domain.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Lan584EnglishQuizMigrationTests {

  static final String MIGRATION = "V139__correct_english_quiz_chips.sql";
  static final ObjectMapper MAPPER = new ObjectMapper();

  @DisplayName("검토 명세는 기존 4,000개 표현의 첫 두 예문만 포함한다.")
  @Test
  void pinsReviewedManifestAndScope() throws Exception {
    String manifest = manifestText();
    assertThat(
            HexFormat.of()
                .formatHex(
                    MessageDigest.getInstance("SHA-256")
                        .digest(manifest.getBytes(StandardCharsets.UTF_8))))
        .isEqualTo("d87b35468cdca1feedf2b9f063e9ee3eb7c32be680126533a35ee85bd1edbd47");
    var keys = new HashSet<String>();
    var ids = new HashSet<Long>();
    int production = 0;
    for (JsonNode patch : readManifest()) {
      long id = patch.path("expressionId").asLong();
      int number = patch.path("exampleNumber").asInt();
      assertThat(id).isBetween(1L, 4000L);
      assertThat(number).isIn(1, 2);
      assertThat(keys.add(id + "." + number)).isTrue();
      ids.add(id);
      production += id <= 3000 ? 1 : 0;
    }
    assertThat(keys).hasSize(275);
    assertThat(ids).hasSize(189);
    assertThat(production).isEqualTo(226);
  }

  @DisplayName("칩을 묶어도 원래 영어 단어열과 오답 개수가 유지되고 중복 정답도 충분하다.")
  @Test
  void preservesSentenceAndDistractorCounts() throws Exception {
    for (JsonNode patch : readManifest()) {
      JsonNode original = patch.path("expected");
      List<String> words = tokens(patch.path("sentenceWords"));
      List<String> choices = tokens(patch.path("sentenceWordChoices"));
      assertThat(normalizeWords(words))
          .isEqualTo(normalizeWords(tokens(original.path("sentenceWords"))));
      assertThat(normalizeSentence(String.join(" ", words)))
          .isEqualTo(normalizeSentence(original.path("sentenceText").asText()));
      assertThat(words.size()).isGreaterThanOrEqualTo(3);
      assertThat(choices.size() - words.size())
          .isEqualTo(
              original.path("sentenceWordChoices").size() - original.path("sentenceWords").size());
      Map<String, Integer> counts = counts(choices);
      counts(words)
          .forEach(
              (word, count) ->
                  assertThat(counts.getOrDefault(word, 0))
                      .as("expression %s: %s", patch.path("expressionId"), word)
                      .isGreaterThanOrEqualTo(count));
      for (JsonNode review : patch.path("review")) {
        if (review.path("kind").asText().equals("distractor")) {
          assertThat(choices).doesNotContain(review.path("before").asText());
        }
      }
    }
  }

  @DisplayName("영어 묶음 칩은 정답 하나로 전달되며 한국어 배열은 그대로 유지된다.")
  @Test
  void preservesEnglishChipsAndKoreanAnswersThroughApi() throws Exception {
    for (JsonNode patch : readManifest()) {
      ObjectNode example = patch.path("expected").deepCopy();
      example.put("highlightingPart", "fixture");
      example.set("sentenceWords", patch.path("sentenceWords"));
      example.set("sentenceWordChoices", patch.path("sentenceWordChoices"));
      example.putArray("sentenceTranslateWords").add("한국어").add("유지");
      example.putArray("sentenceTranslateWordChoices").add("유지").add("한국어").add("오답");
      example.set(
          "sentenceTranslateAcceptedAnswers",
          MAPPER.readTree("[[\"한국어\",\"유지\"],[\"유지\",\"한국어\"]]"));
      ParsedPracticeSentence parsed = ParsedPracticeSentence.from(example);
      WritingSentenceResponse english = WritingSentenceResponse.from(parsed, Locale.EN);
      WritingSentenceResponse korean = WritingSentenceResponse.from(parsed, Locale.KR);
      assertThat(english.writingSentenceWords()).isEqualTo(tokens(patch.path("sentenceWords")));
      assertThat(english.writingSentenceWordChoices())
          .isEqualTo(tokens(patch.path("sentenceWordChoices")));
      assertThat(english.writingSentenceAcceptedAnswers())
          .containsExactly(english.writingSentenceWords());
      assertThat(korean.writingSentenceWords()).containsExactly("한국어", "유지");
      assertThat(korean.writingSentenceAcceptedAnswers())
          .containsExactly(List.of("한국어", "유지"), List.of("유지", "한국어"));
    }
  }

  @DisplayName("제보된 story 보기는 제거하고 stories 정답과 문장은 유지한다.")
  @Test
  void fixesReportedStoryChoice() throws Exception {
    JsonNode patch = null;
    for (JsonNode candidate : readManifest()) {
      if (candidate.path("expressionId").asInt() == 1944) {
        patch = candidate;
      }
    }
    assertThat(patch).isNotNull();
    assertThat(patch.path("exampleNumber").asInt()).isEqualTo(2);
    assertThat(tokens(patch.path("sentenceWords")))
        .containsExactly("What", "I", "like", "about", "movies", "is", "the", "stories");
    assertThat(tokens(patch.path("sentenceWordChoices")))
        .contains("stories")
        .doesNotContain("story");
  }

  static String readSql() throws Exception {
    try (var input = new ClassPathResource("db/postgresql/" + MIGRATION).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  static JsonNode readManifest() throws Exception {
    return MAPPER.readTree(manifestText());
  }

  private static String manifestText() throws Exception {
    String sql = readSql();
    String delimiter = "$lan584_data$";
    int start = sql.indexOf(delimiter) + delimiter.length();
    return sql.substring(start, sql.indexOf(delimiter, start)).strip();
  }

  private static String normalizeWords(List<String> words) {
    return String.join(" ", words).replace('-', ' ').replace("'", "");
  }

  private static String normalizeSentence(String sentence) {
    return sentence.replace('’', '\'').replace("'", "").replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
  }

  private static List<String> tokens(JsonNode node) {
    List<String> tokens = new ArrayList<>();
    assertThat(node.isArray()).isTrue();
    for (JsonNode value : node) {
      assertThat(value.isTextual()).isTrue();
      assertThat(value.asText()).isNotBlank();
      tokens.add(value.asText());
    }
    return tokens;
  }

  private static Map<String, Integer> counts(List<String> tokens) {
    Map<String, Integer> counts = new HashMap<>();
    tokens.forEach(token -> counts.merge(token, 1, Integer::sum));
    return counts;
  }
}
