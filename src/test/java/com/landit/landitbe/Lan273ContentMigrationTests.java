// LAN-273 신규 시나리오 inner_thought 마이그레이션의 고정 매핑을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** LAN-273 신규 시나리오 inner_thought 마이그레이션의 고정 매핑을 검증한다. */
class Lan273ContentMigrationTests {

  private static final String MIGRATION_PATH =
      "db/postgresql/V45__add_new_scenario_inner_thoughts.sql";

  /** 신규 시나리오(21~40) 중 AI가 먼저 말하는 14개의 1번 질문 ID. USER 먼저 6개는 제외한다. */
  private static final List<Integer> AI_FIRST_OPENING_QUESTION_IDS =
      List.of(64, 67, 73, 79, 82, 88, 91, 94, 97, 100, 103, 106, 109, 112);

  private static final Pattern UPDATE_PATTERN =
      Pattern.compile(
          "UPDATE scenario_question_language_variant SET\\s+"
              + "inner_thought = '(?:[^']|'')+',\\s+"
              + "inner_thought_type = '(GOOD|NORMAL)', updated_at = now\\(\\)\\s+"
              + "WHERE scenario_question_id = (\\d+)"
              + " AND target_locale = 'EN' AND base_locale = 'KR';");

  @Test
  void migrationUpdatesInnerThoughtsOfAllAiFirstOpeningQuestionsOnly() throws Exception {
    String migrationSql = readMigrationSql();
    Matcher matcher = UPDATE_PATTERN.matcher(migrationSql);
    List<Integer> questionIds = new ArrayList<>();
    int goodCount = 0;

    while (matcher.find()) {
      if ("GOOD".equals(matcher.group(1))) {
        goodCount++;
      }
      questionIds.add(Integer.parseInt(matcher.group(2)));
    }

    assertThat(questionIds).containsExactlyElementsOf(AI_FIRST_OPENING_QUESTION_IDS);
    assertThat(goodCount).isEqualTo(3);
    // 정규식에 걸리지 않는 형식의 UPDATE 문이 섞여 있지 않은지 전체 개수로 재확인한다.
    assertThat(migrationSql.split("UPDATE scenario_question_language_variant", -1))
        .hasSize(AI_FIRST_OPENING_QUESTION_IDS.size() + 1);
  }

  @Test
  void migrationDoesNotTouchQuestionTexts() throws Exception {
    String migrationSql = readMigrationSql();

    assertThat(migrationSql).doesNotContain("question_text", "question_translation");
  }

  private String readMigrationSql() throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(MIGRATION_PATH).getInputStream(), StandardCharsets.UTF_8);
  }
}
