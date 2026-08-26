// LAN-358 대표 예문 이미지 URL 마이그레이션의 데이터 계약을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
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

/** LAN-358 대표 예문 이미지 URL 마이그레이션의 행 수와 ID-URL 대응을 검증한다. */
class Lan358ContentMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V59__update_writing_expression_representative_image_urls.sql";
  private static final Pattern UPDATE_VALUE =
      Pattern.compile(
          "\\((\\d+), 'https://d19azau1un4t7r\\.cloudfront\\.net/content/"
              + "writing-expressions/(\\d+)/representative/([0-9a-f]{64})\\.webp'\\)");

  @DisplayName("V59는 ID 1부터 981까지 대표 이미지 URL을 정확히 한 개씩 매핑한다.")
  @Test
  void mapsEveryWritingExpressionToOneRepresentativeImage() throws Exception {
    Matcher matcher = UPDATE_VALUE.matcher(readMigrationSql());
    List<Long> expressionIds = new ArrayList<>();
    Set<String> imageHashes = new HashSet<>();

    while (matcher.find()) {
      long expressionId = Long.parseLong(matcher.group(1));
      long pathExpressionId = Long.parseLong(matcher.group(2));
      expressionIds.add(expressionId);
      imageHashes.add(matcher.group(3));
      assertThat(pathExpressionId).isEqualTo(expressionId);
    }

    assertThat(expressionIds).containsExactlyElementsOf(expectedExpressionIds());
    assertThat(imageHashes).hasSize(981);
  }

  @DisplayName("V59는 대상 누락과 적용 실패를 감지하고 기존 URL과 다른 행만 갱신한다.")
  @Test
  void guardsMigrationTargetsAndVerification() throws Exception {
    assertThat(readMigrationSql())
        .contains(
            "pg_advisory_xact_lock",
            "LOCK TABLE writing_expression IN SHARE ROW EXCLUSIVE MODE",
            "LAN-358 representative image target expression is missing",
            "target.representative_image_url IS DISTINCT FROM source.image_url",
            "LAN-358 representative image URL update verification failed");
  }

  private List<Long> expectedExpressionIds() {
    List<Long> ids = new ArrayList<>();
    for (long id = 1; id <= 981; id++) {
      ids.add(id);
    }
    return ids;
  }

  private String readMigrationSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(MIGRATION_PATH).getInputStream(), StandardCharsets.UTF_8);
  }
}
