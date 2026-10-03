// 격리 PostgreSQL에서 V141의 실제 제약·충돌 차단·실패 롤백을 검증한다.

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

/** 전용 로컬 DB 주소와 실제 pgvector 타입이 준비된 경우에만 실행한다. */
@EnabledIfEnvironmentVariable(named = "V141_TEST_POSTGRES_URL", matches = ".+")
class FreeTalkExpressionV141PostgresTests {

  private static final String URL = System.getenv("V141_TEST_POSTGRES_URL");
  private static final String MIGRATION =
      "V141__insert_reviewed_free_talk_expressions_4329_6000.sql";

  @TempDir Path migrations;

  private final String schema = "v141_" + UUID.randomUUID().toString().replace("-", "");
  private Connection connection;
  private Flyway flyway;

  @BeforeEach
  void createIsolatedSchema() throws Exception {
    assertThat(URL).isEqualTo("jdbc:postgresql://127.0.0.1:55441/v141_test?user=v141_test");
    connection = DriverManager.getConnection(URL);
    execute("CREATE SCHEMA " + schema);
    connection.setSchema(schema);
    execute(readResource("db/v141-writing-expression.sql"));
    Files.writeString(migrations.resolve(MIGRATION), readResource("db/postgresql/" + MIGRATION));
    flyway =
        Flyway.configure()
            .dataSource(URL, "v141_test", "")
            .schemas(schema)
            .defaultSchema(schema)
            .locations("filesystem:" + migrations)
            .baselineOnMigrate(true)
            .baselineVersion("140")
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

  @Test
  @DisplayName("전 난이도 1,672행을 적재하고 기존 행과 앞선 시퀀스 및 재실행 상태를 보존한다.")
  void migratesApprovedDataAndPreservesExistingRows() throws Exception {
    String before = query("SELECT to_jsonb(w)::text FROM writing_expression w WHERE id = 1");
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(query("SELECT count(*) FROM writing_expression")).isEqualTo("1673");
    assertThat(query("SELECT count(DISTINCT difficulty_level) FROM writing_expression"))
        .isEqualTo("5");
    assertThat(query("SELECT count(*) FROM writing_expression WHERE scenario_id IS NULL"))
        .isEqualTo("1672");
    assertThat(query("SELECT to_jsonb(w)::text FROM writing_expression w WHERE id = 1"))
        .isEqualTo(before);
    assertThat(query("SELECT last_value FROM writing_expression_id_seq")).isEqualTo("7000");
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(query("SELECT count(*) FROM writing_expression")).isEqualTo("1673");
  }

  @Test
  @DisplayName("기존 시퀀스가 낮으면 다음 신규 표현 ID를 6001로 전진시킨다.")
  void advancesSequenceBeyondInsertedIds() throws Exception {
    execute("SELECT setval(pg_get_serial_sequence('writing_expression', 'id'), 4328, true)");
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(query("SELECT nextval(pg_get_serial_sequence('writing_expression', 'id'))"))
        .isEqualTo("6001");
  }

  @ParameterizedTest
  @ValueSource(strings = {"id", "display_order"})
  @DisplayName("기존 ID 또는 프리톡 표시 순서가 겹치면 어떤 데이터도 추가하지 않는다.")
  void rejectsOccupiedRange(String field) throws Exception {
    execute(
        field.equals("id")
            ? "UPDATE writing_expression SET id = 4329"
            : "UPDATE writing_expression SET expression_source = 'FREE_TALK', "
                + "scenario_id = NULL, display_order = 3518");
    String before = query("SELECT to_jsonb(w)::text FROM writing_expression w");
    assertThatThrownBy(() -> flyway.migrate())
        .isInstanceOf(FlywayException.class)
        .hasStackTraceContaining("already occupied");
    assertThat(query("SELECT count(*) FROM writing_expression")).isEqualTo("1");
    assertThat(query("SELECT to_jsonb(w)::text FROM writing_expression w")).isEqualTo(before);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "NEW.embedding = NULL;",
        "NEW.representative_image_url = 'invalid';",
        "NEW.practice_examples_payload = jsonb_set(NEW.practice_examples_payload, "
            + "'{3,imageUrl}', NEW.practice_examples_payload->2->'imageUrl');"
      })
  @DisplayName("이미지·임베딩 사후 조건 위반 시 1,672개 삽입 전체를 롤백한다.")
  void rollsBackInvalidImageOrEmbedding(String mutation) throws Exception {
    corruptLastRow(mutation);
    assertThatThrownBy(() -> flyway.migrate())
        .isInstanceOf(FlywayException.class)
        .hasStackTraceContaining("content verification failed");
    assertThat(query("SELECT count(*) FROM writing_expression")).isEqualTo("1");
    assertThat(query("SELECT last_value FROM writing_expression_id_seq")).isEqualTo("7000");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 6})
  @DisplayName("현재 스키마의 난이도 1~5 제약을 벗어나면 전체 삽입을 롤백한다.")
  void enforcesActualDifficultyConstraint(int level) throws Exception {
    corruptLastRow("NEW.difficulty_level = " + level + ";");
    assertThatThrownBy(() -> flyway.migrate())
        .isInstanceOf(FlywayException.class)
        .hasStackTraceContaining("chk_writing_expression_difficulty_level");
    assertThat(query("SELECT count(*) FROM writing_expression")).isEqualTo("1");
  }

  private void corruptLastRow(String mutation) throws Exception {
    execute(
        "CREATE FUNCTION corrupt() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN "
            + "IF NEW.id = 6000 THEN "
            + mutation
            + " END IF; RETURN NEW; END $$");
    execute(
        "CREATE TRIGGER corrupt BEFORE INSERT ON writing_expression "
            + "FOR EACH ROW EXECUTE FUNCTION corrupt()");
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
