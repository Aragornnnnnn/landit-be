// 격리한 PostgreSQL에서 영어 칩 교정과 원본 변경 감지 및 트랜잭션 롤백을 검증한다.

package com.landit.landitbe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;
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

@EnabledIfEnvironmentVariable(named = "LAN584_TEST_POSTGRES_URL", matches = ".+")
class Lan584EnglishQuizPostgresTests {

  private static final String URL = System.getenv("LAN584_TEST_POSTGRES_URL");
  private static final String LOCAL_URL =
      "jdbc:postgresql://127.0.0.1:55484/lan584_test?user=lan584_test";

  @TempDir Path migrations;

  private final String schema = "lan584_" + UUID.randomUUID().toString().replace("-", "");
  private Connection connection;
  private Flyway flyway;
  private JsonNode manifest;

  @BeforeEach
  void createIsolatedSchema() throws Exception {
    assertThat(URL).isEqualTo(LOCAL_URL);
    connection = DriverManager.getConnection(URL);
    execute("CREATE SCHEMA " + schema);
    connection.setSchema(schema);
    execute(
        "CREATE TABLE writing_expression (id BIGINT PRIMARY KEY, expression_source TEXT, "
            + "target_locale TEXT, base_locale TEXT, practice_examples_payload JSONB, "
            + "updated_at TIMESTAMPTZ DEFAULT '2026-01-01T00:00:00Z', untouched_metadata JSONB)");
    manifest = Lan584EnglishQuizMigrationTests.readManifest();
    insertFixture();
    Files.writeString(
        migrations.resolve(Lan584EnglishQuizMigrationTests.MIGRATION),
        Lan584EnglishQuizMigrationTests.readSql());
    flyway =
        Flyway.configure()
            .dataSource(URL, "lan584_test", "")
            .schemas(schema)
            .defaultSchema(schema)
            .locations("filesystem:" + migrations)
            .baselineOnMigrate(true)
            .baselineVersion("138")
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

  @DisplayName("전체 4,000개와 범위 밖 표현에서 두 영어 필드 외 모든 값을 보존한다.")
  @Test
  void migratesOnlyReviewedEnglishFieldsAndSkipsAppliedVersion() throws Exception {
    Map<Long, ObjectNode> expected = snapshot();
    for (JsonNode patch : manifest) {
      ObjectNode example =
          (ObjectNode)
              expected
                  .get(patch.path("expressionId").asLong())
                  .path("practice_examples_payload")
                  .get(patch.path("exampleNumber").asInt() - 1);
      example.set("sentenceWords", patch.path("sentenceWords"));
      example.set("sentenceWordChoices", patch.path("sentenceWordChoices"));
    }
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertThat(snapshot()).hasSize(4002).isEqualTo(expected);
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(snapshot()).isEqualTo(expected);
  }

  @DisplayName("원본·출처·언어·예문 순서가 달라지면 아무 데이터도 바꾸지 않는다.")
  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing",
        "words",
        "choiceOrder",
        "choiceCount",
        "question",
        "questionTranslation",
        "translation",
        "text",
        "source",
        "locale",
        "baseLocale",
        "exampleOrder",
        "malformed",
        "exampleCount"
      })
  void rejectsDriftAndRollsBack(String fault) throws Exception {
    execute(sourceFault(fault));
    Map<Long, ObjectNode> before = snapshot();
    assertThatThrownBy(() -> flyway.migrate())
        .isInstanceOf(FlywayException.class)
        .hasStackTraceContaining("LAN-584 source precondition failed");
    assertRolledBack(before);
  }

  @DisplayName("갱신 중 칩·한국어·이미지·메타데이터가 변조되면 전체 적용을 롤백한다.")
  @ParameterizedTest
  @ValueSource(
      strings = {
        "sentenceWords",
        "sentenceWordChoices",
        "sentenceTranslateWords",
        "sentenceTranslateAcceptedAnswers",
        "imageUrl",
        "updated_at"
      })
  void rejectsPostconditionCorruptionAndRollsBack(String field) throws Exception {
    String mutation =
        field.equals("updated_at")
            ? "NEW.updated_at = CURRENT_TIMESTAMP;"
            : "NEW.practice_examples_payload = jsonb_set(NEW.practice_examples_payload, '{0,"
                + field
                + "}', '[\"corrupted\"]');";
    execute(
        "CREATE FUNCTION corrupt() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN "
            + "IF NEW.id = 4 THEN "
            + mutation
            + " END IF; RETURN NEW; END $$");
    execute(
        "CREATE TRIGGER corrupt BEFORE UPDATE ON writing_expression "
            + "FOR EACH ROW EXECUTE FUNCTION corrupt()");
    Map<Long, ObjectNode> before = snapshot();
    assertThatThrownBy(() -> flyway.migrate())
        .isInstanceOf(FlywayException.class)
        .hasStackTraceContaining("LAN-584 postcondition failed");
    assertRolledBack(before);
  }

  private void insertFixture() throws Exception {
    Map<Long, ObjectNode> rows = fixtureRows();
    connection.setAutoCommit(false);
    try (var insert =
        connection.prepareStatement(
            "INSERT INTO writing_expression "
                + "(id, expression_source, target_locale, base_locale, "
                + "practice_examples_payload, untouched_metadata) "
                + "VALUES (?, ?, ?, ?, ?::jsonb, ?::jsonb)")) {
      for (var entry : rows.entrySet()) {
        ObjectNode row = entry.getValue();
        insert.setLong(1, entry.getKey());
        insert.setString(2, row.path("expression_source").asText());
        insert.setString(3, row.path("target_locale").asText());
        insert.setString(4, row.path("base_locale").asText());
        insert.setString(5, row.path("practice_examples_payload").toString());
        ObjectNode metadata = row.deepCopy();
        metadata.remove("practice_examples_payload");
        insert.setString(6, metadata.toString());
        insert.addBatch();
      }
      insert.executeBatch();
    }
    connection.commit();
    connection.setAutoCommit(true);
  }

  private Map<Long, ObjectNode> fixtureRows() throws Exception {
    Map<Long, ObjectNode> rows = new HashMap<>();
    String snapshotPath = System.getenv("LAN584_TEST_SNAPSHOT");
    if (snapshotPath != null) {
      for (JsonNode row :
          Lan584EnglishQuizMigrationTests.MAPPER.readTree(
              Files.readString(Path.of(snapshotPath)))) {
        rows.put(row.path("id").asLong(), (ObjectNode) row);
      }
      assertThat(rows).hasSize(4000);
    } else {
      for (long id = 1; id <= 4000; id++) {
        rows.put(id, dummyRow(id));
      }
      for (JsonNode patch : manifest) {
        ObjectNode row = rows.get(patch.path("expressionId").asLong());
        row.put("expression_source", patch.path("expressionSource").asText());
        ObjectNode example =
            (ObjectNode)
                row.path("practice_examples_payload").get(patch.path("exampleNumber").asInt() - 1);
        example.setAll((ObjectNode) patch.path("expected"));
      }
    }
    rows.put(4001L, dummyRow(4001));
    rows.put(4329L, dummyRow(4329));
    return rows;
  }

  private ObjectNode dummyRow(long id) {
    ObjectNode row = Lan584EnglishQuizMigrationTests.MAPPER.createObjectNode();
    row.put("id", id)
        .put("expression_source", "FREE_TALK")
        .put("target_locale", "EN")
        .put("base_locale", "KR")
        .put("representative_sentence_text", "keep representative " + id);
    ArrayNode examples = row.putArray("practice_examples_payload");
    for (int number = 1; number <= 4; number++) {
      ObjectNode example = examples.addObject();
      example.put("imageUrl", "https://fixture.invalid/" + id + "/" + number);
      example.put("unknownFutureField", "keep " + number);
      example.putArray("sentenceWords").add("Keep").add("this").add("sentence");
      example.putArray("sentenceWordChoices").add("this").add("sentence").add("Keep").add("extra");
      example.putArray("sentenceTranslateWords").add("한국어").add("유지");
      example.putArray("sentenceTranslateAcceptedAnswers").addArray().add("한국어").add("유지");
    }
    return row;
  }

  private String sourceFault(String fault) {
    String prefix = "UPDATE writing_expression SET ";
    String mutation =
        switch (fault) {
          case "missing" -> null;
          case "source" -> "expression_source = 'OTHER'";
          case "locale" -> "target_locale = 'JP'";
          case "baseLocale" -> "base_locale = 'EN'";
          case "exampleOrder" ->
              "practice_examples_payload = jsonb_build_array("
                  + "practice_examples_payload->1, practice_examples_payload->0, "
                  + "practice_examples_payload->2, practice_examples_payload->3)";
          case "malformed" -> "practice_examples_payload = '{}'::jsonb";
          case "exampleCount" ->
              "practice_examples_payload = practice_examples_payload || '[{}]'::jsonb";
          case "choiceOrder" ->
              "practice_examples_payload = jsonb_set(practice_examples_payload, "
                  + "'{0,sentenceWordChoices}', (SELECT jsonb_agg(value ORDER BY ord DESC) FROM "
                  + "jsonb_array_elements(practice_examples_payload->0->'sentenceWordChoices') "
                  + "WITH ORDINALITY x(value, ord)))";
          case "choiceCount" ->
              "practice_examples_payload = jsonb_set(practice_examples_payload, "
                  + "'{0,sentenceWordChoices}', "
                  + "(practice_examples_payload->0->'sentenceWordChoices') || '[\"extra\"]')";
          default -> alteredExampleField(fault);
        };
    return mutation == null
        ? "DELETE FROM writing_expression WHERE id = 4"
        : prefix + mutation + " WHERE id = 4";
  }

  private String alteredExampleField(String fault) {
    String field =
        switch (fault) {
          case "words" -> "sentenceWords";
          case "question" -> "practiceQuestion";
          case "questionTranslation" -> "practiceQuestionTranslation";
          case "translation" -> "sentenceTranslation";
          case "text" -> "sentenceText";
          default -> throw new IllegalArgumentException(fault);
        };
    return "practice_examples_payload = jsonb_set(practice_examples_payload, '{0,"
        + field
        + "}', '\"changed\"')";
  }

  private void assertRolledBack(Map<Long, ObjectNode> before) throws Exception {
    assertThat(snapshot()).isEqualTo(before);
    try (var statement = connection.createStatement();
        var result =
            statement.executeQuery(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '139'")) {
      result.next();
      assertThat(result.getInt(1)).isZero();
    }
  }

  private Map<Long, ObjectNode> snapshot() throws Exception {
    Map<Long, ObjectNode> rows = new HashMap<>();
    try (var statement = connection.createStatement();
        var result =
            statement.executeQuery(
                "SELECT id, to_jsonb(e)::text FROM writing_expression e ORDER BY id")) {
      while (result.next()) {
        rows.put(
            result.getLong(1),
            (ObjectNode) Lan584EnglishQuizMigrationTests.MAPPER.readTree(result.getString(2)));
      }
    }
    return rows;
  }

  private void execute(String sql) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }
}
