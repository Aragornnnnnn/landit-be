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
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** 검토본과 SQL의 6,000개 매핑, 토큰 중복 횟수와 기존 응답 호환성을 검증한다. */
class Lan491KoreanQuizMigrationTests {

  static final String MIGRATION_PATH = "db/postgresql/V109__add_korean_quiz_accepted_answers.sql";

  private static final ObjectMapper MAPPER = new ObjectMapper();
  // 검토 JSONL에서 독립적으로 복원한 매핑과 일치함을 확인한 뒤 고정한 UTF-8 체크섬이다.
  private static final String REVIEWED_MANIFEST_SHA256 =
      "4863d2d5272b551e7379e3f7efeb9e6e05ac7a06a8cd54d51033e21902edffb6";

  @Test
  void preservesReviewedManifestAndExactlyScopedChoiceEdits() throws Exception {
    String manifest = readManifestText();
    assertThat(
            HexFormat.of()
                .formatHex(
                    MessageDigest.getInstance("SHA-256")
                        .digest(manifest.getBytes(StandardCharsets.UTF_8))))
        .isEqualTo(REVIEWED_MANIFEST_SHA256);
    Set<String> seen = new HashSet<>();
    Set<Long> expressionIds = new HashSet<>();
    int changedChoices = 0;
    for (JsonNode mapping : MAPPER.readTree(manifest)) {
      String key = key(mapping);
      assertThat(seen.add(key)).as(key).isTrue();
      assertThat(mapping.path("practiceExampleNumber").asInt()).isIn(1, 2);
      expressionIds.add(mapping.path("expressionId").asLong());
      assertThat(mapping.path("expressionSource").asText()).isIn("SCENARIO", "FREE_TALK");
      if (!mapping
          .path("expectedExample")
          .get("sentenceTranslateWordChoices")
          .equals(mapping.get("wordChoices"))) {
        assertSingleDistractorRemoval(mapping);
        changedChoices++;
      }
    }
    assertThat(seen).hasSize(6000);
    assertThat(expressionIds).hasSize(3000);
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
    return MAPPER.readTree(readManifestText());
  }

  private static String readManifestText() throws Exception {
    String sql = readSql();
    String delimiter = "$lan491_data$";
    int start = sql.indexOf(delimiter) + delimiter.length();
    return sql.substring(start, sql.indexOf(delimiter, start));
  }

  private void assertSingleDistractorRemoval(JsonNode mapping) {
    JsonNode before = mapping.path("expectedExample").get("sentenceTranslateWordChoices");
    JsonNode after = mapping.get("wordChoices");
    JsonNode canonical = mapping.path("expectedExample").get("sentenceTranslateWords");
    Map<String, Integer> removed = tokenCounts(before);
    tokenCounts(after).forEach((word, count) -> removed.merge(word, -count, Integer::sum));
    removed.values().removeIf(count -> count == 0);
    assertThat(removed).hasSize(1);
    var deletion = removed.entrySet().iterator().next();
    assertThat(deletion.getValue()).isEqualTo(1);
    assertThat(tokenCounts(canonical)).doesNotContainKey(deletion.getKey());
    assertThat(before.size() - canonical.size()).isEqualTo(4);
    assertThat(after.size() - canonical.size()).isEqualTo(3);
    assertThat(MAPPER.convertValue(before, String[].class))
        .containsSubsequence(MAPPER.convertValue(after, String[].class));
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
