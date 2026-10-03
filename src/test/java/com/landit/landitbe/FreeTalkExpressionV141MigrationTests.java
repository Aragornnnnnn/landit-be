// V141의 검수 원문 보존과 전체 이미지·퀴즈·임베딩 소비 계약을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.feature.content.expression.practice.dto.ParsedPracticeSentence;
import com.landit.landitbe.feature.content.expression.practice.dto.WritingSentenceResponse;
import com.landit.landitbe.feature.content.expression.practice.service.ExpressionPracticeService;
import com.landit.landitbe.shared.domain.Locale;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 검수된 1,672개 표현의 원본 데이터와 5,016개 게시 이미지 연결을 검사한다. */
class FreeTalkExpressionV141MigrationTests {

  private static final Path MIGRATION =
      Path.of(
          "src/main/resources/db/postgresql/"
              + "V141__insert_reviewed_free_talk_expressions_4329_6000.sql");
  private static final Path ASSETS = Path.of("docs/tasks/LAN-610/image-assets.jsonl");
  private static final String CDN = "https://d19azau1un4t7r.cloudfront.net/content/";
  private static final Pattern PRACTICE = Pattern.compile("'(\\[\\{.*}])'::jsonb");
  private static final Pattern VECTOR = Pattern.compile("'\\[([^]]+)]'::extensions\\.vector");
  private static final String UUID_WEBP =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp";
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  @DisplayName("이미지 URL을 제외한 검수 문구·정답·보기·임베딩을 바이트 단위로 보존한다.")
  void preservesEveryApprovedNonImageValue() throws Exception {
    List<String> originalRows =
        rows().stream()
            .map(row -> row.replaceAll("'https://d19azau1un4t7r[.]cloudfront[.]net/[^']+'", "NULL"))
            .map(
                row ->
                    row.replaceAll("\"https://d19azau1un4t7r[.]cloudfront[.]net/[^\"]+\"", "null"))
            .toList();
    byte[] bytes = (String.join("\n", originalRows) + "\n").getBytes(StandardCharsets.UTF_8);
    assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)))
        .isEqualTo("17fa28c525b6e9cae7efc9c1073ce20f943f360000b0ab94ae58faf4ffa74723");
  }

  @Test
  @DisplayName("대표·예문 3·4번에 같은 표현의 검증된 고유 이미지 URL을 연결한다.")
  void mapsAllImagesToTheirApprovedExpressionAndExample() throws Exception {
    List<String> rows = rows();
    Map<String, String> approved = approvedUrls();
    Set<String> urls = new HashSet<>();
    for (int offset = 0; offset < rows.size(); offset++) {
      String row = rows.get(offset);
      int id = 4329 + offset;
      assertThat(row).startsWith("(" + id + ",NULL,");
      assertThat(row.split(",", 9)[6]).isEqualTo(String.valueOf(id - 811));
      Matcher representative =
          Pattern.compile(
                  Pattern.quote(CDN + "writing-expressions/" + id + "/representative/") + UUID_WEBP)
              .matcher(row);
      assertThat(representative.find()).isTrue();
      assertThat(representative.group()).isEqualTo(approved.get(id + ":0"));
      assertThat(urls.add(representative.group())).isTrue();
      assertThat(representative.find()).isFalse();
      verifyPracticeImages(examples(row), id, approved, urls);
    }
    assertThat(urls).hasSize(5016);
  }

  @Test
  @DisplayName("모든 표현을 영어·한국어 퀴즈와 뒤쪽 이미지 예문으로 손실 없이 소비한다.")
  void servesEveryReviewedQuizWithoutDroppingAnExampleOrAcceptedAnswer() throws Exception {
    ExpressionPracticeService service = new ExpressionPracticeService();
    for (String row : rows()) {
      long id = Long.parseLong(row.substring(1, row.indexOf(',')));
      JsonNode examples = examples(row);
      var response = service.buildPracticeResponse(id, "expression", "meaning", "usage", examples);
      assertThat(response.practiceSentence()).hasSize(2);
      assertThat(response.practiceSentence().getFirst().imageUrl())
          .isEqualTo(examples.get(2).path("imageUrl").asText());
      assertThat(response.practiceSentence().getLast().imageUrl())
          .isEqualTo(examples.get(3).path("imageUrl").asText());
      assertThat(response.writingSentence())
          .extracting(WritingSentenceResponse::quizLanguage)
          .containsExactlyInAnyOrder(Locale.EN, Locale.KR);
      for (JsonNode example : examples) {
        verifyQuizArrays(example);
      }
    }
  }

  @Test
  @DisplayName("모든 신규 프리톡 표현에 유한한 1,536차원 임베딩을 보존한다.")
  void retainsEveryCompleteEmbedding() throws Exception {
    for (String row : rows()) {
      Matcher vector = VECTOR.matcher(row);
      assertThat(vector.find()).isTrue();
      String[] coordinates = vector.group(1).split(",");
      assertThat(coordinates).hasSize(1536);
      assertThat(coordinates).allMatch(value -> Double.isFinite(Double.parseDouble(value)));
      assertThat(coordinates).anyMatch(value -> Double.parseDouble(value) != 0);
      assertThat(row).contains(",'FREE_TALK','ACTIVE',");
    }
  }

  private void verifyPracticeImages(
      JsonNode examples, int id, Map<String, String> approved, Set<String> urls) {
    assertThat(examples.size()).isEqualTo(4);
    for (int index = 0; index < 4; index++) {
      JsonNode image = examples.get(index).path("imageUrl");
      if (index < 2) {
        assertThat(image.isNull()).isTrue();
      } else {
        assertThat(image.asText()).isEqualTo(approved.get(id + ":" + (index + 1)));
        assertThat(image.asText())
            .matches(Pattern.quote(CDN + "expressions/" + id + "/practice-examples/") + UUID_WEBP);
        assertThat(urls.add(image.asText())).isTrue();
      }
    }
  }

  private void verifyQuizArrays(JsonNode example) {
    ParsedPracticeSentence parsed = ParsedPracticeSentence.from(example);
    for (Locale language : List.of(Locale.EN, Locale.KR)) {
      WritingSentenceResponse quiz = WritingSentenceResponse.from(parsed, language);
      assertThat(quiz.writingSentenceWords()).isNotEmpty().allMatch(word -> !word.isBlank());
      assertThat(counts(quiz.writingSentenceWordChoices()))
          .satisfies(
              choices ->
                  counts(quiz.writingSentenceWords())
                      .forEach(
                          (word, count) ->
                              assertThat(choices.get(word)).isGreaterThanOrEqualTo(count)));
      assertThat(quiz.writingSentenceAcceptedAnswers().getFirst())
          .containsExactlyElementsOf(quiz.writingSentenceWords());
    }
    JsonNode expected = example.get("sentenceTranslateAcceptedAnswers");
    if (expected == null) {
      expected = mapper.createArrayNode().add(example.get("sentenceTranslateWords"));
    }
    assertThat(mapper.<JsonNode>valueToTree(parsed.sentenceTranslateAcceptedAnswers()))
        .isEqualTo(expected);
  }

  private Map<String, Integer> counts(List<String> words) {
    Map<String, Integer> counts = new HashMap<>();
    words.forEach(word -> counts.merge(word, 1, Integer::sum));
    return counts;
  }

  private JsonNode examples(String row) throws Exception {
    Matcher practice = PRACTICE.matcher(row);
    assertThat(practice.find()).isTrue();
    return mapper.readTree(practice.group(1).replace("''", "'"));
  }

  private List<String> rows() throws Exception {
    assertThat(MIGRATION).exists();
    List<String> rows = Files.readString(MIGRATION).lines().filter(x -> x.startsWith("(")).toList();
    assertThat(rows).hasSize(1672);
    return rows;
  }

  private Map<String, String> approvedUrls() throws Exception {
    Map<String, String> approved = new HashMap<>();
    for (String line : Files.readAllLines(ASSETS)) {
      JsonNode asset = mapper.readTree(line);
      String key = asset.path("expressionId").asInt() + ":" + asset.path("exampleNumber").asInt();
      assertThat(approved.put(key, asset.path("cloudFrontUrl").asText())).isNull();
      assertThat(asset.path("sha256").asText()).matches("[0-9a-f]{64}");
      assertThat(asset.path("byteSize").asLong()).isPositive();
    }
    assertThat(approved).hasSize(5016);
    return approved;
  }
}
