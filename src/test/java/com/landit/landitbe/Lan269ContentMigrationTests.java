// LAN-269 시나리오 썸네일과 표현학습 데이터 마이그레이션의 고정 매핑을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

class Lan269ContentMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V44__add_scenario_thumbnails_and_writing_expressions.sql";
  private static final Pattern THUMBNAIL_URL_PATTERN =
      Pattern.compile(
          "WHEN (\\d+) THEN '(https://d19azau1un4t7r\\.cloudfront\\.net/content/scenarios/(\\d+)/thumbnail/[^']+\\.png)'");
  private static final Pattern PRACTICE_EXAMPLE_URL_PATTERN =
      Pattern.compile(
          "https://d19azau1un4t7r\\.cloudfront\\.net/content/scenarios/(\\d+)/expressions/(\\d+)/practice-examples/[^\\\"]+\\.png");

  @Test
  void migrationContainsAllScenarioThumbnailUrls() throws Exception {
    String migrationSql = readMigrationSql();
    Matcher matcher = THUMBNAIL_URL_PATTERN.matcher(migrationSql);
    Set<Integer> scenarioIds = new HashSet<>();

    while (matcher.find()) {
      assertThat(matcher.group(1)).isEqualTo(matcher.group(3));
      scenarioIds.add(Integer.parseInt(matcher.group(1)));
    }

    assertThat(scenarioIds).containsExactlyInAnyOrderElementsOf(allScenarioIds());
    assertThat(countMatches(THUMBNAIL_URL_PATTERN, migrationSql)).isEqualTo(40);
    assertThat(thumbnailMappingDigest(migrationSql))
        .isEqualTo("c606f53ab414db1d513c379508c311de51d535531ee1f795af8b0f51a48e1ecd");
  }

  @Test
  void migrationContainsAllWritingExpressionsAndPracticeExampleUrls() throws Exception {
    String migrationSql = readMigrationSql();
    Matcher matcher = PRACTICE_EXAMPLE_URL_PATTERN.matcher(migrationSql);
    Set<String> urls = new HashSet<>();
    int exampleIndex = 0;

    while (matcher.find()) {
      int scenarioId = Integer.parseInt(matcher.group(1));
      int expressionId = Integer.parseInt(matcher.group(2));
      int expectedExpressionId = 84 + (scenarioId - 21) * 4 + (exampleIndex / 4) % 4;

      assertThat(scenarioId).isBetween(21, 40);
      assertThat(expressionId).isEqualTo(expectedExpressionId);
      urls.add(matcher.group());
      exampleIndex++;
    }

    assertThat(exampleIndex).isEqualTo(320);
    assertThat(urls).hasSize(320);
    assertThat(countOccurrences(migrationSql, "INSERT INTO writing_expression")).isEqualTo(80);
    assertThat(practiceExampleMappingDigest(migrationSql))
        .isEqualTo("12c6aa477acf368e381abefef3093172bb82c5b22734c15ecf1f06b7453dc093");
    assertThat(migrationSql)
        .contains(
            "LOCK TABLE writing_expression IN SHARE ROW EXCLUSIVE MODE;",
            "SELECT setval(pg_get_serial_sequence('writing_expression', 'id'),"
                + " (SELECT MAX(id) FROM writing_expression));");
  }

  private Set<Integer> allScenarioIds() {
    Set<Integer> scenarioIds = new HashSet<>();
    for (int scenarioId = 1; scenarioId <= 40; scenarioId++) {
      scenarioIds.add(scenarioId);
    }
    return scenarioIds;
  }

  private int countMatches(Pattern pattern, String value) {
    int count = 0;
    Matcher matcher = pattern.matcher(value);
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  private int countOccurrences(String value, String target) {
    return (value.length() - value.replace(target, "").length()) / target.length();
  }

  private String thumbnailMappingDigest(String migrationSql) throws Exception {
    Matcher matcher = THUMBNAIL_URL_PATTERN.matcher(migrationSql);
    StringBuilder mapping = new StringBuilder();
    while (matcher.find()) {
      mapping.append(matcher.group(1)).append('|').append(matcher.group(2)).append('\n');
    }
    return sha256(mapping.toString());
  }

  private String practiceExampleMappingDigest(String migrationSql) throws Exception {
    Matcher matcher = PRACTICE_EXAMPLE_URL_PATTERN.matcher(migrationSql);
    Map<Integer, Integer> exampleNumbersByExpressionId = new HashMap<>();
    StringBuilder mapping = new StringBuilder();
    while (matcher.find()) {
      int expressionId = Integer.parseInt(matcher.group(2));
      int displayOrder = (expressionId - 84) % 4 + 1;
      int exampleNumber = exampleNumbersByExpressionId.merge(expressionId, 1, Integer::sum);
      mapping
          .append(matcher.group(1))
          .append('|')
          .append(expressionId)
          .append('|')
          .append(displayOrder)
          .append('|')
          .append(exampleNumber)
          .append('|')
          .append(matcher.group())
          .append('\n');
    }
    return sha256(mapping.toString());
  }

  private String sha256(String value) throws Exception {
    byte[] digest =
        MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest);
  }

  private String readMigrationSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(MIGRATION_PATH).getInputStream(), StandardCharsets.UTF_8);
  }
}
