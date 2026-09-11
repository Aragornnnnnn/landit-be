// 기본 레벨 보정이 기존 선택 수준과 수준 변경 시각·승급 신호를 보존하는지 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class Lan483LearningLevelMigrationTests {
  @Test
  void backfillsOnlyUnsetLevelAndPreservesAssessmentState() throws Exception {
    try (var connection =
        DriverManager.getConnection("jdbc:h2:mem:lan483_default;MODE=PostgreSQL")) {
      var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
      jdbc.execute(
          "CREATE TABLE user_profile (id INT, learning_level INT, "
              + "learning_level_updated_at TIMESTAMP, promotion_streak INT DEFAULT 0)");
      jdbc.execute(
          "INSERT INTO user_profile (id, learning_level) VALUES (1, NULL), (2, 1), (3, 5)");
      jdbc.execute("UPDATE user_profile SET promotion_streak=1 WHERE id=3");
      ScriptUtils.executeSqlScript(
          connection,
          new ClassPathResource("db/migration/V98__default_user_learning_level_to_three.sql"));
      jdbc.execute("INSERT INTO user_profile (id) VALUES (4)");
      assertThat(
              jdbc.queryForList(
                  "SELECT learning_level FROM user_profile ORDER BY id", Integer.class))
          .containsExactly(3, 1, 5, 3);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM user_profile WHERE learning_level_updated_at IS NOT NULL",
                  Integer.class))
          .isZero();
      assertThat(
              jdbc.queryForObject(
                  "SELECT promotion_streak FROM user_profile WHERE id=3", Integer.class))
          .isEqualTo(1);
    }
  }
}
