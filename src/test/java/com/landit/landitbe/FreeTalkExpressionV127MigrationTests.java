// V127 표현 ID와 검수된 이미지 URL 및 임베딩의 적재 계약을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** V127의 표현 1,000개와 이미지 URL 3,000개의 행별 대응을 검사한다. */
class FreeTalkExpressionV127MigrationTests {

  private static final String PATH = "db/postgresql/V127__insert_1000_free_talk_expressions.sql";
  private static final String CDN = "https://d19azau1un4t7r.cloudfront.net/content/";
  private static final Pattern PRACTICE = Pattern.compile("'(\\[\\{.*}])'::jsonb");
  private static final Pattern VECTOR = Pattern.compile("'\\[([^]]+)]'::extensions\\.vector");
  private static final String UUID_WEBP =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp";

  @DisplayName("ID 3001~4000의 대표 이미지와 추가 예문 3·4의 고유 URL을 매핑한다.")
  @Test
  void mapsAuditedImagesToEveryExpression() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    Set<String> urls = new HashSet<>();
    List<String> rows = rows();
    assertThat(rows).hasSize(1000);
    for (int offset = 0; offset < rows.size(); offset++) {
      String row = rows.get(offset);
      int id = 3001 + offset;
      assertThat(row).startsWith("(" + id + ",NULL,");
      assertThat(row.split(",", 9)[6]).isEqualTo(String.valueOf(2518 + offset));

      Matcher representative =
          Pattern.compile(
                  Pattern.quote(CDN + "writing-expressions/" + id + "/representative/")
                      + "[0-9a-f]{64}\\.webp")
              .matcher(row);
      assertThat(representative.find()).isTrue();
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
        if (index < 2) {
          assertThat(example.path("imageUrl").isMissingNode() || example.path("imageUrl").isNull())
              .isTrue();
        } else {
          String url = example.path("imageUrl").asText();
          assertThat(url)
              .matches(
                  Pattern.quote(CDN + "expressions/" + id + "/practice-examples/") + UUID_WEBP);
          assertThat(urls.add(url)).isTrue();
        }
      }
    }
    assertThat(urls).hasSize(3000);
  }

  @DisplayName("1,000개 표현의 임베딩은 유한한 1,536차원이며 프리톡으로 적재한다.")
  @Test
  void retainsFiniteEmbeddingsAndFreeTalkSource() throws Exception {
    List<String> rows = rows();
    assertThat(rows).hasSize(1000);
    for (String row : rows) {
      Matcher vector = VECTOR.matcher(row);
      assertThat(vector.find()).isTrue();
      String[] coordinates = vector.group(1).split(",");
      assertThat(coordinates).hasSize(1536);
      assertThat(coordinates).allMatch(value -> Double.isFinite(Double.parseDouble(value)));
      assertThat(row).contains(",'FREE_TALK',");
      assertThat(row).matches(".*::extensions\\.vector\\)[,;]");
    }
  }

  @DisplayName("ID·표시 순서 충돌을 막고 Flyway 트랜잭션과 시퀀스 값을 보존한다.")
  @Test
  void guardsCollisionsAndSequence() throws Exception {
    String sql = readSql();
    assertThat(sql)
        .contains(
            "id BETWEEN 3001 AND 4000",
            "display_order BETWEEN 2518 AND 3517",
            "V127 expression ID range is already occupied",
            "V127 display order range is already occupied",
            "V127 inserted row count verification failed",
            "extensions.vector_dims(embedding) <> 1536",
            "GREATEST((SELECT max(id) FROM writing_expression), previous_sequence_value)")
        .doesNotContain(
            "owner_user_profile_id", "ON CONFLICT", "COMMIT;", "UPDATE writing_expression");
    assertThat(sql.lines().filter(line -> line.startsWith("INSERT INTO writing_expression")))
        .hasSize(1);
  }

  private List<String> rows() throws Exception {
    return readSql().lines().filter(line -> line.startsWith("(")).toList();
  }

  private String readSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(PATH).getInputStream(), StandardCharsets.UTF_8);
  }
}
