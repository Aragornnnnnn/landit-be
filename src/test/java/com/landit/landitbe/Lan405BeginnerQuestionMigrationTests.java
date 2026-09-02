// LAN-405 초보용 시나리오 질문 마이그레이션의 고정 데이터 계약을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** LAN-405 초보용 시나리오 질문 마이그레이션의 고정 데이터 계약을 검증한다. */
class Lan405BeginnerQuestionMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V78__insert_beginner_scenario_questions.sql";
  private static final String AUDIO_URL_PREFIX =
      "https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/";
  private static final Pattern QUESTION_ROW_PATTERN =
      Pattern.compile(
          "^\\((\\d+), (\\d+), ([123]), '(LEVEL_1|LEVEL_2_TO_3)', "
              + "'ACTIVE', now\\(\\), now\\(\\)\\)[,;]$",
          Pattern.MULTILINE);
  private static final Pattern VARIANT_ROW_PATTERN =
      Pattern.compile(
          "^\\((\\d+), \\1, 'EN', 'KR', '((?:[^']|'')*)', '((?:[^']|'')*)', '"
              + Pattern.quote(AUDIO_URL_PREFIX)
              + "([^']+\\.mp3)', 'ACTIVE', now\\(\\), now\\(\\), "
              + "(NULL|'(?:[^']|'')*'), (NULL|'GOOD')\\)[,;]$",
          Pattern.MULTILINE);

  @Test
  void insertsThreeQuestionsForEachBeginnerLevelGroupAndScenario() throws Exception {
    Matcher matcher = QUESTION_ROW_PATTERN.matcher(readMigrationSql());
    Set<Integer> questionIds = new HashSet<>();
    Map<String, List<Integer>> ordersByScenarioAndGroup = new HashMap<>();

    while (matcher.find()) {
      questionIds.add(Integer.parseInt(matcher.group(1)));
      String key = matcher.group(2) + ":" + matcher.group(4);
      ordersByScenarioAndGroup
          .computeIfAbsent(key, ignored -> new ArrayList<>())
          .add(Integer.parseInt(matcher.group(3)));
    }

    assertThat(questionIds).containsExactlyInAnyOrderElementsOf(inclusiveRange(121, 360));
    assertThat(ordersByScenarioAndGroup).hasSize(80);
    assertThat(ordersByScenarioAndGroup.values())
        .allSatisfy(orders -> assertThat(orders).containsExactly(1, 2, 3));
  }

  @Test
  void insertsCompleteLanguageVariantsWithImmutableAudioUrls() throws Exception {
    String migrationSql = readMigrationSql();
    Matcher matcher = VARIANT_ROW_PATTERN.matcher(migrationSql);
    Set<Integer> variantIds = new HashSet<>();
    Set<String> audioUrls = new HashSet<>();
    int goodInnerThoughtCount = 0;
    int emptyInnerThoughtCount = 0;

    while (matcher.find()) {
      int variantId = Integer.parseInt(matcher.group(1));
      variantIds.add(variantId);
      audioUrls.add(matcher.group(4));
      assertThat(unescapeSql(matcher.group(2)).split("\\s+")).hasSizeLessThanOrEqualTo(25);
      assertThat(unescapeSql(matcher.group(3))).isNotBlank();
      assertThat(matcher.group(4)).startsWith(variantId + "/");
      if ("'GOOD'".equals(matcher.group(6))) {
        goodInnerThoughtCount++;
        assertThat((variantId - 121) % 3).isZero();
        assertThat(matcher.group(5)).isNotEqualTo("NULL");
      } else {
        emptyInnerThoughtCount++;
        assertThat(matcher.group(5)).isEqualTo("NULL");
      }
    }

    assertThat(variantIds).containsExactlyInAnyOrderElementsOf(inclusiveRange(121, 360));
    assertThat(audioUrls).hasSize(240);
    assertThat(goodInnerThoughtCount).isEqualTo(60);
    assertThat(emptyInnerThoughtCount).isEqualTo(180);
    assertThat(migrationSql)
        .contains(
            "setval(pg_get_serial_sequence('scenario_question', 'id')",
            "setval(pg_get_serial_sequence('scenario_question_language_variant', 'id')")
        .doesNotContain(
            "LEVEL_4_TO_5", "UPDATE scenario_question", "DELETE FROM scenario_question");
  }

  private List<Integer> inclusiveRange(int start, int end) {
    List<Integer> values = new ArrayList<>();
    for (int value = start; value <= end; value++) {
      values.add(value);
    }
    return values;
  }

  private String unescapeSql(String value) {
    return value.replace("''", "'");
  }

  private String readMigrationSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(MIGRATION_PATH).getInputStream(), StandardCharsets.UTF_8);
  }
}
