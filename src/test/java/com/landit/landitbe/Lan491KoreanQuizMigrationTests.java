// LAN-491 마이그레이션 데이터가 검토한 복수 정답과 보기 수정 범위를 보존하는지 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.landit.landitbe.feature.content.expression.practice.dto.ParsedPracticeSentence;
import com.landit.landitbe.feature.content.expression.practice.dto.WritingSentenceResponse;
import com.landit.landitbe.shared.domain.Locale;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** 검토본과 SQL의 6,000개 매핑, 토큰 중복 횟수와 기존 응답 호환성을 검증한다. */
class Lan491KoreanQuizMigrationTests {

  static final String MIGRATION_PATH = "db/postgresql/V106__add_korean_quiz_accepted_answers.sql";

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Path DOCS = Path.of("docs/tasks/LAN-491");

  @Test
  void embedsExactlyTheReviewedSixThousandExamplesAndThirtyThreeChoiceEdits() throws Exception {
    Map<String, JsonNode> reviewed = readByKey("answers-6000.jsonl");
    Map<String, JsonNode> removals = readByKey("distractor-removals.jsonl");
    Set<String> seen = new HashSet<>();
    Set<Long> expressionIds = new HashSet<>();
    int changedChoices = 0;
    for (JsonNode mapping : readManifest()) {
      String key = key(mapping);
      assertThat(seen.add(key)).as(key).isTrue();
      assertThat(mapping.path("practiceExampleNumber").asInt()).isIn(1, 2);
      expressionIds.add(mapping.path("expressionId").asLong());
      JsonNode answer = reviewed.get(key);
      assertThat(answer).as(key).isNotNull();
      ObjectNode expected = expectedExample(answer);
      if (removals.containsKey(key)) {
        JsonNode removal = removals.get(key);
        expected.set("sentenceTranslateWordChoices", removal.get("before"));
        assertThat(mapping.get("wordChoices")).isEqualTo(removal.get("after"));
        assertSingleDistractorRemoval(removal);
        changedChoices++;
      }
      assertThat(mapping.get("expectedExample")).as(key).isEqualTo(expected);
      assertThat(mapping.get("expressionSource")).isEqualTo(answer.get("expressionSource"));
      assertThat(mapping.get("acceptedAnswers"))
          .as(key)
          .isEqualTo(answer.get("sentenceTranslateAcceptedAnswers"));
      assertThat(mapping.get("wordChoices")).isEqualTo(answer.get("sentenceTranslateWordChoices"));
    }
    assertThat(seen).hasSize(6000).containsExactlyInAnyOrderElementsOf(reviewed.keySet());
    assertThat(expressionIds).hasSize(3000);
    assertThat(removals).hasSize(33);
    assertThat(changedChoices).isEqualTo(33);
  }

  @Test
  void preservesCanonicalOrderAndTokenMultiplicityInEveryAcceptedAnswer() throws Exception {
    int acceptedCount = 0;
    int singleAnswerCount = 0;
    for (JsonNode mapping : readManifest()) {
      JsonNode canonical = mapping.path("expectedExample").get("sentenceTranslateWords");
      JsonNode accepted = mapping.get("acceptedAnswers");
      assertThat(accepted.isArray()).isTrue();
      assertThat(accepted.get(0)).isEqualTo(canonical);
      Set<JsonNode> unique = new HashSet<>();
      for (JsonNode answer : accepted) {
        assertThat(unique.add(answer)).as(key(mapping)).isTrue();
        assertThat(tokenCounts(answer)).as(key(mapping)).isEqualTo(tokenCounts(canonical));
      }
      Map<String, Integer> choices = tokenCounts(mapping.get("wordChoices"));
      tokenCounts(canonical)
          .forEach(
              (token, count) ->
                  assertThat(choices.getOrDefault(token, 0))
                      .as(key(mapping))
                      .isGreaterThanOrEqualTo(count));
      acceptedCount += accepted.size();
      singleAnswerCount += accepted.size() == 1 ? 1 : 0;
    }
    assertThat(acceptedCount).isEqualTo(23392);
    assertThat(singleAnswerCount).isEqualTo(124);
  }

  @Test
  void newPayloadKeyPreservesExistingKoreanAndEnglishResponseParsing() throws Exception {
    JsonNode mapping = readManifest().get(0);
    ObjectNode example = mapping.path("expectedExample").deepCopy();
    example.put("highlightingPart", "There's nothing like");
    example.putArray("sentenceWords").add("English");
    example.putArray("sentenceWordChoices").add("English").add("distractor");
    example.set("sentenceTranslateAcceptedAnswers", mapping.get("acceptedAnswers"));
    example.set("sentenceTranslateWordChoices", mapping.get("wordChoices"));
    ParsedPracticeSentence parsed = ParsedPracticeSentence.from(example);
    WritingSentenceResponse korean = WritingSentenceResponse.from(parsed, Locale.KR);
    WritingSentenceResponse english = WritingSentenceResponse.from(parsed, Locale.EN);
    assertThat(MAPPER.<JsonNode>valueToTree(korean.writingSentenceWords()))
        .isEqualTo(example.get("sentenceTranslateWords"));
    assertThat(MAPPER.<JsonNode>valueToTree(korean.writingSentenceWordChoices()))
        .isEqualTo(mapping.get("wordChoices"));
    assertThat(english.writingSentenceWords()).containsExactly("English");
    assertThat(english.writingSentenceWordChoices()).containsExactly("English", "distractor");
    assertThat(english.writingSentenceAcceptedAnswers()).containsExactly(List.of("English"));
    assertThat(MAPPER.<JsonNode>valueToTree(korean.writingSentenceAcceptedAnswers()))
        .isEqualTo(mapping.get("acceptedAnswers"));
  }

  @Test
  void deliversEveryReviewedAnswerThroughKoreanResponseWithoutTruncation() throws Exception {
    for (JsonNode mapping : readManifest()) {
      ObjectNode example = mapping.path("expectedExample").deepCopy();
      example.put("highlightingPart", "fixture");
      example.putArray("sentenceWords").add("English");
      example.putArray("sentenceWordChoices").add("English").add("distractor");
      example.set("sentenceTranslateAcceptedAnswers", mapping.get("acceptedAnswers"));
      example.set("sentenceTranslateWordChoices", mapping.get("wordChoices"));
      WritingSentenceResponse korean =
          WritingSentenceResponse.from(ParsedPracticeSentence.from(example), Locale.KR);
      assertThat(MAPPER.<JsonNode>valueToTree(korean.writingSentenceAcceptedAnswers()))
          .as(key(mapping))
          .isEqualTo(mapping.get("acceptedAnswers"));
    }
  }

  static String readSql() throws Exception {
    try (var input = new ClassPathResource(MIGRATION_PATH).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  static JsonNode readManifest() throws Exception {
    String sql = readSql();
    String delimiter = "$lan491_data$";
    int start = sql.indexOf(delimiter) + delimiter.length();
    return MAPPER.readTree(sql.substring(start, sql.indexOf(delimiter, start)));
  }

  private Map<String, JsonNode> readByKey(String file) throws Exception {
    Map<String, JsonNode> records = new HashMap<>();
    for (String line : Files.readAllLines(DOCS.resolve(file))) {
      JsonNode record = MAPPER.readTree(line);
      assertThat(records.put(key(record), record)).isNull();
    }
    return records;
  }

  private ObjectNode expectedExample(JsonNode answer) {
    ObjectNode expected = answer.deepCopy();
    return expected.retain(
        List.of(
            "sentenceText",
            "sentenceTranslation",
            "practiceQuestion",
            "practiceQuestionTranslation",
            "sentenceTranslateWords",
            "sentenceTranslateWordChoices"));
  }

  private void assertSingleDistractorRemoval(JsonNode removal) {
    Map<String, Integer> before = tokenCounts(removal.get("before"));
    Map<String, Integer> after = tokenCounts(removal.get("after"));
    String removed = removal.path("removedWord").asText();
    assertThat(tokenCounts(removal.get("sentenceTranslateWords"))).doesNotContainKey(removed);
    after.merge(removed, 1, Integer::sum);
    assertThat(after).isEqualTo(before);
    assertThat(removal.get("before").size() - removal.get("sentenceTranslateWords").size())
        .isEqualTo(4);
  }

  private Map<String, Integer> tokenCounts(JsonNode array) {
    assertThat(array.isArray()).isTrue();
    Map<String, Integer> counts = new HashMap<>();
    for (JsonNode token : array) {
      assertThat(token.isTextual()).isTrue();
      assertThat(token.asText()).isNotBlank();
      counts.merge(token.asText(), 1, Integer::sum);
    }
    return counts;
  }

  private static String key(JsonNode record) {
    return record.path("expressionId").asLong()
        + ":"
        + record.path("practiceExampleNumber").asInt();
  }
}
