// LAN-560 단어 음성 URL 공용 주소 전환 마이그레이션의 데이터 계약을 검증한다.

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

/** V126의 허용 목록, JSONB 배열 재작성 방식, 적재 전후 검증, Flyway 호환성을 확인한다. */
class Lan560SharedWordAudioUrlMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V126__share_expression_pronunciation_word_audio_urls.sql";
  private static final int EXPECTED_ALLOW_LIST_ROWS = 10494;
  private static final List<String> ACCENT_LOCALES = List.of("EN_US", "EN_GB", "EN_AU");
  // 검토한 허용 목록이 그대로인지 고정한다. 한 행이라도 바뀌면 이 값이 달라진다.
  private static final String EXPECTED_ALLOW_LIST_SHA256 =
      "9b3f654c1d8f9302076180decd5fa7b9a0db51f1096627b5079240b9951930fd";
  private static final Pattern ALLOW_LIST_ROW =
      Pattern.compile("(?m)^\\('(EN_US|EN_GB|EN_AU)','([0-9a-f]{64})'\\)[,;]$");

  @DisplayName("V126의 허용 목록은 공용 자리에 실물이 있는 (억양, 해시) 10,494개만 담는다.")
  @Test
  void carriesOneAllowListRowPerSharedWordObject() throws Exception {
    Matcher matcher = ALLOW_LIST_ROW.matcher(readMigrationSql());
    Set<String> pairs = new HashSet<>();
    List<String> rows = new ArrayList<>();

    while (matcher.find()) {
      String pair = matcher.group(1) + "/" + matcher.group(2);
      assertThat(pairs).as("allow list must not repeat %s", pair).doesNotContain(pair);
      pairs.add(pair);
      rows.add("('" + matcher.group(1) + "','" + matcher.group(2) + "')");
    }

    assertThat(rows).hasSize(EXPECTED_ALLOW_LIST_ROWS);
    assertThat(pairs.stream().map(pair -> pair.split("/")[0]).distinct().toList())
        .containsExactlyInAnyOrderElementsOf(ACCENT_LOCALES);
    assertThat(sha256(String.join(",\n", rows))).isEqualTo(EXPECTED_ALLOW_LIST_SHA256);
  }

  @DisplayName("V126은 단어 audioUrl만 배열 순서를 지켜 다시 쓰고 다른 컬럼은 건드리지 않는다.")
  @Test
  void rewritesOnlyTheWordAudioUrlKeepingArrayOrder() throws Exception {
    String migrationSql = readMigrationSql();

    assertThat(migrationSql)
        .contains("ARRAY['audioUrl']", "ORDER BY element.ordinality", "'\\1word/\\2/\\3'");
    assertThat(migrationSql)
        .containsPattern("(?s)jsonb_array_elements\\(original\\.words\\)\\s+WITH ORDINALITY");
    // create_missing=false — audioUrl이 없는 원소에 키를 새로 만들지 않는다.
    assertThat(migrationSql)
        .containsPattern("(?s)jsonb_set\\(.{0,200}?ARRAY\\['audioUrl'\\].{0,300}?false");
    assertThat(migrationSql)
        .as("words와 updated_at 외의 컬럼을 UPDATE하지 않는다")
        .containsOnlyOnce("SET words = patched.words,\n    updated_at = CURRENT_TIMESTAMP");
    assertThat(migrationSql).doesNotContain("SET expression_audio_url", "SET sentence_audio_url");
  }

  @DisplayName("V126은 적재 전에 형식·억양·공용 자리 존재를 대조하고, 하나라도 어긋나면 중단한다.")
  @Test
  void verifiesEveryWordUrlBeforeUpdating() throws Exception {
    String migrationSql = readMigrationSql();
    int updateAt = migrationSql.indexOf("UPDATE expression_pronunciation_asset asset");

    assertThat(migrationSql)
        .contains(
            "LAN-560 words precondition failed: % rows are not a non-empty array",
            "LAN-560 words precondition failed: % elements have no audioUrl",
            "LAN-560 word audio precondition failed: % URLs already use the shared path",
            "LAN-560 word audio precondition failed: % URLs are not in the expected old format",
            "LAN-560 word audio precondition failed: % URLs disagree with their row accent locale",
            "LAN-560 word audio precondition failed: shared object is missing for % %");
    assertThat(migrationSql.indexOf("shared object is missing for % %"))
        .as("공용 자리 대조는 UPDATE 앞에서 끝나야 한다")
        .isLessThan(updateAt);
    assertThat(migrationSql.indexOf("CREATE TEMP TABLE lan560_original_assets"))
        .as("스냅샷은 UPDATE 앞에서 떠야 한다")
        .isLessThan(updateAt);
  }

  @DisplayName("V126은 적재 후 옛 형식이 남지 않았고 audioUrl 외에는 변하지 않았음을 증명한다.")
  @Test
  void provesNothingButTheWordAudioUrlChanged() throws Exception {
    String migrationSql = readMigrationSql();
    int updateAt = migrationSql.indexOf("UPDATE expression_pronunciation_asset asset");

    assertThat(migrationSql)
        .contains(
            "LAN-560 word audio postcondition failed: % URLs still use the old path",
            "LAN-560 word audio postcondition failed: % URLs are not in the shared format",
            "LAN-560 word audio postcondition failed: % rows changed a field other than audioUrl",
            "LAN-560 word audio postcondition failed: % rows changed a word fingerprint",
            "LAN-560 word audio postcondition failed: "
                + "% rows changed sentence or expression audio URL");
    assertThat(migrationSql.indexOf("% rows changed a field other than audioUrl"))
        .as("사후 검증은 UPDATE 뒤에 와야 한다")
        .isGreaterThan(updateAt);
    // audioUrl만 떼고 비교해 배열 길이·순서와 나머지 필드가 그대로인지 본다.
    assertThat(migrationSql).contains("e.value - 'audioUrl' ORDER BY e.ordinality");
  }

  @DisplayName("V126은 동시 변경을 막고 Flyway 트랜잭션·플레이스홀더와 충돌하지 않는다.")
  @Test
  void locksTheTableAndStaysCompatibleWithFlyway() throws Exception {
    String migrationSql = readMigrationSql();

    assertThat(migrationSql)
        .contains(
            "pg_advisory_xact_lock(hashtext('lan560-shared-word-audio-urls'))",
            "LOCK TABLE expression_pronunciation_asset IN SHARE ROW EXCLUSIVE MODE",
            "ON COMMIT DROP");
    assertThat(migrationSql).doesNotContain("${");
    assertThat(migrationSql).doesNotContainPattern("(?im)^\\s*(BEGIN|COMMIT)\\s*;");
    assertThat(migrationSql).doesNotContain("DELETE FROM", "TRUNCATE", "DROP TABLE");
  }

  private String sha256(String value) throws Exception {
    byte[] digest =
        MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
    StringBuilder hex = new StringBuilder(digest.length * 2);
    for (byte each : digest) {
      hex.append(String.format("%02x", each));
    }
    return hex.toString();
  }

  private String readMigrationSql() throws Exception {
    ClassPathResource migration = new ClassPathResource(MIGRATION_PATH);
    assertThat(migration.exists()).as("V126 migration must exist").isTrue();
    return StreamUtils.copyToString(migration.getInputStream(), StandardCharsets.UTF_8);
  }
}
