// 콘텐츠 이미지 WebP URL 전환 마이그레이션의 매핑과 안전장치를 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** 콘텐츠 이미지 WebP URL 전환 마이그레이션의 고정 매핑과 트랜잭션 안전장치를 검증한다. */
class ContentImageWebpMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V48__remap_content_images_to_webp.sql";
  private static final Pattern URL_MAPPING_PATTERN =
      Pattern.compile("\\('([^']+\\.png)', '([^']+\\.webp)'\\)");

  @Test
  void migrationMapsEveryUsedPngToOneUniqueWebpUrl() throws Exception {
    List<UrlMapping> mappings = readMappings();
    Set<String> oldUrls = new HashSet<>();
    Set<String> newUrls = new HashSet<>();

    for (UrlMapping mapping : mappings) {
      oldUrls.add(mapping.oldUrl());
      newUrls.add(mapping.newUrl());
      assertThat(mapping.newUrl())
          .startsWith(mapping.oldUrl().substring(0, mapping.oldUrl().lastIndexOf('/') + 1));
    }

    assertThat(mappings).hasSize(692);
    assertThat(oldUrls).hasSize(692);
    assertThat(newUrls).hasSize(692);
    assertThat(oldUrls).allMatch(url -> url.contains("/content/") && url.endsWith(".png"));
    assertThat(newUrls).allMatch(url -> url.contains("/content/") && url.endsWith(".webp"));
  }

  @Test
  void migrationContainsOnlyCurrentReferenceCategories() throws Exception {
    List<UrlMapping> mappings = readMappings();

    assertThat(mappings.stream().filter(mapping -> mapping.oldUrl().contains("/thumbnail/")))
        .hasSize(40);
    assertThat(
            mappings.stream().filter(mapping -> mapping.oldUrl().contains("/practice-examples/")))
        .hasSize(652);
  }

  @Test
  void migrationGuardsReferenceSetAndPreservesJsonArrayOrder() throws Exception {
    String migrationSql = readMigrationSql();

    assertThat(migrationSql)
        .contains(
            "pg_advisory_xact_lock",
            "old_url TEXT PRIMARY KEY",
            "new_url TEXT NOT NULL UNIQUE",
            "Expected 692 old image URL references",
            "Expected no new image URL references before migration",
            "WITH ORDINALITY AS item(value, ordinal)",
            "ORDER BY item.ordinal",
            "Expected no old image URL references after migration",
            "Expected 692 new image URL references")
        .doesNotContain("DELETE FROM", "TRUNCATE");
  }

  private List<UrlMapping> readMappings() throws Exception {
    Matcher matcher = URL_MAPPING_PATTERN.matcher(readMigrationSql());
    List<UrlMapping> mappings = new ArrayList<>();
    while (matcher.find()) {
      mappings.add(new UrlMapping(matcher.group(1), matcher.group(2)));
    }
    return mappings;
  }

  private String readMigrationSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(MIGRATION_PATH).getInputStream(), StandardCharsets.UTF_8);
  }

  private record UrlMapping(String oldUrl, String newUrl) {}
}
