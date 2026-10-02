// 시나리오 대표 이미지의 게시 해시와 원문 대응 및 변경 범위를 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** LAN-391 대표 이미지 328개의 V136 원문과 게시 매핑 무결성을 검증한다. */
class Lan391ScenarioRepresentativeImageMigrationTests {

  private static final String SOURCE =
      "db/postgresql/V136__insert_scenario_41_70_writing_expressions.sql";
  private static final String UPDATE =
      "db/postgresql/V138__update_scenario_41_70_practice_example_image_urls.sql";
  private static final String REPRESENTATIVE_MARKER = "-- 대표 이미지 328개의 원문과 게시 해시를 검증해 URL을 연결한다.";
  private static final String SQL_TEXT = "'(?:[^']|'')*'";
  private static final Pattern SOURCE_ROW =
      Pattern.compile(
          "(?m)^  \\((\\d+), (\\d+),\\s*(?:"
              + SQL_TEXT
              + ",\\s*){4}(\\d+),\\s*(?:"
              + SQL_TEXT
              + ",\\s*){4}("
              + SQL_TEXT
              + "),\\s*("
              + SQL_TEXT
              + "),\\s*("
              + SQL_TEXT
              + "),\\s*("
              + SQL_TEXT
              + "),\\s*ARRAY");
  private static final Pattern IMAGE_ROW =
      Pattern.compile(
          "(?m)^    \\((\\d+), (\\d+), (\\d+), (\\d+), ("
              + SQL_TEXT
              + "), ("
              + SQL_TEXT
              + "), ("
              + SQL_TEXT
              + "), ("
              + SQL_TEXT
              + "), '([0-9a-f]{64})'\\)[,;]$");
  private static final String MAPPING_SHA256 =
      "7834958aee53b664cb99555633f8db9feda37b3433e69b7686af07e2f3437aed";

  @DisplayName("대표 이미지 328개가 V136의 표현·시나리오·순서와 대표 문장·질문 원문에 대응한다.")
  @Test
  void matchesRepresentativeMapToSourceSentencesAndQuestions() throws Exception {
    List<MatchResult> sourceRows = SOURCE_ROW.matcher(readSql(SOURCE)).results().toList();
    List<MatchResult> imageRows = IMAGE_ROW.matcher(representativeSql()).results().toList();
    assertThat(sourceRows).hasSize(328);
    assertThat(imageRows).hasSize(328);
    for (int rowIndex = 0; rowIndex < sourceRows.size(); rowIndex++) {
      MatchResult source = sourceRows.get(rowIndex);
      MatchResult image = imageRows.get(rowIndex);
      assertThat(Integer.parseInt(image.group(1))).isEqualTo(4001 + rowIndex);
      assertThat(image.group(1)).isEqualTo(source.group(1));
      assertThat(image.group(2)).isEqualTo(source.group(2));
      assertThat(image.group(3)).isEqualTo(source.group(3));
      assertThat(Integer.parseInt(image.group(4))).isEqualTo(rowIndex + 1);
      assertThat(sqlText(image.group(5))).isEqualTo(sqlText(source.group(6)));
      assertThat(sqlText(image.group(6))).isEqualTo(sqlText(source.group(7)));
      assertThat(sqlText(image.group(7))).isEqualTo(sqlText(source.group(4)));
      assertThat(sqlText(image.group(8))).isEqualTo(sqlText(source.group(5)));
    }
  }

  @DisplayName("대표 WebP 해시 328개에 중복이 없고 게시 시 확정한 전체 매핑 SHA-256과 일치한다.")
  @Test
  void matchesPublishedMappingDigestWithoutDuplicateImages() throws Exception {
    List<MatchResult> images = IMAGE_ROW.matcher(representativeSql()).results().toList();
    assertThat(images).hasSize(328);
    Set<String> imageHashes = new HashSet<>();
    StringBuilder digestInput = new StringBuilder();
    for (MatchResult image : images) {
      assertThat(imageHashes.add(image.group(9))).as("expression %s", image.group(1)).isTrue();
      for (int group = 1; group <= 4; group++) {
        digestInput.append(image.group(group)).append(':');
      }
      digestInput.append(image.group(9)).append('\n');
    }
    assertThat(imageHashes).hasSize(328);
    byte[] digest =
        MessageDigest.getInstance("SHA-256")
            .digest(digestInput.toString().getBytes(StandardCharsets.UTF_8));
    assertThat(HexFormat.of().formatHex(digest)).isEqualTo(MAPPING_SHA256);
  }

  @DisplayName("대표 이미지 변경은 기존 원문과 미설정 URL을 검사하고 다른 학습 필드를 수정하지 않는다.")
  @Test
  void guardsSourceStateAndUpdatesOnlyRepresentativeImageUrl() throws Exception {
    String sql = representativeSql();
    assertThat(sql)
        .contains(
            "CREATE TEMP TABLE lan391_representative_image_map",
            "IF (SELECT count(*) FROM lan391_representative_image_map) <> 328 THEN",
            "WHERE expression.id IS NULL",
            "expression.scenario_id IS DISTINCT FROM asset.scenario_id",
            "expression.display_order IS DISTINCT FROM asset.expression_display_order",
            "expression.representative_sentence_text IS DISTINCT FROM asset.sentence_en",
            "expression.representative_sentence_translation IS DISTINCT FROM asset.sentence_ko",
            "expression.representative_question_text IS DISTINCT FROM asset.question_en",
            "expression.representative_question_translation IS DISTINCT FROM asset.question_ko",
            "expression.representative_image_url IS NOT NULL",
            "IF affected_rows <> 328 THEN")
        .doesNotContain("ON CONFLICT", "COMMIT;");
    List<MatchResult> updates =
        Pattern.compile("(?s)UPDATE writing_expression expression\\s+SET (.*?)\\s+FROM")
            .matcher(sql)
            .results()
            .toList();
    assertThat(updates).hasSize(1);
    String assignment = updates.getFirst().group(1);
    assertThat(assignment).startsWith("representative_image_url =");
    assertThat(assignment)
        .contains(
            "https://d19azau1un4t7r.cloudfront.net/content/scenario-expression-representative/",
            "sha256-5af4794cc120dad8e3e897dc03117cfbcfd37125de23e8305a1e8b69fd4943f4/",
            "scenario-order-'",
            "asset.scenario_id",
            "'/sql-row-'",
            "lpad(asset.sql_values_row::text, 4, '0')",
            "asset.image_sha256",
            "'.webp'");
    assertThat(assignment.chars().filter(character -> character == '=').count()).isEqualTo(1);
  }

  private String representativeSql() throws Exception {
    String sql = readSql(UPDATE);
    int start = sql.indexOf(REPRESENTATIVE_MARKER);
    assertThat(start).as("representative migration block").isGreaterThanOrEqualTo(0);
    return sql.substring(start);
  }

  private String sqlText(String literal) {
    return literal.substring(1, literal.length() - 1).replace("''", "'");
  }

  private String readSql(String path) throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
  }
}
