// LAN-405 보정 자산 URL 전환 마이그레이션의 고정 매핑과 안전장치를 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** LAN-405 보정 이미지와 질문 음성의 새 immutable URL 전환 계약을 검증한다. */
class Lan405AssetUrlRefreshMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V81__refresh_corrected_content_asset_urls.sql";
  private static final String EXPECTED_MAPPING_SHA256 =
      "d6f9f35cfbef6a1446ca6564aefa70a1e5956d96785e1e1722c2bb5b2e7e170f";
  private static final Pattern IMAGE_MAPPING_PATTERN =
      Pattern.compile(
          "^\\s*\\((\\d+), ([1-4]), '(https://[^']+/(?:practice-examples)/[^']+\\.webp)', "
              + "'(https://[^']+/(?:practice-examples)/[0-9a-f-]{36}\\.webp)'\\)[,;]$",
          Pattern.MULTILINE);
  private static final Pattern AUDIO_MAPPING_PATTERN =
      Pattern.compile(
          "^\\s*\\((\\d+), '(https://[^']+/scenario-question-audio/\\1/[^']+\\.mp3)', "
              + "'(https://[^']+/scenario-question-audio/\\1/revisions/[0-9a-f]{64}\\.mp3)'\\)[,;]$",
          Pattern.MULTILINE);

  @Test
  void mapsExactlyThirtyFivePracticeImagesToFreshUuidUrls() throws Exception {
    Matcher matcher = IMAGE_MAPPING_PATTERN.matcher(readMigrationSql());
    List<ImageMapping> mappings = new ArrayList<>();

    while (matcher.find()) {
      mappings.add(
          new ImageMapping(
              Long.parseLong(matcher.group(1)),
              Integer.parseInt(matcher.group(2)),
              matcher.group(3),
              matcher.group(4)));
    }

    assertThat(mappings).hasSize(35);
    assertThat(mappings.stream().map(ImageMapping::key).collect(Collectors.toSet())).hasSize(35);
    assertThat(mappings.stream().map(ImageMapping::oldUrl).collect(Collectors.toSet())).hasSize(35);
    assertThat(mappings.stream().map(ImageMapping::newUrl).collect(Collectors.toSet())).hasSize(35);
    assertThat(mappings)
        .allSatisfy(
            mapping -> {
              assertThat(mapping.newUrl()).isNotEqualTo(mapping.oldUrl());
              assertThat(parentPath(mapping.newUrl())).isEqualTo(parentPath(mapping.oldUrl()));
            });
  }

  @Test
  void preservesTheReviewedAssetMappingExactly() throws Exception {
    String migrationSql = readMigrationSql();
    List<String> canonicalMappings = new ArrayList<>();

    Matcher imageMatcher = IMAGE_MAPPING_PATTERN.matcher(migrationSql);
    while (imageMatcher.find()) {
      canonicalMappings.add(
          "IMAGE|"
              + imageMatcher.group(1)
              + "|"
              + imageMatcher.group(2)
              + "|"
              + imageMatcher.group(3)
              + "|"
              + imageMatcher.group(4));
    }

    Matcher audioMatcher = AUDIO_MAPPING_PATTERN.matcher(migrationSql);
    while (audioMatcher.find()) {
      canonicalMappings.add(
          "AUDIO|"
              + audioMatcher.group(1)
              + "|"
              + audioMatcher.group(2)
              + "|"
              + audioMatcher.group(3));
    }

    String canonicalMappingText =
        canonicalMappings.stream().sorted().collect(Collectors.joining("\n"));
    String actualSha256 =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(canonicalMappingText.getBytes(StandardCharsets.UTF_8)));

    assertThat(canonicalMappings).hasSize(49);
    assertThat(actualSha256).isEqualTo(EXPECTED_MAPPING_SHA256);
  }

  @Test
  void mapsExactlyFourteenQuestionsToContentAddressedRevisionUrls() throws Exception {
    Matcher matcher = AUDIO_MAPPING_PATTERN.matcher(readMigrationSql());
    Set<Long> questionIds = new HashSet<>();
    Set<String> oldUrls = new HashSet<>();
    Set<String> newUrls = new HashSet<>();

    while (matcher.find()) {
      questionIds.add(Long.parseLong(matcher.group(1)));
      oldUrls.add(matcher.group(2));
      newUrls.add(matcher.group(3));
    }

    assertThat(questionIds)
        .containsExactlyInAnyOrder(
            13L, 14L, 21L, 56L, 96L, 111L, 124L, 128L, 142L, 158L, 245L, 252L, 298L, 299L);
    assertThat(oldUrls).hasSize(14);
    assertThat(newUrls).hasSize(14);
  }

  @Test
  void guardsCurrentUrlsAndVerifiesEveryReplacement() throws Exception {
    assertThat(readMigrationSql())
        .contains(
            "pg_advisory_xact_lock",
            "LOCK TABLE writing_expression, scenario_question_language_variant",
            "image URL precondition failed",
            "audio URL precondition failed",
            "WITH ORDINALITY AS example(value, ordinality)",
            "ORDER BY example.ordinality",
            "image URL postcondition failed",
            "audio URL postcondition failed",
            "old image URLs remain",
            "old audio URLs remain")
        .doesNotContain("DELETE FROM", "TRUNCATE");
  }

  private String readMigrationSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(MIGRATION_PATH).getInputStream(), StandardCharsets.UTF_8);
  }

  private String parentPath(String url) {
    return url.substring(0, url.lastIndexOf('/'));
  }

  private record ImageMapping(long expressionId, int exampleIndex, String oldUrl, String newUrl) {
    private String key() {
      return expressionId + ":" + exampleIndex;
    }
  }
}
