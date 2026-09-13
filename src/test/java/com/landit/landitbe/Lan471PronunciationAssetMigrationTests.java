// LAN-471 프리톡 표현 발음 평가 자산 마이그레이션의 데이터 계약을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** LAN-471 발음 자산의 표현·억양 대응, 음성 URL 형식, 적재 전후 검증을 확인한다. */
class Lan471PronunciationAssetMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V95__insert_free_talk_expression_pronunciation_assets.sql";
  private static final long FIRST_EXPRESSION_ID = 2259;
  private static final long LAST_EXPRESSION_ID = 3000;
  private static final List<String> ACCENT_LOCALES = List.of("EN_US", "EN_GB", "EN_AU");
  private static final Pattern ASSET_ROW =
      Pattern.compile("(?m)^\\((\\d+),'(EN_US|EN_GB|EN_AU)',(NULL|'[^']*'),'([^']*)',");
  private static final Pattern PRECHECK_ROW = Pattern.compile("(?m)^ {8}\\((\\d+), '");
  private static final String AUDIO_URL_PREFIX =
      "https://d19azau1un4t7r.cloudfront.net/content/expression-pronunciation-audio/";

  @DisplayName("V95는 표현 2259부터 3000까지 억양 세 가지마다 자산을 정확히 한 행씩 적재한다.")
  @Test
  void insertsOneAssetPerExpressionAndAccentLocale() throws Exception {
    Matcher matcher = ASSET_ROW.matcher(readMigrationSql());
    Map<Long, Set<String>> localesByExpressionId = new HashMap<>();
    int rowCount = 0;

    while (matcher.find()) {
      rowCount++;
      long expressionId = Long.parseLong(matcher.group(1));
      assertThat(localesByExpressionId.computeIfAbsent(expressionId, id -> new HashSet<>()))
          .as("expression %d must not repeat a locale", expressionId)
          .doesNotContain(matcher.group(2));
      localesByExpressionId.get(expressionId).add(matcher.group(2));
    }

    assertThat(rowCount).isEqualTo(2226);
    assertThat(localesByExpressionId).hasSize(742);
    assertThat(localesByExpressionId.keySet())
        .allMatch(id -> id >= FIRST_EXPRESSION_ID && id <= LAST_EXPRESSION_ID);
    assertThat(localesByExpressionId.values())
        .allSatisfy(
            locales -> assertThat(locales).containsExactlyInAnyOrderElementsOf(ACCENT_LOCALES));
  }

  @DisplayName("V95의 음성 URL은 자기 표현·억양 경로를 가리키고, 패턴형 표현만 표현 음성이 없다.")
  @Test
  void pointsEveryAudioUrlAtItsOwnExpressionAndLocale() throws Exception {
    Matcher matcher = ASSET_ROW.matcher(readMigrationSql());
    int missingExpressionAudio = 0;

    while (matcher.find()) {
      String ownPath = AUDIO_URL_PREFIX + matcher.group(1) + "/" + matcher.group(2) + "/";
      assertThat(matcher.group(4)).startsWith(ownPath + "sentence/").endsWith(".mp3");
      if ("NULL".equals(matcher.group(3))) {
        missingExpressionAudio++;
      } else {
        assertThat(matcher.group(3)).startsWith("'" + ownPath + "expression/").endsWith(".mp3'");
      }
    }

    assertThat(missingExpressionAudio).isEqualTo(231);
  }

  @DisplayName("V95는 적재 전에 742개 표현의 표현·예문이 음성을 만든 문장과 같은지 대조한다.")
  @Test
  void verifiesExpressionTextsBeforeInsertingAssets() throws Exception {
    String migrationSql = readMigrationSql();
    Matcher matcher = PRECHECK_ROW.matcher(migrationSql);
    Set<Long> checkedIds = new HashSet<>();

    while (matcher.find()) {
      checkedIds.add(Long.parseLong(matcher.group(1)));
    }

    assertThat(checkedIds).hasSize(742);
    assertThat(checkedIds).allMatch(id -> id >= FIRST_EXPRESSION_ID && id <= LAST_EXPRESSION_ID);
    assertThat(migrationSql.indexOf("differ from the texts the audio was generated from"))
        .as("text precheck must run before the insert")
        .isLessThan(migrationSql.indexOf("INSERT INTO expression_pronunciation_asset"));
    assertThat(migrationSql)
        .contains(
            "we.target_expression_text IS DISTINCT FROM expected.expression_text",
            "we.representative_sentence_text IS DISTINCT FROM expected.sentence_text");
  }

  @DisplayName("V95는 적재 결과를 검증하고 Flyway 트랜잭션·플레이스홀더와 충돌하지 않는다.")
  @Test
  void verifiesResultAndStaysCompatibleWithFlyway() throws Exception {
    String migrationSql = readMigrationSql();

    assertThat(migrationSql)
        .contains(
            "expected 2226 rows for 2259~3000",
            "every accent locale must have 742 rows",
            "sentence audio URL is missing",
            "expected 231 templated expressions without expression audio");
    assertThat(migrationSql).doesNotContain("${");
    assertThat(migrationSql).doesNotContainPattern("(?im)^\\s*(BEGIN|COMMIT)\\s*;");
  }

  private String readMigrationSql() throws Exception {
    ClassPathResource migration = new ClassPathResource(MIGRATION_PATH);
    assertThat(migration.exists()).as("V95 migration must exist").isTrue();
    return StreamUtils.copyToString(migration.getInputStream(), StandardCharsets.UTF_8);
  }
}
