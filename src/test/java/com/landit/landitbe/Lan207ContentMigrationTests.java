// LAN-207 시나리오와 Writing 표현 데이터 마이그레이션 결과를 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class Lan207ContentMigrationTests {

  @Test
  void migratesExistingAndNewScenarioContent() {
    JdbcTemplate jdbcTemplate = migrateLan207Content();

    assertThat(count(jdbcTemplate, "category")).isEqualTo(4);
    assertThat(count(jdbcTemplate, "category_language_variant")).isEqualTo(4);
    assertThat(count(jdbcTemplate, "scenario")).isEqualTo(40);
    assertThat(count(jdbcTemplate, "scenario_language_variant")).isEqualTo(40);
    assertThat(count(jdbcTemplate, "scenario_question")).isEqualTo(120);
    assertThat(count(jdbcTemplate, "scenario_question_language_variant")).isEqualTo(120);
    assertThat(count(jdbcTemplate, "writing_expression")).isEqualTo(163);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT name FROM category_language_variant WHERE category_id = 4", String.class))
        .isEqualTo("쇼핑");
    assertThat(
            jdbcTemplate.queryForObject("SELECT ai_role FROM scenario WHERE id = 1", String.class))
        .isEqualTo("미국인 대학생 룸메이트, 외향적이고 친화적인 성격");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT title FROM scenario_language_variant WHERE scenario_id = 1", String.class))
        .isEqualTo("입주 첫날, 룸메이트 Marco와 첫 만남");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT question_text FROM scenario_question_language_variant WHERE id = 1",
                String.class))
        .contains("I'm Marco");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM scenario WHERE id BETWEEN 21 AND 40 AND first_speaker = 'AI'",
                Integer.class))
        .isEqualTo(14);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM scenario
                WHERE id BETWEEN 21 AND 40
                  AND first_speaker = 'USER'
                """,
                Integer.class))
        .isEqualTo(6);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM writing_expression WHERE id BETWEEN 84 AND 163",
                Integer.class))
        .isEqualTo(80);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM writing_expression
                WHERE id BETWEEN 84 AND 163
                  AND expression_source = 'SCENARIO'
                  AND scenario_id BETWEEN 21 AND 40
                  AND owner_user_profile_id IS NULL
                """,
                Integer.class))
        .isEqualTo(80);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM writing_expression
                WHERE id BETWEEN 84 AND 163
                  AND representative_image_url IS NULL
                """,
                Integer.class))
        .isEqualTo(80);

    String expression128Payload =
        jdbcTemplate.queryForObject(
            """
            SELECT CAST(practice_examples_payload AS VARCHAR)
            FROM writing_expression
            WHERE id = 128
            """,
            String.class);
    String expression149Payload =
        jdbcTemplate.queryForObject(
            """
            SELECT CAST(practice_examples_payload AS VARCHAR)
            FROM writing_expression
            WHERE id = 149
            """,
            String.class);

    assertThat(expression128Payload)
        .contains("c16e5dae-c78e-4ee5-bf9b-3bdd57178d42")
        .doesNotContain("6473db73-17e8-4857-9b44-4f6a7924657c");
    assertThat(expression149Payload)
        .contains("f9ff0409-baf2-4d75-93ae-21dc40949ddd")
        .doesNotContain("8f63dd96-b7d0-4ec4-ab81-9ff0c802db49");
  }

  private JdbcTemplate migrateLan207Content() {
    String databaseUrl =
        "jdbc:h2:mem:lan207-"
            + UUID.randomUUID()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
            + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
    Flyway.configure()
        .dataSource(databaseUrl, "sa", "")
        .locations("classpath:db/migration")
        .load()
        .migrate();
    return new JdbcTemplate(new DriverManagerDataSource(databaseUrl, "sa", ""));
  }

  private int count(JdbcTemplate jdbcTemplate, String tableName) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
  }
}
