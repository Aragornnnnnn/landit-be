// 격리 PostgreSQL에서 V127의 5단계 난이도 적재와 실패 시 전체 롤백을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** 전용 로컬 PostgreSQL과 extensions 스키마의 pgvector가 설정된 경우에만 실행한다. */
@EnabledIfEnvironmentVariable(named = "V127_TEST_POSTGRES_URL", matches = ".+")
class FreeTalkExpressionV127PostgresTests {

  private static final String URL = System.getenv("V127_TEST_POSTGRES_URL");
  private static final String MIGRATION = "V127__insert_1000_free_talk_expressions.sql";

  @TempDir Path migrations;

  private final String schema = "v127_" + UUID.randomUUID().toString().replace("-", "");
  private Connection connection;
  private Flyway flyway;

  @BeforeEach
  void createIsolatedSchema() throws Exception {
    assertThat(URL).isEqualTo("jdbc:postgresql://127.0.0.1:55427/v127_test?user=v127_test");
    connection = DriverManager.getConnection(URL);
    execute("CREATE SCHEMA " + schema);
    connection.setSchema(schema);
    execute(readResource("db/v127-writing-expression.sql"));
    Files.writeString(migrations.resolve(MIGRATION), readResource("db/postgresql/" + MIGRATION));
    flyway =
        Flyway.configure()
            .dataSource(URL, "v127_test", "")
            .schemas(schema)
            .defaultSchema(schema)
            .locations("filesystem:" + migrations)
            .baselineOnMigrate(true)
            .baselineVersion("126")
            .load();
  }

  @AfterEach
  void dropIsolatedSchema() throws Exception {
    if (connection != null) {
      try {
        execute("DROP SCHEMA " + schema + " CASCADE");
      } finally {
        connection.close();
      }
    }
  }

  @DisplayName("5단계 표현 1,000개를 적재하고 기존 행·앞선 시퀀스와 재실행 결과를 보존한다.")
  @Test
  void migratesAllFiveLevelsAndSkipsAppliedVersion() throws Exception {
    String existing = query("SELECT to_jsonb(w)::text FROM writing_expression w WHERE id = 1");
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(query("SELECT count(*) FROM writing_expression")).isEqualTo("1001");
    assertThat(query("SELECT count(DISTINCT difficulty_level) FROM writing_expression"))
        .isEqualTo("5");
    assertThat(query("SELECT count(*) FROM writing_expression WHERE difficulty_level IN (4, 5)"))
        .isEqualTo("252");
    assertThat(query("SELECT to_jsonb(w)::text FROM writing_expression w WHERE id = 1"))
        .isEqualTo(existing);
    assertThat(query("SELECT last_value FROM writing_expression_id_seq")).isEqualTo("7000");
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(query("SELECT count(*) FROM writing_expression")).isEqualTo("1001");
  }

  @DisplayName("범위 밖 난이도·이미지·임베딩 변조는 1,000개 적재 전체를 롤백한다.")
  @ParameterizedTest
  @ValueSource(
      strings = {
        "NEW.difficulty_level = 0;",
        "NEW.difficulty_level = 6;",
        "NEW.representative_image_url = 'invalid';",
        "NEW.embedding = NULL;"
      })
  void rejectsInvalidDataAndRollsBack(String mutation) throws Exception {
    execute(
        "CREATE FUNCTION corrupt() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN "
            + "IF NEW.id = 4000 THEN "
            + mutation
            + " END IF; RETURN NEW; END $$");
    execute(
        "CREATE TRIGGER corrupt BEFORE INSERT ON writing_expression "
            + "FOR EACH ROW EXECUTE FUNCTION corrupt()");
    String before = query("SELECT to_jsonb(w)::text FROM writing_expression w");
    assertThatThrownBy(() -> flyway.migrate())
        .isInstanceOf(FlywayException.class)
        .hasStackTraceContaining("V127 expression, embedding or image mapping verification failed");
    assertThat(query("SELECT count(*) FROM writing_expression")).isEqualTo("1");
    assertThat(query("SELECT to_jsonb(w)::text FROM writing_expression w")).isEqualTo(before);
    assertThat(query("SELECT last_value FROM writing_expression_id_seq")).isEqualTo("7000");
    execute("DROP TRIGGER corrupt ON writing_expression");
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
  }

  @DisplayName("기존 ID 또는 표시 순서가 겹치면 적재를 거부한다.")
  @ParameterizedTest
  @ValueSource(strings = {"id", "display_order"})
  void rejectsOccupiedRange(String field) throws Exception {
    execute(
        field.equals("id")
            ? "UPDATE writing_expression SET id = 3001"
            : "UPDATE writing_expression SET expression_source = 'FREE_TALK', "
                + "target_locale = 'EN', base_locale = 'KR', display_order = 2518");
    String before = query("SELECT to_jsonb(w)::text FROM writing_expression w");
    assertThatThrownBy(() -> flyway.migrate())
        .isInstanceOf(FlywayException.class)
        .hasStackTraceContaining("range is already occupied");
    assertThat(query("SELECT count(*) FROM writing_expression")).isEqualTo("1");
    assertThat(query("SELECT to_jsonb(w)::text FROM writing_expression w")).isEqualTo(before);
  }

  private void execute(String sql) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private String query(String sql) throws Exception {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      return result.getString(1);
    }
  }

  private String readResource(String path) throws Exception {
    return StreamUtils.copyToString(
        new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
  }
}
