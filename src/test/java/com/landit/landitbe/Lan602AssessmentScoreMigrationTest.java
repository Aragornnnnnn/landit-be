// 100점 평가 저장 정밀도를 확장하고 과거 평가 원본을 보존하는지 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class Lan602AssessmentScoreMigrationTest {
  @Test
  void expandsAllScoresWithoutReinterpretingHistoricalResults() throws Exception {
    var columns =
        List.of(
            "situation_performance_score",
            "grammar_score",
            "vocabulary_score",
            "discourse_score",
            "interaction_pragmatics_score",
            "assessed_score");
    try (var connection = DriverManager.getConnection("jdbc:h2:mem:lan602;MODE=PostgreSQL")) {
      var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
      jdbc.execute(
          "CREATE TABLE user_level_assessment (id INT, assessment_version VARCHAR(50), "
              + "assessed_level INT, core_payload VARCHAR(200), "
              + String.join(", ", columns.stream().map(c -> c + " NUMERIC(3,2)").toList())
              + ")");
      jdbc.update(
          "INSERT INTO user_level_assessment VALUES "
              + "(1, 'text-level-v1.3', 3, '{\"level\":3}', 3.25, 3.25, 3.25, 3.25, NULL, NULL)");
      var original = jdbc.queryForMap("SELECT * FROM user_level_assessment WHERE id=1");
      ScriptUtils.executeSqlScript(
          connection,
          new ClassPathResource("db/migration/V134__expand_level_assessment_scores.sql"));
      assertThat(jdbc.queryForMap("SELECT * FROM user_level_assessment WHERE id=1"))
          .isEqualTo(original);
      jdbc.update(
          "INSERT INTO user_level_assessment VALUES "
              + "(2, 'text-score-v2.0', 5, '{\"score\":100}', 100, 100, 100, 100, 100, 100)");
      for (String column : columns) {
        assertThat(
                jdbc.queryForObject(
                    "SELECT " + column + " FROM user_level_assessment WHERE id=2",
                    BigDecimal.class))
            .isEqualByComparingTo("100.00");
      }
    }
  }
}
