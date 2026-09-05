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
  private static final Pattern EXPRESSION_ROW_PATTERN =
      Pattern.compile(
          "^\\((\\d+),(\\d+),'[^']+','[^']+','EN','KR',(\\d+),.*,'SCENARIO','ACTIVE',"
              + "([123]),now\\(\\),now\\(\\)\\)[,;]$",
          Pattern.MULTILINE);
  private static final Pattern REPRESENTATIVE_IMAGE_PATTERN =
      Pattern.compile(
          "https://d19azau1un4t7r\\.cloudfront\\.net/content/writing-expressions/(\\d+)/representative/[0-9a-f-]{36}\\.webp");
  private static final Pattern PRACTICE_IMAGE_PATTERN =
      Pattern.compile(
          "https://d19azau1un4t7r\\.cloudfront\\.net/content/scenarios/(\\d+)/expressions/(\\d+)/practice-examples/[0-9a-f-]{36}\\.webp");
  private static final Pattern PRONUNCIATION_ROW_PATTERN =
      Pattern.compile(
          "^\\((\\d+),'(EN_US|EN_GB|EN_AU)',(NULL|'[^']+'),'"
              + "https://d19azau1un4t7r\\.cloudfront\\.net/content/"
              + "expression-pronunciation-audio/\\1/\\2/sentence/[^']+\\.mp3',",
          Pattern.MULTILINE);

  @Test
  void insertsThreeQuestionsForEachBeginnerLevelGroupAndScenario() throws Exception {
    Matcher matcher = QUESTION_ROW_PATTERN.matcher(readMigrationSql());
    Set<Integer> questionIds = new HashSet<>();
    Map<String, List<Integer>> ordersByScenarioAndGroup = new HashMap<>();
    Map<Integer, Set<String>> groupsByScenario = new HashMap<>();

    while (matcher.find()) {
      questionIds.add(Integer.parseInt(matcher.group(1)));
      int scenarioId = Integer.parseInt(matcher.group(2));
      String levelGroup = matcher.group(4);
      String key = scenarioId + ":" + levelGroup;
      ordersByScenarioAndGroup
          .computeIfAbsent(key, ignored -> new ArrayList<>())
          .add(Integer.parseInt(matcher.group(3)));
      groupsByScenario.computeIfAbsent(scenarioId, ignored -> new HashSet<>()).add(levelGroup);
    }

    assertThat(questionIds).containsExactlyInAnyOrderElementsOf(inclusiveRange(121, 360));
    assertThat(ordersByScenarioAndGroup).hasSize(80);
    assertThat(ordersByScenarioAndGroup.values())
        .allSatisfy(orders -> assertThat(orders).containsExactly(1, 2, 3));
    assertThat(groupsByScenario.keySet())
        .containsExactlyInAnyOrderElementsOf(inclusiveRange(1, 40));
    assertThat(groupsByScenario.values())
        .allSatisfy(
            groups -> assertThat(groups).containsExactlyInAnyOrder("LEVEL_1", "LEVEL_2_TO_3"));
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
        .doesNotContain("UPDATE scenario_question", "DELETE FROM scenario_question");
  }

  @Test
  void insertsFourExpressionsForEachBeginnerLevelGroupAndScenario() throws Exception {
    Matcher matcher = EXPRESSION_ROW_PATTERN.matcher(readMigrationSql());
    Set<Integer> expressionIds = new HashSet<>();
    Map<String, List<Integer>> ordersByScenarioAndGroup = new HashMap<>();

    while (matcher.find()) {
      expressionIds.add(Integer.parseInt(matcher.group(1)));
      int scenarioId = Integer.parseInt(matcher.group(2));
      int difficultyLevel = Integer.parseInt(matcher.group(4));
      String key = scenarioId + ":" + (difficultyLevel == 1 ? "LEVEL_1" : "LEVEL_2_TO_3");
      ordersByScenarioAndGroup
          .computeIfAbsent(key, ignored -> new ArrayList<>())
          .add(Integer.parseInt(matcher.group(3)));
    }

    assertThat(expressionIds).containsExactlyInAnyOrderElementsOf(inclusiveRange(1939, 2258));
    assertThat(ordersByScenarioAndGroup).hasSize(80);
    assertThat(ordersByScenarioAndGroup.values())
        .allSatisfy(orders -> assertThat(orders).containsExactlyInAnyOrder(1, 2, 3, 4));
    assertThat(readMigrationSql())
        .contains(
            "DROP CONSTRAINT uk_writing_expression_scenario_order",
            "CREATE UNIQUE INDEX uk_writing_expression_scenario_level_order",
            "WHEN difficulty_level BETWEEN 2 AND 3 THEN 'LEVEL_2_TO_3'");
  }

  @Test
  void insertsVerifiedRepresentativeAndPracticeImages() throws Exception {
    String migrationSql = readMigrationSql();
    Matcher representativeMatcher = REPRESENTATIVE_IMAGE_PATTERN.matcher(migrationSql);
    Matcher practiceMatcher = PRACTICE_IMAGE_PATTERN.matcher(migrationSql);
    Matcher expressionMatcher = EXPRESSION_ROW_PATTERN.matcher(migrationSql);
    Set<Integer> representativeExpressionIds = new HashSet<>();
    Set<String> practiceImageUrls = new HashSet<>();
    Map<Integer, Integer> practiceImageCountByExpression = new HashMap<>();
    Map<Integer, Integer> scenarioByExpression = new HashMap<>();

    while (expressionMatcher.find()) {
      scenarioByExpression.put(
          Integer.parseInt(expressionMatcher.group(1)),
          Integer.parseInt(expressionMatcher.group(2)));
    }
    while (representativeMatcher.find()) {
      representativeExpressionIds.add(Integer.parseInt(representativeMatcher.group(1)));
    }
    while (practiceMatcher.find()) {
      int scenarioId = Integer.parseInt(practiceMatcher.group(1));
      int expressionId = Integer.parseInt(practiceMatcher.group(2));
      assertThat(scenarioId).isEqualTo(scenarioByExpression.get(expressionId));
      practiceImageUrls.add(practiceMatcher.group());
      practiceImageCountByExpression.merge(expressionId, 1, Integer::sum);
    }

    assertThat(representativeExpressionIds)
        .containsExactlyInAnyOrderElementsOf(inclusiveRange(1939, 2258));
    assertThat(practiceImageUrls).hasSize(640);
    assertThat(practiceImageCountByExpression.keySet())
        .containsExactlyInAnyOrderElementsOf(inclusiveRange(1939, 2258));
    assertThat(practiceImageCountByExpression.values()).allMatch(count -> count == 2);
    assertThat(migrationSql.split("\\\"imageUrl\\\": null", -1)).hasSize(641);
  }

  @Test
  void insertsThreePronunciationAccentsForEveryExpression() throws Exception {
    String migrationSql = readMigrationSql();
    Matcher matcher = PRONUNCIATION_ROW_PATTERN.matcher(migrationSql);
    Map<Integer, Set<String>> accentsByExpression = new HashMap<>();
    int nullExpressionAudioCount = 0;

    while (matcher.find()) {
      accentsByExpression
          .computeIfAbsent(Integer.parseInt(matcher.group(1)), ignored -> new HashSet<>())
          .add(matcher.group(2));
      if ("NULL".equals(matcher.group(3))) {
        nullExpressionAudioCount++;
      }
    }

    assertThat(accentsByExpression.keySet())
        .containsExactlyInAnyOrderElementsOf(inclusiveRange(1939, 2258));
    assertThat(accentsByExpression.values())
        .allSatisfy(
            accents -> assertThat(accents).containsExactlyInAnyOrder("EN_US", "EN_GB", "EN_AU"));
    assertThat(nullExpressionAudioCount).isEqualTo(183);
    assertThat(migrationSql)
        .contains("setval(pg_get_serial_sequence('writing_expression', 'id')")
        .doesNotContain("__SCN_");
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
