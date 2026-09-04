// LAN-347 추가 예문 이미지 URL 마이그레이션의 데이터 계약을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** LAN-347 표현 982~1938의 추가 예문 3·4 이미지 URL 대응을 검증한다. */
class Lan347PracticeExampleImageMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V80__update_free_talk_practice_example_image_urls.sql";
  private static final Pattern UPDATE_VALUE =
      Pattern.compile(
          "\\((\\d+), ([34]), '(?:[^']|'')*', "
              + "'https://d19azau1un4t7r\\.cloudfront\\.net/content/expressions/"
              + "(\\d+)/practice-examples/([0-9a-f-]{36})\\.webp'\\)");
  private static final String EXPECTED_MAPPING_SHA256 =
      "496bd0d9d0fd32a3350beedb19ef48a44d885bf158c7f805ca4a93574eac704c";

  @DisplayName("V80은 표현 982부터 1938까지 예문 3과 4를 정확히 한 번씩 매핑한다.")
  @Test
  void mapsEveryTargetExpressionToExamplesThreeAndFour() throws Exception {
    Matcher matcher = UPDATE_VALUE.matcher(readMigrationSql());
    List<String> mappings = new ArrayList<>();
    StringBuilder digestSource = new StringBuilder();
    Set<String> assetIds = new HashSet<>();

    while (matcher.find()) {
      long expressionId = Long.parseLong(matcher.group(1));
      int exampleIndex = Integer.parseInt(matcher.group(2));
      long pathExpressionId = Long.parseLong(matcher.group(3));
      mappings.add(expressionId + ":" + exampleIndex);
      digestSource
          .append(expressionId)
          .append(':')
          .append(exampleIndex)
          .append(":https://d19azau1un4t7r.cloudfront.net/content/expressions/")
          .append(pathExpressionId)
          .append("/practice-examples/")
          .append(matcher.group(4))
          .append(".webp")
          .append('\n');
      assetIds.add(matcher.group(4));
      assertThat(pathExpressionId).isEqualTo(expressionId);
    }

    assertThat(mappings).containsExactlyElementsOf(expectedMappings());
    assertThat(assetIds).hasSize(1914);
    assertThat(sha256(digestSource.toString())).isEqualTo(EXPECTED_MAPPING_SHA256);
  }

  @DisplayName("V80은 URL 외 payload 필드를 보존하고 대상 누락과 적용 실패를 감지한다.")
  @Test
  void preservesPayloadAndGuardsMigration() throws Exception {
    assertThat(readMigrationSql())
        .contains(
            "pg_advisory_xact_lock",
            "LOCK TABLE writing_expression IN SHARE ROW EXCLUSIVE MODE",
            "LAN-347 requires 1914 image updates for 957 expressions",
            "LAN-347 DB mapping mismatch",
            "ARRAY['2', 'imageUrl']",
            "ARRAY['3', 'imageUrl']",
            "LAN-347 post-update verification failed",
            "LAN-347 changed practice payload fields outside example 3 and 4 imageUrl")
        .doesNotContain("representative_image_url");
  }

  private List<String> expectedMappings() {
    List<String> mappings = new ArrayList<>();
    for (long expressionId = 982; expressionId <= 1938; expressionId++) {
      mappings.add(expressionId + ":3");
      mappings.add(expressionId + ":4");
    }
    return mappings;
  }

  private String readMigrationSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(MIGRATION_PATH).getInputStream(), StandardCharsets.UTF_8);
  }

  private String sha256(String value) throws Exception {
    byte[] digest =
        MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    return java.util.HexFormat.of().formatHex(digest);
  }
}
