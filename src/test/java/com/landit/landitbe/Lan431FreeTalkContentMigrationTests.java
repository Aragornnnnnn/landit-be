// LAN-431 표현·임베딩·이미지의 행별 대응과 단일 마이그레이션 계약을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** LAN-431의 742개 표현과 2,226개 이미지 매핑을 회귀 검증한다. */
class Lan431FreeTalkContentMigrationTests {

  private static final String PATH = "db/postgresql/V94__insert_lan431_free_talk_expressions.sql";
  private static final String CDN = "https://d19azau1un4t7r.cloudfront.net/content/";
  private static final Pattern PRACTICE = Pattern.compile("'(\\[\\{.*}])'");
  private static final Pattern VECTOR = Pattern.compile("'\\[([^]]+)]'::extensions\\.vector");
  private static final String UUID_WEBP =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp";

  @Test
  void mapsEveryExpressionToItsOwnRepresentativeAndLastTwoPracticeImages() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> approvedUrls = readApprovedUrls(mapper);
    Set<String> urls = new HashSet<>();
    List<String> rows = readSql().lines().filter(line -> line.startsWith("(")).toList();
    assertThat(rows).hasSize(742);
    for (int offset = 0; offset < rows.size(); offset++) {
      String row = rows.get(offset);
      int id = 2259 + offset;
      assertThat(row).startsWith("(" + id + ",NULL,");
      assertThat(row.split(",", 9)[6]).isEqualTo(String.valueOf(1776 + offset));
      Matcher representative =
          Pattern.compile(
                  Pattern.quote(CDN + "writing-expressions/" + id + "/representative/") + UUID_WEBP)
              .matcher(row);
      assertThat(representative.find()).isTrue();
      assertThat(representative.group()).isEqualTo(approvedUrls.get(id + ":0"));
      assertThat(urls.add(representative.group())).isTrue();
      assertThat(representative.find()).isFalse();

      Matcher practice = PRACTICE.matcher(row);
      assertThat(practice.find()).isTrue();
      JsonNode examples = mapper.readTree(practice.group(1).replace("''", "'"));
      assertThat(examples.isArray()).isTrue();
      assertThat(examples.size()).isEqualTo(4);
      for (int index = 0; index < 4; index++) {
        JsonNode example = examples.get(index);
        assertThat(example.path("sentenceText").asText()).isNotBlank();
        assertThat(example.path("sentenceTranslation").asText()).isNotBlank();
        assertThat(example.path("sentenceTranslateWords").isArray()).isTrue();
        assertThat(example.path("sentenceTranslateWordChoices").isArray()).isTrue();
        if (index < 2) {
          assertThat(example.path("imageUrl").isNull()).isTrue();
        } else {
          String url = example.path("imageUrl").asText();
          assertThat(url).isEqualTo(approvedUrls.get(id + ":" + (index + 1)));
          assertThat(url)
              .matches(
                  Pattern.quote(CDN + "expressions/" + id + "/practice-examples/") + UUID_WEBP);
          assertThat(urls.add(url)).isTrue();
        }
      }
    }
    assertThat(urls).hasSize(2226);
  }

  @Test
  void includesFinite1536DimensionalEmbeddingsAndOnlyFreeTalkRows() throws Exception {
    List<String> rows = readSql().lines().filter(line -> line.startsWith("(")).toList();
    for (String row : rows) {
      Matcher vector = VECTOR.matcher(row);
      assertThat(vector.find()).isTrue();
      String[] coordinates = vector.group(1).split(",");
      assertThat(coordinates).hasSize(1536);
      assertThat(coordinates).allMatch(value -> Double.isFinite(Double.parseDouble(value)));
      assertThat(row).contains(",'FREE_TALK','");
      assertThat(row).matches(".*::extensions\\.vector,[123]\\)[,;]");
    }
    assertThat(rows).hasSize(742);
  }

  @Test
  void guardsCollisionsAndKeepsFlywayTransactionAndSequenceMonotonicity() throws Exception {
    String sql = readSql();
    assertThat(sql)
        .contains(
            "LOCK TABLE writing_expression IN SHARE ROW EXCLUSIVE MODE",
            "LAN-431 expression ID range is already occupied",
            "LAN-431 display order range is already occupied",
            "LAN-431 inserted row count verification failed",
            "LAN-431 expression, embedding or image mapping verification failed",
            "extensions.vector_dims(embedding) <> 1536",
            "pg_get_serial_sequence('writing_expression', 'id')",
            "GREATEST((SELECT max(id) FROM writing_expression), previous_sequence_value)")
        .doesNotContain(
            "owner_user_profile_id", "owner_user_id", "ON CONFLICT", "COMMIT;", "BEGIN;");
    assertThat(sql.lines().filter(line -> line.startsWith("INSERT INTO writing_expression")))
        .hasSize(1);
    assertThat(sql).doesNotContain("UPDATE writing_expression", "DELETE FROM writing_expression");
  }

  private String readSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(PATH).getInputStream(), StandardCharsets.UTF_8);
  }

  private Map<String, String> readApprovedUrls(ObjectMapper mapper) throws Exception {
    Map<String, String> urls = new HashMap<>();
    for (String line : Files.readAllLines(Path.of("docs/tasks/LAN-431/image-assets.jsonl"))) {
      JsonNode asset = mapper.readTree(line);
      String key = asset.path("expressionId").asInt() + ":" + asset.path("exampleNumber").asInt();
      assertThat(urls.put(key, asset.path("cloudFrontUrl").asText())).isNull();
      assertThat(asset.path("sha256").asText()).matches("[0-9a-f]{64}");
      assertThat(asset.path("byteSize").asLong()).isPositive();
    }
    assertThat(urls).hasSize(2226);
    return urls;
  }
}
