// LAN-358 추가 프리톡 표현과 대표 이미지 마이그레이션의 데이터 계약을 검증한다.

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

/** LAN-358 추가 표현식의 ID, 임베딩, 대표 이미지 URL 대응을 검증한다. */
class Lan358AdditionalContentMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V68__insert_additional_free_talk_expressions.sql";
  private static final Pattern EXPRESSION_VALUE =
      Pattern.compile("(?m)^\\((\\d+),NULL,'[^']+','[^']+','EN','KR',(\\d+),");
  private static final Pattern IMAGE_VALUE =
      Pattern.compile(
          "\\((\\d+), 'https://d19azau1un4t7r\\.cloudfront\\.net/content/"
              + "writing-expressions/(\\d+)/representative/([0-9a-f]{64})\\.webp'\\)");

  @DisplayName("V68은 display order 819부터 1775까지를 ID 982부터 1938로 적재한다.")
  @Test
  void insertsEveryAdditionalFreeTalkExpressionWithStableId() throws Exception {
    Matcher matcher = EXPRESSION_VALUE.matcher(readMigrationSql());
    List<Long> expressionIds = new ArrayList<>();
    List<Integer> displayOrders = new ArrayList<>();

    while (matcher.find()) {
      expressionIds.add(Long.parseLong(matcher.group(1)));
      displayOrders.add(Integer.parseInt(matcher.group(2)));
    }

    assertThat(expressionIds).containsExactlyElementsOf(expectedExpressionIds());
    assertThat(displayOrders).containsExactlyElementsOf(expectedDisplayOrders());
  }

  @DisplayName("V68은 모든 추가 표현식에 1536차원 임베딩을 포함한다.")
  @Test
  void includesEmbeddingForEveryAdditionalFreeTalkExpression() throws Exception {
    String migrationSql = readMigrationSql();

    assertThat(countOccurrences(migrationSql, "::extensions.vector)")).isEqualTo(957);
    assertThat(migrationSql)
        .contains(
            "LAN-358 additional expression embedding is missing", "vector_dims(embedding) <> 1536");
  }

  @DisplayName("V68은 현재 writing_expression 스키마에 없는 컬럼을 참조하지 않는다.")
  @Test
  void usesOnlyCurrentWritingExpressionColumns() throws Exception {
    assertThat(readMigrationSql()).doesNotContain("owner_user_profile_id");
  }

  @DisplayName("V68은 ID 982부터 1938까지 대표 이미지 URL을 정확히 한 개씩 매핑한다.")
  @Test
  void mapsEveryAdditionalWritingExpressionToOneRepresentativeImage() throws Exception {
    Matcher matcher = IMAGE_VALUE.matcher(readMigrationSql());
    List<Long> expressionIds = new ArrayList<>();
    Set<String> imageHashes = new HashSet<>();

    while (matcher.find()) {
      long expressionId = Long.parseLong(matcher.group(1));
      expressionIds.add(expressionId);
      imageHashes.add(matcher.group(3));
      assertThat(Long.parseLong(matcher.group(2))).isEqualTo(expressionId);
    }

    assertThat(expressionIds).containsExactlyElementsOf(expectedExpressionIds());
    assertThat(imageHashes).hasSize(957);
  }

  @DisplayName("V68은 표현식과 대표 이미지 적용 결과를 검증하고 시퀀스를 동기화한다.")
  @Test
  void verifiesInsertedContentAndSynchronizesSequence() throws Exception {
    assertThat(readMigrationSql())
        .contains(
            "pg_advisory_xact_lock",
            "LOCK TABLE writing_expression IN SHARE ROW EXCLUSIVE MODE",
            "LAN-358 additional expression row count verification failed",
            "LAN-358 additional representative image URL update verification failed",
            "pg_get_serial_sequence('writing_expression', 'id')");
  }

  private List<Long> expectedExpressionIds() {
    List<Long> ids = new ArrayList<>();
    for (long id = 982; id <= 1938; id++) {
      ids.add(id);
    }
    return ids;
  }

  private List<Integer> expectedDisplayOrders() {
    List<Integer> displayOrders = new ArrayList<>();
    for (int displayOrder = 819; displayOrder <= 1775; displayOrder++) {
      displayOrders.add(displayOrder);
    }
    return displayOrders;
  }

  private int countOccurrences(String text, String value) {
    return (text.length() - text.replace(value, "").length()) / value.length();
  }

  private String readMigrationSql() throws Exception {
    ClassPathResource migration = new ClassPathResource(MIGRATION_PATH);
    assertThat(migration.exists()).as("V68 migration must exist").isTrue();
    return StreamUtils.copyToString(migration.getInputStream(), StandardCharsets.UTF_8);
  }
}
