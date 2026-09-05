// 온보딩 질문의 평가 요소만 갱신하고 질문과 번역을 보존하는지 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class Lan438AssessmentMetadataMigrationTests {
  @Test
  void registersSemanticRequirementsWithoutChangingQuestionContent() throws Exception {
    try (var connection =
        DriverManager.getConnection("jdbc:h2:mem:lan438_metadata;MODE=PostgreSQL")) {
      var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
      jdbc.execute(
          "CREATE TABLE scenario_question (id BIGINT PRIMARY KEY, scenario_id BIGINT, "
              + "response_demand VARCHAR(10))");
      jdbc.execute(
          "CREATE TABLE scenario_question_language_variant (scenario_question_id BIGINT, "
              + "target_locale VARCHAR(5), base_locale VARCHAR(5), question_text TEXT, "
              + "question_translation TEXT, required_response_element TEXT)");
      for (int id : new int[] {1, 2, 3, 121, 122, 123, 124, 125, 126, 999}) {
        jdbc.update("INSERT INTO scenario_question VALUES (?, ?, 'HIGH')", id, id == 999 ? 2 : 1);
        jdbc.update(
            "INSERT INTO scenario_question_language_variant VALUES "
                + "(?, 'EN', 'KR', 'Original question', '기존 번역', 'Original question')",
            id);
      }
      ScriptUtils.executeSqlScript(
          connection,
          new ClassPathResource("db/migration/V82__refine_onboarding_assessment_metadata.sql"));
      assertThat(
              jdbc.queryForObject(
                      "SELECT required_response_element FROM scenario_question_language_variant "
                          + "WHERE scenario_question_id=2",
                      String.class)
                  .lines()
                  .toList())
          .containsExactly(
              "State a hobby or leisure activity.", "Explain how you became interested in it.");
      assertThat(
              jdbc.queryForObject(
                  "SELECT required_response_element FROM scenario_question_language_variant "
                      + "WHERE scenario_question_id=3",
                  String.class))
          .isEqualTo("Recommend a first place to visit in Korea.");
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM scenario_question_language_variant "
                      + "WHERE required_response_element <> question_text",
                  Integer.class))
          .isEqualTo(9);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM scenario_question_language_variant "
                      + "WHERE question_text='Original question' AND question_translation='기존 번역'",
                  Integer.class))
          .isEqualTo(10);
      assertThat(
              jdbc.queryForList(
                  "SELECT response_demand FROM scenario_question WHERE scenario_id=1 ORDER BY id",
                  String.class))
          .containsExactly("MEDIUM", "HIGH", "LOW", "LOW", "LOW", "LOW", "MEDIUM", "MEDIUM", "LOW");
      assertThat(
              jdbc.queryForObject(
                  "SELECT response_demand FROM scenario_question WHERE id=3", String.class))
          .isEqualTo("LOW");
      assertThat(
              jdbc.queryForObject(
                  "SELECT response_demand FROM scenario_question WHERE id=999", String.class))
          .isEqualTo("HIGH");
    }
  }
}
